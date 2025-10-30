package com.robertx22.mine_and_slash.vanilla_mc.commands.report;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.structure.DungeonMapData;
import com.robertx22.dungeon_realm.structure.DungeonMapStructure;
import com.robertx22.library_of_exile.dimension.structure.dungeon.BuiltRoom;
import com.robertx22.library_of_exile.dimension.structure.dungeon.DungeonBuilder;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static net.minecraft.commands.Commands.literal;

public class ReportMapIssue {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("report").requires(e -> e.hasPermission(0))
                                .then(literal("map_bug")
                                        .executes(ctx -> run(ctx.getSource())))));
    }


    private static int run(CommandSourceStack source) {

        try {

            if (source.getEntity() instanceof ServerPlayer p) {


                String text = "Map Bug Report, Problem Room: ";

                Optional<DungeonMapData> dungeonMapData = DungeonMain.ifMapData(p.level(), p.getOnPos());
                if (dungeonMapData.isEmpty()) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.RED + "You must be standing in a dungeon to use this command."));
                    return 1;
                }

                DungeonBuilder builder = new DungeonBuilder(DungeonMapStructure.dungeonSettings(p.chunkPosition(), dungeonMapData.get().dungeon));
                builder.build();
                BuiltRoom room = builder.builtDungeon.getRoomForChunk(DungeonMain.MAIN_DUNGEON_STRUCTURE, p.chunkPosition());

                text += room.room.loc.toString();

                p.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "-----------------------"));
                p.sendSystemMessage(Component.literal(ChatFormatting.AQUA + "Please make sure the problem is in the same chunk, or stand in the same spot as the map problem when using the command."));
                p.sendSystemMessage(Component.literal(ChatFormatting.RED + text));
                p.sendSystemMessage(Component.literal(ChatFormatting.YELLOW + "" + ChatFormatting.BOLD + "Click Here to Copy Text. Then Paste the text in a bug report.")
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))));
                p.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "-----------------------"));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }
}

