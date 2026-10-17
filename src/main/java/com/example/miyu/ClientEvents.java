package com.example.miyu;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Miyu.ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post ev) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        while (ClientSetup.WHEEL.consumeClick()) {
            MiyuEntity best = null; double bd = 36;
            for (var en : mc.level.getEntitiesOfClass(MiyuEntity.class, mc.player.getBoundingBox().inflate(6))) {
                if (!en.isTame() || en.isOwnedBy(mc.player)) {
                    double d = en.distanceToSqr(mc.player);
                    if (d < bd) { bd = d; best = en; }
                }
            }
            if (best != null) mc.setScreen(new MiyuWheel(best));
        }
    }
}
