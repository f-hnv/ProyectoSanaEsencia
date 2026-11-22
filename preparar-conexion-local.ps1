param([string]$Dispositivo = '')
$ErrorActionPreference = 'Stop'
$adbSana = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adbSana)) { throw 'No se encontró adb en el SDK de Android de esta PC.' }
if ([string]::IsNullOrWhiteSpace($Dispositivo)) {
    $dispositivosSana = @(& $adbSana devices | ForEach-Object {
        if ($_ -match '^(\S+)\s+device\s*$') { $Matches[1] }
    })
    if ($dispositivosSana.Count -ne 1) {
        throw 'Conecta un celular o inicia un emulador. Si hay varios, usa -Dispositivo con el ID mostrado por adb devices.'
    }
    $Dispositivo = $dispositivosSana[0]
}
foreach ($puertoSana in 8080,8081,8082) {
    & $adbSana -s $Dispositivo reverse "tcp:$puertoSana" "tcp:$puertoSana"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo redirigir el puerto $puertoSana." }
}
Write-Host "Conexión local preparada para $Dispositivo. Mantén los servicios y MySQL activos."
