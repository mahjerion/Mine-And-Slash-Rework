package com.robertx22.mine_and_slash.gui.inv_gui.actions;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.CycleRunedSocketFilter;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.OpenGearSubFilterAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.OpenMapLayoutFilterAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ResetGearTypeSalvage;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleGearTypeSalvage;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleMapLayoutSalvage;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.map_device.MapDeviceEquipAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.mercenary.MercEquipAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.mercenary.MercPickSkillAction;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.library_of_exile.registry.IGUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class GuiAction<T> implements IGUID {


    private static HashMap<String, GuiAction> map = new HashMap<>();

    public static GuiAction get(String id) {
        if (!map.containsKey(id)) {
            regenActionMap();
        }

        return map.getOrDefault(id, new GuiAction() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {

            }

            @Override
            public Object loadExtraData(FriendlyByteBuf buf) {
                return null;
            }

            @Override
            public List<Component> getTooltip(Player p) {
                return Arrays.asList();
            }

            @Override
            public void doAction(Player p, Object d) {

            }

            @Override
            public void clientAction(Player p, Object obj) {

            }

            @Override
            public String GUID() {
                return "empty";
            }
        });
    }

    private static void of(GuiAction a) {
        if (!map.containsKey(a.GUID())) {
            map.put(a.GUID(), a);
        }
    }

    // do every time before constructing the gui, because we want to use datapack stuff and not care about when its loaded
    public static void regenActionMap() {
        // each group on its own so one action that fails to construct cannot silently truncate the
        // registry - that is exactly what happened when an action class dragged a client Screen onto
        // the dedicated server. Throwable on purpose: a failed class link is an Error, not an Exception
        register("config toggles", () -> {
            for (PlayerConfigData.Config v : PlayerConfigData.Config.values()) {
                of(new GuiConfigToggle(v));
            }
        });
        register("salvage rarity toggles", () -> {
            for (ToggleAutoSalvageRarity.SalvageType type : ToggleAutoSalvageRarity.SalvageType.values()) {
                for (GearRarity rar : ExileDB.GearRarities().getList()) {
                    of(new ToggleAutoSalvageRarity(type, rar));
                }
            }
        });
        register("salvage gear type toggles", () -> {
            for (BaseGearType gt : ExileDB.GearTypes().getList()) {
                of(new ResetGearTypeSalvage(gt));
                for (GearRarity rar : ExileDB.GearRarities().getList()) {
                    of(new ToggleGearTypeSalvage(gt, rar));
                }
            }
        });
        register("salvage sub filters", () -> {
            for (ToggleAutoSalvageRarity.SalvageType type : OpenGearSubFilterAction.TYPES) {
                of(new OpenGearSubFilterAction(type));
            }
            of(new CycleRunedSocketFilter());
            of(new OpenMapLayoutFilterAction());
        });
        register("map layout salvage toggles", () -> {
            // datapacked layouts are in this registry too, so they register with no extra code
            for (Dungeon layout : DungeonDatabase.Dungeons().getList()) {
                of(new ToggleMapLayoutSalvage(layout));
            }
        });
        register("spell picks", () -> {
            for (Spell rw : ExileDB.Spells().getList()) {
                of(new PickSpellAction(rw));
                of(new MercPickSkillAction(rw));
            }
        });
        register("mercenary equip", () -> {
            // the mercenary equip picker is keyed by player inventory slot - the action map is looked up
            // by GUID on the server, so the slot has to be part of the id rather than extra data.
            for (int i = 0; i < MercEquipAction.MAX_INVENTORY_SLOTS; i++) {
                of(new MercEquipAction(i));
            }
        });
        register("map device equip", () -> {
            // same idea for the map device's map and relic slots
            for (int i = 0; i < MapDeviceEquipAction.MAX_INVENTORY_SLOTS; i++) {
                of(new MapDeviceEquipAction(i));
            }
        });
    }

    private static void register(String group, Runnable adder) {
        try {
            adder.run();
        } catch (Throwable e) {
            ExileLog.get().error("Failed to register inv gui actions: " + group + ". The rest of the registry still loads, but these actions will do nothing.", e);
        }
    }


    public ResourceLocation getIcon() {
        return SlashRef.id("textures/gui/inv_gui/icons/" + GUID() + ".png");
    }

    public ResourceLocation getBackGroundIcon() {
        return null;
    }

    // a non empty stack is rendered instead of getIcon(), lets datapacked content have an icon without shipping a png
    public ItemStack getItemStackIcon() {
        return ItemStack.EMPTY;
    }

    // rgba multiplied onto getIcon(), used to show state without needing more textures
    public float[] getIconTint() {
        return new float[]{1F, 1F, 1F, 1F};
    }

    // true skips the packet to the server, for actions that only navigate the gui
    public boolean isClientOnly() {
        return false;
    }

    public abstract void saveExtraData(FriendlyByteBuf buf);

    public abstract T loadExtraData(FriendlyByteBuf buf);

    public abstract List<Component> getTooltip(Player p);

    public abstract void doAction(Player p, Object obj);

    public abstract void clientAction(Player p, Object obj);


}
