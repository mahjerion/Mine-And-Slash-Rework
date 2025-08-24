package com.robertx22.mine_and_slash.mechanics.thresholds.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.robertx22.mine_and_slash.mechanics.thresholds.SpendThresholdRegistry;
import com.robertx22.mine_and_slash.mechanics.thresholds.SpendThresholdManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpendThresholdsReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public SpendThresholdsReloadListener() {
        super(GSON, "spend_thresholds");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map,
                         ResourceManager rm, ProfilerFiller profiler) {
        SpendThresholdRegistry.clearAll();
        SpendThresholdManager.registerDefaults();

        int loaded = 0;
        for (var e : map.entrySet()) {
            try {
                SpendThresholdDef def = GSON.fromJson(e.getValue(), SpendThresholdDef.class);
                if (def != null && def.enabled && def.key != null && !def.key.isEmpty()) {
                    SpendThresholdRegistry.registerGlobal(def.toSpec(), def.priority);
                    loaded++;
                }
            } catch (Exception ex) {
                System.err.println("[SpendThresholds] Failed " + e.getKey() + ": " + ex.getMessage());
            }
        }

        SpendThresholdRegistry.freeze();
        System.out.println("[SpendThresholds] Loaded " + loaded + " datapack specs; total=" + SpendThresholdRegistry.size());
    }

    @SubscribeEvent
    public static void onAddReload(AddReloadListenerEvent evt) {
        evt.addListener(new SpendThresholdsReloadListener());
    }
}
