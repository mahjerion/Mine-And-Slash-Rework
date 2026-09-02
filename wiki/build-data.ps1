# Builds wiki/data/<page>.json from the merged repo + CTE2 pack datapacks.
#
# Each page file is { rows: [...], labels: {statId -> {k,t}}, keyMeta: {key -> {label,cat,dim}} }.
#
# Row shape consumed by tpl/app.js:
#   cat     which section of the page it belongs to
#   id      raw registry id (shown under the name, and searchable)
#   name    display name, resolved from the lang file where one exists
#   keys[]  filter keys; each is described in keyMeta
#   tags[]  chips for the "applies to" style column   (excl[] renders struck through)
#   f{}     scalars addressed by column `field` (weight, tier, league, ...)
#   facts[] {k,v} pairs for the generic key/value column
#   stats[] {stat,type,min,max}
#   groups[] {label, stats[]}  - used where one entry has several stat sets
#   items[]  {id, num, note}   - item ladders and recipe materials

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\common.ps1')

$OUT = Join-Path $PSScriptRoot 'data'
$LANG = Import-Lang
Write-Host "lang keys: $($LANG.Count)"

$script:KEYMETA = @{}
function Reg-Key {
    param([string]$Key, [string]$Label, [string]$Cat, [string]$Dim = 'slot')
    if (-not $script:KEYMETA.ContainsKey($Key)) {
        $script:KEYMETA[$Key] = [ordered]@{ label = $Label; cat = $Cat; dim = $Dim }
    }
    return $Key
}

function Save-Page {
    param([string]$PageId, $Rows, [hashtable]$ExtraLabels = $null)

    # Drop filler entries, and flag anything that exists but can never be randomly
    # generated so the page can badge it rather than silently omitting real content.
    $keep = New-Object System.Collections.ArrayList
    foreach ($r in $Rows) {
        if (Test-Placeholder $r.id) { continue }
        if ($r.f.Contains('weight') -and [double]$r.f.weight -le 0) { $r.f.noRoll = $true }
        [void]$keep.Add($r)
    }
    $Rows = @($keep)

    $bag = @{}
    foreach ($r in $Rows) {
        foreach ($s in $r.stats)  { $bag[$s.stat] = 1 }
        foreach ($g in $r.groups) { foreach ($s in $g.stats) { $bag[$s.stat] = 1 } }
    }
    $labels = [ordered]@{}
    foreach ($id in ($bag.Keys | Sort-Object)) { $labels[$id] = Get-StatLabel $LANG $id }
    if ($ExtraLabels) { foreach ($k in $ExtraLabels.Keys) { $labels[$k] = $ExtraLabels[$k] } }

    # only ship the key metadata this page actually references
    $used = @{}
    foreach ($r in $Rows) { foreach ($k in $r.keys) { $used[$k] = 1 } }
    $km = [ordered]@{}
    foreach ($k in ($used.Keys | Sort-Object)) { if ($script:KEYMETA.ContainsKey($k)) { $km[$k] = $script:KEYMETA[$k] } }

    $obj = [ordered]@{ rows = @($Rows); labels = $labels; keyMeta = $km }
    $json = ConvertTo-Json -InputObject $obj -Depth 12 -Compress
    Write-Utf8NoBom (Join-Path $OUT "$PageId.json") $json
    $cnt = [ordered]@{}
    foreach ($r in $Rows) { if (-not $cnt.Contains($r.cat)) { $cnt[$r.cat] = 0 }; $cnt[$r.cat]++ }
    $byCat = (@($cnt.Keys) | ForEach-Object { "$_=$($cnt[$_])" }) -join ' '
    Write-Host ("  {0,-14} rows={1,-5} labels={2,-4} keys={3,-4} [{4}]" -f $PageId, @($Rows).Count, $labels.Count, $km.Count, $byCat)
}

function New-Row {
    param([string]$Cat, [string]$Id, [string]$Name)
    return [ordered]@{
        cat = $Cat; id = $Id; name = $Name
        keys = (New-Object System.Collections.ArrayList)
        tags = (New-Object System.Collections.ArrayList)
        excl = (New-Object System.Collections.ArrayList)
        f = [ordered]@{}
        facts = (New-Object System.Collections.ArrayList)
        stats = @()
        groups = (New-Object System.Collections.ArrayList)
        items = (New-Object System.Collections.ArrayList)
    }
}

