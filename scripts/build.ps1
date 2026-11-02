param([string[]]$Tasks = @(':app:assembleDebug'))
$ErrorActionPreference = 'Stop'
$taskRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$env:JAVA_HOME = Join-Path $taskRoot '.tools\jbr'
$env:ANDROID_HOME = Join-Path $taskRoot '.tools\android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $taskRoot '.tools\android-user'
$env:GRADLE_USER_HOME = Join-Path $taskRoot '.tools\gradle-user-home'
$env:TEMP = Join-Path $taskRoot '.tools\temp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Path $env:TEMP,$env:ANDROID_USER_HOME,$env:GRADLE_USER_HOME -Force | Out-Null
$taskGradle = Join-Path $taskRoot '.tools\gradle-9.4.1\bin\gradle.bat'
Push-Location $taskRoot
try {
    & $taskGradle --no-daemon --console=plain @Tasks
    if ($LASTEXITCODE -ne 0) { throw "Derleme başarısız: $LASTEXITCODE" }
} finally { Pop-Location }
