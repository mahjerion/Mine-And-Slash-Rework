package com.robertx22.mine_and_slash.database.data.wizard;

import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.wizard.WizardSpellShapes.TelegraphKind;
import com.robertx22.mine_and_slash.database.data.wizard.entity.WizardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws where a wizard's skill is about to land, in red particles, while it telegraphs.
 * <p>
 * A filled disc for an area - under the target for a skill that drops on it, around the wizard for
 * a nova. Projectile lines are not particles, see {@code WizardTelegraphRenderer}.
 * <p>
 * Spawned on each client from the wizard's own tick, off the state {@link WizardEntity#publishCast}
 * synced when the cast started, rather than sent as particle packets: {@code ParticlesPacket} can
 * only carry a {@code SimpleParticleType}, which rules out coloured dust, and a map that is a quarter
 * wizards would otherwise send a steady stream of packets for pure decoration.
 * <p>
 * No client-only classes in here on purpose - only {@link Level#addParticle}, which the server
 * ignores - so this can never be the class that breaks a dedicated server.
 */
public class WizardTelegraphParticles {

    private static final DustParticleOptions FILL = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.0F);
    /** the edge is what a player actually steps across, so it is drawn bigger than the fill */
    private static final DustParticleOptions RIM = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.5F);

    /**
     * One wave every this many ticks. Dust lives long enough that consecutive waves overlap, so it
     * reads as a solid shape without spawning a full shape every tick.
     */
    private static final int WAVE_TICKS = 4;
    /** nobody further than this can see the particles anyway */
    private static final double VIEW_DISTANCE = 48;

    private static final int MAX_FILL = 80;
    private static final int MAX_RIM = 64;

    /** how far a disc point may sit below or above its centre before it is on another floor */
    private static final int GROUND_SEARCH_DOWN = 4;
    private static final int GROUND_SEARCH_UP = 2;

    public static void tick(WizardEntity wizard) {
        Level level = wizard.level();

        if (!level.isClientSide || wizard.tickCount % WAVE_TICKS != 0) {
            return;
        }
        if (wizard.getTelegraphSpell().isEmpty()) {
            return;
        }
        if (!ClientConfigs.getConfig().RENDER_WIZARD_TELEGRAPHS.get()) {
            return;
        }

        float elapsed = wizard.getTelegraphElapsed(0);
        int total = wizard.getTelegraphTotalTicks();

        // the server clears the cast when it fires, but the clear can arrive a tick or two late
        if (elapsed < 0 || elapsed > total) {
            return;
        }
        if (level.getNearestPlayer(wizard.getX(), wizard.getY(), wizard.getZ(), VIEW_DISTANCE, false) == null) {
            return;
        }

        TelegraphKind kind = wizard.getTelegraphKind();
        float size = wizard.getTelegraphSize();

        if (size <= 0 || kind == TelegraphKind.NONE) {
            return;
        }

        double multi = ClientConfigs.getConfig().SPELL_PARTICLE_MULTI.get();
        RandomSource rand = wizard.getRandom();

        switch (kind) {
            case PROJECTILE_LINE -> {
                // drawn as a solid beam every frame by WizardTelegraphRenderer instead - particles laid
                // down in waves left gaps and trailed stale lines behind a turning wizard
            }
            case SELF_CIRCLE, AT_TARGET_CIRCLE -> {
                // an area lands the moment the telegraph ends, after which the skill's own effects
                // are the thing to look at
                if (elapsed > WizardSpellCaster.TELEGRAPH_TICKS) {
                    return;
                }
                Vec3 centre;
                if (kind == TelegraphKind.AT_TARGET_CIRCLE) {
                    Vector3f a = wizard.getTelegraphAnchor();
                    centre = new Vec3(a.x, a.y, a.z);
                } else {
                    centre = wizard.position();
                }
                // thin at first, thickening as the skill gets closer to going off
                float progress = Mth.clamp(elapsed / WizardSpellCaster.TELEGRAPH_TICKS, 0F, 1F);
                disc(level, centre, size, (0.4D + 0.6D * progress) * multi, rand);
            }
            default -> {
            }
        }
    }

    private static void disc(Level level, Vec3 centre, double radius, double multi, RandomSource rand) {
        // ground height per block column, so a disc of eighty points isn't eighty block scans
        Map<Long, Double> ground = new HashMap<>();

        int fill = Mth.clamp((int) (1.2D * Math.PI * radius * radius * multi), 8, MAX_FILL);
        for (int i = 0; i < fill; i++) {
            // sqrt spreads the points evenly over the area - a plain random radius crowds the centre
            double r = radius * Math.sqrt(rand.nextDouble());
            double theta = rand.nextDouble() * Math.PI * 2;
            spawnOnGround(level, ground, FILL, centre, centre.x + Math.cos(theta) * r, centre.z + Math.sin(theta) * r);
        }

        int rim = Mth.clamp((int) (Math.PI * 2 * radius / 0.6D * Math.min(1D, multi + 0.3D)), 8, MAX_RIM);
        double offset = rand.nextDouble();
        for (int i = 0; i < rim; i++) {
            double theta = (i + offset) / rim * Math.PI * 2;
            spawnOnGround(level, ground, RIM, centre, centre.x + Math.cos(theta) * radius, centre.z + Math.sin(theta) * radius);
        }
    }

    private static void spawnOnGround(Level level, Map<Long, Double> ground, DustParticleOptions particle, Vec3 centre, double x, double z) {
        BlockPos column = BlockPos.containing(x, centre.y, z);
        Double y = ground.computeIfAbsent(column.asLong(), k -> groundY(level, column));
        if (y == null || Double.isNaN(y)) {
            // no floor near the centre's height - an edge over a drop, or a wall. leave the gap
            return;
        }
        level.addParticle(particle, x, y + 0.1D, z, 0, 0, 0);
    }

    /**
     * The top of the floor in this column near the given height, or NaN when there isn't one close.
     * <p>
     * The walk {@code SummonBlockAction.findSurface} does - first open block with something solid
     * under it, down first then up - but short, because further away is a different floor the circle
     * shouldn't be painted onto, and never into an unloaded chunk.
     */
    private static double groundY(Level level, BlockPos start) {
        if (!level.hasChunkAt(start)) {
            return Double.NaN;
        }
        BlockPos pos = start.above();
        for (int i = 0; i <= GROUND_SEARCH_DOWN + 1; i++) {
            if (isFloor(level, pos)) {
                return pos.getY();
            }
            pos = pos.below();
        }
        pos = start.above(2);
        for (int i = 0; i < GROUND_SEARCH_UP; i++) {
            if (isFloor(level, pos)) {
                return pos.getY();
            }
            pos = pos.above();
        }
        return Double.NaN;
    }

    private static boolean isFloor(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

}
