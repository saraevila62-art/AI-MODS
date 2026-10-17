package com.example.miyu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Круговое меню (колесо) Мию. */
public class MiyuWheel extends Screen {
    static final String[] L = {"Ко мне", "За мной", "Жди здесь", "Коснуться", "Обнять", "Меню"};
    static final int R = 68, IN = 24;
    final MiyuEntity e;
    boolean touch;
    int cx, cy, hover = -1;
    final List<int[]> runs = new ArrayList<>();

    public MiyuWheel(MiyuEntity e) { super(Component.literal("Мию")); this.e = e; }
    @Override public boolean isPauseScreen() { return false; }

    static int sectorAt(int dx, int dy) {
        double d = Math.hypot(dx, dy);
        if (d > R || d < IN) return -1;
        if (d > R - 2 || d < IN + 2) return 6;
        double rel = (Math.toDegrees(Math.atan2(dy, dx)) + 120 + 720) % 360;
        double m = rel % 60;
        if (m < 1.2 || m > 58.8) return -1;
        return (int) (rel / 60);
    }

    @Override protected void init() {
        cx = width / 2; cy = height / 2 - 6; runs.clear();
        if (!touch) {
            for (int dy = -R; dy <= R; dy++) {
                int start = 0, sec = -1;
                for (int dx = -R; dx <= R + 1; dx++) {
                    int s = dx > R ? -1 : sectorAt(dx, dy);
                    if (s != sec) {
                        if (sec >= 0) runs.add(new int[]{start, dy, dx - start, sec});
                        sec = s; start = dx;
                    }
                }
            }
        } else {
            String[] n = {"Макушка", "Рука", "Плечо", "← Назад"};
            for (int i = 0; i < n.length; i++) {
                int idx = i;
                addRenderableWidget(new MiyuScreen.RowButton(cx - 50, cy - 36 + i * 22, 100, 18, Component.literal(n[i]), b -> touchAct(idx), false));
            }
        }
    }

    void send(int c) { PacketDistributor.sendToServer(new CmdPayload(e.getId(), c)); }
    void say(String t) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.displayClientMessage(Component.literal("§d[Мию]§r " + t), false);
    }

    void touchAct(int i) {
        switch (i) {
            case 0 -> { send(11); say("Мм... так приятно~"); onClose(); }
            case 1 -> { send(13); say("Твоя рука такая тёплая..."); onClose(); }
            case 2 -> { send(12); say("Я здесь!"); onClose(); }
            default -> { touch = false; rebuildWidgets(); }
        }
    }

    void act(int i) {
        switch (i) {
            case 0 -> { send(2); onClose(); }
            case 1 -> { send(0); onClose(); }
            case 2 -> { send(1); onClose(); }
            case 3 -> { touch = true; rebuildWidgets(); }
            case 4 -> { send(10); say("*крепко-крепко обнимает*"); onClose(); }
            default -> Minecraft.getInstance().setScreen(new MiyuScreen(e));
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (!touch && hover >= 0) { act(hover); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x88000000);
        if (!touch) {
            int s = sectorAt(mx - cx, my - cy);
            hover = (s >= 0 && s < 6) ? s : -1;
            for (int[] r : runs) {
                int col = r[3] == 6 ? 0xFFC98B6A : (r[3] == hover ? 0xFF4A2358 : 0xFF1B1228);
                g.fill(cx + r[0], cy + r[1], cx + r[0] + r[2], cy + r[1] + 1, col);
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(-90 + i * 60);
                int lx = cx + (int) (Math.cos(a) * (R + IN) / 2.0), ly = cy + (int) (Math.sin(a) * (R + IN) / 2.0);
                g.drawCenteredString(font, L[i], lx, ly - 4, i == hover ? 0xFFFFD27F : 0xFFFFFFFF);
            }
            g.drawCenteredString(font, "Мию", cx, cy - 4, 0xFFFFFFFF);
            g.drawCenteredString(font, "Колесо: персонаж · Esc: закрыть", cx, cy + R + 10, 0xFFCCCCCC);
        } else {
            g.drawCenteredString(font, "Коснуться", cx, cy - 54, 0xFFFFD27F);
        }
        super.render(g, mx, my, pt);
    }
}
