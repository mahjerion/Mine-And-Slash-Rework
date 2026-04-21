package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public enum AllyOrEnemy {



    summonShouldAttack() {
        @Override
        public boolean is(Entity caster, LivingEntity target) {

            if (!enemies.is(caster, target)) {
                return false;
            }

            var type = Load.Unit(target).getType();

            if (type == EntityTypeUtils.EntityClassification.AMBIENT) {
                return false;
            }
            if (type == EntityTypeUtils.EntityClassification.ANIMAL) {
                return false;
            }
            if (type == EntityTypeUtils.EntityClassification.NPC) {
                return false;
            }
            // For neutral mobs, check if they're angry at the player owner, not the summon
            if (target instanceof NeutralMob neutralTarget) {
                if (!neutralTarget.isAngry()) {
                    return false;
                }
            }
            if (ServerContainer.get().isEntitySummonBlacklisted(target)) {
                return false;
            }

            return true;
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },


    allies() {
        @Override
        public boolean is(Entity caster, LivingEntity target) {
            return !enemies.is(caster, target);
        }

        @Override
        public boolean includesCaster() {
            return true;
        }
    },

    allies_not_self() {
        @Override
        public boolean is(Entity caster, LivingEntity target) {
            return allies.is(caster, target);
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },

    pets() {
        @Override
        public boolean is(Entity caster, LivingEntity target) {
            if (caster instanceof Player p) {
                if (EntityFinder.isTamedByAlly(p, target) && target instanceof OwnableEntity pet && pet.getOwner() == caster) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },

    casters_summons() {
        @Override
        public boolean is(Entity caster, LivingEntity target) {
            if (caster instanceof Player p) {
                if (target instanceof SummonEntity) {
                    if (EntityFinder.isTamedByAlly(p, target) && target instanceof OwnableEntity pet && pet.getOwner() == caster) {
                        return !Load.Unit(target).summonedPetData.isEmpty();
                    }
                }
            }
            return false;
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },

    enemies {
        @Override
        public boolean is(Entity caster, LivingEntity target) {

            if (caster instanceof Player casterPlayer) {
                if (EntityFinder.isTamedByAlly(casterPlayer, target)) {
                    return false;
                }
                if (target instanceof Player targetPlayer) {
                    if (!caster.level().getServer().isPvpAllowed()) {
                        return false;
                    }
                    return target != caster && !TeamUtils.areOnSameTeam(casterPlayer, targetPlayer, false);
                }

            } else {
                if (target instanceof Player) {
                    return true;
                } else {
                    if (target instanceof OwnableEntity o) {
                        if (o.getOwner() instanceof Player p) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },

    non_ai_enemies {
        @Override
        public boolean is(Entity caster, LivingEntity target) {

            if (caster instanceof Player casterPlayer) {
                if (EntityFinder.isTamedByAlly(casterPlayer, target)) {
                    return false;
                }
                if (target instanceof Mob mob && mob.isNoAi()) {
                    return false;
                }
                // Check for villagers and other NPCs
                /*var type = Load.Unit(target).getType();
                if (type == EntityTypeUtils.EntityClassification.NPC) {
                    return false;
                } // failsafe incase mod doesn't use villager class
                if (target instanceof net.minecraft.world.entity.npc.AbstractVillager) {
                    return false;
                }*/
                if (target instanceof Player targetPlayer) {
                    if (!caster.level().getServer().isPvpAllowed()) {
                        return false;
                    }
                    return target != caster && !TeamUtils.areOnSameTeam(casterPlayer, targetPlayer, false);
                }

            } else {
                if (target instanceof Player) {
                    return true;
                } else {
                    if (target instanceof OwnableEntity o) {
                        if (o.getOwner() instanceof Player p) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },
    all {
        @Override
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list;
        }

        @Override
        public boolean is(Entity caster, LivingEntity target) {
            return true;
        }

        @Override
        public boolean includesCaster() {
            return true;
        }
    };

    public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
        return list.stream()
                .filter(x -> is(caster, x))
                .collect(Collectors.toList());
    }

    /**
     * @return whether {@code target} is an appropriate target for this target type for the caster {@code caster}
     */
    public abstract boolean is(Entity caster, LivingEntity target);

    public abstract boolean includesCaster();
}
