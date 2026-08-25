[CmdletBinding()]
param(
    [ValidateSet('judge-a', 'judge-b', 'judge-c', 'judge-d')]
    [string]$DemoUser = 'judge-a',
    [string]$ApiBaseUrl
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
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

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $stateFile = Join-Path (Join-Path $env:TEMP 'tenant-leaf-mobile-data') 'tunnels.json'
    if (Test-Path $stateFile) {
        $ApiBaseUrl = (Get-Content -Raw -Encoding UTF8 $stateFile | ConvertFrom-Json).apiUrl
    } else {
        $apiLog = Join-Path (Join-Path $env:TEMP 'tenant-leaf-mobile-data') 'api.err.log'
        $ApiBaseUrl = Select-String -Path $apiLog -Pattern 'https://[-a-z0-9]+\.trycloudflare\.com' -AllMatches |
            ForEach-Object { "$($_.Matches.Value)/api/v1/" } |
            Select-Object -First 1
    }
}
if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    throw '모바일 데이터 API 주소를 찾지 못했습니다. start-mobile-data-tunnel.ps1을 먼저 실행하거나 -ApiBaseUrl로 직접 넣으세요.'
}
if (-not $ApiBaseUrl.EndsWith('/')) { $ApiBaseUrl = "$ApiBaseUrl/" }

$androidDirectory = Join-Path $repositoryRoot 'android'
$artifactDirectory = Join-Path $repositoryRoot 'artifacts/mobile-data'
New-Item -ItemType Directory -Force -Path $artifactDirectory | Out-Null

Push-Location $androidDirectory
try {
    & .\gradlew.bat :app:assembleDebug `
        "-PTENANT_LEAF_DEBUG_API_BASE_URL=$ApiBaseUrl" `
        "-PTENANT_LEAF_DEMO_USER=$DemoUser"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

$sourceApk = Join-Path $androidDirectory 'app/build/outputs/apk/debug/app-debug.apk'
$targetApk = Join-Path $artifactDirectory "tenant-leaf-mobile-data-$DemoUser.apk"
Copy-Item -Force $sourceApk $targetApk
Write-Host "생성 완료: $targetApk"
Write-Host "연결 대상: $ApiBaseUrl"
Write-Host "데모 사용자: $DemoUser"
