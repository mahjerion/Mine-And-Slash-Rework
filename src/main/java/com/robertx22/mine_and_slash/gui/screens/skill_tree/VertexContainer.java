package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Map;

public class VertexContainer {

    public HashMultimap<ResourceLocation, BufferInfo> map = HashMultimap.create(100, 500);

    public void refresh(){
        this.map = HashMultimap.create(100, 500);
    }

    public void draw(MultiBufferSource bufferSource){
        for (Map.Entry<ResourceLocation, Collection<BufferInfo>> entry : this.map.asMap().entrySet()) {
            ResourceLocation key = entry.getKey();
            RenderType skillTreeRenderType = SkillTreeRenderType.getSkillTreeRenderType(key.toString(), key);
            VertexConsumer buffer = bufferSource.getBuffer(skillTreeRenderType);

            for (BufferInfo bufferInfo : entry.getValue()) {
                bufferInfo.upload(buffer);
            }

        }
    }

}
