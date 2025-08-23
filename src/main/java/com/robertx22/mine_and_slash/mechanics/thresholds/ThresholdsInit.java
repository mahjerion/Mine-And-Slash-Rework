package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = SlashRef.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ThresholdsInit {
    private ThresholdsInit() {}

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent e) {
        e.enqueueWork(SpendThresholdManager::registerDefaults);
        System.out.println("[SpendThresholds] defaults registered");
    }
}
