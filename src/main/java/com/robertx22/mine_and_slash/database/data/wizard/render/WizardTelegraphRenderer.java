package com.robertx22.mine_and_slash.database.data.wizard.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.a_libraries.neat.NeatConfig;
import com.robertx22.mine_and_slash.a_libraries.neat.NeatRenderType;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.wizard.WizardSpellShapes.TelegraphKind;
import com.robertx22.mine_and_slash.database.data.wizard.entity.WizardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * The icon of the skill a wizard is winding up, floating over its head and filling bottom to top
 * like a cooldown. Full means the skill is going off. For a projectile skill, also a red beam along
 * the wizard's aim per projectile.
 * <p>
 * Called from {@code NeatRenderMixin} right after the health plate, for the same reason the plate
 * lives there: a camera-relative pose with the entity renderer's transforms already unwound. A render
 * layer on {@link WizardRenderer} would inherit the body yaw and the model's -1,-1,1 flip instead.
 * <p>
 * Its own visibility rules rather than the plate's: the plate hides past 12 blocks by default, at full
 * health, and optionally unless looked at - all wrong for a warning about a skill thrown from twenty
 * blocks away. No line of sight check either, a meteor cast from behind a wall still needs announcing.
 */
public class WizardTelegraphRenderer {

    private static final int LIGHT = 0xF000F0;
    /** icon edge, in the plate's scaled units - about two thirds of a block */
    private static final float SIZE = 24F;
    private static final float SCALE = 0.0267F;
    private static final double MAX_DISTANCE = 32;

    /** how far out from the eyes a projectile beam starts, so it doesn't sprout from the face */
    private static final double BEAM_START = 0.6D;
    private static final double BEAM_HALF_WIDTH = 0.18D;
    private static final double CORE_HALF_WIDTH = 0.05D;
    private static final int BEAM_ALPHA = 140;
    private static final int CORE_ALPHA = 220;
    private static final int BEAM_R = 255;
    private static final int BEAM_G = 40;
    private static final int BEAM_B = 40;

    /**
     * Whether each spell's icon exists, so a pack wizard skill with no icon is skipped instead of
     * drawn as the missing texture. Render thread only; a resource reload can't add a mod asset
     * without a restart, so this never needs clearing.
     */
    private static final Map<String, Boolean> HAS_ICON = new HashMap<>();

    public static void hookRender(Entity entity, PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraOrientation, float partialTicks) {
        // runs for every entity rendered every frame - get out before anything else
        if (!(entity instanceof WizardEntity wizard)) {
            return;
        }

        String spellId = wizard.getTelegraphSpell();
        if (spellId.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui || !ClientConfigs.getConfig().RENDER_WIZARD_TELEGRAPHS.get()) {
            return;
        }
        if (mc.player != null && wizard.isInvisibleTo(mc.player)) {
            return;
        }
        Entity camera = mc.gameRenderer.getMainCamera().getEntity();
        if (camera == null || camera.distanceToSqr(wizard) > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }

        float elapsed = wizard.getTelegraphElapsed(partialTicks);
        int total = wizard.getTelegraphTotalTicks();
        // the clear from the server can land a tick or two after the skill fires
        if (elapsed < 0 || elapsed > total + 1) {
            return;
        }

        float fill = Mth.clamp(elapsed / Math.max(1, wizard.getTelegraphFillTicks()), 0F, 1F);
        // a multicast still firing after its icon filled pulses, rather than sitting static
        boolean firing = fill >= 1F && elapsed > wizard.getTelegraphFillTicks();
        float pulse = firing ? 0.67F + 0.33F * (0.5F + 0.5F * Mth.sin(elapsed * 0.6F)) : 1F;

        if (wizard.getTelegraphKind() == TelegraphKind.PROJECTILE_LINE) {
            renderBeams(wizard, poseStack, buffers, mc, partialTicks, firing ? pulse : 0.25F + 0.75F * fill);
        }

        // a missing icon only skips the icon - the beam above still warns
        ResourceLocation icon = Spell.getIconLoc(spellId);
        if (!HAS_ICON.computeIfAbsent(icon.toString(), k -> mc.getResourceManager().getResource(icon).isPresent())) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0, wizard.getBbHeight() + NeatConfig.instance.heightAbove() + 0.55F, 0);
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(-SCALE, -SCALE, SCALE);

        // right before it goes off the whole icon pops a little, scaled about its own centre so the
        // two halves below stay lined up
        if (fill > 0.9F) {
            poseStack.translate(0, -SIZE / 2F, 0);
            poseStack.scale(1.12F, 1.12F, 1F);
            poseStack.translate(0, SIZE / 2F, 0);
        }

        VertexConsumer buffer = buffers.getBuffer(NeatRenderType.getHealthBarIconType(icon));
        Matrix4f pose = poseStack.last().pose();

        float half = SIZE / 2F;
        float fillLine = -SIZE * fill;

        // the icon is split at the fill line into two quads that never overlap. stacking a lit copy
        // over a dark full icon z-fought: z here is scaled by SCALE too, so any offset small enough to
        // look flat is far below depth buffer precision

        // the unlit part still to fill, on top
        if (fill < 1F) {
            quad(buffer, pose, -half, half, -SIZE, fillLine, 0, 1 - fill, 70, 70, 70, 180);
        }

