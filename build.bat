@echo off
setlocal EnableExtensions DisableDelayedExpansion
pushd "%~dp0" || goto :failure
set "PUSHD_DONE=1"
set "MAVEN_BUILD_ARGS=clean package"
if /i "%~1"=="jar" set "MAVEN_BUILD_ARGS=-Dmaven.resources.skip=true -Dmaven.compiler.useIncrementalCompilation=false package"

rem --- 1. Locate Maven ---
set "MVN="
for %%F in (mvn.cmd) do set "MVN=%%~$PATH:F"
if not defined MVN if exist "D:\Program Files\maven\apache-maven-3.9.9\bin\mvn.cmd" set "MVN=D:\Program Files\maven\apache-maven-3.9.9\bin\mvn.cmd"
if not defined MVN if exist "D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd" set "MVN=D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd"
if not defined MVN for /r "D:\Program Files\maven" %%F in (mvn.cmd) do if not defined MVN set "MVN=%%F"
if not defined MVN goto :maven_missing
echo Using Maven: %MVN%

rem --- 2. Ensure JAVA_HOME is usable ---
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto :java_home_ready
set "JAVA_HOME="
for /f "delims=" %%D in ('dir /b /ad /o-n "D:\Program Files\Java\jdk-*" 2^>nul') do if not defined JAVA_HOME set "JAVA_HOME=D:\Program Files\Java\%%D"
if not defined JAVA_HOME for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Java\jdk-*" 2^>nul') do if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\%%D"
if not defined JAVA_HOME goto :jdk_missing
:java_home_ready
echo Using JAVA_HOME: %JAVA_HOME%

rem --- 3. Force UTF-8 to protect Chinese source files ---
set "MAVEN_OPTS=-Dfile.encoding=UTF-8 %MAVEN_OPTS%"

rem --- 4. Read project metadata from pom.xml ---
set "VERSION="
set "PROJECT_NAME="
for /f "tokens=3 delims=<>" %%A in ('findstr /r /c:"<version>[^<]*</version>" pom.xml') do if not defined VERSION set "VERSION=%%A"
for /f "tokens=3 delims=<>" %%A in ('findstr /r /c:"<name>[^<]*</name>" pom.xml') do if not defined PROJECT_NAME set "PROJECT_NAME=%%A"
if not defined VERSION goto :version_missing
if not defined PROJECT_NAME goto :name_missing
echo Building version: %VERSION%

rem --- 5. Back up saves before clean, then restore copies after Maven ---
rem A junction inside target is removed by `mvn clean`; a temporary copy is the only safe source.
set "DIST_DIR=target\%PROJECT_NAME% (win)"
set "DATA_DIR=%DIST_DIR%\data"
set "PERSISTENT_DATA_DIR=%CD%\data"
set "SAVE_BACKUP_DIR=%TEMP%\liliths-throne-save-backup-%RANDOM%%RANDOM%"
set "SAVE_BACKUP_DATA_DIR=%SAVE_BACKUP_DIR%\data"
set "HAS_SAVE_BACKUP=0"

if /i "%~1"=="jar" goto :build
call :backup_saves
if errorlevel 1 goto :backup_failed

:build
call "%MVN%" %MAVEN_BUILD_ARGS%
set "BUILD_EXIT=%ERRORLEVEL%"
call :restore_saves
if errorlevel 1 set "BUILD_EXIT=1"

if "%BUILD_EXIT%"=="0" goto :verify_output
echo Build failed with exit code %BUILD_EXIT%.
set "EXIT_CODE=%BUILD_EXIT%"
goto :failure
rem --- 6. Verify the output ---
:verify_output
set "JAR=%DIST_DIR%\%PROJECT_NAME%-%VERSION%.jar"
if exist "%JAR%" goto :output_found
echo Build output was not found: %JAR%
goto :failure

:output_found
for %%F in ("%JAR%") do set "JAR_SIZE=%%~zF"
echo.
echo Build succeeded: %JAR% ^(%JAR_SIZE% bytes^)
echo Run it from that directory with java -jar and JDK 17+; the res directory is included.
popd
pause
exit /b 0

:maven_missing
echo Maven was not found. Install it or add it to PATH.
goto :failure

:jdk_missing
echo No JDK was found. Install JDK 17+ or set JAVA_HOME.
goto :failure

:version_missing
echo Could not read the version from pom.xml.
goto :failure

:name_missing
echo Could not read the project name from pom.xml.
goto :failure

:backup_failed
echo Save backup failed; build was not started.
goto :failure

:backup_saves
set "SAVE_SOURCE_DIR="
if exist "%PERSISTENT_DATA_DIR%\" set "SAVE_SOURCE_DIR=%PERSISTENT_DATA_DIR%"
if not defined SAVE_SOURCE_DIR if exist "%DATA_DIR%\" set "SAVE_SOURCE_DIR=%DATA_DIR%"
if not defined SAVE_SOURCE_DIR exit /b 0
echo Backing up saves from: %SAVE_SOURCE_DIR%
mkdir "%SAVE_BACKUP_DATA_DIR%" 2>nul
if not exist "%SAVE_BACKUP_DATA_DIR%\" exit /b 1
robocopy "%SAVE_SOURCE_DIR%" "%SAVE_BACKUP_DATA_DIR%" /E /COPY:DAT /DCOPY:T /R:1 /W:1 >nul
if errorlevel 8 exit /b 1
set "HAS_SAVE_BACKUP=1"
exit /b 0

:restore_saves
if not "%HAS_SAVE_BACKUP%"=="1" exit /b 0
echo Restoring saves to: %PERSISTENT_DATA_DIR%
if not exist "%PERSISTENT_DATA_DIR%\" mkdir "%PERSISTENT_DATA_DIR%"
if not exist "%PERSISTENT_DATA_DIR%\" exit /b 1
robocopy "%SAVE_BACKUP_DATA_DIR%" "%PERSISTENT_DATA_DIR%" /E /COPY:DAT /DCOPY:T /R:1 /W:1 >nul
if errorlevel 8 exit /b 1

echo Copying saves into build output: %DATA_DIR%
if exist "%DATA_DIR%\" (
	fsutil reparsepoint query "%DATA_DIR%" >nul 2>&1
	if errorlevel 1 (rmdir /s /q "%DATA_DIR%") else (rmdir "%DATA_DIR%")
)
if exist "%DATA_DIR%\" exit /b 1
if not exist "%DATA_DIR%\" mkdir "%DATA_DIR%"
if not exist "%DATA_DIR%\" exit /b 1
robocopy "%SAVE_BACKUP_DATA_DIR%" "%DATA_DIR%" /E /COPY:DAT /DCOPY:T /R:1 /W:1 >nul
if errorlevel 8 exit /b 1
rmdir /s /q "%SAVE_BACKUP_DIR%"
exit /b 0
:failure
if not defined EXIT_CODE set "EXIT_CODE=1"
if defined PUSHD_DONE popd
pause
exit /b %EXIT_CODE%
