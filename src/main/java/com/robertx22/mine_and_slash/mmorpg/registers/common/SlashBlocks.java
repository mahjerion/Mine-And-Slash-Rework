package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlock;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.addons.dungeon_realm.BonusEncounterTopperBlock;
import com.robertx22.addons.dungeon_realm.ImprisonedMonsterBlock;
import com.robertx22.addons.dungeon_realm.ShrineBlock;
import com.robertx22.addons.dungeon_realm.StrongboxBlock;
import com.robertx22.mine_and_slash.prophecy.ProphecyAltarBlock;
import com.robertx22.mine_and_slash.vanilla_mc.blocks.BlackHoleBlock;
import com.robertx22.mine_and_slash.vanilla_mc.blocks.TotemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashMap;

public class SlashBlocks {


    public static RegObj<BlackHoleBlock> BLACK_HOLE = Def.block("black_hole", () -> new BlackHoleBlock());
    public static RegObj<TotemBlock> BLUE_TOTEM = Def.block("blue_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> GREEN_TOTEM = Def.block("green_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> GUARD_TOTEM = Def.block("guard_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> PROJECTILE_TOTEM = Def.block("attack_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> THORN_BUSH = Def.block("thorn_bush", () -> new TotemBlock());
    public static RegObj<TotemBlock> MAGMA_FLOWER = Def.block("magma_flower", () -> new TotemBlock());
    public static RegObj<TotemBlock> FROST_FLOWER = Def.block("frost_flower", () -> new TotemBlock());
    public static RegObj<TotemBlock> TRAP = Def.block("trap", () -> new TotemBlock());
    public static RegObj<TotemBlock> GLYPH = Def.block("glyph", () -> new TotemBlock());
    //    public static RegObj<LeagueTeleportBlock> HARVEST_TELEPORT = Def.block("harvest_teleport", () -> new LeagueTeleportBlock(LeagueMechanics.HARVEST_ID));
    public static RegObj<ProphecyAltarBlock> PROPHECY_ALTAR = Def.block("prophecy_altar", () -> new ProphecyAltarBlock());
    public static RegObj<StrongboxBlock> STRONGBOX = Def.block("strongbox", () -> new StrongboxBlock());
    public static RegObj<ImprisonedMonsterBlock> IMPRISONED_MONSTER = Def.block("imprisoned_monster", () -> new ImprisonedMonsterBlock());
    public static RegObj<ShrineBlock> SHRINE = Def.block("shrine", () -> new ShrineBlock());
    public static RegObj<BonusEncounterTopperBlock> STRONGBOX_TOP = Def.block("strongbox_top", () -> new BonusEncounterTopperBlock(() -> SlashBlocks.STRONGBOX.get(), BlockBehaviour.Properties.copy(Blocks.CHEST).noOcclusion().lightLevel(x -> 7)));
    public static RegObj<BonusEncounterTopperBlock> IMPRISONED_MONSTER_TOP = Def.block("imprisoned_monster_top", () -> new BonusEncounterTopperBlock(() -> SlashBlocks.IMPRISONED_MONSTER.get(), BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).noOcclusion().lightLevel(x -> 7)));
    public static RegObj<BonusEncounterTopperBlock> SHRINE_TOP = Def.block("shrine_top", () -> new BonusEncounterTopperBlock(() -> SlashBlocks.SHRINE.get(), BlockBehaviour.Properties.copy(Blocks.LODESTONE).noOcclusion().lightLevel(x -> 10)));


    public static HashMap<String, RegObj<ProfessionBlock>> STATIONS = new HashMap<>();

    public static void init() {


        for (String p : Professions.STATION_PROFESSIONS) {
            STATIONS.put(p, Def.block("station/" + p, () -> new ProfessionBlock(p)));
        }
    }

}
