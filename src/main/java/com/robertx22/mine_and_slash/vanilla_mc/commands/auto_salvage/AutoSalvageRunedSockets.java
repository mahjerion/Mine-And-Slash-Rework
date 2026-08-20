package com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// the command form of the Runed Sockets button on the salvage screen
public class AutoSalvageRunedSockets {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("auto_salvage")
                                .then(literal("runed_sockets")
                                        // no upper bound here: commands register before a datapack reload can
                                        // change what the runed rarities roll, so setRunedMinSockets clamps instead
                                        .then(argument("amount", IntegerArgumentType.integer(0))
                                                .executes(e -> execute(e.getSource(),
                                                        e.getSource().getPlayerOrException(),
                                                        IntegerArgumentType.getInteger(e, "amount")
                                                )))))
        );
    }

    private static int execute(CommandSourceStack commandSource, Player player, int amount) {

        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 0;
            }
        }

        PlayerConfigData config = Load.player(player).config;

        config.salvage.setRunedMinSockets(amount);

        Load.player(player).playerDataSync.setDirtyAndSync(player);

        int set = config.salvage.getRunedMinSockets();

        if (set < 1) {
            player.sendSystemMessage(Component.literal("Runed socket filtering is now off.").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("Runed gear with fewer than " + set + " sockets will now be salvaged on pickup.").withStyle(ChatFormatting.GREEN));
        }

        if (set != amount) {
            player.sendSystemMessage(Component.literal("(" + amount + " was clamped, the most sockets any runed rarity rolls is " + GearRarity.maxRunedSockets() + ")").withStyle(ChatFormatting.GRAY));
        }

        return 1;
    }
}
