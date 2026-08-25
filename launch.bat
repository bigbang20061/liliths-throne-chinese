@echo off
setlocal
title Liliths Throne
cd /d "%~dp0"

set "JAVA_EXE=D:\Program Files\Java\jdk-17.0.18\bin\java.exe"
if not exist "%JAVA_EXE%" goto :nojava

cd /d "%~dp0target"
if errorlevel 1 goto :nogame
cd Lilith*
if errorlevel 1 goto :nogame

set "JAR="
for %%F in (*.jar) do set "JAR=%%F"
if not defined JAR goto :nojar

echo Starting game...
echo Folder: %CD%
echo.

"%JAVA_EXE%" -jar "%JAR%"
if errorlevel 1 goto :failed
exit /b 0

:nojava
echo [ERROR] JDK not found:
echo %JAVA_EXE%
goto :hold

:nogame
echo [ERROR] Game folder not found. Compile first: mvn package
goto :hold

:nojar
echo [ERROR] jar not found
echo %CD%
goto :hold

:failed
echo.
echo [ERROR] Game exited with an error.
echo Check data\error.log
goto :hold

:hold
echo.
pause
exit /b 1
