package com.example.miyu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MiyuEntity extends TamableAnimal {
    /** Длительность короткого жеста в тиках. */
    public static int len(int g) {
        return switch (g) { case 1 -> 140; case 2 -> 120; case 3 -> 90; case 4 -> 160; case 5 -> 100; case 6 -> 140; case 7 -> 120; case 8 -> 120; default -> 60; };
    }
    static final EntityDataAccessor<Integer> GESTURE = SynchedEntityData.defineId(MiyuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> SCENE = SynchedEntityData.defineId(MiyuEntity.class, EntityDataSerializers.INT);
    int seq, gUntil, gStart;                      // жесты
    int sSeq, sT = -1, sIdx = -1, cStart;         // сцены (sT/sIdx — сервер, cStart — клиент)
    double dirX, dirZ;
    String curItem = "";

    public MiyuEntity(EntityType<? extends MiyuEntity> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GESTURE, 0);
        b.define(SCENE, 0);
    }

    // ----- жесты: 1 обнять, 2 погладить, 3 помахать, 4 за руку, 5 ревнивый топот, 6 прижаться, 7 встретить, 8 смущение -----
    public void playGesture(int g) {
        if (sIdx >= 0) return;
        seq = (seq + 1) & 0xFFF;
        gUntil = tickCount + len(g);
        entityData.set(GESTURE, (seq << 4) | (g & 15));
    }
    public int gestureId() { return entityData.get(GESTURE) & 15; }
    public int gestureStart() { return gStart; }

    // ----- сцены -----
    public int sceneIndex() { return (entityData.get(SCENE) & 255) - 1; }
    public int sceneStart() { return cStart; }
    public MiyuScene activeScene() { int i = sceneIndex(); return i >= 0 ? MiyuScene.get(i) : null; }

    public void startScene(int idx) {
        var o = getOwner();
        MiyuScene sc = MiyuScene.get(idx);
        if (level().isClientSide || o == null || sc == null) return;
        double dx = getX() - o.getX(), dz = getZ() - o.getZ();
        double l = Math.hypot(dx, dz);
        if (l < 0.3) { var look = o.getLookAngle(); dx = look.x; dz = look.z; l = Math.hypot(dx, dz); if (l < 1e-3) { dx = 0; dz = 1; l = 1; } }
        dirX = dx / l; dirZ = dz / l;
        sIdx = idx; sT = 0; curItem = "";
        sSeq = (sSeq + 1) & 0xFFFFF;
        entityData.set(SCENE, (sSeq << 8) | (idx + 1));
        setOrderedToSit(false);
        getNavigation().stop();
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    public void stopScene() {
        if (sIdx < 0) return;
        sIdx = -1; sT = -1; curItem = "";
        entityData.set(SCENE, 0);
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private void tickScene() {
        MiyuScene sc = MiyuScene.get(sIdx);
        var o = getOwner();
        if (sc == null || o == null || o.level() != level() || sT > sc.duration || distanceToSqr(o) > 400) { stopScene(); return; }
        float s = sc.dist(sT);
        double tx = o.getX() + dirX * s, tz = o.getZ() + dirZ * s;
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(o.getZ() - tz, o.getX() - tx)) - 90f);
        if (level().noCollision(this, getBoundingBox().move(tx - getX(), 0, tz - getZ()))) moveTo(tx, getY(), tz, yaw, 0f);
        else setYRot(yaw);
        setYHeadRot(yaw); setYBodyRot(yaw);
        getNavigation().stop();
        sc.fireCues(sT, o, Cfg.name());
        String item = sc.mainHand(sT);
        if (!item.equals(curItem)) {
            curItem = item;
            setItemSlot(EquipmentSlot.MAINHAND, item.isEmpty() ? ItemStack.EMPTY
                : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item))));
        }
        sT++;
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide) {
            if (key == GESTURE) gStart = tickCount;
            if (key == SCENE) cStart = tickCount;
        }
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (sIdx >= 0) tickScene();
            else if (tickCount < gUntil) {
                var o = getOwner();
                if (o != null) getLookControl().setLookAt(o, 30f, 30f);
                getNavigation().stop();
            }
        } else {
            MiyuScene sc = activeScene();
            if (sc != null) {
                int t = tickCount - cStart;
                if (tickCount % 4 == 0 && t >= 0 && t <= sc.duration) {
                    if ("broken".equals(sc.fx) && sc.npc.glowAt(t)) {
                        for (int i = 0; i < 2; i++) particle(ParticleTypes.DAMAGE_INDICATOR, 0.8, 0.05);
                    } else if ("hearts".equals(sc.fx)) {
                        particle(ParticleTypes.HEART, 1.6, 0.05);
                    }
                }
            } else if (tickCount - gStart < len(gestureId()) && tickCount % 3 == 0) {
                int g = gestureId();
                if (g == 5) { for (int i = 0; i < 2; i++) particle(ParticleTypes.DAMAGE_INDICATOR, 0.8, 0.05); }
                else if (g == 1 || g == 2 || g == 4 || g == 6 || g == 7) particle(ParticleTypes.HEART, 1.6, 0.05);
            }
        }
    }

    private void particle(net.minecraft.core.particles.ParticleOptions type, double h, double vy) {
        level().addParticle(type, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + h + random.nextDouble() * 0.6,
            getZ() + (random.nextDouble() - 0.5) * 1.2, 0, vy, 0);
    }

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
