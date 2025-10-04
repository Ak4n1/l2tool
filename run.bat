@echo off
echo Configurando JavaFX...
if not exist "javafx" mkdir javafx
cd javafx

if not exist "javafx-sdk-17.0.2" (
    echo Extrayendo JavaFX desde archivo local...
    powershell -Command "Expand-Archive -Path 'javafx-17.0.2.zip' -DestinationPath '.' -Force"
)

cd ..
echo Ejecutando L2Tool...
java --module-path "javafx\javafx-sdk-17.0.2\lib" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics,javafx.swing -jar build\libs\l2tool.jar

pause
