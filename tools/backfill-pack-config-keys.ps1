<#
.SYNOPSIS
    Backfill config keys the mod added into the CTE2 pack's datapack overrides.

.DESCRIPTION
    When a runtime class such as SpellConfiguration gains a field, Gson starts writing a key that no
    existing datapack JSON contains. JsonExileRegistry.compareLoadedJsonAndFinalClass re-serialises
    every loaded entry and compares Gson trees, so a missing key makes every pre-existing override
    fail the check - one "[Mine and Slash Datapack Check Failed]" warning per file, on every load.

    The mod's own JSON gets the new key for free the next time you regenerate in-game. The pack's
    hand-maintained overrides do not, and that is what this fixes: for each pack file, insert any
    config key the mod now writes but the file lacks.

    Values come from the mod's own entry of the same identifier, so a pack override of a spell the
    mod gave real data (the golem summons and their summon_spells) inherits that data rather than a
    blank default. Pack-only entries with no mod counterpart fall back to the value the mod writes
    most often for that key.

    Keys already present are never touched - this only ever adds.

.PARAMETER Registry
    Registry folder under data/mmorpg to process. Defaults to mmorpg_spells.

.PARAMETER Section
    Object inside each entry whose keys are compared. Defaults to config.

.EXAMPLE
    powershell -File tools\backfill-pack-config-keys.ps1 -WhatIf
    Report what would change, write nothing.

.EXAMPLE
    powershell -File tools\backfill-pack-config-keys.ps1
    Apply the backfill.

.NOTES
    Windows PowerShell 5.1. Keep this file ASCII-only: 5.1 reads .ps1 as ANSI, so a non-ASCII
    literal becomes mojibake. Writes to the CurseForge instance only, never the server copy.
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$Registry = 'mmorpg_spells',
    [string]$Section  = 'config'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '..\wiki\lib\common.ps1')

$modDir  = Join-Path $script:DATA_REPO $Registry
$packDir = Join-Path $script:DATA_PACK $Registry

# The live server copy is hand-synced by the user and is not a git repo - nothing here may write to it.
if ($packDir -match 'Craft-to-Exile-2-Server') {
    throw "Refusing to write to the live server copy: $packDir"
}
foreach ($d in @($modDir, $packDir)) {
    if (-not (Test-Path $d)) { throw "Missing directory: $d" }
}

# ---------------------------------------------------------------- json rendering
#
# Values are re-emitted as text rather than round-tripped through ConvertTo-Json: the pack files are
# LF, BOM-less, 2-space indent, and a whole-file rewrite would reformat all of them and normalise
# number literals for the sake of adding one key.

function Format-JsonValue {
    param($Value, [string]$Indent)

    if ($null -eq $Value) { return 'null' }
    if ($Value -is [bool]) { if ($Value) { return 'true' } else { return 'false' } }
    if ($Value -is [string]) { return '"' + ($Value -replace '\\', '\\\\' -replace '"', '\"') + '"' }

    if ($Value -is [System.Object[]]) {
        if ($Value.Count -eq 0) { return '[]' }
        $inner = $Value | ForEach-Object { "$Indent  " + (Format-JsonValue $_ "$Indent  ") }
        return "[`n" + ($inner -join ",`n") + "`n$Indent]"
    }

    if ($Value -is [System.Management.Automation.PSCustomObject]) {
        $props = @($Value.PSObject.Properties)
        if ($props.Count -eq 0) { return '{}' }
        $inner = $props | ForEach-Object {
            "$Indent  " + '"' + $_.Name + '": ' + (Format-JsonValue $_.Value "$Indent  ")
        }
        return "{`n" + ($inner -join ",`n") + "`n$Indent}"
    }

    # numbers: keep the literal Gson would accept, and keep a whole double looking like a double
    $d = [double]$Value
    if ($Value -is [int] -or $Value -is [long] -or $d -eq [math]::Floor($d)) {
        if ($Value -is [double] -or $Value -is [decimal]) { return ([string]$d) + '.0' }
        return [string][long]$d
    }
    return [string]$d
}

# Canonical text of a value, used only to pick the most common default across the mod's entries.
function Get-ValueKey {
    param($Value)
    return (Format-JsonValue $Value '')
}

# ---------------------------------------------------------------- mod reference
#
# Two things are read off the mod's regenerated output: which keys the class writes now, and what it
# writes for each - per identifier where a counterpart exists, and as a mode over all entries where
# it does not.

$modByIdentifier = @{}
$keyOrder        = New-Object System.Collections.ArrayList
$valueCounts     = @{}
$valueSamples    = @{}

foreach ($f in (Get-ChildItem $modDir -Recurse -Filter *.json | Sort-Object FullName)) {
    $j = Get-Content $f.FullName -Raw | ConvertFrom-Json
    $sec = $j.$Section
    if ($null -eq $sec) { continue }

    $id = [string]$j.identifier
    if (-not [string]::IsNullOrWhiteSpace($id)) { $modByIdentifier[$id] = $sec }

    foreach ($p in $sec.PSObject.Properties) {
        if (-not $keyOrder.Contains($p.Name)) { [void]$keyOrder.Add($p.Name) }
        $vk = Get-ValueKey $p.Value
        if (-not $valueCounts.ContainsKey($p.Name)) {
            $valueCounts[$p.Name]  = @{}
            $valueSamples[$p.Name] = @{}
        }
        if (-not $valueCounts[$p.Name].ContainsKey($vk)) {
            $valueCounts[$p.Name][$vk]  = 0
            $valueSamples[$p.Name][$vk] = $p.Value
        }
        $valueCounts[$p.Name][$vk] += 1
    }
}

