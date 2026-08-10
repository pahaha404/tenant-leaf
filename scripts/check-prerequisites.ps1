$requiredTools = @("git", "java", "python", "docker")

foreach ($tool in $requiredTools) {
    $command = Get-Command $tool -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        Write-Host "[MISSING] $tool" -ForegroundColor Yellow
        continue
    }

    $version = switch ($tool) {
        "java" { (& java -version 2>&1 | Select-Object -First 1) }
        default { (& $tool --version 2>&1 | Select-Object -First 1) }
    }

    Write-Host "[OK] $tool - $version" -ForegroundColor Green
}

Write-Host "`nAndroid 담당자는 Android Studio와 JDK 17을, 안경 담당자는 Meta SDK와 Mock Device Kit 또는 실제 기기를 추가로 준비하세요."
