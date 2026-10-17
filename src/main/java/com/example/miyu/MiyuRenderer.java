package com.example.miyu;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MiyuRenderer extends HumanoidMobRenderer<MiyuEntity, MiyuModel> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Miyu.ID, "textures/entity/miyu.png");

    public MiyuRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MiyuModel(ctx.bakeLayer(ClientSetup.LAYER), Cfg.slim()), 0.5f);
    }
    @Override public ResourceLocation getTextureLocation(MiyuEntity e) { return TEX; }
}
