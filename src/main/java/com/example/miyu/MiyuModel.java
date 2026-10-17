package com.example.miyu;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Модель игрока + жесты Мию (поверх обычной анимации). */
public class MiyuModel extends PlayerModel<MiyuEntity> {
    static final float R = (float) Math.PI / 180f;

    public MiyuModel(ModelPart root) { super(root, false); }

    static float lerp(float a, float b, float w) { return a + (b - a) * w; }
    static void set(ModelPart p, float x, float y, float z, float w) {
        p.xRot = lerp(p.xRot, x, w); p.yRot = lerp(p.yRot, y, w); p.zRot = lerp(p.zRot, z, w);
    }

    @Override
    public void setupAnim(MiyuEntity e, float ls, float lsa, float age, float yaw, float pitch) {
        super.setupAnim(e, ls, lsa, age, yaw, pitch);
        int g = e.gestureId();
        if (g == 0) return;
        float t = age - e.gestureStart();
        if (t < 0 || t > MiyuEntity.G_LEN) return;
        float w = Math.min(1f, Math.min(t / 8f, (MiyuEntity.G_LEN - t) / 8f));
        switch (g) {
            case 1 -> { // обнять
                float s = (float) Math.sin(t * 0.2f) * 2 * R;
                set(rightArm, -80 * R + s, -45 * R, 0, w); set(leftArm, -80 * R + s, 45 * R, 0, w);
            }
            case 2 -> { // погладить по голове
                float s = (float) Math.sin(t * 0.6f) * 14 * R;
                set(rightArm, -105 * R + s, -10 * R, 0, w);
            }
            case 3 -> { // помахать
                float s = (float) Math.sin(t * 0.7f) * 25 * R;
                set(rightArm, -165 * R, 0, -10 * R + s, w);
            }
            case 4 -> { // взять за руку
                set(rightArm, -50 * R, -30 * R, 0, w); set(leftArm, -50 * R, 30 * R, 0, w);
            }
            case 5 -> { // ревнивый топот, руки скрещены
                set(rightArm, -35 * R, -60 * R, 0, w); set(leftArm, -35 * R, 60 * R, 0, w);
                float st = Math.max(0f, (float) Math.sin(t * 0.9f));
                rightLeg.xRot = lerp(rightLeg.xRot, -35 * R * st, w);
            }
            default -> { return; }
        }
        leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg); hat.copyFrom(head);
    }
}
