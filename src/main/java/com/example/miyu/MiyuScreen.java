package com.example.miyu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class MiyuScreen extends Screen {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Miyu.ID, "textures/entity/miyu.png");
    static int love = 40, hp = 100;
    static String say = "Ты пришёл... я так ждала~", mood = "happy";
    static final List<String> hist = new ArrayList<>();
    static final String[][] ACT = {{"Погладить по голове", "*нежно глажу тебя по голове*"}, {"Взять за руку", "*беру тебя за руку*"},
        {"Обнять", "*крепко обнимаю тебя*"}, {"Похвалить", "*говорю, что ты умница*"}};
    static final String[][] ROM = {{"Признаться в любви", "Я люблю тебя."}, {"Позвать на свидание", "Пойдём на свидание?"},
        {"Подарить цветок", "*дарю тебе цветок*"}};

    final MiyuEntity e;
    String view = "menu";
    EditBox box;
    boolean busy;
    int px, py, pw = 300, ph = 330;

    public MiyuScreen(MiyuEntity e) { super(Component.literal("Мию")); this.e = e; }
    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        px = (width - pw) / 2; py = (height - ph) / 2; box = null;
        int y = py + 98;
        switch (view) {
            case "menu" -> {
                String[][] m = {{"Общение", "chat"}, {"Взаимодействия", "act"}, {"Романтика", "rom"}, {"Приказы и место", "ord"},
                    {"Переодеть", "stub"}, {"Инвентарь", "stub"}, {"Настройки", "stub"}, {"Закрыть", "close"}};
                for (String[] r : m) { row(y, r[0], () -> { if (r[1].equals("close")) onClose(); else go(r[1]); }); y += 28; }
            }
            case "chat" -> {
                row(y, "← Назад", () -> go("menu"));
                box = new EditBox(font, px + 12, y + 34, pw - 24, 20, Component.literal(""));
                box.setMaxLength(200); addRenderableWidget(box); setInitialFocus(box);
            }
            case "act" -> { row(y, "← Назад", () -> go("menu")); y += 28; for (String[] a : ACT) { String t = a[1]; row(y, a[0], () -> send(t)); y += 28; } }
            case "rom" -> { row(y, "← Назад", () -> go("menu")); y += 28; for (String[] a : ROM) { String t = a[1]; row(y, a[0], () -> send(t)); y += 28; } }
            case "ord" -> {
                row(y, "← Назад", () -> go("menu")); y += 28;
                row(y, "Следуй за мной", () -> cmd(0, "Пойдём со мной.")); y += 28;
                row(y, "Жди здесь", () -> cmd(1, "Подожди меня здесь.")); y += 28;
                row(y, "Ко мне!", () -> cmd(2, "Иди ко мне.")); 
            }
            default -> row(y, "← Назад (раздел скоро появится)", () -> go("menu"));
        }
    }

    void row(int y, String t, Runnable r) { addRenderableWidget(new RowButton(px + 12, y, pw - 24, 24, Component.literal(t), b -> r.run())); }
    void go(String v) { view = v; rebuildWidgets(); }
    void cmd(int c, String msg) { PacketDistributor.sendToServer(new CmdPayload(e.getId(), c)); send(msg); }

    @Override public boolean keyPressed(int k, int s, int m) {
        if ((k == 257 || k == 335) && box != null && !box.getValue().isBlank()) { send(box.getValue()); box.setValue(""); return true; }
        return super.keyPressed(k, s, m);
    }

    void send(String msg) {
        if (busy) return;
        busy = true; say = "…";
        String h = String.join("\n", hist.subList(Math.max(0, hist.size() - 10), hist.size()));
        MiyuAI.ask(h, msg, love, hp).thenAccept(r -> Minecraft.getInstance().execute(() -> {
            busy = false; say = r.text(); mood = r.mood();
            love = Math.max(0, Math.min(100, love + r.love()));
            int d = switch (mood) { case "happy" -> 4; case "shy" -> 2; case "jealous" -> -3; case "sad" -> -6; default -> 0; };
            hp = Math.max(0, Math.min(100, hp + d));
            hist.add("Игрок: " + msg); hist.add("Мию: " + r.text());
            var p = Minecraft.getInstance().player;
            if (p != null) p.displayClientMessage(Component.literal("§d[Мию]§r " + r.text()), false);
        }));
    }

    static void bar(GuiGraphics g, int x, int y, int w, int h, int pct, int col, String label, net.minecraft.client.gui.Font f) {
        g.fill(x, y, x + w, y + h, 0xFF07050C);
        g.fill(x, y, x + w * pct / 100, y + h, col);
        g.renderOutline(x, y, w, h, 0xFF2D2048);
        g.drawString(f, label, x + 4, y + 3, 0xFFFFFFFF, true);
        String s = pct + "%"; g.drawString(f, s, x + w - 4 - f.width(s), y + 3, 0xFFFFFFFF, true);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(px, py, px + pw, py + ph, 0xFF120C1E);
        g.renderOutline(px, py, pw, ph, 0xFF5B3A8A); g.renderOutline(px + 3, py + 3, pw - 6, ph - 6, 0xFF2D2048);
        g.blit(TEX, px + 12, py + 12, 64, 64, 8, 8, 8, 8, 64, 64);
        g.blit(TEX, px + 12, py + 12, 64, 64, 40, 8, 8, 8, 64, 64);
        g.renderOutline(px + 12, py + 12, 64, 64, 0xFF5B3A8A);
        g.drawString(font, "Мию", px + 84, py + 14, 0xFFECE6F7, true);
        g.drawString(font, "— одержима владельцем, ревнивая", px + 84, py + 27, 0xFFA898C8, false);
        bar(g, px + 84, py + 40, pw - 96, 14, hp, 0xFFFF4A63, "Здоровье", font);
        bar(g, px + 84, py + 58, pw - 96, 14, love, 0xFFFF3D8B, "Одержимость", font);
        g.fill(px + 12, py + 84, px + pw - 12, py + 86, 0xFF2D2048);
        super.render(g, mx, my, pt);
        int dy = py + ph + 6;
        g.fill(px, dy, px + pw, dy + 46, 0xCC000000); g.fill(px, dy, px + 3, dy + 46, 0xFFFF3D8B);
        int i = 0;
        for (var l : font.split(Component.literal("[Мию] " + say), pw - 16)) { if (i >= 4) break; g.drawString(font, l, px + 8, dy + 5 + i * 10, 0xFFFFFFFF, false); i++; }
    }

    static class RowButton extends Button {
        RowButton(int x, int y, int w, int h, Component m, OnPress p) { super(x, y, w, h, m, p, DEFAULT_NARRATION); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hv = isHoveredOrFocused();
            g.fill(getX(), getY(), getX() + width, getY() + height, hv ? 0xFF1C1230 : 0xFF150E22);
            g.renderOutline(getX(), getY(), width, height, hv ? 0xFFA259E0 : 0xFF2D2048);
            if (hv) g.fill(getX(), getY(), getX() + 3, getY() + height, 0xFFFF3D8B);
            g.drawString(Minecraft.getInstance().font, getMessage(), getX() + 12, getY() + (height - 8) / 2, 0xFFECE6F7, false);
        }
    }
}
