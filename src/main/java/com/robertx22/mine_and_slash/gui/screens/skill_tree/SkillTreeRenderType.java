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


public class SkillTreeRenderType extends RenderStateShard {

    public SkillTreeRenderType(String pName, Runnable pSetupState, Runnable pClearState) {
        super(pName, pSetupState, pClearState);
    }

    public static RenderType CONNECTION = getSkillTreeRenderType("connection", SlashRef.id("textures/gui/skill_tree/skill_connection.png"));

    private static final TransparencyStateShard blend = new TransparencyStateShard("boring_blend", RenderSystem::enableBlend, RenderSystem::disableBlend);



    public static RenderType getSkillTreeRenderType(String name, ResourceLocation texture) {
        RenderType.CompositeState renderTypeState = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(new TransparencyStateShard("boring_blend", RenderSystem::enableBlend, RenderSystem::disableBlend))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false);
        return AccessorRenderType.neat_create(name, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, true, renderTypeState);
    }
}
