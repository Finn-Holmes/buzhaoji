@echo off
setlocal

rem The host environment contains both Path and PATH, including malformed
rem quoted Java entries. Clear them before Gradle computes java.library.path.
set "Path="
set "PATH=C:\Program Files\Android\Android Studio\jbr\bin;C:\Windows\System32"
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "CLASSPATH="

call "%~dp0.tools\gradle-9.5.0\bin\gradle.bat" %*
set "buildExitCode=%ERRORLEVEL%"
endlocal & exit /b %buildExitCode%