# =====================================================================  AFFIXES

Write-Host "affixes..."
$affixes = Import-Registry -Registry 'mmorpg_affixes' -IdField 'guid'
foreach ($t in @('might','finesse','ingenuity')) { Reg-Key $t (ConvertTo-TitleCase $t) 'style' 'style' | Out-Null }

function Build-AffixRows {
    param([string[]]$Types)
    $rows = New-Object System.Collections.ArrayList
    foreach ($k in ($affixes.Keys | Sort-Object)) {
        $j = $affixes[$k]
        if ($Types -notcontains [string]$j.type) { continue }
        $name = Resolve-Name $LANG $j.guid @("mmorpg.affix.$($j.guid)")
        $r = New-Row ([string]$j.type) $j.guid $name
        $r.f.weight = [int]$j.weight
        $reqAll = $false
        foreach ($req in $j.requirements.tag_requirements) {
            if ($req.req_type -eq 'HAS_ALL') { $reqAll = $true }
            foreach ($i in $req.included) { if (-not $r.tags.Contains($i)) { [void]$r.tags.Add($i) } }
            foreach ($i in $req.excluded) { if (-not $r.excl.Contains($i)) { [void]$r.excl.Add($i) } }
        }
        $r.f.reqAll = $reqAll
        foreach ($i in $r.tags) { [void]$r.keys.Add($i) }
        if ($j.eye_aura_req -and $j.eye_aura_req -ne '') {
            $an = Resolve-Name $LANG $j.eye_aura_req @("mmorpg.aura.$($j.eye_aura_req)", "mmorpg.spell.$($j.eye_aura_req)")
            $r.f.aura = $an
            [void]$r.keys.Add((Reg-Key "aura:$($j.eye_aura_req)" $an 'aura' 'aura'))
        }
        $r.stats = ConvertTo-StatList $j.stats
        [void]$rows.Add($r)
    }
    return ,@($rows)
}

Save-Page 'affix'      (Build-AffixRows @('prefix','suffix'))
Save-Page 'implicit'   (Build-AffixRows @('implicit'))
Save-Page 'enchant'    (Build-AffixRows @('enchant'))
Save-Page 'jewel'      (Build-AffixRows @('jewel','jewel_corruption','crafted_jewel_unique'))
Save-Page 'corruption' (Build-AffixRows @('chaos_stat'))
Save-Page 'watcher'    (Build-AffixRows @('watcher_eye'))
Save-Page 'tool'       (Build-AffixRows @('tool'))

# =====================================================================  GEAR BASES

Write-Host "gear bases..."
$rows = New-Object System.Collections.ArrayList
$bases = Import-Registry -Registry 'mmorpg_base_gear_types' -IdField 'guid' -KeepZeroWeight
foreach ($k in ($bases.Keys | Sort-Object)) {
    $j = $bases[$k]
    $r = New-Row 'base' $j.guid (Resolve-Name $LANG $j.guid @("mmorpg.gear_type.$($j.guid)", "mmorpg.gearslot.$($j.guid)"))
    $r.f.weight = [int]$j.weight
    $r.f.slot = [string]$j.gear_slot
    $r.f.style = [string]$j.style
    foreach ($t in $j.tags.tags) { [void]$r.tags.Add($t); [void]$r.keys.Add($t) }
    if ($j.style) { [void]$r.keys.Add((Reg-Key "style:$($j.style)" (ConvertTo-TitleCase $j.style) 'attribute' 'attribute')) }
    if ($j.weapon_type) { [void]$r.facts.Add((New-Fact 'Weapon type' (ConvertTo-TitleCase $j.weapon_type))) }
    foreach ($p in $j.req.scaling_req.PSObject.Properties) {
        [void]$r.facts.Add((New-Fact ((ConvertTo-TitleCase $p.Name) + ' scaling') ([string]$p.Value)))
    }
    foreach ($p in $j.req.base_req.PSObject.Properties) {
        [void]$r.facts.Add((New-Fact ((ConvertTo-TitleCase $p.Name) + ' required') ([string]$p.Value)))
    }
    $r.stats = ConvertTo-StatList $j.base_stats
    foreach ($it in $j.possible_items) {
        [void]$r.items.Add([ordered]@{ id = [string]$it.item_id; num = $null; note = (ConvertTo-TitleCase $it.min_rar) + ' min - w' + [string]$it.weight })
    }
    [void]$rows.Add($r)
}
Save-Page 'gear-bases' @($rows)

