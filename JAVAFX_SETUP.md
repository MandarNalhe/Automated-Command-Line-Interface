# JavaFX Setup Instructions

## Problem
JavaFX runtime components are missing because JavaFX is not part of the standard JDK since Java 11. You need to add JavaFX to the module path when running the application.

## Solution Options

### Option 1: Use the Batch Script (Easiest)
Simply run:
```batch
run-javafx.bat
```

This script will:
1. Download Maven dependencies
2. Compile the project
3. Start Spring Boot backend
4. Launch JavaFX UI with correct module path

### Option 2: Manual Run with Maven

**Step 1:** Start Spring Boot backend in one terminal:
```batch
mvn spring-boot:run
```

**Step 2:** Wait for Spring Boot to start (about 10-15 seconds), then in another terminal run:
```batch
mvn exec:java -Dexec.mainClass="com.mcp.host.mcp_host.DesktopAppUI" -Dexec.args="--module-path %USERPROFILE%\.m2\repository\org\openjfx --add-modules javafx.controls,javafx.fxml"
```

### Option 3: Run with Java Directly

**Step 1:** Build the project:
```batch
mvn clean package
mvn dependency:copy-dependencies
```

**Step 2:** Find your JavaFX JARs location (usually in `%USERPROFILE%\.m2\repository\org\openjfx\`)

**Step 3:** Run with module path:
```batch
java --module-path "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\17.0.2;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\17.0.2;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\17.0.2;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\17.0.2" --add-modules javafx.controls,javafx.fxml -cp "target/classes;target/dependency/*" com.mcp.host.mcp_host.DesktopAppUI
```

### Option 4: IDE Configuration (IntelliJ IDEA / Eclipse)

**IntelliJ IDEA:**
1. Go to Run → Edit Configurations
2. Add VM options:
   ```
   --module-path <path-to-javafx-lib> --add-modules javafx.controls,javafx.fxml
   ```
3. Replace `<path-to-javafx-lib>` with your Maven repository path:
   ```
   C:\Users\<YourUsername>\.m2\repository\org\openjfx
   ```

**Eclipse:**
1. Right-click project → Run As → Run Configurations
2. Go to Arguments tab
3. Add VM arguments:
   ```
   --module-path <path-to-javafx-lib> --add-modules javafx.controls,javafx.fxml
   ```

## Quick Fix
If you just want to test quickly, the batch script `run-javafx.bat` should handle everything automatically.

