package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mechanics.thresholds.ui.ThresholdUiClient;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ThresholdUiPacket extends MyPacket<ThresholdUiPacket> {

    public String key = "";
    public String resourceId = "";
    public boolean show = false;
    public float progress = 0f;

    public ThresholdUiPacket() {
    }

    public ThresholdUiPacket(String key, String resourceId, boolean show, float progress) {
        this.key = key == null ? "" : key;
        this.resourceId = resourceId == null ? "" : resourceId;
        this.show = show;
        this.progress = progress;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "threshold_ui");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.key = buf.readUtf(256);
        this.resourceId = buf.readUtf(64);
        this.show = buf.readBoolean();
        this.progress = buf.readFloat();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(key);
        buf.writeUtf(resourceId);
        buf.writeBoolean(show);
        buf.writeFloat(progress);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        ThresholdUiClient.applyUpdate(key, resourceId, show);
        if (show) {
            ThresholdUiClient.setProgress(key, progress);
        }
    }

    @Override
    public MyPacket<ThresholdUiPacket> newInstance() {
        return new ThresholdUiPacket();
    }
}

