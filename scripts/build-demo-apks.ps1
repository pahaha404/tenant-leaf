[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$LanAddress,
    [ValidateSet('judge-a', 'judge-b', 'judge-c', 'judge-d')]
    [string[]]$Users = @('judge-a', 'judge-b', 'judge-c', 'judge-d')
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $repositoryRoot 'android'
$outputDirectory = Join-Path $repositoryRoot 'artifacts/demo-apks'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

foreach ($user in $Users) {
    Write-Host "${user} APK를 빌드합니다."
    Push-Location $androidRoot
    try {
        & .\gradlew.bat :app:assembleDebug --no-daemon `
            "-PTENANT_LEAF_DEBUG_API_BASE_URL=http://${LanAddress}:8080/api/v1/" `
            "-PTENANT_LEAF_DEMO_USER=$user"
    } finally {
        Pop-Location
    }
    if ($LASTEXITCODE -ne 0) { throw "${user} APK 빌드에 실패했습니다." }

    Copy-Item (Join-Path $androidRoot 'app/build/outputs/apk/debug/app-debug.apk') `
        (Join-Path $outputDirectory "tenant-leaf-${user}.apk") -Force
}

Write-Host "완료: $outputDirectory"
Write-Host '각 APK는 같은 서버를 쓰지만 X-Demo-User 값이 달라 매물·임장·리포트가 분리됩니다.'
