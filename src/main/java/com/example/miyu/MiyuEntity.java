package com.example.miyu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MiyuEntity extends TamableAnimal {
    /** Длительность жеста в тиках. */
    public static int len(int g) {
        return switch (g) { case 1 -> 140; case 2 -> 120; case 3 -> 90; case 4 -> 160; case 5 -> 100; case 6 -> 140; case 7 -> 120; case 8 -> 120; default -> 60; };
    }
    static final EntityDataAccessor<Integer> GESTURE = SynchedEntityData.defineId(MiyuEntity.class, EntityDataSerializers.INT);
    int seq, gUntil, gStart;

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GESTURE, 0);
    }
    /** 1 обнять, 2 погладить, 3 помахать, 4 взять за руку, 5 ревнивый топот, 6 прижаться, 7 встретить, 8 смущение */
    public void playGesture(int g) {
        seq = (seq + 1) & 0xFFF;
        gUntil = tickCount + len(g);
        entityData.set(GESTURE, (seq << 4) | (g & 15));
    }
    public int gestureId() { return entityData.get(GESTURE) & 15; }
    public int gestureStart() { return gStart; }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == GESTURE && level().isClientSide) gStart = tickCount;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide && tickCount - gStart < len(gestureId()) && tickCount % 3 == 0) {
            int g = gestureId();
            if (g == 5) {
                for (int i = 0; i < 2; i++)
                    level().addParticle(ParticleTypes.DAMAGE_INDICATOR, getX() + (random.nextDouble() - 0.5) * 1.2,
                        getY() + 0.8 + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0.05, 0);
            } else if (g == 1 || g == 2 || g == 4 || g == 6 || g == 7) {
                level().addParticle(ParticleTypes.HEART, getX() + (random.nextDouble() - 0.5), getY() + 1.6 + random.nextDouble() * 0.4,
                    getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
            }
        }
        if (!level().isClientSide && tickCount < gUntil) {
            var o = getOwner();
            if (o != null) getLookControl().setLookAt(o, 30f, 30f);
            getNavigation().stop();
        }
    }
    public MiyuEntity(EntityType<? extends MiyuEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 6, 2));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) {
            if (!isTame() || isOwnedBy(player)) ClientHooks.open(this);
        } else if (!isTame()) {
            tame(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
