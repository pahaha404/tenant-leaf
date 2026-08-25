[CmdletBinding()]
param(
    [string]$CloudflaredPath = 'cloudflared',
    [switch]$RestartApi
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot 'server/infra/docker/compose.yml'
$envFile = Join-Path $repositoryRoot '.env'
$stateDirectory = Join-Path $env:TEMP 'tenant-leaf-mobile-data'
$stateFile = Join-Path $stateDirectory 'tunnels.json'

if (-not (Test-Path $envFile)) {
    throw '.env 파일이 없습니다. 로컬 DB·MinIO·Gemini 설정을 먼저 준비하세요.'
}

$cloudflaredCommand = Get-Command $CloudflaredPath -ErrorAction SilentlyContinue
if (-not $cloudflaredCommand) {
    throw 'cloudflared가 없습니다. `winget install --id Cloudflare.cloudflared --exact`로 설치한 뒤 다시 실행하세요.'
}

# .env 값은 출력하지 않고 이 PowerShell과 자식 프로세스에만 전달한다.
Get-Content -Encoding UTF8 $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)=(.*)$') {
        Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
    }
}

$javaHomes = @(
    $env:JAVA_HOME,
    (Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'),
    (Join-Path ${env:ProgramFiles(x86)} 'Android/Android Studio/jbr')
) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
$javaHome = $javaHomes | Where-Object { Test-Path (Join-Path $_ 'bin/java.exe') } | Select-Object -First 1
if (-not $javaHome) {
    throw 'JDK 21을 찾지 못했습니다. Android Studio 또는 JDK 21을 설치하세요.'
}
$env:JAVA_HOME = $javaHome

function Get-ListeningProcessId([int]$port) {
    return Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        Select-Object -First 1
}

function Wait-HttpOk([string]$url, [int]$timeoutSeconds = 90) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) { return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "시간 안에 준비되지 않았습니다: $url"
}

function Start-QuickTunnel([string]$name, [string]$localUrl) {
    $outLog = Join-Path $stateDirectory "$name.out.log"
    $errLog = Join-Path $stateDirectory "$name.err.log"
    Remove-Item -Force $outLog, $errLog -ErrorAction SilentlyContinue
    $process = Start-Process -FilePath $cloudflaredCommand.Source `
        -ArgumentList @('tunnel', '--url', $localUrl, '--no-autoupdate') `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    $deadline = (Get-Date).AddSeconds(45)
    do {
        $urls = @($outLog, $errLog) |
            Where-Object { Test-Path $_ } |
            ForEach-Object { Select-String -Path $_ -Pattern 'https://[-a-z0-9]+\.trycloudflare\.com' -AllMatches } |
            ForEach-Object { $_.Matches.Value } |
            Select-Object -Unique
        if ($urls.Count -gt 0) {
            return [PSCustomObject]@{ Name = $name; Url = $urls[0]; ProcessId = $process.Id }
        }
        if ($process.HasExited) { break }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    throw "$name 터널 주소를 만들지 못했습니다. 로그: $errLog"
}

New-Item -ItemType Directory -Force -Path $stateDirectory | Out-Null

Write-Host '[1/5] PostgreSQL과 MinIO를 확인합니다.'
& docker compose --env-file $envFile -f $composeFile up -d
Wait-HttpOk 'http://127.0.0.1:9000/minio/health/live'

Write-Host '[2/5] MinIO 임시 HTTPS 터널을 만듭니다.'
$minioTunnel = Start-QuickTunnel 'minio' 'http://127.0.0.1:9000'

$apiPortOwner = Get-ListeningProcessId 8080
if ($apiPortOwner -and -not $RestartApi) {
    throw '기존 API가 8080에서 실행 중입니다. 이 서버는 LAN용 MinIO 주소를 쓰고 있을 수 있으니 `-RestartApi`를 붙여 다시 실행하세요.'
}
if ($apiPortOwner) {
    Write-Host '[3/5] 기존 API를 모바일 데이터용 설정으로 교체합니다.'
    Stop-Process -Id $apiPortOwner -Force
    Start-Sleep -Seconds 1
}

$env:OBJECT_STORAGE_ENDPOINT = 'http://127.0.0.1:9000'
$env:OBJECT_STORAGE_PUBLIC_ENDPOINT = $minioTunnel.Url
$apiLogDirectory = Join-Path $env:TEMP 'tenant-leaf-api'
New-Item -ItemType Directory -Force -Path $apiLogDirectory | Out-Null
Start-Process -FilePath (Join-Path $repositoryRoot 'server/api/gradlew.bat') `
    -WorkingDirectory (Join-Path $repositoryRoot 'server/api') `
    -ArgumentList 'bootRun --no-daemon' `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $apiLogDirectory 'api.out.log') `
    -RedirectStandardError (Join-Path $apiLogDirectory 'api.err.log') | Out-Null
Wait-HttpOk 'http://127.0.0.1:8080/actuator/health'

Write-Host '[4/5] API 임시 HTTPS 터널을 만듭니다.'
$apiTunnel = Start-QuickTunnel 'api' 'http://127.0.0.1:8080'
Wait-HttpOk "$($apiTunnel.Url)/actuator/health"

$state = [PSCustomObject]@{
    createdAt = (Get-Date).ToString('o')
    apiUrl = "$($apiTunnel.Url)/api/v1/"
    minioUrl = $minioTunnel.Url
    apiTunnelProcessId = $apiTunnel.ProcessId
    minioTunnelProcessId = $minioTunnel.ProcessId
}
$state | ConvertTo-Json | Set-Content -Encoding UTF8 $stateFile

Write-Host '[5/5] 모바일 데이터 테스트 준비 완료'
Write-Host "Android API 주소: $($state.apiUrl)"
Write-Host "MinIO 공개 주소: $($state.minioUrl)"
Write-Host '이 창을 닫아도 터널은 남습니다. 테스트가 끝나면 scripts/stop-mobile-data-tunnel.ps1을 실행하세요.'
Write-Host '실제 주소·음성·개인 사진은 사용하지 말고, 테스트 데이터만 사용하세요.'
