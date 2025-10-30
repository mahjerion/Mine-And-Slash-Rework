package com.robertx22.mine_and_slash.vanilla_mc.commands.entity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.MasterLootGen;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


public class GiveLoot {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("give").requires(e -> e.hasPermission(2))
                                .then(literal("loot")
                                        .requires(e -> e.hasPermission(2))
                                        .then(argument("target", EntityArgument.player())
                                            .then(argument("source", EntityArgument.entity())
                                                .then(argument("count", IntegerArgumentType.integer())
                                                    .then(argument("log_only", IntegerArgumentType.integer())
                                                        .then(argument("filter", StringArgumentType.string())
                                                            .executes(ctx -> run(
                                                                EntityArgument.getPlayer(ctx, "target"),
                                                                EntityArgument.getEntity(ctx, "source"),
                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                IntegerArgumentType.getInteger(ctx, "log_only") != 0,
                                                                StringArgumentType.getString(ctx, "filter")
                                                            ))))))))));
    }

    private static int run(Player player, Entity source, int count, boolean logOnly, String filter) {

        if (!(source instanceof LivingEntity living)) {
            player.sendSystemMessage(Component.literal("Source must be a living entity"));
            return 0;
        }

        LootInfo lootInfo = LootInfo.ofMobKilled(player, living);
        lootInfo.gatherLootMultipliers();

        Predicate<ItemStack> predicate = s -> s.getItem().toString().contains(filter);

        try {
            if (logOnly) {
                logItems(player, lootInfo, count, predicate);
            } else {
                dropItems(player, lootInfo, count, predicate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }

    private static void dropItems(Player player, LootInfo lootInfo, int count, Predicate<ItemStack> filter) {
        for (int i = 0; i < count; i++) {
            List<ItemStack> loot = MasterLootGen.generateLoot(lootInfo);
            for (ItemStack stack : loot) {
                if (filter.test(stack)) {
                    PlayerUtils.giveItem(stack, player);
                }
            }
        }
    }

    private static void logItems(Player player, LootInfo lootInfo, int count, Predicate<ItemStack> filter) {
        Map<Item, Integer> itemCounts = new HashMap<Item, Integer>();

        for (int i = 0; i < count; i++) {
            List<ItemStack> loot = MasterLootGen.generateLoot(lootInfo);
            for (ItemStack stack : loot) {
                if (!filter.test(stack)) {
                    continue;
                }
                Item item = stack.getItem();
                itemCounts.put(item, itemCounts.getOrDefault(item, 0) + stack.getCount());
            }
        }

        List<Map.Entry<Item, Integer>> list = new LinkedList(itemCounts.entrySet());
        list.sort(Comparator.comparingInt(e -> e.getValue()));

        player.sendSystemMessage(Component.literal("Loot:").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        for (var entry : list) {
            ResourceLocation itemResource = BuiltInRegistries.ITEM.getKey(entry.getKey());
            String itemString = itemResource.getNamespace() + ":" + itemResource.getPath();
            player.sendSystemMessage(Component.literal(itemString + ": " + entry.getValue()));
        }
    }
}