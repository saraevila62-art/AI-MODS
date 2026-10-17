package com.example.miyu;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Модель игрока + плавные многофазные жесты Мию (поверх обычной анимации). */
public class MiyuModel extends PlayerModel<MiyuEntity> {
    static final float R = (float) Math.PI / 180f;

    public MiyuModel(ModelPart root, boolean slim) { super(root, slim); }

    static float lerp(float a, float b, float w) { return a + (b - a) * w; }
    static float ss(float x) { x = Math.max(0f, Math.min(1f, x)); return x * x * (3f - 2f * x); }
    static void set(ModelPart p, float x, float y, float z, float w) {
        p.xRot = lerp(p.xRot, x, w); p.yRot = lerp(p.yRot, y, w); p.zRot = lerp(p.zRot, z, w);
    }
    static void add(ModelPart p, float x, float y, float z, float w) { p.xRot += x * w; p.yRot += y * w; p.zRot += z * w; }

    @Override
    public void setupAnim(MiyuEntity e, float ls, float lsa, float age, float yaw, float pitch) {
        super.setupAnim(e, ls, lsa, age, yaw, pitch);
        int g = e.gestureId();
        if (g == 0) return;
        int len = MiyuEntity.len(g);
        float t = age - e.gestureStart();
        if (t < 0 || t > len) return;
        float w = ss(Math.min(t / 22f, (len - t) / 22f));
        float s1 = (float) Math.sin(t * 0.07f), s2 = (float) Math.sin(t * 0.19f);
        switch (g) {
            case 1 -> { // обнять: руки раскрываются, потом смыкаются, лёгкое покачивание
                float c = ss((t - 24f) / 30f);
                set(rightArm, lerp(-72 * R, -86 * R, c) + s1 * 2 * R, lerp(24 * R, -48 * R, c), 0, w);
                set(leftArm, lerp(-72 * R, -86 * R, c) + s1 * 2 * R, lerp(-24 * R, 48 * R, c), 0, w);
                add(head, 8 * R * c, 0, s1 * 5 * R * c, w);
            }
            case 2 -> { // погладить по голове
                set(rightArm, -102 * R + s2 * 10 * R, -8 * R, 0, w);
                add(head, 10 * R, 0, -s1 * 4 * R, w);
            }
            case 3 -> { // помахать
                set(rightArm, -160 * R, 0, -8 * R + (float) Math.sin(t * 0.45f) * 28 * R, w);
                add(head, 0, 0, 6 * R, w);
            }
            case 4 -> { // взять за руку
                set(rightArm, -52 * R, -28 * R, 0, w); set(leftArm, -52 * R, 28 * R, 0, w);
                add(head, 4 * R, 0, s1 * 3 * R, w);
            }
            case 5 -> { // ревнивый топот: руки скрещены, головой качает, топает ногой
                set(rightArm, -35 * R, -62 * R, 0, w); set(leftArm, -35 * R, 62 * R, 0, w);
                add(head, 12 * R, (float) Math.sin(t * 0.5f) * 8 * R, 0, w);
                float st = Math.max(0f, (float) Math.sin(t * 0.9f));
                rightLeg.xRot = lerp(rightLeg.xRot, -35 * R * st, w);
            }
            case 6 -> { // прижаться ближе: обнимает, голову кладёт набок
                set(rightArm, -62 * R, -38 * R, 0, w); set(leftArm, -62 * R, 38 * R, 0, w);
                add(head, 6 * R, 0, 12 * R + s1 * 2 * R, w);
            }
            case 7 -> { // встретить: руки широко, затем объятие
                float c = ss((t - 30f) / 34f);
                set(rightArm, lerp(-30 * R, -86 * R, c), lerp(10 * R, -48 * R, c), lerp(-72 * R, 0, c), w);
                set(leftArm, lerp(-30 * R, -86 * R, c), lerp(-10 * R, 48 * R, c), lerp(72 * R, 0, c), w);
                add(head, -4 * R * (1 - c), 0, 6 * R * c, w);
            }
            case 8 -> { // смущение: прячется, голова вниз, покачивает головой
                set(rightArm, -52 * R, -52 * R, 0, w); set(leftArm, -52 * R, 52 * R, 0, w);
                add(head, 16 * R, s1 * 7 * R, 8 * R, w);
            }
            default -> { return; }
        }
        leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg); hat.copyFrom(head);
    }
}
