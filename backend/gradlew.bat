@echo off
setlocal
set "GRADLE_USER_HOME=%CD%\.gradle"
set "GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  set "GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.14.2-bin\2pb3mgt1p815evrl3weanttgr\gradle-8.14.2"
)
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Gradle distribution not found in local wrapper cache.
  exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
