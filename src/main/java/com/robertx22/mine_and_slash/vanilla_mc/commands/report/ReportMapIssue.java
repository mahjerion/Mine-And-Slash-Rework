package com.robertx22.mine_and_slash.vanilla_mc.commands.report;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.structure.DungeonMapCapability;
import com.robertx22.dungeon_realm.structure.DungeonMapData;
import com.robertx22.library_of_exile.dimension.structure.dungeon.BuiltDungeon;
import com.robertx22.library_of_exile.dimension.structure.dungeon.BuiltRoom;
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


                Optional<DungeonMapData> dungeonMapData = DungeonMain.ifMapData(p.level(), p.getOnPos());
                if (dungeonMapData.isEmpty()) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.RED + "You must be standing in a dungeon to use this command."));
                    return 1;
                }

                // reproduce the real generated layout: seed from the dungeon's start chunk (not the
                // player's current chunk) and reuse the shared built grid, so the reported room matches
                // what's actually placed - including rooms larger than one chunk.
                var struc = DungeonMain.MAIN_DUNGEON_STRUCTURE;
                var start = struc.getStartChunkPos(p.chunkPosition());

                // This command resolves the layout LIVE, but the blocks under the player were written
                // once, when their chunk generated, and can never be rewritten. So "what it should be"
                // and "what it is" are answers about different moments, and printing only the first is
                // what made this report say 'watcher' while the floor was a nature room. Print the
                // whole chain so a report is conclusive on its own.
                String wanted = dungeonMapData.get().dungeon;
                boolean registered = DungeonDatabase.Dungeons().isRegistered(wanted);
                var worldData = DungeonMapCapability.get(p.level()).data;
                String placeholder = worldData.generatedDungeonAtStart.get(worldData.data.getKey(start));

                // getCachedDungeon, not getBuiltDungeon: this command is permission 0, and
                // getBuiltDungeon either builds a grid on the server thread or evicts the cached one.
                // Reporting is not worth handing players a way to drive either. Everything that makes
                // this report conclusive is below, and none of it needs a layout.
                var builder = struc.getMap(start);
                BuiltDungeon built = struc.getCachedDungeon(start);
                BuiltRoom room = built == null ? null : built.getRoomForChunk(struc, p.chunkPosition());

                String text = "Map Bug Report"
                        + " | instance: " + start
                        + " | chunk: " + p.chunkPosition()
                        + " | map wants dungeon: '" + wanted + "'"
                        + " | registered: " + registered
                        + " | resolved from map data: " + builder.resolvedFromMapData
                        + " | placeholder ever used here: " + (placeholder == null ? "no" : "'" + placeholder + "'")
                        + " | room the layout says: " + (built == null ? "layout not built yet"
                                : room == null ? "none (chunk is outside the room grid)" : room.room.loc.toString());

                if (placeholder != null && !placeholder.equals(wanted)) {
                    // this is the bug, named. the ground was carved from a guess that the map data
                    // later disagreed with, and a carved chunk is permanent.
                    text += " | MISMATCH: some chunks of this instance were carved as '" + placeholder
                            + "' before its map data could be read.";
                }

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