# =====================================================================  UNIQUES + SETS

Write-Host "uniques + sets..."
$rows = New-Object System.Collections.ArrayList
$uniq = Import-Registry -Registry 'mmorpg_unique_gears' -IdField 'guid' -KeepZeroWeight
foreach ($k in ($uniq.Keys | Sort-Object)) {
    $j = $uniq[$k]
    $r = New-Row 'unique' $j.guid (Resolve-Name $LANG $j.guid @("mmorpg.unique_gear.$($j.guid).name"))
    $r.f.weight = [int]$j.weight
    $r.f.base = ConvertTo-TitleCase $j.base_gear
    $league = if ($j.league) { [string]$j.league } else { 'base' }
    $r.f.league = ConvertTo-TitleCase $league
    [void]$r.keys.Add((Reg-Key "league:$league" (ConvertTo-TitleCase $league) 'league' 'league'))
    if ($j.base_gear) { [void]$r.tags.Add([string]$j.base_gear); [void]$r.keys.Add([string]$j.base_gear) }
    [void]$r.facts.Add((New-Fact 'Min drop level' ([string]$j.min_drop_lvl)))
    if ([int]$j.min_tier -gt 0) { [void]$r.facts.Add((New-Fact 'Min map tier' ([string]$j.min_tier))) }
    if ($j.runable -eq $true) { [void]$r.facts.Add((New-Fact 'Runeable' 'yes')) }
    $flav = Clear-Markup $j.flavor_text
    if ($flav) { $r.f.flavor = $flav }
    $r.stats = ConvertTo-StatList $j.unique_stats
    [void]$rows.Add($r)
}
$sets = Import-Registry -Registry 'mmorpg_sets' -IdField 'id' -KeepZeroWeight
foreach ($k in ($sets.Keys | Sort-Object)) {
    $j = $sets[$k]
    $r = New-Row 'set' $j.id (Resolve-Name $LANG $j.id @("mmorpg.item_set.$($j.id)"))
    foreach ($u in $j.uniques) {
        $un = Resolve-Name $LANG $u @("mmorpg.unique_gear.$u.name")
        [void]$r.tags.Add("unique:$u")
        Reg-Key "unique:$u" $un 'jewelry' 'slot' | Out-Null
    }
    foreach ($b in $j.bonuses) {
        [void]$r.groups.Add([ordered]@{ label = "$($b.pieces) pieces"; stats = (ConvertTo-StatList $b.stats) })
    }
    [void]$r.facts.Add((New-Fact 'Pieces' ([string]@($j.uniques).Count)))
    [void]$rows.Add($r)
}
Save-Page 'uniques' @($rows)

# =====================================================================  SOCKETS: gems, runes, runewords

