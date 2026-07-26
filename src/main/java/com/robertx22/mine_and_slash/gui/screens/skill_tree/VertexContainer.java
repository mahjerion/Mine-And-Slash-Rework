package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Batches the skill tree's quads by texture and flushes them in an explicit back-to-front order.
//
// This used to be a single HashMultimap<ResourceLocation, BufferInfo>. That had two problems:
//   - HashMultimap iterates in hash order, so which texture group reached the screen last was
//     effectively arbitrary. The per-quad z (BufferInfo.pBlitOffset) does not save you here: the
//     skill tree render type draws blended quads with no depth write, so the last group drawn wins
//     regardless of z. That is why connection lines landed on top of nodes and the allocated-node
//     colour covered its own icon.
//   - It is a set multimap, so it hashes every BufferInfo (including its Matrix4f) on insert and
//     silently drops any two quads that compare equal.
//
// Layers are drawn lowest-first with a flush between each, which is what actually guarantees the
// ordering. Within a layer, order is irrelevant (nothing in a layer overlaps itself) but insertion
// order is preserved anyway so the result is deterministic frame to frame.
public class VertexContainer {

    // draw order, back to front
    public static final int LAYER_CONNECTION = 0;
    public static final int LAYER_PERK_COLOR = 1;
    public static final int LAYER_PERK_BORDER = 2;
    public static final int LAYER_PERK_ICON = 3;

    private TreeMap<Integer, LinkedHashMap<ResourceLocation, List<BufferInfo>>> layers = new TreeMap<>();

    public void put(int layer, ResourceLocation texture, BufferInfo info) {
        layers.computeIfAbsent(layer, x -> new LinkedHashMap<>())
                .computeIfAbsent(texture, x -> new ArrayList<>())
                .add(info);
    }

    public void refresh() {
        this.layers = new TreeMap<>();
    }

    public void draw(MultiBufferSource.BufferSource bufferSource) {
        for (Map.Entry<Integer, LinkedHashMap<ResourceLocation, List<BufferInfo>>> layer : this.layers.entrySet()) {

            for (Map.Entry<ResourceLocation, List<BufferInfo>> entry : layer.getValue().entrySet()) {
                ResourceLocation key = entry.getKey();
                RenderType renderType = SkillTreeRenderType.getSkillTreeRenderType(key.toString(), key);
                VertexConsumer buffer = bufferSource.getBuffer(renderType);

                for (BufferInfo bufferInfo : entry.getValue()) {
                    bufferInfo.upload(buffer);
                }
            }

            // flush before starting the next layer - without this every layer ends up in the same
            // batch and the ordering guarantee is lost again
            bufferSource.endBatch();
        }
    }

}
