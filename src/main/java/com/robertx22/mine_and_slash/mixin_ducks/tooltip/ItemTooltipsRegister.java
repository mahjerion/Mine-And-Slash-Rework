package com.robertx22.mine_and_slash.mixin_ducks.tooltip;

import com.robertx22.library_of_exile.tooltip.ExileTooltipUtils;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemTooltipsRegister {

    public static void init() {

        ItemTooltip.register(new BaseItemTooltip(() -> SlashItems.MAP_DEVICE.get()) {
            @Override
            public void renderTooltip(Player p, List<Component> tip, ItemStack stack) {
                tip.addAll(ExileTooltipUtils.splitLongText(Chats.MAP_DEVICE_DESC.locName().withStyle(ChatFormatting.AQUA)));
            }
        });

        ItemTooltip.register(new BaseItemTooltip(() -> SlashItems.MAP_SETTER.get()) {
            @Override
            public void renderTooltip(Player p, List<Component> tip, ItemStack stack) {
                tip.addAll(ExileTooltipUtils.splitLongText(Chats.MAP_SET_DESC.locName().withStyle(ChatFormatting.AQUA)));
            }
        });

    }
}
