package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public enum AllyOrEnemy {



    summonShouldAttack() {
        @Override
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream()
                    .filter(x -> is(caster, x))
                    .collect(Collectors.toList());
        }

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
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream().filter(x -> is(caster, x)).collect(Collectors.toList());
        }

        @Override
        public boolean is(Entity caster, LivingEntity target) {
            return enemies.is(caster, target) == false;
        }

        @Override
        public boolean includesCaster() {
            return true;
        }
    },
    allies_not_self() {
        @Override
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream().filter(x -> is(caster, x)).collect(Collectors.toList());
        }

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
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream()
                    .filter(x -> is(caster, x))
                    .collect(Collectors.toList());
        }

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
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream()
                    .filter(x -> is(caster, x))
                    .collect(Collectors.toList());
        }

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

            caster = resolveOwner(caster);

            if (caster instanceof Player p) {
                if (EntityFinder.isTamedByAlly(p, target)) {
                    return false;
                }
                if (target instanceof Player) {
                    if (!caster.level().getServer().isPvpAllowed()) {
                        return false;
                    }
                    if (target == caster || TeamUtils.areOnSameTeam((Player) caster, (Player) target, false)) {
                        return false;
                    } else {
                        return true;
                    }
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
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream()
                    .filter(x -> is(caster, x))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean includesCaster() {
            return false;
        }
    },
    non_ai_enemies {
        @Override
        public boolean is(Entity caster, LivingEntity target) {

            caster = resolveOwner(caster);

            if (caster instanceof Player p) {
                if (EntityFinder.isTamedByAlly(p, target)) {
                    return false;
                }
                if (target.serializeNBT().contains("NoAI")) {
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
                if (target instanceof Player) {
                    if (!caster.level().getServer().isPvpAllowed()) {
                        return false;
                    }
                    if (target == caster || TeamUtils.areOnSameTeam((Player) caster, (Player) target, false)) {
                        return false;
                    } else {
                        return true;
                    }
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
        public <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster) {
            return list.stream()
                    .filter(x -> is(caster, x))
                    .collect(Collectors.toList());
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

    public abstract <T extends LivingEntity> List<T> getMatchingEntities(List<T> list, Entity caster);

    public abstract boolean is(Entity caster, LivingEntity target);

    public abstract boolean includesCaster();

    // when a summon or a mercenary is the one casting, ally-ness has to be judged from its owner's
    // point of view. the non player branch in `enemies` calls every player - the owner included -
    // an enemy and every ordinary monster an ally, which is exactly backwards for an owned entity.
    // public because SummonPetAction needs the same walk to decide who a mercenary's pet belongs to
    public static Entity resolveOwner(Entity caster) {
        if (caster instanceof Player) {
            return caster;
        }
        if (caster instanceof OwnableEntity owned && owned.getOwner() instanceof Player owner) {
            return owner;
        }
        return caster;
    }
}
