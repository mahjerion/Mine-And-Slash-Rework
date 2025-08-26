package com.robertx22.mine_and_slash.itemstack;

import com.robertx22.mine_and_slash.gui.texts.textblocks.AdditionalBlock;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class CommonTooltips {

    public static AdditionalBlock potentialCorruptionAndQuality(ExileStack exStack, boolean doQuality) {

        List<MutableComponent> list = new ArrayList<>();

        MutableComponent potential = Itemtips.POTENTIAL.locName(exStack.get(StackKeys.POTENTIAL).getOrCreate().potential);

        if (exStack.isCorrupted() || exStack.isMirrored()) {
            potential = Component.literal("").append(potential.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));

            if (exStack.isCorrupted()) {
                potential.append(Component.literal(" ")).append(Words.Corrupted.locName().withStyle(ChatFormatting.RED));
            }
            if (exStack.isMirrored()) {
                potential.append(Component.literal(" ")).append(Words.Mirrored.locName().withStyle(ChatFormatting.WHITE));
            }
        } else {
            potential.withStyle(ChatFormatting.GOLD);
        }

        list.add(potential);

        if (doQuality) {
            list.add(Itemtips.QUALITY.locName(exStack.get(StackKeys.CUSTOM).getOrCreate().data.get(CustomItemData.KEYS.QUALITY)).withStyle(ChatFormatting.GOLD));
        }
        return new AdditionalBlock(list).showWhen(() -> Screen.hasShiftDown());
    }

    public static AdditionalBlock craftedItem(ExileStack exStack) {
        List<MutableComponent> all = new ArrayList<>();
        if (exStack.get(StackKeys.CUSTOM).hasAndTrue(x -> x.data.get(CustomItemData.KEYS.CRAFTED))) {
            all.add(Words.CRAFTED.locName().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return new AdditionalBlock(all).showWhen(() -> Screen.hasShiftDown());
    }

}
