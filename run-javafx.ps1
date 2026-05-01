# AuCLI Desktop Application Launcher (PowerShell)
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AuCLI Desktop Application Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check Maven dependencies
Write-Host "Step 1: Checking Maven dependencies..." -ForegroundColor Yellow
$mavenResult = & mvn dependency:resolve 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Maven dependencies failed. Please run: mvn clean install" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Step 2: Get JavaFX module path
$mavenRepo = "$env:USERPROFILE\.m2\repository"
$javafxBase = "$mavenRepo\org\openjfx"

if (-not (Test-Path $javafxBase)) {
    Write-Host "ERROR: JavaFX dependencies not found in Maven repository." -ForegroundColor Red
    Write-Host "Please run: mvn clean install" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Find JavaFX JAR files
$javafxControls = Get-ChildItem -Path "$javafxBase\javafx-controls" -Recurse -Filter "*.jar" | Select-Object -First 1
$javafxBaseJar = Get-ChildItem -Path "$javafxBase\javafx-base" -Recurse -Filter "*.jar" | Select-Object -First 1
$javafxGraphics = Get-ChildItem -Path "$javafxBase\javafx-graphics" -Recurse -Filter "*.jar" | Select-Object -First 1
$javafxFxml = Get-ChildItem -Path "$javafxBase\javafx-fxml" -Recurse -Filter "*.jar" | Select-Object -First 1

if (-not $javafxControls -or -not $javafxBaseJar -or -not $javafxGraphics) {
    Write-Host "ERROR: Could not find JavaFX JAR files." -ForegroundColor Red
    Write-Host "Please run: mvn clean install" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Build module path
$modulePath = "$($javafxBaseJar.DirectoryName);$($javafxControls.DirectoryName);$($javafxGraphics.DirectoryName)"
if ($javafxFxml) {
    $modulePath += ";$($javafxFxml.DirectoryName)"
}

# Step 3: Compile project
Write-Host "Step 2: Compiling project..." -ForegroundColor Yellow
$compileResult = & mvn clean compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilation failed." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Step 4: Copy dependencies
Write-Host "Step 3: Preparing dependencies..." -ForegroundColor Yellow
& mvn dependency:copy-dependencies -q -DoutputDirectory=target\dependency | Out-Null

# Build classpath
$classpath = "target\classes"
if (Test-Path "target\dependency") {
    $deps = Get-ChildItem -Path "target\dependency\*.jar" | ForEach-Object { $_.FullName }
    $classpath += ";" + ($deps -join ";")
}

# Step 5: Start Spring Boot in background
Write-Host "Step 4: Starting Spring Boot backend..." -ForegroundColor Yellow
$springBootJob = Start-Job -ScriptBlock {
    Set-Location $using:PWD
    & mvn spring-boot:run | Out-Null
}

# Step 6: Wait for Spring Boot
Write-Host "Step 5: Waiting for Spring Boot to start..." -ForegroundColor Yellow
$waited = 0
$maxWait = 30
$ready = $false

while ($waited -lt $maxWait -and -not $ready) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/" -TimeoutSec 1 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $ready = $true
            Write-Host "Spring Boot backend is ready!" -ForegroundColor Green
        }
    } catch {
        # Server not ready yet
    }
    
    if (-not $ready) {
        Start-Sleep -Seconds 1
        $waited++
        if ($waited % 5 -eq 0) {
            Write-Host "Waiting... ($waited s)" -ForegroundColor Gray
        }
    }
}

if (-not $ready) {
    Write-Host "Assuming Spring Boot is ready. Launching UI..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}

# Step 7: Launch JavaFX UI
Write-Host "Step 6: Launching JavaFX UI..." -ForegroundColor Yellow
Write-Host ""

$javaArgs = @(
    "--module-path", $modulePath
    "--add-modules", "javafx.controls,javafx.fxml"
    "-cp", $classpath
    "com.mcp.host.mcp_host.DesktopAppUI"
)

try {
    & java $javaArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERROR: Failed to launch JavaFX UI." -ForegroundColor Red
        Write-Host ""
        Write-Host "Troubleshooting:" -ForegroundColor Yellow
        Write-Host "1. Make sure Java 17+ is installed and in PATH"
        Write-Host "2. Run: mvn clean install"
        Write-Host "3. Check that JavaFX dependencies are downloaded"
        Write-Host ""
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Make sure Java is installed and in your PATH." -ForegroundColor Yellow
}

# Cleanup
if ($springBootJob) {
    Write-Host ""
    Write-Host "Stopping Spring Boot backend..." -ForegroundColor Yellow
    Stop-Job $springBootJob -ErrorAction SilentlyContinue
    Remove-Job $springBootJob -ErrorAction SilentlyContinue
}

Write-Host ""
Read-Host "Press Enter to exit"

