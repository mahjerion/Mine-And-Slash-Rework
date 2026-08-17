package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.capability.player.container.BackpackMenu;
import com.robertx22.mine_and_slash.capability.player.container.JewelsMenu;
import com.robertx22.mine_and_slash.capability.player.container.SkillGemsMenu;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks.BackpackType;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.profession.screen.CraftingStationMenu;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import java.util.HashMap;
import java.util.function.Supplier;

public class SlashContainers {
    public static HashMap<String, RegObj<MenuType<CraftingStationMenu>>> STATIONS = new HashMap<>();
    public static HashMap<BackpackType, RegObj<MenuType<BackpackMenu>>> BACKPACK_TABS = new HashMap<>();

    public static void init() {


        addStation(Professions.SALVAGING, () -> IForgeMenuType.create((x, y, z) -> new CraftingStationMenu(Professions.SALVAGING, x, y)));
        addStation(Professions.GEAR_CRAFTING, () -> IForgeMenuType.create((x, y, z) -> new CraftingStationMenu(Professions.GEAR_CRAFTING, x, y)));
        addStation(Professions.ALCHEMY, () -> IForgeMenuType.create((x, y, z) -> new CraftingStationMenu(Professions.ALCHEMY, x, y)));
        addStation(Professions.COOKING, () -> IForgeMenuType.create((x, y, z) -> new CraftingStationMenu(Professions.COOKING, x, y)));
        addStation(Professions.INFUSING, () -> IForgeMenuType.create((x, y, z) -> new CraftingStationMenu(Professions.INFUSING, x, y)));

        addBackpackTab(BackpackType.GEARS);
        addBackpackTab(BackpackType.MAPS);
        addBackpackTab(BackpackType.CURRENCY);
        addBackpackTab(BackpackType.SOCKETABLE);
        addBackpackTab(BackpackType.SKILL_GEMS);
        addBackpackTab(BackpackType.PROFESSION);
    }

    static void addStation(String prof, Supplier<MenuType<CraftingStationMenu>> sup) {
        STATIONS.put(prof, Def.container(prof, sup));
    }

    static void addBackpackTab(BackpackType type, Supplier<MenuType<BackpackMenu>> sup) {
        BACKPACK_TABS.put(type, Def.container("backpack_" + type.id, sup));
    }

    static void addBackpackTab(BackpackType type) {
        addBackpackTab(type, () -> IForgeMenuType.create((id, pInv, buf) -> new BackpackMenu(type, id, pInv)));
    }

    public static RegObj<MenuType<SkillGemsMenu>> SKILL_GEMS = Def.container("runeword", () -> IForgeMenuType.create((x, y, z) -> new SkillGemsMenu(x, y)));

    public static RegObj<MenuType<JewelsMenu>> JEWEL = Def.container("jewel", () -> IForgeMenuType.create((id, pInv, buf) -> new JewelsMenu(id, pInv)));

}
