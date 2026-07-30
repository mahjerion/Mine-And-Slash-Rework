package com.robertx22.mine_and_slash.vanilla_mc.commands.giveitems;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions.GearRaritySuggestions;
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
                                        .then(argument("target", EntityArgument.player())
                                                .then(argument("level", IntegerArgumentType.integer()).then(argument(
                                                        "rarity", StringArgumentType.string()).suggests(new GearRaritySuggestions())
                                                        .then(argument("amount", IntegerArgumentType.integer(1, 5000)).executes(e -> execute(
                                                                e.getSource(), EntityArgument.getPlayer(e, "target"), IntegerArgumentType.getInteger(e, "level"), StringArgumentType.getString(e, "rarity"),
                                                                IntegerArgumentType.getInteger(e, "amount"), -1
                                                        ))
                                                                // optional trailing tier: the only way to get a specific high tier
                                                                // for testing, since tier is otherwise rolled. Overrides the rarity
                                                                // argument, because rarity is derived from tier.
                                                                .then(argument("tier", IntegerArgumentType.integer(0, 100)).executes(e -> execute(
                                                                        e.getSource(), EntityArgument.getPlayer(e, "target"), IntegerArgumentType.getInteger(e, "level"), StringArgumentType.getString(e, "rarity"),
                                                                        IntegerArgumentType.getInteger(e, "amount"), IntegerArgumentType.getInteger(e, "tier")
                                                                ))))))))));
    }

    private static int execute(CommandSourceStack commandSource, Player player, int lvl,
                               String rarity, int amount, int tier) {


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

            var data = b.createData();

            if (tier >= 0) {
                data.setTier(tier);
            }

            StackSaving.MAP.saveTo(mapStack, data);
            PlayerUtils.giveItem(mapStack, player);
        }

        return 0;
    }
}
