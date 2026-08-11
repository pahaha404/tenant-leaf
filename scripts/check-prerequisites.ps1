$requiredJavaMajor = 21
$hasRequiredToolError = $false

# 설치 직후 열려 있던 PowerShell에서도 최신 Windows PATH를 사용한다.
$machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$currentProcessPath = $env:Path
$env:Path = @($machinePath, $userPath, $currentProcessPath) -join ";"

function Write-ToolVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        Write-Host "[MISSING] $Name" -ForegroundColor Yellow
        return $false
    }

    $versionOutput = & $Name --version 2>&1
    $versionExitCode = $LASTEXITCODE
    $version = $versionOutput | Select-Object -First 1
    if ($versionExitCode -ne 0) {
        Write-Host "[MISSING] $Name - 명령을 실행할 수 없습니다." -ForegroundColor Yellow
        return $false
    }

    Write-Host "[OK] $Name - $version" -ForegroundColor Green
    return $true
}

Write-ToolVersion -Name "git" | Out-Null
Write-ToolVersion -Name "python" | Out-Null

$javaCommand = Get-Command "java" -ErrorAction SilentlyContinue
$javacCommand = Get-Command "javac" -ErrorAction SilentlyContinue

if ($null -eq $javaCommand -or $null -eq $javacCommand) {
    Write-Host "[ERROR] JDK $requiredJavaMajor 설치를 찾을 수 없습니다. java와 javac가 모두 PATH에 있어야 합니다." -ForegroundColor Red
    $hasRequiredToolError = $true
}
else {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    $javacVersion = & javac -version 2>&1 | Select-Object -First 1

    $javaMajor = if ($javaVersion -match 'version "(?<major>\d+)') {
        [int]$Matches.major
    }
    else {
        $null
    }

    $javacMajor = if ($javacVersion -match 'javac (?<major>\d+)') {
        [int]$Matches.major
    }
    else {
        $null
    }

    if ($javaMajor -ne $requiredJavaMajor -or $javacMajor -ne $requiredJavaMajor) {
        Write-Host "[ERROR] JDK $requiredJavaMajor이 필요합니다. java=$javaVersion, javac=$javacVersion" -ForegroundColor Red
        $hasRequiredToolError = $true
    }
    else {
        Write-Host "[OK] java - $javaVersion" -ForegroundColor Green
        Write-Host "[OK] javac - $javacVersion" -ForegroundColor Green
    }
}

$dockerCommand = Get-Command "docker" -ErrorAction SilentlyContinue
if ($null -eq $dockerCommand) {
    Write-Host "[ERROR] Docker CLI를 찾을 수 없습니다." -ForegroundColor Red
    $hasRequiredToolError = $true
}
else {
    $dockerVersion = & docker --version 2>&1 | Select-Object -First 1
    Write-Host "[OK] docker - $dockerVersion" -ForegroundColor Green

    $dockerServerVersion = & docker info --format '{{.ServerVersion}}' 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Docker Desktop engine - $dockerServerVersion" -ForegroundColor Green
    }
    else {
        Write-Host "[NOTICE] Docker Desktop은 설치되어 있지만 engine이 실행 중인지 확인하세요." -ForegroundColor Yellow
    }
}

Write-Host "`nAndroid 담당자는 Android Studio와 JDK 21을, 안경 담당자는 Meta SDK와 Mock Device Kit 또는 실제 기기를 추가로 준비하세요."

if ($hasRequiredToolError) {
    exit 1
}

exit 0
