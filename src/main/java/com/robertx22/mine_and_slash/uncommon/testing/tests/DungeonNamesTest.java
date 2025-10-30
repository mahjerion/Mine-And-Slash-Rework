package com.robertx22.mine_and_slash.uncommon.testing.tests;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.mine_and_slash.uncommon.testing.CommandTest;
import com.robertx22.mine_and_slash.uncommon.testing.TestResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DungeonNamesTest extends CommandTest {
    @Override
    public TestResult runINTERNAL(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Map translation key - Value"));
        DungeonDatabase.Dungeons().getAll().forEach((id, dungeon) -> {
            String translationKey = DungeonWords.MapGUID(dungeon.id);
            player.sendSystemMessage(Component.literal(translationKey).append(Component.literal(" - ")).append(Component.translatable(translationKey)));
        });
        return TestResult.SUCCESS;
    }

    @Override
    public boolean shouldRunEveryLogin() {
        return true;
    }

    @Override
    public String GUID() {
        return "dungeon_names";
    }
}
