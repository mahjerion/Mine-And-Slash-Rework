package com.robertx22.mine_and_slash.vanilla_mc.commands.giveitems;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.library_of_exile.database.league.LibLeagues;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.bases.UniqueGearPart;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions.DatabaseSuggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class GiveExactUnique {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {

        commandDispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("give").requires(e -> e.hasPermission(2))
                                .then(literal("unique_gear")
                                        .requires(e -> e.hasPermission(2))
                                        .then(argument("target", EntityArgument.player())
                                                .then(argument("uniqueID", StringArgumentType.word())
                                                        .suggests(new DatabaseSuggestions(ExileRegistryTypes.UNIQUE_GEAR, "random"))
                                                        .then(argument("level", IntegerArgumentType.integer())
                                                                .then(argument("amount", IntegerArgumentType
                                                                        .integer(1, 5000))
                                                                        .executes(e -> execute(e.getSource(), EntityArgument
                                                                                .getPlayer(e, "target"), StringArgumentType
                                                                                .getString(e, "uniqueID"), IntegerArgumentType
                                                                                .getInteger(e, "level"), IntegerArgumentType
                                                                                .getInteger(e, "amount")

                                                                        )))))))));
    }

    private static int execute(CommandSourceStack commandSource, Player player,
                               String id, int lvl, int amount) {

        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 1;
            }
        }

        boolean isRandom = id.equals("random");

        // an explicit id is a deliberate admin action, so it stays permissive - league-locked and
        // retired uniques can still be handed out by typing their exact id. Only the random roll is gated.
        UniqueGear exact = null;
        if (!isRandom) {
            // the registry returns its (nonexistent) default rather than erroring on an unknown guid
            exact = ExileDB.UniqueGears().get(id);
            if (exact == null) {
                commandSource.sendFailure(Component.literal("No unique gear with id: " + id));
                return 1;
            }
        }

        for (int i = 0; i < amount; i++) {
            GearBlueprint blueprint = new GearBlueprint(LootInfo.ofLevel(lvl));
            blueprint.level.set(lvl);

            if (!isRandom) {
                blueprint.rarity.set(ExileDB.GearRarities().get(exact.rarity));
                blueprint.uniquePart.set(exact);
                blueprint.gearItemSlot.set(exact.getBaseGear());
            } else {
                GearRarity rar = ExileDB.GearRarities()
                        .getFilterWrapped(x -> x.is_unique_item)
                        .random();

                // roll through the same filter the loot pipeline uses, so this can't hand out
                // league-locked or retired uniques. Tier is deliberately unbounded: the command has no
                // map context (LootInfo.ofLevel leaves map_tier at 0), so gating on it would exclude
                // every high-tier unique rather than gate anything.
                UniqueGear uniq = UniqueGearPart.eligibleUniques(
                        rar.GUID(), Integer.MAX_VALUE, lvl, LibLeagues.INSTANCE.EMPTY.get()).random();

                if (uniq == null) {
                    commandSource.sendFailure(Component.literal(
                            "No unique gear is eligible at level " + lvl + " (league-locked and retired uniques are excluded)."));
                    return 1;
                }

                blueprint.rarity.set(rar);
                blueprint.uniquePart.set(uniq);
                blueprint.gearItemSlot.set(uniq.getBaseGear());
            }

            player.addItem(blueprint.createStack());
        }

        return 0;
    }
}
