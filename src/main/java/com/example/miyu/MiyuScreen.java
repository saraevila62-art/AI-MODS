package com.example.miyu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    static String say = null, mood = "happy";
    static String g(String f, String m) { return Cfg.male() ? m : f; }
    static String shown() { return say != null ? say : g("Ты пришёл... я так ждала~", "Ты пришёл... я так ждал~"); }
    static final List<String> hist = new ArrayList<>();
    /** {название, сообщение для ИИ, номер жеста} */
    static final String[][] ACT = {{"Погладить", "*нежно глажу тебя по голове*", "2"}, {"Обнять", "*крепко обнимаю тебя*", "1"},
        {"Взять за руку", "*беру тебя за руку*", "4"}, {"Помахать", "*машу тебе рукой*", "3"},
        {"Признаться", "Я люблю тебя.", "1"}, {"Прижаться", "*прижимаюсь к тебе ближе*", "6"}};
    static final String[] TABS = {"Общение", "Действия", "Приказы", "Сцены"};
    static final String[] TKEY = {"chat", "act", "ord", "sc"};

    final MiyuEntity e;
    String view = "act";
    EditBox box;
    boolean busy;
    int px, py, pw = 240, ph = 176, y0;

    public MiyuScreen(MiyuEntity e) { super(Component.literal("Мию")); this.e = e; }
    @Override public boolean isPauseScreen() { return false; }
    /** Без размытия фона. */
    @Override public void renderBackground(GuiGraphics gg, int mx, int my, float pt) { }

    @Override protected void init() {
        px = (width - pw) / 2; py = Math.max(2, (height - ph - 44) / 2); box = null; y0 = py + 70;
        int tw = (pw - 28) / 4;
        for (int i = 0; i < 4; i++) {
            int idx = i;
            addRenderableWidget(new RowButton(px + 8 + i * (tw + 4), py + 50, tw, 16, Component.literal(TABS[i]), b -> go(TKEY[idx]), view.equals(TKEY[i])));
        }
        switch (view) {
            case "chat" -> {
                box = new EditBox(font, px + 8, y0, pw - 16, 18, Component.literal(""));
                box.setMaxLength(200); addRenderableWidget(box); setInitialFocus(box);
            }
            case "act" -> {
                String[] l = new String[ACT.length];
                for (int i = 0; i < l.length; i++) l[i] = ACT[i][0];
                grid(y0, l, i -> { gesture(Integer.parseInt(ACT[i][2])); send(ACT[i][1]); });
            }
            case "sc" -> {
                int n = Math.min(MiyuScene.count(), 7);
                String[] l = new String[n + 1];
                for (int i = 0; i < n; i++) l[i] = MiyuScene.get(i).name;
                l[n] = "■ Стоп";
                grid(y0, l, i -> {
                    if (i < n) { PacketDistributor.sendToServer(new CmdPayload(e.getId(), 100 + i)); onClose(); }
                    else PacketDistributor.sendToServer(new CmdPayload(e.getId(), 99));
                });
            }
            default -> grid(y0, new String[]{"За мной", "Жди здесь", "Ко мне"}, i -> {
                if (i == 0) cmd(0, "Пойдём со мной."); else if (i == 1) cmd(1, "Подожди меня здесь."); else cmd(2, "Иди ко мне.");
            });
        }
        int hw = (pw - 20) / 2;
        addRenderableWidget(new RowButton(px + 8, py + ph - 22, hw, 16, Component.literal("Колесо"), b -> Minecraft.getInstance().setScreen(new MiyuWheel(e)), false));
        addRenderableWidget(new RowButton(px + 12 + hw, py + ph - 22, hw, 16, Component.literal("Закрыть"), b -> onClose(), false));
    }

    void grid(int y, String[] l, java.util.function.IntConsumer f) {
        int w = (pw - 20) / 2;
        for (int i = 0; i < l.length; i++) {
            int idx = i;
            addRenderableWidget(new RowButton(px + 8 + (i % 2) * (w + 4), y + (i / 2) * 20, w, 16, Component.literal(l[i]), b -> f.accept(idx), false));
        }
    }
    void go(String v) { view = v; rebuildWidgets(); }
    void gesture(int gg) { if (gg > 0) PacketDistributor.sendToServer(new CmdPayload(e.getId(), 9 + gg)); }
    void cmd(int c, String msg) { PacketDistributor.sendToServer(new CmdPayload(e.getId(), c)); send(msg); }

    @Override public boolean keyPressed(int k, int s, int m) {
        if ((k == 257 || k == 335) && box != null && !box.getValue().isBlank()) { send(box.getValue()); box.setValue(""); return true; }
        return super.keyPressed(k, s, m);
    }

    static int col(String m) {
        return switch (m) { case "jealous" -> 0xFFFF5555; case "sad" -> 0xFFA9B8FF; case "happy" -> 0xFFFFB3D1;
            case "shy" -> 0xFFFFB6C1; case "error" -> 0xFFFFAA00; default -> 0xFFFFFFFF; };
    }
    static String lab(String m) {
        String n = Cfg.name();
        return switch (m) { case "jealous" -> "[" + n + " — ЯРОСТЬ] //"; case "sad" -> "[" + n + " — ГРУСТЬ] //";
            case "happy" -> "[" + n + " — РАДОСТЬ] //"; case "shy" -> "[" + n + " — СМУЩЕНИЕ] //"; default -> "[" + n + "]"; };
    }

    void send(String msg) {
        if (busy) return;
        busy = true; say = "…"; mood = "normal";
        String h = String.join("\n", hist.subList(Math.max(0, hist.size() - 10), hist.size()));
        MiyuAI.ask(h, msg, love, hp).thenAccept(r -> Minecraft.getInstance().execute(() -> {
            busy = false; say = r.text(); mood = r.mood();
            if (!mood.equals("error")) {
                if (mood.equals("jealous")) gesture(5);
                love = Math.max(0, Math.min(100, love + r.love()));
                int d = switch (mood) { case "happy" -> 4; case "shy" -> 2; case "jealous" -> -3; case "sad" -> -6; default -> 0; };
                hp = Math.max(0, Math.min(100, hp + d));
                hist.add("Игрок: " + msg); hist.add(Cfg.name() + ": " + r.text());
            }
            var p = Minecraft.getInstance().player;
            String c = mood.equals("jealous") ? "§c" : "§d";
            if (p != null) p.displayClientMessage(Component.literal(c + lab(mood) + "§r " + r.text()), false);
        }));
    }

    static void bar(GuiGraphics gg, int x, int y, int w, int h, int pct, int colr, String label, Font f) {
        gg.fill(x, y, x + w, y + h, 0xFF07050C);
        gg.fill(x, y, x + w * pct / 100, y + h, colr);
        gg.renderOutline(x, y, w, h, 0xFF2D2048);
        gg.drawString(f, label, x + 3, y + 1, 0xFFFFFFFF, true);
        String s = pct + "%"; gg.drawString(f, s, x + w - 3 - f.width(s), y + 1, 0xFFFFFFFF, true);
    }

    @Override public void render(GuiGraphics gg, int mx, int my, float pt) {
        gg.fill(0, 0, width, height, 0x99000000);
        gg.fill(px, py, px + pw, py + ph, 0xFF120C1E);
        gg.renderOutline(px, py, pw, ph, 0xFF5B3A8A); gg.renderOutline(px + 2, py + 2, pw - 4, ph - 4, 0xFF2D2048);
        gg.blit(TEX, px + 8, py + 8, 36, 36, 8, 8, 8, 8, 64, 64);
        gg.blit(TEX, px + 8, py + 8, 36, 36, 40, 8, 8, 8, 64, 64);
        gg.renderOutline(px + 8, py + 8, 36, 36, 0xFF5B3A8A);
        gg.drawString(font, Cfg.name(), px + 50, py + 8, 0xFFECE6F7, true);
        gg.drawString(font, g("— одержима, ревнивая", "— одержим, ревнив"), px + 50 + font.width(Cfg.name() + " "), py + 8, 0xFFA898C8, false);
        bar(gg, px + 50, py + 20, pw - 58, 10, hp, 0xFFFF4A63, "Здоровье", font);
        bar(gg, px + 50, py + 33, pw - 58, 10, love, 0xFFFF3D8B, "Одержимость", font);
        gg.fill(px + 8, py + 46, px + pw - 8, py + 47, 0xFF2D2048);
        if (view.equals("chat")) {
            String st = MiyuAI.status();
            gg.drawString(font, st.isEmpty() ? "Enter — отправить" : st, px + 10, y0 + 24, st.isEmpty() ? 0xFFA898C8 : 0xFFFFAA00, false);
        }
        super.render(gg, mx, my, pt);
        int dy = py + ph + 4;
        int ac = col(mood);
        gg.fill(px, dy, px + pw, dy + 38, 0xCC000000); gg.fill(px, dy, px + 3, dy + 38, mood.equals("jealous") ? 0xFFFF3030 : 0xFFFF3D8B);
        int i = 0;
        for (var l : font.split(Component.literal(lab(mood) + " " + shown()), pw - 14)) { if (i >= 3) break; gg.drawString(font, l, px + 7, dy + 4 + i * 10, ac, false); i++; }
    }

    static class RowButton extends Button {
        final boolean sel;
        RowButton(int x, int y, int w, int h, Component m, OnPress p, boolean sel) { super(x, y, w, h, m, p, DEFAULT_NARRATION); this.sel = sel; }
        @Override public void renderWidget(GuiGraphics gg, int mx, int my, float pt) {
            boolean hv = isHoveredOrFocused();
            gg.fill(getX(), getY(), getX() + width, getY() + height, hv || sel ? 0xFF1C1230 : 0xFF150E22);
            gg.renderOutline(getX(), getY(), width, height, sel ? 0xFFFF3D8B : (hv ? 0xFFA259E0 : 0xFF2D2048));
            if (hv) gg.fill(getX(), getY(), getX() + 2, getY() + height, 0xFFFF3D8B);
            var f = Minecraft.getInstance().font;
            gg.drawString(f, getMessage(), getX() + (width - f.width(getMessage())) / 2, getY() + (height - 8) / 2, 0xFFECE6F7, false);
        }
    }
}
