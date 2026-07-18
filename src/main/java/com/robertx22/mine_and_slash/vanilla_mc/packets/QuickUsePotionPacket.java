package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.vanilla_mc.items.SlashPotionItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import com.robertx22.mine_and_slash.database.data.profession.LeveledItem;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuickUsePotionPacket extends MyPacket<QuickUsePotionPacket> {
    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("quick_use_potion");
    }

    @Override
    public void loadFromData(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void saveToData(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void onReceived(ExilePacketContext exilePacketContext) {
        if (exilePacketContext.getPlayer() instanceof ServerPlayer player) {
            int playerLevel = Load.Unit(player).getLevel();

            List<ItemStack> potionItems = new ArrayList<>();
            for (Slot slot : player.inventoryMenu.slots) {
                if (slot.getItem().getItem() instanceof SlashPotionItem) {
                    potionItems.add(slot.getItem());
                }
            }

            potionItems.stream()
                    .filter(x -> !player.getCooldowns().isOnCooldown(x.getItem()))
                    .map(x -> Pair.of(x, ((SlashPotionItem) x.getItem())))
                    .collect(Collectors.groupingBy(x -> x.getRight().getType()))
                    .values()
                    .stream()
                    // within each type, sort strongest-first, then take the strongest one the player can actually use
                    .map(v -> v.stream()
                            .sorted((p1, p2) -> -Float.compare(
                                    p1.getRight().getType().getHealPercent(p1.getKey()),
                                    p2.getRight().getType().getHealPercent(p2.getKey())))
                            .filter(p -> LeveledItem.getLevel(p.getKey()) <= playerLevel)
                            .findFirst())
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .map(x -> x.getRight().handlePotionRestore(player, x.getLeft()))
                    .filter(x -> x)
                    .findFirst()
                    .ifPresentOrElse(x -> SoundUtils.playSound(player, SoundEvents.VILLAGER_YES), () -> SoundUtils.playSound(player, SoundEvents.VILLAGER_NO));
        }
    }

    @Override
    public MyPacket<QuickUsePotionPacket> newInstance() {
        return new QuickUsePotionPacket();
    }
}
