param([string]$DirectorioFirma = (Join-Path $env:LOCALAPPDATA 'SanaEsencia\firma-release'))
$ErrorActionPreference = 'Stop'
$archivoJks = Join-Path $DirectorioFirma 'sanaesencia-release.jks'
$archivoPassword = Join-Path $DirectorioFirma 'password.clixml'
if (!(Test-Path -LiteralPath $archivoJks) -or !(Test-Path -LiteralPath $archivoPassword)) {
    throw 'Falta el almacén de firma privado o su contraseña cifrada. No se genera otra llave automáticamente.'
}
$env:SANA_RELEASE_STORE_FILE = $archivoJks
$passwordSeguro = Import-Clixml -LiteralPath $archivoPassword
$env:SANA_RELEASE_STORE_PASSWORD = [Net.NetworkCredential]::new('', $passwordSeguro).Password
$env:SANA_RELEASE_KEY_PASSWORD = $env:SANA_RELEASE_STORE_PASSWORD
Push-Location $PSScriptRoot
try {
    & .\gradlew.bat :app:assembleRelease :app:lintRelease
    if ($LASTEXITCODE -ne 0) { throw 'La compilación release no terminó correctamente.' }
    $apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\release\app-release.apk'
    if (!(Test-Path -LiteralPath $apk)) { throw 'No se encontró el APK release firmado.' }
    Write-Output "APK generado: $apk"
} finally {
    Remove-Item Env:SANA_RELEASE_STORE_FILE, Env:SANA_RELEASE_STORE_PASSWORD, Env:SANA_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