Write-Host "sockets..."
$rows = New-Object System.Collections.ArrayList
$gems = Import-Registry -Registry 'mmorpg_gems' -IdField 'identifier' -KeepZeroWeight
foreach ($k in ($gems.Keys | Sort-Object)) {
    $j = $gems[$k]
    $nm = (ConvertTo-TitleCase $j.gem_type) + ' T' + [string]$j.tier
    $r = New-Row 'gem' $j.identifier (Resolve-Name $LANG $j.identifier @("item.mmorpg.gems.$($j.gem_type).$($j.tier)") $nm)
    $r.f.weight = [int]$j.weight
    $r.f.tier = [int]$j.tier
    $r.f.item = [string]$j.item_id
    $r.f.rarity = ConvertTo-TitleCase $j.rar
    [void]$r.keys.Add((Reg-Key "gem:$($j.gem_type)" (ConvertTo-TitleCase $j.gem_type) 'jewel' 'slot'))
    [void]$r.keys.Add((Reg-Key "rar:$($j.rar)" (ConvertTo-TitleCase $j.rar) 'rarity' 'rarity'))
    [void]$r.groups.Add([ordered]@{ label = 'Armor';   stats = (ConvertTo-StatList $j.on_armor_stats) })
    [void]$r.groups.Add([ordered]@{ label = 'Jewelry'; stats = (ConvertTo-StatList $j.on_jewelry_stats) })
    [void]$r.groups.Add([ordered]@{ label = 'Weapon';  stats = (ConvertTo-StatList $j.on_weapons_stats) })
    [void]$rows.Add($r)
}
$runes = Import-Registry -Registry 'mmorpg_runes' -IdField 'id' -KeepZeroWeight
foreach ($k in ($runes.Keys | Sort-Object)) {
    $j = $runes[$k]
    $r = New-Row 'rune' $j.id (Resolve-Name $LANG $j.id @("item.mmorpg.runes.$($j.id)"))
    $r.f.weight = [int]$j.weight
    $r.f.tier = [int]$j.tier
    $r.f.item = [string]$j.item_id
    [void]$r.keys.Add((Reg-Key "tier:$($j.tier)" ('Tier ' + [string]$j.tier) 'tier' 'tier'))
    [void]$r.facts.Add((New-Fact 'Min level multi' ([string]$j.min_lvl_multi)))
    [void]$r.groups.Add([ordered]@{ label = 'Armor';   stats = (ConvertTo-StatList $j.on_armor_stats) })
    [void]$r.groups.Add([ordered]@{ label = 'Jewelry'; stats = (ConvertTo-StatList $j.on_jewelry_stats) })
    [void]$r.groups.Add([ordered]@{ label = 'Weapon';  stats = (ConvertTo-StatList $j.on_weapons_stats) })
    [void]$rows.Add($r)
}
$rw = Import-Registry -Registry 'mmorpg_runeword' -IdField 'id' -KeepZeroWeight
foreach ($k in ($rw.Keys | Sort-Object)) {
    $j = $rw[$k]
    $r = New-Row 'runeword' $j.id (Resolve-Name $LANG $j.id @("mmorpg.runeword.$($j.id)"))
    foreach ($rn in $j.runes) {
        [void]$r.items.Add([ordered]@{ id = (Resolve-Name $LANG $rn @("item.mmorpg.runes.$rn")); num = $null; note = $rn })
    }
    foreach ($s in $j.slots) { [void]$r.tags.Add([string]$s); [void]$r.keys.Add([string]$s) }
    [void]$r.facts.Add((New-Fact 'Runes' ([string]@($j.runes).Count)))
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
Save-Page 'sockets' @($rows)

# =====================================================================  PROFESSION RECIPES

Write-Host "recipes..."
$rows = New-Object System.Collections.ArrayList
$rec = Import-Registry -Registry 'mmorpg_profession_recipe' -IdField 'id' -KeepZeroWeight
foreach ($k in ($rec.Keys | Sort-Object)) {
    $j = $rec[$k]
    $short = ($j.result -replace '^[a-z_0-9]+:', '')
    $r = New-Row 'recipe' $j.id (ConvertTo-TitleCase $short)
    $r.f.tier = [int]$j.tier
    $r.f.exp = [int]$j.exp
    $r.f.result = [string]$j.result
    $r.f.resultNum = [int]$j.result_num
    $r.f.profession = ConvertTo-TitleCase $j.profession
    [void]$r.keys.Add((Reg-Key "prof:$($j.profession)" (ConvertTo-TitleCase $j.profession) 'profession' 'profession'))
    [void]$r.keys.Add((Reg-Key "tier:$($j.tier)" ('Tier ' + [string]$j.tier) 'tier' 'tier'))
    if ($j.requires_pinnacle_unlock -eq $true) {
        [void]$r.keys.Add((Reg-Key 'pinnacle' 'Pinnacle unlock' 'league' 'flagdim'))
        [void]$r.facts.Add((New-Fact 'Requires' 'Pinnacle unlock'))
    }
    foreach ($m in $j.mats) {
        [void]$r.items.Add([ordered]@{ id = [string]$m.id; num = [int]$m.num; note = $(if ($m.type -eq 'TAG') { 'tag' } else { '' }) })
    }
    [void]$rows.Add($r)
}
Save-Page 'recipes' @($rows)

# =====================================================================  SUPPORT GEMS + AUGMENTS

Write-Host "support gems + augments..."
$rows = New-Object System.Collections.ArrayList
$sg = Import-Registry -Registry 'mmorpg_support_gem' -IdField 'id' -KeepZeroWeight
foreach ($k in ($sg.Keys | Sort-Object)) {
    $j = $sg[$k]
    $r = New-Row 'support' $j.id (Resolve-Name $LANG $j.id @("mmorpg.support_gem.$($j.id)"))
    $r.f.weight = [int]$j.weight
    $r.f.minLvl = [int]$j.min_lvl
    $r.f.manaMulti = [double]$j.manaMulti
    $r.f.style = ConvertTo-TitleCase $j.style
    if ($j.style) { [void]$r.keys.Add((Reg-Key "style:$($j.style)" (ConvertTo-TitleCase $j.style) 'attribute' 'attribute')) }
    [void]$r.facts.Add((New-Fact 'Mana multiplier' (Format-Multi $j.manaMulti)))
    [void]$r.facts.Add((New-Fact 'Min level' ([string]$j.min_lvl)))
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
$aura = Import-Registry -Registry 'mmorpg_aura' -IdField 'id' -KeepZeroWeight
foreach ($k in ($aura.Keys | Sort-Object)) {
    $j = $aura[$k]
    $r = New-Row 'augment' $j.id (Resolve-Name $LANG $j.id @("mmorpg.aura.$($j.id)", "mmorpg.spell.$($j.id)"))
    $r.f.minLvl = [int]$j.min_lvl
    $r.f.reservation = [double]$j.reservation
    $r.f.style = ConvertTo-TitleCase $j.style
    if ($j.style) { [void]$r.keys.Add((Reg-Key "style:$($j.style)" (ConvertTo-TitleCase $j.style) 'attribute' 'attribute')) }
    [void]$r.facts.Add((New-Fact 'Reservation' (([string]([math]::Round([double]$j.reservation * 100, 1))) + '%')))
    [void]$r.facts.Add((New-Fact 'Min level' ([string]$j.min_lvl)))
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
Save-Page 'support' @($rows)

# =====================================================================  EXILE EFFECTS

Write-Host "effects..."
$rows = New-Object System.Collections.ArrayList
$eff = Import-Registry -Registry 'mmorpg_exile_effect' -IdField 'id' -KeepZeroWeight
foreach ($k in ($eff.Keys | Sort-Object)) {
    $j = $eff[$k]
    $r = New-Row 'effect' $j.id (Resolve-Name $LANG $j.id @("mmorpg.effect.$($j.id)"))
    $ty = if ($j.type) { [string]$j.type } else { 'neutral' }
    $r.f.type = ConvertTo-TitleCase $ty
    $r.f.maxStacks = [int]$j.max_stacks
    [void]$r.keys.Add((Reg-Key "eff:$ty" (ConvertTo-TitleCase $ty) 'effect' 'slot'))
    foreach ($t in $j.tags.tags) {
        if ($t -eq $ty) { continue }
        [void]$r.tags.Add("tag:$t")
        [void]$r.keys.Add((Reg-Key "tag:$t" (ConvertTo-TitleCase $t) 'group' 'tag'))
    }
    [void]$r.facts.Add((New-Fact 'Max stacks' ([string]$j.max_stacks)))
    if ($j.stacks_affect_stats -eq $true) { [void]$r.facts.Add((New-Fact 'Stacks scale stats' 'yes')) }
    $hasSpell = ($j.spell -and (@($j.spell.on_cast).Count -gt 0 -or $j.spell.entity_components.PSObject.Properties.Count -gt 0))
    if ($hasSpell) { [void]$r.facts.Add((New-Fact 'Behaviour' 'runs a spell component')) }
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
Save-Page 'effects' @($rows)

# =====================================================================  MONSTERS

Write-Host "monsters..."
$rows = New-Object System.Collections.ArrayList
$ent = Import-Registry -Registry 'mmorpg_entity' -IdField 'identifier' -KeepZeroWeight
foreach ($k in ($ent.Keys | Sort-Object)) {
    $j = $ent[$k]
    $scope = if ($j._sub) { [string]$j._sub } else { 'root' }
    $r = New-Row 'entity' $j.identifier (ConvertTo-TitleCase $j.identifier)
    $r.f.scope = ConvertTo-TitleCase $scope
    [void]$r.keys.Add((Reg-Key "scope:$scope" (ConvertTo-TitleCase $scope) 'scope' 'slot'))
    [void]$r.facts.Add((New-Fact 'Health' (Format-Multi $j.hp_multi)))
    [void]$r.facts.Add((New-Fact 'Damage' (Format-Multi $j.dmg_multi)))
    [void]$r.facts.Add((New-Fact 'Stats' (Format-Multi $j.stat_multi)))
    [void]$r.facts.Add((New-Fact 'Experience' (Format-Multi $j.exp_multi)))
    [void]$r.facts.Add((New-Fact 'Loot' (Format-Multi $j.loot_multi)))
    $mn = [int]$j.min_lvl; $mx = [int]$j.max_lvl
    if ($mn -gt 1 -or $mx -lt 1000000) {
        $hi = if ($mx -ge 1000000) { 'any' } else { [string]$mx }
        [void]$r.facts.Add((New-Fact 'Level range' ("$mn - $hi")))
    }
    if ($j.set_rar) { [void]$r.facts.Add((New-Fact 'Forced rarity' (ConvertTo-TitleCase $j.set_rar))) }
    $r.stats = ConvertTo-StatList $j.stats.stats
    [void]$rows.Add($r)
}
$mr = Import-Registry -Registry 'mmorpg_mob_rarity' -IdField 'id' -KeepZeroWeight
foreach ($k in ($mr.Keys | Sort-Object)) {
    $j = $mr[$k]
    $r = New-Row 'rarity' $j.id (Resolve-Name $LANG $j.id @("mmorpg.mob_rarity.$($j.id)") $j.name)
    $r.f.weight = [int]$j.weight
    $r.f.affixes = [int]$j.affixes
    [void]$r.facts.Add((New-Fact 'Health' (Format-Multi $j.extra_hp_multi)))
    [void]$r.facts.Add((New-Fact 'Damage' (Format-Multi $j.dmg_multi)))
    [void]$r.facts.Add((New-Fact 'Stats' (Format-Multi $j.stat_multi)))
    [void]$r.facts.Add((New-Fact 'Experience' (Format-Multi $j.exp_multi)))
    [void]$r.facts.Add((New-Fact 'Loot' (Format-Multi $j.loot_multi)))
    [void]$r.facts.Add((New-Fact 'Affixes' ([string]$j.affixes)))
    if ($j.is_elite -eq $true)   { [void]$r.facts.Add((New-Fact 'Elite' 'yes')) }
    if ($j.is_special -eq $true) { [void]$r.facts.Add((New-Fact 'Special spawn' 'yes')) }
    if ([int]$j.force_custom_hp -gt 0) { [void]$r.facts.Add((New-Fact 'Forced HP' ([string]$j.force_custom_hp))) }
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
$ma = Import-Registry -Registry 'mmorpg_mob_affix' -IdField 'id' -KeepZeroWeight
foreach ($k in ($ma.Keys | Sort-Object)) {
    $j = $ma[$k]
    $r = New-Row 'mobaffix' $j.id (Resolve-Name $LANG $j.id @("mmorpg.mob_affix.$($j.id)"))
    $r.f.weight = [int]$j.weight
    $r.f.type = ConvertTo-TitleCase $j.type
    [void]$r.keys.Add((Reg-Key "mtype:$($j.type)" (ConvertTo-TitleCase $j.type) 'effect' 'slot'))
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
$dim = Import-Registry -Registry 'mmorpg_dimension' -IdField 'dimension_id'
foreach ($k in ($dim.Keys | Sort-Object)) {
    $j = $dim[$k]
    $r = New-Row 'dimension' $j.dimension_id (ConvertTo-TitleCase (($j.dimension_id -split ':')[-1]))
    $r.f.dimension = [string]$j.dimension_id
    $r.f.tier = [int]$j.mob_tier
    [void]$r.facts.Add((New-Fact 'Level range' ([string]$j.min_lvl + ' - ' + [string]$j.max_lvl)))
    if ($j.secondary_lvl_range) { [void]$r.facts.Add((New-Fact 'Deep-area range' ([string]$j.secondary_lvl_range.min + ' - ' + [string]$j.secondary_lvl_range.max))) }
    [void]$r.facts.Add((New-Fact 'Mob tier' ([string]$j.mob_tier)))
    [void]$r.facts.Add((New-Fact 'Mob strength' (Format-Multi $j.mob_strength_multi)))
    [void]$r.facts.Add((New-Fact 'Experience' (Format-Multi $j.exp_multi)))
    [void]$r.facts.Add((New-Fact 'Drops' (Format-Multi $j.all_drop_multi)))
    if ([int]$j.mob_lvl_per_distance -gt 0) { [void]$r.facts.Add((New-Fact 'Blocks per mob level' ([string]$j.mob_lvl_per_distance))) }
    if ($j.scale_to_nearest_player -eq $true) { [void]$r.facts.Add((New-Fact 'Scales to nearest player' 'yes')) }
    $r.stats = ConvertTo-StatList $j.stats.stats
    [void]$rows.Add($r)
}
Save-Page 'monsters' @($rows)

# =====================================================================  OMENS

Write-Host "omens..."
$rows = New-Object System.Collections.ArrayList
# OmenPart.DroppableOmens gates on "lvl >= MAX_LEVEL * lvl_req", so lvl_req is a fraction of
# the configured max level, not of the map's level.
$maxLvl = 100
foreach ($root in @($script:DATA_PACK, $script:DATA_REPO)) {
    $gb = Join-Path $root 'mmorpg_game_balance\original_balance.json'
    if (Test-Path $gb) { $maxLvl = [int]((Get-Content $gb -Raw | ConvertFrom-Json).MAX_LEVEL); break }
}
$om = Import-Registry -Registry 'mmorpg_omen' -IdField 'id' -KeepZeroWeight
foreach ($k in ($om.Keys | Sort-Object)) {
    $j = $om[$k]
    $r = New-Row 'omen' $j.id (Resolve-Name $LANG $j.id @("mmorpg.omen.$($j.id)"))
    $r.f.weight = [int]$j.weight
    $drop = [int][math]::Ceiling([double]$j.lvl_req * $maxLvl)
    if ($drop -lt 1) { $drop = 1 }
    $r.f.dropLvl = $drop
    foreach ($t in $j.affix_types) { [void]$r.tags.Add("at:$t"); [void]$r.keys.Add((Reg-Key "at:$t" (ConvertTo-TitleCase $t) 'group' 'slot')) }
    [void]$r.facts.Add((New-Fact 'Drops from level' ([string]$drop)))
    $r.stats = ConvertTo-StatList $j.mods
    [void]$rows.Add($r)
}
Save-Page 'omens' @($rows)

# =====================================================================  MAP AFFIXES

Write-Host "map affixes..."
$rows = New-Object System.Collections.ArrayList
$mapx = Import-Registry -Registry 'mmorpg_map_affix' -IdField 'id' -KeepZeroWeight
foreach ($k in ($mapx.Keys | Sort-Object)) {
    $j = $mapx[$k]
    $r = New-Row 'mapaffix' $j.id (Resolve-Name $LANG $j.id @("mmorpg.map_affix.$($j.id)", "mmorpg.mob_affix.$($j.id)"))
    $r.f.weight = [int]$j.weight
    $r.f.affected = [string]$j.affected
    if ($j.affected) { [void]$r.keys.Add((Reg-Key "aff:$($j.affected)" ([string]$j.affected) 'affected' 'slot')) }
    if ($j.map_resist) {
        $r.f.resist = [string]$j.map_resist
        [void]$r.keys.Add((Reg-Key "res:$($j.map_resist)" ([string]$j.map_resist) 'element' 'element'))
        [void]$r.facts.Add((New-Fact 'Resistance demanded' ([string]$j.map_resist + ' +' + [string]$j.map_resist_bonus_needed)))
    }
    if ($j.prophecy_type) { [void]$r.facts.Add((New-Fact 'Prophecy' (ConvertTo-TitleCase $j.prophecy_type))) }
    if ($j.req) { [void]$r.facts.Add((New-Fact 'Requires' (ConvertTo-TitleCase $j.req))) }
    $r.stats = ConvertTo-StatList $j.stats
    [void]$rows.Add($r)
}
Save-Page 'maps' @($rows)

# =====================================================================  STAT DICTIONARY

Write-Host "stat dictionary..."
$rows = New-Object System.Collections.ArrayList
$stats = Import-Registry -Registry 'mmorpg_stat' -IdField 'auto' -KeepZeroWeight
foreach ($k in ($stats.Keys | Sort-Object)) {
    $j = $stats[$k]
    $id = [string]$j._id
    $lbl = Get-StatLabel $LANG $id
    # derived-stat lang strings are themselves sentence templates; the dictionary wants the name alone
    $disp = ($lbl.t -replace '\[VAL1\]', '') -replace '\s+', ' '
    $r = New-Row 'stat' $id $disp.Trim()
    $r.f.desc = Get-StatDesc $LANG $id
    $ser = if ($j.ser) { [string]$j.ser } else { 'data' }
    $r.f.ser = $ser
    [void]$r.keys.Add((Reg-Key "ser:$ser" (ConvertTo-TitleCase $ser) 'group' 'ser'))

    if ($ser -eq 'data') {
        $grp = if ($j.group) { [string]$j.group } else { 'Misc' }
        $gName = ConvertTo-TitleCase ($grp.ToLower())
        $r.f.group = $gName
        [void]$r.keys.Add((Reg-Key "grp:$grp" $gName 'attribute' 'group'))
        $r.f.unit = if ($j.is_perc -eq $true) { '%' } else { 'flat' }
        [void]$r.facts.Add((New-Fact 'Unit' $(if ($j.is_perc -eq $true) { 'percent' } else { 'flat' })))
        $mn = [double]$j.min; $mx = [double]$j.max
        $mxs = if ($mx -ge 1e8) { 'uncapped' } else { [string]([math]::Round($mx, 2)) }
        [void]$r.facts.Add((New-Fact 'Clamp' ([string]([math]::Round($mn, 2)) + ' to ' + $mxs)))
        if ($j.scaling -and $j.scaling -ne 'NONE') { [void]$r.facts.Add((New-Fact 'Scaling' (ConvertTo-TitleCase $j.scaling))) }
        if ($j.has_softcap -eq $true) { [void]$r.facts.Add((New-Fact 'Softcap' ([string]$j.softcap))) }
        if ($j.gui_group -and $j.gui_group -ne 'NONE') { [void]$r.facts.Add((New-Fact 'GUI group' (ConvertTo-TitleCase ([string]$j.gui_group).ToLower()))) }
        if ($j.ele) { [void]$r.facts.Add((New-Fact 'Element' ([string]$j.ele))) }
        if ($j.show_in_gui -eq $true) { [void]$r.keys.Add((Reg-Key 'gui:yes' 'Shown in GUI' 'rarity' 'vis')) }
        else { [void]$r.keys.Add((Reg-Key 'gui:no' 'Hidden from GUI' 'rarity' 'vis')) }
    } else {
        $d = $j.data
        $r.f.group = 'Derived'
        [void]$r.keys.Add((Reg-Key 'grp:Derived' 'Derived' 'attribute' 'group'))
        $addTo   = if ($d.add_to)     { Resolve-Name $LANG $d.add_to     @("mmorpg.stat.$($d.add_to)") }     else { '' }
        $adder   = if ($d.adder_stat) { Resolve-Name $LANG $d.adder_stat @("mmorpg.stat.$($d.adder_stat)") } else { '' }
        $per     = if ($d.per_amount) { [double]$d.per_amount } else { 1 }
        $unit    = if ($d.perc -eq $true) { '%' } else { '' }
        if ($addTo -and $adder) {
            $r.f.formula = "$addTo$unit per $per $adder"
            [void]$r.facts.Add((New-Fact 'Formula' $r.f.formula))
        }
        if ($d.ele) { [void]$r.facts.Add((New-Fact 'Element' ([string]$d.ele))) }
        if ($d.scale -and $d.scale -ne 'NONE') { [void]$r.facts.Add((New-Fact 'Scaling' (ConvertTo-TitleCase $d.scale))) }
        [void]$r.keys.Add((Reg-Key 'gui:no' 'Hidden from GUI' 'rarity' 'vis'))
    }
    [void]$rows.Add($r)
}
Save-Page 'stats' @($rows)

Write-Host ""
Write-Host "done -> $OUT"
