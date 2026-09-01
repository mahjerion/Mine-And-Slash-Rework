package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * The sticker book's entry point, on the salvaging station.
 * <p>
 * Carries the station's position through to the screen, because reconstructing a unique has to happen
 * at the station and the server checks that position rather than trusting the click.
 * <p>
 * The position is read on press, not on construction: StationSyncData is a static the server pushes,
 * and the screen's init() can run before the first sync packet lands. Reading it early would hand the
 * book a stale position and every craft would come back "you must be at a Salvaging Station". Same
 * reason CraftButton reads it inside its own press handler.
 */
public class OpenUniqueBookButton extends Button {

    public static final int SIZE = 18;

    public OpenUniqueBookButton(int x, int y, Supplier<BlockPos> station) {
        super(x, y, SIZE, SIZE, Component.literal("★"),
                b -> Minecraft.getInstance().setScreen(new UniqueCollectionScreen(station.get())),
                DEFAULT_NARRATION);
        this.setTooltip(Tooltip.create(Words.UNIQUE_COLLECTION.locName()));
    }
}
