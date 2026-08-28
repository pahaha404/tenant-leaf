[CmdletBinding()]
param(
    [string]$LanAddress,
    [switch]$RestartApi,
    [switch]$RestartWorker
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot 'server/infra/docker/compose.yml'
$envFile = Join-Path $repositoryRoot '.env'

$javaHomes = @(
    $env:JAVA_HOME,
    (Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'),
    (Join-Path ${env:ProgramFiles(x86)} 'Android/Android Studio/jbr')
) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
$javaHome = $javaHomes | Where-Object { Test-Path (Join-Path $_ 'bin/java.exe') } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    throw 'JDK 21을 찾지 못했습니다. Android Studio 또는 JDK 21을 설치한 뒤 JAVA_HOME을 설정하세요.'
}
$env:JAVA_HOME = $javaHome

if (-not (Test-Path $envFile)) {
    throw '.env 파일이 없습니다. .env.example을 복사하고 DB/MinIO/Gemini 설정을 먼저 준비하세요.'
}

# .env는 Git에 올리지 않는 로컬 비밀값이다. 이 스크립트는 값을 출력하지 않고 자식 프로세스에만 전달한다.
Get-Content -Encoding UTF8 $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)=(.*)$') {
        Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
    }
}

if ([string]::IsNullOrWhiteSpace($LanAddress)) {
    # Hyper-V/WSL 가상 어댑터(172.*)보다 실제 게이트웨이가 있는 Wi-Fi/이더넷 주소를 우선한다.
    $LanAddress = Get-NetIPConfiguration |
        Where-Object {
            $_.IPv4DefaultGateway -and
            $_.IPv4Address.IPAddress -notlike '127.*' -and
            $_.IPv4Address.IPAddress -notlike '169.254.*'
        } |
        ForEach-Object { $_.IPv4Address.IPAddress } |
        Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($LanAddress)) {
    throw 'Wi-Fi 또는 유선 네트워크 IPv4를 찾지 못했습니다. -LanAddress 192.168.x.x 형태로 직접 지정하세요.'
}

$env:OBJECT_STORAGE_ENDPOINT = 'http://localhost:9000'
$env:OBJECT_STORAGE_PUBLIC_ENDPOINT = "http://${LanAddress}:9000"

Write-Host "[1/4] PostgreSQL과 MinIO를 시작합니다."
& docker compose --env-file $envFile -f $composeFile up -d

Write-Host "[2/4] 컨테이너 상태를 확인합니다."
& docker compose --env-file $envFile -f $composeFile ps

function Test-LocalPort([int]$Port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

if ($RestartApi -and (Test-LocalPort 8080)) {
    Get-NetTCPConnection -State Listen -LocalPort 8080 | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }
    Start-Sleep -Seconds 1
}
if (-not (Test-LocalPort 8080)) {
    Write-Host "[3/4] API 서버를 백그라운드에서 시작합니다."
    $apiLogDirectory = Join-Path $env:TEMP 'tenant-leaf-api'
    New-Item -ItemType Directory -Force -Path $apiLogDirectory | Out-Null
    Start-Process -FilePath (Join-Path $repositoryRoot 'server/api/gradlew.bat') `
        -WorkingDirectory (Join-Path $repositoryRoot 'server/api') `
        -ArgumentList 'bootRun --no-daemon' `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $apiLogDirectory 'api.out.log') `
        -RedirectStandardError (Join-Path $apiLogDirectory 'api.err.log') | Out-Null
} else {
    Write-Host "[3/4] API 서버는 이미 8080 포트에서 실행 중입니다."
}

if ($RestartWorker) {
    Get-CimInstance Win32_Process -Filter "Name = 'python.exe'" | Where-Object { $_.CommandLine -like '*tenant_leaf_worker.worker*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
}
if (-not (Get-CimInstance Win32_Process -Filter "Name = 'python.exe'" | Where-Object { $_.CommandLine -like '*tenant_leaf_worker.worker*' })) {
    $python = Join-Path $repositoryRoot 'server/ai-worker/.venv/Scripts/python.exe'
    if (-not (Test-Path $python)) { throw 'AI Worker 가상환경이 없습니다: server/ai-worker/.venv' }
    Write-Host "[4/4] AI Worker를 백그라운드에서 시작합니다."
    $workerLogDirectory = Join-Path $env:TEMP 'tenant-leaf-ai-worker'
    New-Item -ItemType Directory -Force -Path $workerLogDirectory | Out-Null
    Start-Process -FilePath $python `
        -WorkingDirectory (Join-Path $repositoryRoot 'server/ai-worker') `
        -ArgumentList '-m tenant_leaf_worker.worker' `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $workerLogDirectory 'worker.out.log') `
        -RedirectStandardError (Join-Path $workerLogDirectory 'worker.err.log') | Out-Null
} else {
    Write-Host "[4/4] AI Worker는 이미 실행 중입니다."
}

Write-Host ''
Write-Host '발표용 LAN 서버 설정 완료'
Write-Host "Android API 주소: http://${LanAddress}:8080/api/v1/"
Write-Host "MinIO 공개 주소: http://${LanAddress}:9000"
Write-Host 'Windows 방화벽에서 TCP 8080, 9000 인바운드를 허용해야 다른 폰이 연결할 수 있습니다.'
Write-Host '각 폰 APK는 build-demo-apks.ps1로 judge-a~judge-d를 각각 설치하세요.'