if ($keyOrder.Count -eq 0) { throw "No '$Section' objects found under $modDir - is src/generated/resources regenerated?" }

$fallback = @{}
foreach ($k in $keyOrder) {
    $best = ($valueCounts[$k].GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 1).Key
    $fallback[$k] = $valueSamples[$k][$best]
}

Write-Host ("mod reference: {0} entries, {1} '{2}' keys" -f $modByIdentifier.Count, $keyOrder.Count, $Section)

# ---------------------------------------------------------------- insertion
#
# Direct keys of the section, in file order. Nested objects and arrays are skipped by depth, so
# "min" inside mana_cost never registers as a config key.

function Get-SectionKeyLines {
    param([string[]]$Lines, [string]$SectionName)

    $start = -1
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i] -match ('^\s*"' + [regex]::Escape($SectionName) + '"\s*:\s*\{\s*$')) { $start = $i; break }
    }
    if ($start -lt 0) { return $null }

    $depth = 1
    $keys  = New-Object System.Collections.ArrayList
    for ($i = $start + 1; $i -lt $Lines.Count; $i++) {
        $line = $Lines[$i]
        if ($depth -eq 1 -and $line -match '^(\s*)"([^"]+)"\s*:') {
            [void]$keys.Add([pscustomobject]@{ Name = $matches[2]; Line = $i; Indent = $matches[1] })
        }
        $opens  = ([regex]::Matches($line, '[\{\[]')).Count
        $closes = ([regex]::Matches($line, '[\}\]]')).Count
        $depth += $opens - $closes
        if ($depth -le 0) { return [pscustomobject]@{ Keys = $keys; Start = $start; End = $i } }
    }
    return $null
}

$stats     = @{}
$changed   = 0
$unchanged = 0
$files     = @(Get-ChildItem $packDir -Recurse -Filter *.json | Sort-Object FullName)

foreach ($f in $files) {
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    $j   = $raw | ConvertFrom-Json
    $sec = $j.$Section
    if ($null -eq $sec) { Write-Warning "no '$Section' object: $($f.FullName)"; continue }

    $have    = @($sec.PSObject.Properties.Name)
    $missing = @($keyOrder | Where-Object { $have -notcontains $_ })
    if ($missing.Count -eq 0) { $unchanged++; continue }

    $modSec = $modByIdentifier[[string]$j.identifier]

    $nl    = if ($raw.Contains("`r`n")) { "`r`n" } else { "`n" }
    $lines = [System.Collections.ArrayList]@($raw -split "`r?`n")

    foreach ($key in ($missing | Sort-Object)) {
        $info = Get-SectionKeyLines -Lines $lines -SectionName $Section
        if ($null -eq $info) { throw "could not locate the '$Section' block in $($f.FullName)" }

        # the leading commas matter: PowerShell unrolls a statement's output, so a plain
        # "{ $fallback[$key] }" hands back $null for an empty array and the key lands as null
        $value = if ($modSec -and ($modSec.PSObject.Properties.Name -contains $key)) { ,$modSec.$key } else { ,$fallback[$key] }

        # slot it in ahead of the first key that sorts after it, so the near-alphabetical order the
        # pack files already use is preserved
        $after = $info.Keys | Where-Object { [string]::Compare($_.Name, $key, $false) -gt 0 } | Select-Object -First 1

        if ($after) {
            $indent = $after.Indent
            $text   = $indent + '"' + $key + '": ' + (Format-JsonValue $value $indent) + ','
            $lines.Insert($after.Line, $text)
        } else {
            # sorts last: it becomes the final entry, so the one before it needs a comma
            $last   = $info.Keys | Select-Object -Last 1
            $indent = if ($last) { $last.Indent } else { '    ' }
            $prev   = $info.End - 1
            $lines[$prev] = $lines[$prev] -replace ',\s*$', ''
            $text   = $indent + '"' + $key + '": ' + (Format-JsonValue $value $indent)
            $lines.Insert($info.End, $text)
        }

        if (-not $stats.ContainsKey($key)) { $stats[$key] = 0 }
        $stats[$key] += 1
    }

    $out = ($lines -join $nl)

    # never write something that does not parse
    try { $null = $out | ConvertFrom-Json } catch { throw "insertion produced invalid JSON in $($f.FullName): $_" }

    if ($PSCmdlet.ShouldProcess($f.FullName, "add $($missing.Count) $Section key(s): $($missing -join ', ')")) {
        Write-Utf8NoBom -Path $f.FullName -Text $out
    }
    $changed++
}

Write-Host ""
Write-Host ("files scanned : {0}" -f $files.Count)
Write-Host ("files changed : {0}" -f $changed)
Write-Host ("already ok    : {0}" -f $unchanged)
foreach ($e in ($stats.GetEnumerator() | Sort-Object Name)) {
    Write-Host ("  +{0,-28} {1} file(s)" -f $e.Key, $e.Value)
}
