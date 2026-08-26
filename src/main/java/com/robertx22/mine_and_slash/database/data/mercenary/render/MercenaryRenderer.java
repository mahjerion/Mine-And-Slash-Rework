package com.robertx22.mine_and_slash.database.data.mercenary.render;

import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a mercenary on the vanilla <b>player</b> rig.
 * <p>
 * Not the illager rig, even though the first placeholder was meant to look like a vindicator:
 * {@code IllagerModel<T extends AbstractIllager>} is generic-bounded, so a non-illager can't use it,
 * and the vanilla illager renderer carries no armor layer. Since equipping the mercenary is the whole
 * point of the feature, the armor has to be visible - hence {@link HumanoidArmorLayer}.
 * {@link HumanoidMobRenderer}'s own constructor already adds the item-in-hand, custom-head and elytra
 * layers, so weapons show without any extra work.
 * <p>
 * And not the zombie rig either, which is what this used to be. Mercenary skins are authored as player
 * skins, and a player skin is painted on two layers - the body, plus an outer jacket/sleeves/trousers
 * layer drawn slightly larger over the top. A plain {@code HumanoidModel} has no cubes for that outer
 * layer except the hat, so every pixel of it was silently discarded. {@code PlayerModel} is generic
 * over {@code LivingEntity}, so a mercenary can use it directly and get those cubes.
 */
public class MercenaryRenderer extends HumanoidMobRenderer<MercenaryEntity, PlayerModel<MercenaryEntity>> {

    private static final ResourceLocation FALLBACK = new ResourceLocation("textures/entity/zombie/zombie.png");

    public MercenaryRenderer(EntityRendererProvider.Context ctx) {
        // false = classic 4px arms rather than the slim/Alex 3px ones. the shipped fighter skin is
        // drawn for classic, and a datapack skin that isn't would only look slightly narrow-shouldered.
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        // armor models are plain HumanoidModel even on the player rig - this mirrors PlayerRenderer
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(MercenaryEntity entity) {
        MercenaryClass mc = entity.getMercClass();
        if (mc == null) {
            return FALLBACK;
        }
        try {
            return mc.getTextureLoc();
        } catch (Exception e) {
            // a datapack can hand us a malformed ResourceLocation. a wrong looking mercenary beats
            // a render crash that takes the whole world view with it.
            return FALLBACK;
        }
    }
}
