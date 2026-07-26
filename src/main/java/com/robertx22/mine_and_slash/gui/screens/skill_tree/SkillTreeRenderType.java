package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.robertx22.mine_and_slash.a_libraries.neat.NeatRenderType;
import com.robertx22.mine_and_slash.mixins.AccessorRenderType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;


public class SkillTreeRenderType extends RenderStateShard {

    public SkillTreeRenderType(String pName, Runnable pSetupState, Runnable pClearState) {
        super(pName, pSetupState, pClearState);
    }



    // One RenderType per texture, created once and reused. This used to build a fresh
    // CompositeState + TextureStateShard + TransparencyStateShard on every call, and
    // VertexContainer.draw() calls it once per distinct texture *every frame* - on the talents tree
    // that is ~300 allocations per frame. Worse, a new RenderType instance also compares unequal to
    // the previous one, so MultiBufferSource had to flush its buffer on every single group instead of
    // batching, which is what made the draw order depend on hash iteration order.
    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();

    public static RenderType getSkillTreeRenderType(String name, ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, tex -> build(name, tex));
    }

    private static RenderType build(String name, ResourceLocation texture) {
        RenderType.CompositeState renderTypeState = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(new TransparencyStateShard("boring_blend", RenderSystem::enableBlend, RenderSystem::disableBlend))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false);
        return AccessorRenderType.neat_create(name, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, true, renderTypeState);
    }
}
