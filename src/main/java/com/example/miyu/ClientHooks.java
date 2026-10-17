package com.example.miyu;

import net.minecraft.client.Minecraft;

public class ClientHooks {
    public static void open(MiyuEntity e) { Minecraft.getInstance().setScreen(new MiyuWheel(e)); }
}