        // the lit part, wiped up from the bottom. a multicast still firing past full pulses instead
        if (fill > 0F) {
            int alpha = (int) (255 * pulse);
            quad(buffer, pose, -half, half, fillLine, 0, 1 - fill, 1, 255, 255, 255, alpha);
        }

        poseStack.popPose();
    }

    /**
     * A red beam per projectile along the wizard's aim, redrawn every frame.
     * <p>
     * Not particles: dust spawned in waves left gaps, and each wave lingered along wherever the wizard
     * was facing when it spawned, so a turning wizard trailed a smear of stale lines. This follows the
     * interpolated head rotation - which {@code WizardSpellCaster.aimAt} keeps equal to the aim - and
     * is gone the frame the cast ends.
     * <p>
     * The pose here is already at the wizard's interpolated feet, so everything is built as an offset
     * from {@code wizard.getPosition(partialTicks)}.
     */
    private static void renderBeams(WizardEntity wizard, PoseStack poseStack, MultiBufferSource buffers, Minecraft mc,
                                    float partialTicks, float strength) {
        float yaw = Mth.rotLerp(partialTicks, wizard.yHeadRotO, wizard.yHeadRot);
        float pitch = Mth.lerp(partialTicks, wizard.xRotO, wizard.getXRot());
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);

        int count = Math.max(1, wizard.getTelegraphProjCount());
        float apart = wizard.getTelegraphProjApart();
        double length = Math.min(wizard.getTelegraphSize(), WizardEntity.TELEGRAPH_BEAM_MAX_LENGTH);

        if (length <= BEAM_START) {
            return;
        }

        Vec3 wizardPos = wizard.getPosition(partialTicks);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 eye = new Vec3(0, wizard.getEyeHeight(), 0);

        VertexConsumer buffer = buffers.getBuffer(NeatRenderType.getTelegraphBeamType());
        Matrix4f pose = poseStack.last().pose();

        for (int i = 0; i < count; i++) {
            Vec3 dir = forward;
            if (count > 1) {
                // the fan ProjectileCastHelper throws: SPREAD_OUT_IN_RADIUS, offset * apart / count
                float offset = i - (count - 1) / 2F;
                dir = forward.yRot((float) Math.toRadians(offset * apart / count));
            }

            Vec3 start = eye.add(dir.scale(BEAM_START));
            Vec3 end = eye.add(dir.scale(length));

            // turned about its own axis to face the camera, so it never goes edge-on and vanishes
            Vec3 toCamera = camera.subtract(wizardPos.add(start.add(end).scale(0.5D)));
            Vec3 side = dir.cross(toCamera);
            if (side.lengthSqr() < 1.0E-6D) {
                side = dir.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1.0E-6D) {
                    side = new Vec3(1, 0, 0);
                }
            }
            side = side.normalize();

            ribbon(buffer, pose, start, end, side, BEAM_HALF_WIDTH, (int) (BEAM_ALPHA * strength));
            ribbon(buffer, pose, start, end, side, CORE_HALF_WIDTH, (int) (CORE_ALPHA * strength));
        }
    }

    /** one quad from start to end, fading to a third of its alpha at the far end */
    private static void ribbon(VertexConsumer b, Matrix4f pose, Vec3 start, Vec3 end, Vec3 side, double halfWidth, int alpha) {
        int farAlpha = (int) (alpha * 0.3F);
        Vec3 s = side.scale(halfWidth);
        b.vertex(pose, (float) (start.x + s.x), (float) (start.y + s.y), (float) (start.z + s.z)).color(BEAM_R, BEAM_G, BEAM_B, alpha).endVertex();
        b.vertex(pose, (float) (start.x - s.x), (float) (start.y - s.y), (float) (start.z - s.z)).color(BEAM_R, BEAM_G, BEAM_B, alpha).endVertex();
        b.vertex(pose, (float) (end.x - s.x), (float) (end.y - s.y), (float) (end.z - s.z)).color(BEAM_R, BEAM_G, BEAM_B, farAlpha).endVertex();
        b.vertex(pose, (float) (end.x + s.x), (float) (end.y + s.y), (float) (end.z + s.z)).color(BEAM_R, BEAM_G, BEAM_B, farAlpha).endVertex();
    }

    /**
     * One textured quad in the plate's flipped space: y runs from {@code top} (negative, upwards) to
     * {@code bottom}, and the texture's v from {@code vTop} to {@code vBottom} - trimming both together
     * is what crops the icon rather than squashing it.
     */
    private static void quad(VertexConsumer b, Matrix4f pose, float left, float right, float top, float bottom,
                             float vTop, float vBottom, int r, int g, int bl, int a) {
        // the same winding EffectIcon uses - the icon render type culls back faces
        b.vertex(pose, left, top, 0).color(r, g, bl, a).uv(0, vTop).uv2(LIGHT).endVertex();
        b.vertex(pose, left, bottom, 0).color(r, g, bl, a).uv(0, vBottom).uv2(LIGHT).endVertex();
        b.vertex(pose, right, bottom, 0).color(r, g, bl, a).uv(1, vBottom).uv2(LIGHT).endVertex();
        b.vertex(pose, right, top, 0).color(r, g, bl, a).uv(1, vTop).uv2(LIGHT).endVertex();
    }
}
