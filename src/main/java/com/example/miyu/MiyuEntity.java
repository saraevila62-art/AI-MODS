package com.example.miyu;

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
    public static final int G_LEN = 60;
    static final EntityDataAccessor<Integer> GESTURE = SynchedEntityData.defineId(MiyuEntity.class, EntityDataSerializers.INT);
    int seq, gUntil, gStart;

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GESTURE, 0);
    }
    /** 1 обнять, 2 погладить по голове, 3 помахать, 4 взять за руку, 5 ревнивый топот */
    public void playGesture(int g) {
        seq = (seq + 1) & 0xFFF;
        gUntil = tickCount + G_LEN;
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
