@echo off
echo Building L2Tool...
echo.

gradlew.bat build

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo JAR file location: build\libs\l2tool.jar
) else (
    echo.
    echo Build failed!
    exit /b %ERRORLEVEL%
)

