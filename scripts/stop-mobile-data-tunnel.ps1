$ErrorActionPreference = 'Stop'
$stateFile = Join-Path (Join-Path $env:TEMP 'tenant-leaf-mobile-data') 'tunnels.json'
if (-not (Test-Path $stateFile)) {
    Write-Host '중지할 모바일 데이터 터널 정보가 없습니다.'
    exit 0
}

$state = Get-Content -Raw -Encoding UTF8 $stateFile | ConvertFrom-Json
foreach ($processId in @($state.apiTunnelProcessId, $state.minioTunnelProcessId)) {
    if ($processId) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}
Remove-Item -Force $stateFile
Write-Host 'API·MinIO 임시 터널을 중지했습니다. 로컬 DB와 MinIO 데이터는 삭제하지 않았습니다.'
