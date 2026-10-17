package com.example.miyu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.HumanoidArm;

/** Модель игрока с локтями и коленями + плавные жесты и сцены Мию. */
public class MiyuModel extends PlayerModel<MiyuEntity> {
    static final float R = (float) Math.PI / 180f;
    final ModelPart rightForearm, leftForearm, rightShin, leftShin;
    final ModelPart rightSleeveLower, leftSleeveLower, rightPantsLower, leftPantsLower;
    final boolean slimArms;

    public MiyuModel(ModelPart root, boolean slim) {
        super(root, slim);
        slimArms = slim;
        rightForearm = rightArm.getChild("right_forearm"); leftForearm = leftArm.getChild("left_forearm");
        rightShin = rightLeg.getChild("right_shin"); leftShin = leftLeg.getChild("left_shin");
        rightSleeveLower = rightSleeve.getChild("right_sleeve_lower"); leftSleeveLower = leftSleeve.getChild("left_sleeve_lower");
        rightPantsLower = rightPants.getChild("right_pants_lower"); leftPantsLower = leftPants.getChild("left_pants_lower");
    }

    // ---------- геометрия: руки и ноги из двух частей ----------
    public static LayerDefinition createLayer(boolean slim) {
        CubeDeformation d0 = CubeDeformation.NONE, d1 = new CubeDeformation(0.25f);
        MeshDefinition mesh = PlayerModel.createMesh(d0, slim);
        PartDefinition root = mesh.getRoot();
        float aw = slim ? 3f : 4f, ay = slim ? 2.5f : 2f, rx = slim ? -2f : -3f;
        arm(root, "right_arm", "right_forearm", 40, 16, rx, aw, -5f, ay, d0);
        arm(root, "left_arm", "left_forearm", 32, 48, -1f, aw, 5f, ay, d0);
        arm(root, "right_sleeve", "right_sleeve_lower", 40, 32, rx, aw, -5f, ay, d1);
        arm(root, "left_sleeve", "left_sleeve_lower", 48, 48, -1f, aw, 5f, ay, d1);
        leg(root, "right_leg", "right_shin", 0, 16, -1.9f, d0);
        leg(root, "left_leg", "left_shin", 16, 48, 1.9f, d0);
        leg(root, "right_pants", "right_pants_lower", 0, 32, -1.9f, d1);
        leg(root, "left_pants", "left_pants_lower", 0, 48, 1.9f, d1);
        return LayerDefinition.create(mesh, 64, 64);
    }
    static void arm(PartDefinition root, String name, String lower, int u, int v, float x0, float w, float px, float py, CubeDeformation d) {
        PartDefinition p = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x0, -2f, -2f, w, 6f, 4f, d), PartPose.offset(px, py, 0f));
        p.addOrReplaceChild(lower, CubeListBuilder.create().texOffs(u, v + 6).addBox(x0, 0f, -2f, w, 6f, 4f, d), PartPose.offset(0f, 4f, 0f));
    }
    static void leg(PartDefinition root, String name, String lower, int u, int v, float px, CubeDeformation d) {
        PartDefinition p = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(-2f, 0f, -2f, 4f, 6f, 4f, d), PartPose.offset(px, 12f, 0f));
        p.addOrReplaceChild(lower, CubeListBuilder.create().texOffs(u, v + 6).addBox(-2f, 0f, -2f, 4f, 6f, 4f, d), PartPose.offset(0f, 6f, 0f));
    }

    // предмет в руке должен идти за предплечьем
    @Override public void translateToHand(HumanoidArm side, PoseStack ps) {
        ModelPart upper = getArm(side);
        float sh = slimArms ? 0.5f * (side == HumanoidArm.RIGHT ? 1f : -1f) : 0f;
        upper.x += sh; upper.translateAndRotate(ps); upper.x -= sh;
        (side == HumanoidArm.RIGHT ? rightForearm : leftForearm).translateAndRotate(ps);
        ps.translate(0f, -0.25f, 0f);
    }

    static float lerp(float a, float b, float w) { return a + (b - a) * w; }
    static float ss(float x) { x = Math.max(0f, Math.min(1f, x)); return x * x * (3f - 2f * x); }
    static void set(ModelPart p, float x, float y, float z, float w) {
        p.xRot = lerp(p.xRot, x, w); p.yRot = lerp(p.yRot, y, w); p.zRot = lerp(p.zRot, z, w);
    }
    static void add(ModelPart p, float x, float y, float z, float w) { p.xRot += x * w; p.yRot += y * w; p.zRot += z * w; }

    void part(ModelPart m, MiyuScene.Pose p, int i, boolean additive) {
        float w = p.w[i];
        if (w <= 0.001f) return;
        float x = p.v[i][0] * R, y = p.v[i][1] * R, z = p.v[i][2] * R;
        if (additive) add(m, x, y, z, w); else set(m, x, y, z, w);
    }

    @Override
    public void setupAnim(MiyuEntity e, float ls, float lsa, float age, float yaw, float pitch) {
        super.setupAnim(e, ls, lsa, age, yaw, pitch);
        for (ModelPart p : new ModelPart[]{rightForearm, leftForearm, rightShin, leftShin}) { p.xRot = 0; p.yRot = 0; p.zRot = 0; }

        MiyuScene sc = e.activeScene();
        if (sc != null) {
            float t = age - e.sceneStart();
            if (t >= 0 && t <= sc.duration) {
                MiyuScene.Pose p = sc.npc.sample(t);
                part(head, p, 0, true);
                part(rightArm, p, 3, false); part(leftArm, p, 4, false);
                part(rightForearm, p, 5, false); part(leftForearm, p, 6, false);
                part(rightLeg, p, 7, false); part(leftLeg, p, 8, false);
                part(rightShin, p, 9, false); part(leftShin, p, 10, false);
            }
        } else {
            gesture(e, age);
        }
        leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
        leftSleeveLower.copyFrom(leftForearm); rightSleeveLower.copyFrom(rightForearm);
        leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg);
        leftPantsLower.copyFrom(leftShin); rightPantsLower.copyFrom(rightShin);
        hat.copyFrom(head); jacket.copyFrom(body);
    }

    void gesture(MiyuEntity e, float age) {
        int g = e.gestureId();
        if (g == 0) return;
        int len = MiyuEntity.len(g);
        float t = age - e.gestureStart();
        if (t < 0 || t > len) return;
        float w = ss(Math.min(t / 22f, (len - t) / 22f));
        float s1 = (float) Math.sin(t * 0.07f), s2 = (float) Math.sin(t * 0.19f);
        switch (g) {
            case 1 -> { // обнять
                float c = ss((t - 24f) / 30f);
                set(rightArm, lerp(-72 * R, -86 * R, c) + s1 * 2 * R, lerp(24 * R, -48 * R, c), 0, w);
                set(leftArm, lerp(-72 * R, -86 * R, c) + s1 * 2 * R, lerp(-24 * R, 48 * R, c), 0, w);
                set(rightForearm, lerp(-10 * R, -35 * R, c), 0, 0, w); set(leftForearm, lerp(-10 * R, -35 * R, c), 0, 0, w);
                add(head, 8 * R * c, 0, s1 * 5 * R * c, w);
            }
            case 2 -> { // погладить по голове
                set(rightArm, -102 * R, -8 * R, 0, w);
                set(rightForearm, -40 * R + s2 * 18 * R, 0, 0, w);
                add(head, 10 * R, 0, -s1 * 4 * R, w);
            }
            case 3 -> { // помахать
                set(rightArm, -150 * R, 0, 8 * R, w);
                set(rightForearm, -25 * R, 0, (float) Math.sin(t * 0.45f) * 22 * R, w);
                add(head, 0, 0, 6 * R, w);
            }
            case 4 -> { // взять за руку
                set(rightArm, -52 * R, -28 * R, 0, w); set(leftArm, -52 * R, 28 * R, 0, w);
                set(rightForearm, -20 * R, 0, 0, w); set(leftForearm, -20 * R, 0, 0, w);
                add(head, 4 * R, 0, s1 * 3 * R, w);
            }
            case 5 -> { // ревнивый топот
                set(rightArm, -35 * R, -62 * R, 10 * R, w); set(leftArm, -35 * R, 62 * R, -10 * R, w);
                set(rightForearm, -100 * R, 0, 0, w); set(leftForearm, -100 * R, 0, 0, w);
                add(head, 12 * R, (float) Math.sin(t * 0.5f) * 8 * R, 0, w);
                float st = Math.max(0f, (float) Math.sin(t * 0.9f));
                set(rightLeg, -35 * R * st, 0, 0, w); set(rightShin, 30 * R * st, 0, 0, w);
            }
            case 6 -> { // прижаться
                set(rightArm, -62 * R, -38 * R, 0, w); set(leftArm, -62 * R, 38 * R, 0, w);
                set(rightForearm, -30 * R, 0, 0, w); set(leftForearm, -30 * R, 0, 0, w);
                add(head, 6 * R, 0, 12 * R + s1 * 2 * R, w);
            }
            case 7 -> { // встретить: руки широко, потом объятие
                float c = ss((t - 30f) / 34f);
                set(rightArm, lerp(-30 * R, -86 * R, c), lerp(10 * R, -48 * R, c), lerp(72 * R, 0, c), w);
                set(leftArm, lerp(-30 * R, -86 * R, c), lerp(-10 * R, 48 * R, c), lerp(-72 * R, 0, c), w);
                set(rightForearm, lerp(-10 * R, -35 * R, c), 0, 0, w); set(leftForearm, lerp(-10 * R, -35 * R, c), 0, 0, w);
                add(head, -4 * R * (1 - c), 0, 6 * R * c, w);
            }
            case 8 -> { // смущение
                set(rightArm, -52 * R, -52 * R, 0, w); set(leftArm, -52 * R, 52 * R, 0, w);
                set(rightForearm, -95 * R, 0, 0, w); set(leftForearm, -95 * R, 0, 0, w);
                add(head, 16 * R, s1 * 7 * R, 8 * R, w);
            }
            default -> { }
        }
    }
}
