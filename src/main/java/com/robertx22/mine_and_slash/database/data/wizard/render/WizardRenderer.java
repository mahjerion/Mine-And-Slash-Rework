package com.robertx22.mine_and_slash.database.data.wizard.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.wizard.WizardType;
import com.robertx22.mine_and_slash.database.data.wizard.entity.WizardEntity;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a wizard on the vanilla witch rig.
 * <p>
 * Vanilla's own {@code WitchRenderer} can't be reused - it is typed to {@code Witch} and its held
 * item layer reads {@code isDrinkingPotion()} - but {@code WitchModel<T extends LivingEntity>} and
 * {@code ModelLayers.WITCH} are generic enough to use directly, the same situation
 * {@code MercenaryRenderer} documents for {@code PlayerModel}. So there is no layer definition to
 * register: this reuses a vanilla baked layer, like every other renderer in the mod.
 * <p>
 * No item-in-hand layer either. A wizard holds nothing - its skills come from a datapack, not from
 * a weapon - and the empty hand is what tells a player at a glance that this is not a mob that will
 * walk up and hit them.
 */
public class WizardRenderer extends MobRenderer<WizardEntity, WitchModel<WizardEntity>> {

    private static final ResourceLocation FALLBACK = new ResourceLocation("textures/entity/witch.png");

    public WizardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchModel<>(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(WizardEntity entity) {
        WizardType type = entity.getWizardType();
        if (type == null) {
            return FALLBACK;
        }
        try {
            return type.getTextureLoc();
        } catch (Exception e) {
            // a datapack can hand us a malformed ResourceLocation. a wrong looking wizard beats a
            // render crash that takes the whole world view with it.
            return FALLBACK;
        }
    }

    // the witch model is authored slightly oversized and vanilla shrinks it here rather than in the
    // model - without this the wizard stands noticeably taller than a witch does.
    @Override
    protected void scale(WizardEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
