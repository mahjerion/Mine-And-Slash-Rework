package com.robertx22.mine_and_slash.gui.screens.map;

import com.robertx22.dungeon_realm.client.DungeonStatsStore;
import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.library_of_exile.database.map_finish_rarity.MapFinishRarity;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.util.UNICODE;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRequirement.CHECK_YES_ICON;

public class MapBarButton extends ImageButton {

    public static int BAR_WIDTH = 228;
    public static int BAR_HEIGHT = 13;
    static ResourceLocation BAR = new ResourceLocation(SlashRef.MODID, "textures/gui/map/map_bar.png");

    public MapBarButton(int xPos, int yPos) {
        super(xPos, yPos, BAR_WIDTH, BAR_HEIGHT, 0, 0, 0, new ResourceLocation(SlashRef.MODID, ""), (button) -> {
        });
    }

    @Override
    public void onPress() {

    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        int progressPercent = DungeonStatsStore.getRarityProgressPercent();
        float multi = progressPercent / 100F;
        int barWidthMultiplied = (int) (multi * BAR_WIDTH);
        gui.blit(BAR, this.getX(), this.getY(), barWidthMultiplied, BAR_HEIGHT, 0, 0, barWidthMultiplied, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        this.setTooltip(Tooltip.create(TextUTIL.mergeList(getBarTooltip())));
    }

    public List<Component> getBarTooltip() {
        List<Component> all = new ArrayList<>();

        // this is the same percent that actually gates rarity tier-ups and the boss teleport unlock
        // (DungeonMapData.calculateKillCompletionPercent) - keep this bar/tooltip driven by it so it
        // never disagrees with the rarity icon or the boss-unlock state
        int progressPercent = DungeonStatsStore.getRarityProgressPercent();

        if (DungeonStatsStore.isBossTeleportUnlocked()) {
            all.add(Chats.BOSS_ARENA_UNLOCKED.locName().withStyle(ChatFormatting.GREEN));
        } else {
            all.add(Chats.BOSS_LOCKED.locName(DungeonConfig.get().MAP_PERCENT_COMPLETE_NEEDED_FOR_BOSS_ARENA.get() + "%").withStyle(ChatFormatting.RED));
        }

        all.add(Chats.CURRENT_MAP_EXPLORATION_PERCENT.locName(progressPercent + "%").withStyle(ChatFormatting.YELLOW));
        all.add(Component.empty());

        List<MapFinishRarity> rarities = LibDatabase.MapFinishRarity().getList();
        rarities.sort((a, b) -> Integer.compare(a.perc_to_unlock, b.perc_to_unlock));

        for (MapFinishRarity rar : rarities) {
            MutableComponent tick = Component.literal(UNICODE.NO_ICON).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);

            if (progressPercent >= rar.perc_to_unlock) {
                tick = Component.literal(CHECK_YES_ICON).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            }
            tick.append(" : " + rar.perc_to_unlock + "%");

            all.add(Chats.MAP_EXPLORATION_RARITY.locName(rar.getTranslation(TranslationType.NAME).getTranslatedName().withStyle(rar.textFormatting()), tick).withStyle(ChatFormatting.YELLOW));
        }

        all.add(Component.empty());
        all.add(Chats.MAP_PROGRESS_HELP.locName().withStyle(ChatFormatting.BLUE));

        return all;
    }

}
