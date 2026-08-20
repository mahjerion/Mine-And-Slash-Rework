package com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

import static net.minecraft.commands.Commands.literal;

public class AutoSalvageHelp {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("auto_salvage")
                                .then(literal("help")
                                        .executes(e -> execute(e.getSource(), e.getSource().getPlayerOrException()))
                                ))
        );
    }

    private static int execute(CommandSourceStack commandSource, Player player) {

        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 0;
            }
        }

        player.sendSystemMessage(Component.literal("/mine_and_slash auto_salvage").withStyle(ChatFormatting.GREEN).append(Component.literal(" commands are used to configure more advanced auto-salvaging.").withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Sometimes you want to auto salvage all common support gems except a particular type you want in any rarity. Maybe you don't use shields, so you want to auto salvage all shields, regardless of rarity. These commands help you do that.").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("/mine_and_slash auto_salvage list <mmorpg_support_gem | mmorpg_base_gear_types> <search_query> (optional)").withStyle(ChatFormatting.GREEN).append(Component.literal(" will show you a list of all possible types for gear types or support gems. You can use an optional search query as a final parameter to search for a specific id.").withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("/mine_and_slash auto_salvage show <mmorpg_support_gem | mmorpg_base_gear_types> <search_query> (optional)").withStyle(ChatFormatting.GREEN).append(Component.literal(" will show you a list of all your currently configured options for gear types or support gems. You can use an optional search query as a final parameter to search for a specific id.").withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("/mine_and_slash auto_salvage config <mmorpg_support_gem | mmorpg_base_gear_types> <id> <enable | disable | clear> <rarity> (optional)").withStyle(ChatFormatting.GREEN).append(Component.literal(" lets you actually configure advanced auto salvaging. Advanced auto salvaging configs take precedence over the rarity config you can configure on the Salvaging configuration screen.").withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("Gear types are things like sword, staff, cloth_chest or plate_boots, so you can keep staves while salvaging swords, or keep plate while salvaging cloth. Leaving the rarity out applies the action to every rarity. The same settings are editable on the Salvaging screen.").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("/mine_and_slash auto_salvage runed_sockets <amount>").withStyle(ChatFormatting.GREEN).append(Component.literal(" salvages runed gear that has fewer than that many sockets, whatever the other filters say. Gear that has enough sockets is left to the rarity and gear type filters. Use 0 to turn it off. The same setting is the Runed Sockets button on the Salvaging screen.").withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("/mine_and_slash auto_salvage map_layout <layout> <enable | disable>").withStyle(ChatFormatting.GREEN).append(Component.literal(" salvages every map of that layout on pickup, at any rarity. Uber and Pinnacle maps are never salvaged this way. The same settings are the Map Layouts page on the Salvaging screen.").withStyle(ChatFormatting.WHITE)));


        return 1;

    }

}
