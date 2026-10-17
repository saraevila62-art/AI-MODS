package com.example.miyu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Круговое меню (колесо) Мию + списки «Коснуться» и «Жесты». */
public class MiyuWheel extends Screen {
    static final String[] L = {"Ко мне", "За мной", "Жди здесь", "Коснуться", "Жесты", "Меню"};
    /** {название, команда (9 + номер жеста), реплика в чат} */
    static final Object[][] TOUCH = {{"Макушка", 11, "Мм... так приятно~"}, {"Рука", 13, "Твоя рука такая тёплая..."},
        {"Плечо", 15, "Можно, я прислонюсь?.."}};
    static final Object[][] GEST = {{"Обнять", 10, "*крепко-крепко обнимает*"}, {"Взять за руку", 13, "*берёт за руку*"},
        {"Прижаться", 15, "*прижимается ближе*"}, {"Встретить", 16, "Наконец-то ты рядом!"},
        {"Помахать", 12, "Привет~"}, {"Смущение", 17, "Ой... не смотри так..."}};
    static final int R = 68, IN = 24;
    final MiyuEntity e;
    int mode; // 0 колесо, 1 коснуться, 2 жесты
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
        if (mode == 0) {
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
            Object[][] items = mode == 1 ? TOUCH : GEST;
            int n = items.length + 1;
            for (int i = 0; i < n; i++) {
                int idx = i;
                boolean back = i == items.length;
                String label = back ? "← Назад" : (String) items[i][0];
                int x = cx - 98 + (i % 2) * 100, y = cy - 44 + (i / 2) * 22;
                addRenderableWidget(new MiyuScreen.RowButton(x, y, 96, 18, Component.literal(label), b -> {
                    if (back) { mode = 0; rebuildWidgets(); }
                    else { send((Integer) items[idx][1]); say((String) items[idx][2]); onClose(); }
                }, false));
            }
        }
    }

    void send(int c) { PacketDistributor.sendToServer(new CmdPayload(e.getId(), c)); }
    void say(String t) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.displayClientMessage(Component.literal("§d[Мию]§r " + t), false);
    }

    void act(int i) {
        switch (i) {
            case 0 -> { send(2); onClose(); }
            case 1 -> { send(0); onClose(); }
            case 2 -> { send(1); onClose(); }
            case 3 -> { mode = 1; rebuildWidgets(); }
            case 4 -> { mode = 2; rebuildWidgets(); }
            default -> Minecraft.getInstance().setScreen(new MiyuScreen(e));
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (mode == 0 && hover >= 0) { act(hover); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x88000000);
        if (mode == 0) {
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
            g.drawCenteredString(font, Cfg.name(), cx, cy - 4, 0xFFFFFFFF);
            g.drawCenteredString(font, "Колесо: персонаж · Esc: закрыть", cx, cy + R + 10, 0xFFCCCCCC);
        } else {
            g.drawCenteredString(font, mode == 1 ? "Коснуться" : "Жесты", cx, cy - 62, 0xFFFFD27F);
        }
        super.render(g, mx, my, pt);
    }
}
