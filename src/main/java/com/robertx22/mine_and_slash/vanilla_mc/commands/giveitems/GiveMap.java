package com.robertx22.mine_and_slash.vanilla_mc.commands.giveitems;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions.GearRaritySuggestions;
import com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions.MapTypeSuggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GiveMap {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {

        commandDispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("give").requires(e -> e.hasPermission(2))
                                .then(literal("map")
                                        .then(argument("type", StringArgumentType.string()).suggests(new MapTypeSuggestions())
                                            .then(argument("target", EntityArgument.player())
                                                    .then(argument("level", IntegerArgumentType.integer()).then(argument(
                                                            "rarity", StringArgumentType.string()).suggests(new GearRaritySuggestions())
                                                            .then(argument("amount", IntegerArgumentType.integer(1, 5000)).executes(e -> execute(
                                                                    e.getSource(), EntityArgument.getPlayer(e, "target"), IntegerArgumentType.getInteger(e, "level"), StringArgumentType.getString(e, "rarity"),
                                                                    IntegerArgumentType.getInteger(e, "amount"), StringArgumentType.getString(e, "type")
                                                            ))))))))));
    }

    private static int execute(CommandSourceStack commandSource, Player player, int lvl,
                               String rarity, int amount, String type) {


        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 1;
            }
        }
        for (int i = 0; i < amount; i++) {
            MapBlueprint blueprint = new MapBlueprint(LootInfo.ofLevel(1));
            ItemStack mapStack = blueprint.createStack();

            MapBlueprint b = new MapBlueprint(LootInfo.ofLevel(lvl));
            b.level.set(lvl);

            if (ExileDB.GearRarities()
                    .isRegistered(rarity)) {
                b.rarity.set(ExileDB.GearRarities()
                        .get(rarity));
            }

            StackSaving.MAP.saveTo(mapStack, b.createData());

            if (!type.equals("random")) {
                DungeonItemMapData map = DungeonItemNbt.DUNGEON_MAP.loadFrom(mapStack);
                map.forced_dungeon_id = type;
                DungeonItemNbt.DUNGEON_MAP.saveTo(mapStack, map);
            }

            PlayerUtils.giveItem(mapStack, player);
        }

        return 0;
    }
}
