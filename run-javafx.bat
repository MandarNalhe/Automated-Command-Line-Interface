@echo off
echo ========================================
echo AuCLI Desktop Application Launcher
echo ========================================
echo.

REM First, ensure Maven dependencies are downloaded
echo Step 1: Checking Maven dependencies...
call mvn dependency:resolve >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven dependencies failed. Please run: mvn clean install
    pause
    exit /b 1
)

REM Get the JavaFX module path from Maven dependencies
set MAVEN_REPO=%USERPROFILE%\.m2\repository
set JAVAFX_BASE=%MAVEN_REPO%\org\openjfx

REM Check if JavaFX is in Maven repo
if not exist "%JAVAFX_BASE%" (
    echo ERROR: JavaFX dependencies not found in Maven repository.
    echo Please run: mvn clean install
    pause
    exit /b 1
)

REM Find JavaFX JAR files (handle different versions)
for /f "delims=" %%i in ('dir /b /s "%JAVAFX_BASE%\javafx-controls\*.jar" 2^>nul') do set JAVAFX_CONTROLS=%%i
for /f "delims=" %%i in ('dir /b /s "%JAVAFX_BASE%\javafx-base\*.jar" 2^>nul') do set JAVAFX_BASE_JAR=%%i
for /f "delims=" %%i in ('dir /b /s "%JAVAFX_BASE%\javafx-graphics\*.jar" 2^>nul') do set JAVAFX_GRAPHICS=%%i
for /f "delims=" %%i in ('dir /b /s "%JAVAFX_BASE%\javafx-fxml\*.jar" 2^>nul') do set JAVAFX_FXML=%%i

REM Build the module path
set MODULE_PATH=%~dp0%JAVAFX_BASE_JAR%;%~dp0%JAVAFX_CONTROLS%;%~dp0%JAVAFX_GRAPHICS%;%~dp0%JAVAFX_FXML%

REM Compile the project first
echo Step 2: Compiling project...
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Compilation failed.
    pause
    exit /b 1
)

REM Copy dependencies
echo Step 3: Preparing dependencies...
call mvn dependency:copy-dependencies -q -DoutputDirectory=target\dependency
if errorlevel 1 (
    echo WARNING: Could not copy dependencies. Continuing anyway...
)

REM Build classpath
set CLASSPATH=target\classes
for %%f in (target\dependency\*.jar) do set CLASSPATH=!CLASSPATH!;%%f

REM First, start Spring Boot in background
echo Step 4: Starting Spring Boot backend...
start "Spring Boot Backend" /min cmd /c "mvn spring-boot:run"

REM Wait for Spring Boot to start
echo Step 5: Waiting for Spring Boot to start...
timeout /t 15 /nobreak >nul

REM Now launch JavaFX UI
echo Step 6: Launching JavaFX UI...
echo.
java --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -cp "%CLASSPATH%" com.mcp.host.mcp_host.DesktopAppUI

if errorlevel 1 (
    echo.
    echo ERROR: Failed to launch JavaFX UI.
    echo.
    echo Troubleshooting:
    echo 1. Make sure Java 17+ is installed and in PATH
    echo 2. Run: mvn clean install
    echo 3. Check that JavaFX dependencies are downloaded
    echo.
    pause
    exit /b 1
)

pause

