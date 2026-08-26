package com.robertx22.mine_and_slash.vanilla_mc.new_commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.library_of_exile.command_wrapper.CommandBuilder;
import com.robertx22.library_of_exile.command_wrapper.IntWrapper;
import com.robertx22.library_of_exile.command_wrapper.PermWrapper;
import com.robertx22.library_of_exile.command_wrapper.PlayerWrapper;
import com.robertx22.library_of_exile.command_wrapper.RegistryWrapper;
import com.robertx22.library_of_exile.command_wrapper.StringWrapper;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.stats.CoreStat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryStorageData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MercenaryCommands {

    public static void init(CommandDispatcher dis) {

        // /mine_and_slash merc set level <player> <n>
        CommandBuilder.of(CommandRefs.ID, dis, x -> {
            PlayerWrapper player = new PlayerWrapper();
            IntWrapper level = new IntWrapper("level");

            x.addLiteral("merc", PermWrapper.OP);
            x.addLiteral("set", PermWrapper.OP);
            x.addLiteral("level", PermWrapper.OP);

            x.addArg(player);
            x.addArg(level);

            x.action(e -> {
                Player p = player.get(e);
                MercenaryData data = Load.player(p).mercs.getActive();

                // the cap is a rule of the system, not of the command - a mercenary above its owner
                // would be clamped straight back down by MercenaryManager on the next tick anyway.
                int max = Load.Unit(p).getLevel();
                data.lvl = Math.max(1, Math.min(level.get(e), max));
                data.exp = 0;

                // a lower level can lock a support slot or shrink Spirit, so hand back anything that
                // no longer fits rather than leaving it equipped but inert
                MercenaryManager.validateEquipment(p, data);
                sync(p);
                p.sendSystemMessage(Component.literal("Mercenary level set to " + data.lvl));
            });
        }, "Sets the level of a player's active mercenary. Capped at the player's own level.");

        // /mine_and_slash merc set stat <player> <core_stat> <n>
        CommandBuilder.of(CommandRefs.ID, dis, x -> {
            PlayerWrapper player = new PlayerWrapper();
            StringWrapper stat = new StringWrapper("core_stat", MercenaryCommands::coreStatIds);
            IntWrapper amount = new IntWrapper("amount");

            x.addLiteral("merc", PermWrapper.OP);
            x.addLiteral("set", PermWrapper.OP);
            x.addLiteral("stat", PermWrapper.OP);

            x.addArg(player);
            x.addArg(stat);
            x.addArg(amount);

            x.action(e -> {
                Player p = player.get(e);
                String id = stat.get(e);

                if (!ExileDB.Stats().isRegistered(id) || !(ExileDB.Stats().get(id) instanceof CoreStat)) {
                    p.sendSystemMessage(Component.literal("Not a core stat: " + id));
                    return;
                }
                MercenaryData data = Load.player(p).mercs.getActive();

                // per mercenary, not on the shared class profile - that is registry content and would
                // change every player's mercenary until the next datapack reload wiped it.
                data.getStatOverrides().put(id, (float) amount.get(e));

                sync(p);
                p.sendSystemMessage(Component.literal("Mercenary " + id + " set to " + amount.get(e)));
            });
        }, "Overrides a core stat on this player's active mercenary. Use /mine_and_slash merc reset to clear.");

        // /mine_and_slash merc set class <player> <mercenary>
        CommandBuilder.of(CommandRefs.ID, dis, x -> {
            PlayerWrapper player = new PlayerWrapper();
            RegistryWrapper<MercenaryClass> merc = new RegistryWrapper<>(ExileRegistryTypes.MERCENARY);

            x.addLiteral("merc", PermWrapper.OP);
            x.addLiteral("set", PermWrapper.OP);
            x.addLiteral("class", PermWrapper.OP);

            x.addArg(player);
            x.addArg(merc);

            x.action(e -> {
                Player p = player.get(e);
                String id = merc.get(e);
                if (!ExileDB.Mercenaries().isRegistered(id)) {
                    return;
                }
                MercenaryManager.switchTo(p, id);
                p.sendSystemMessage(Component.literal("Mercenary class set to " + id));
            });
        }, "Switches a player's active mercenary class.");

        // /mine_and_slash merc reset <player>
        CommandBuilder.of(CommandRefs.ID, dis, x -> {
            PlayerWrapper player = new PlayerWrapper();

            x.addLiteral("merc", PermWrapper.OP);
            x.addLiteral("reset", PermWrapper.OP);

            x.addArg(player);

            x.action(e -> {
                Player p = player.get(e);
                reset(p);
                p.sendSystemMessage(Component.literal("Mercenaries reset. Equipped items were returned to you."));
            });
        }, "Fully resets a player's mercenaries. Everything equipped is handed back, nothing is destroyed.");
    }

    /**
     * Wipes every mercenary back to level 1 with nothing equipped. Items are always handed back - a
     * reset must never eat gear, which is why this goes through the same return path the gui uses.
     */
    public static void reset(Player p) {
        PlayerData data = Load.player(p);

        for (MercenaryData merc : data.mercs.map.values()) {
            if (merc == null) {
                continue;
            }
            for (MercenarySlotType type : MercenarySlotType.values()) {
                var inv = type.inventoryOf(merc);
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    var stack = inv.getItem(i);
                    if (!stack.isEmpty()) {
                        inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                        MercenarySlotType.giveBack(p, stack);
                    }
                }
            }
        }

        MercenaryManager.dismiss(p);
        data.mercs = new MercenaryStorageData();
        MercenaryManager.requestRespawn(p);

        sync(p);
    }

    private static List<String> coreStatIds() {
        return ExileDB.Stats().getList().stream()
                .filter(s -> s instanceof CoreStat)
                .map(s -> s.GUID())
                .collect(Collectors.toList());
    }

    private static void sync(Player p) {
        Load.Unit(p).setEquipsChanged();
        MercenaryManager.refreshGear(p);
        Load.player(p).playerDataSync.setDirtyAndSync(p);
    }
}
