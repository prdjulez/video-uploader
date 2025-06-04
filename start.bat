@echo off
echo === Projekt wird kompiliert ===
javac -d bin src\youtubeconverter\*.java
if errorlevel 1 (
    echo FEHLER: Kompilierung fehlgeschlagen.
    pause
    exit /b
)

echo === JAR-Datei wird erstellt ===
jar cfm VideoMaker.jar manifest.txt -C bin .
if errorlevel 1 (
    echo FEHLER: JAR-Erstellung fehlgeschlagen.
    pause
    exit /b
)

echo === Starte Anwendung ===
java -jar VideoMaker.jar
pause