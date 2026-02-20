@echo off
setlocal

rem Always use Android Studio bundled JBR for this project
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem Keep Gradle cache local to project to avoid corporate ACL issues under %USERPROFILE%\.gradle
set "GRADLE_USER_HOME=%~dp0.gradle-user-home"

call "%~dp0gradlew.bat" %*
set EXIT_CODE=%ERRORLEVEL%
endlocal & exit /b %EXIT_CODE%

