package com.example.miyu;

import com.google.gson.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.loading.FMLPaths;

/** Сцена из JSON (формат как у парных анимаций): дорожка "npc" играет Мию. */
public class MiyuScene {
    public static final int N = 11;
    static final String[] PARTS = {"head", "chest", "body", "right_arm", "left_arm", "right_forearm", "left_forearm",
        "right_leg", "left_leg", "right_shin", "left_shin"};

    public static final class Pose { public final float[][] v = new float[N][3]; public final float[] w = new float[N]; public float z; }
    static final class Frame { int tick; float z; String main; final float[][] v = new float[N][3]; final float[] w = new float[N]; }
    static final class Cue { int tick; String text; int color = 0xFFFFFF; }

    public static final class Track {
        final List<Frame> frames = new ArrayList<>();
        final List<Cue> cues = new ArrayList<>();
        final List<int[]> glow = new ArrayList<>();
        int duration;

        public Pose sample(float t) {
            Pose p = new Pose();
            if (frames.isEmpty()) return p;
            Frame a = frames.get(0), b = a;
            Frame last = frames.get(frames.size() - 1);
            if (t >= last.tick) { a = last; b = last; }
            else if (t > a.tick) {
                for (int i = 0; i < frames.size() - 1; i++) {
                    if (t >= frames.get(i).tick && t < frames.get(i + 1).tick) { a = frames.get(i); b = frames.get(i + 1); break; }
                }
            }
            float f = a == b ? 0f : (t - a.tick) / (float) (b.tick - a.tick);
            f = f * f * (3f - 2f * f);
            for (int i = 0; i < N; i++) {
                for (int k = 0; k < 3; k++) p.v[i][k] = a.v[i][k] + (b.v[i][k] - a.v[i][k]) * f;
                p.w[i] = a.w[i] + (b.w[i] - a.w[i]) * f;
            }
            p.z = a.z + (b.z - a.z) * f;
            return p;
        }
        String mainHand(int t) { String m = ""; for (Frame f : frames) if (f.tick <= t && f.main != null) m = f.main; return m; }
        public boolean glowAt(int t) { boolean on = false; for (int[] g : glow) if (g[0] <= t) on = g[1] == 1; return on; }
    }

    public final String name, fx;
    public final int duration;
    public final Track npc, player;

    MiyuScene(String name, String fx, Track npc, Track player) {
        this.name = name; this.fx = fx; this.npc = npc; this.player = player;
        this.duration = Math.max(npc.duration, player.duration);
    }

    /** Расстояние между Мию и игроком в момент t (как сумма |z| обеих дорожек). */
    public float dist(float t) {
        float d = Math.abs(npc.sample(t).z) + Math.abs(player.sample(t).z);
        return d < 0.15f ? 0.8f : d;
    }
    public String mainHand(int t) { return npc.mainHand(t); }

    /** Показывает реплики сцены владельцу в чате (вызывать на сервере каждый тик). */
    public void fireCues(int t, Entity owner, String who) {
        for (Cue c : npc.cues) if (c.tick == t) {
            owner.sendSystemMessage(Component.literal("[" + who + "] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(c.text).withStyle(Style.EMPTY.withColor(c.color & 0xFFFFFF))));
        }
        for (Cue c : player.cues) if (c.tick == t) {
            owner.sendSystemMessage(Component.literal("[Вы] ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(c.text).withStyle(ChatFormatting.WHITE)));
        }
    }

    // ---------- загрузка ----------
    static Track parseTrack(JsonObject o) {
        Track tr = new Track();
        if (o == null) return tr;
        if (o.has("frames")) for (JsonElement fe : o.getAsJsonArray("frames")) {
            JsonObject fo = fe.getAsJsonObject();
            Frame f = new Frame();
            f.tick = fo.get("tick").getAsInt();
            if (fo.has("motion") && fo.getAsJsonObject("motion").has("z")) f.z = fo.getAsJsonObject("motion").get("z").getAsFloat();
            if (fo.has("pose")) {
                JsonObject po = fo.getAsJsonObject("pose");
                for (int i = 0; i < N; i++) if (po.has(PARTS[i])) {
                    JsonArray a = po.getAsJsonArray(PARTS[i]);
                    if (a.size() > 0) { f.w[i] = 1f; for (int k = 0; k < 3 && k < a.size(); k++) f.v[i][k] = a.get(k).getAsFloat(); }
                }
            }
            if (fo.has("items")) {
                JsonObject it = fo.getAsJsonObject("items");
                f.main = it.has("mainHand") ? it.get("mainHand").getAsString() : "";
            }
            tr.frames.add(f);
        }
        tr.frames.sort(Comparator.comparingInt(f -> f.tick));
        if (o.has("cues")) for (JsonElement ce : o.getAsJsonArray("cues")) {
            JsonObject co = ce.getAsJsonObject();
            if (!co.has("text")) continue;
            Cue c = new Cue();
            c.tick = co.get("tick").getAsInt(); c.text = co.get("text").getAsString();
            if (co.has("color")) c.color = co.get("color").getAsInt();
            tr.cues.add(c);
        }
        if (o.has("eyeGlow")) for (JsonElement ge : o.getAsJsonArray("eyeGlow")) {
            JsonObject go = ge.getAsJsonObject();
            tr.glow.add(new int[]{go.get("tick").getAsInt(), go.get("enabled").getAsBoolean() ? 1 : 0});
        }
        tr.glow.sort(Comparator.comparingInt(g -> g[0]));
        tr.duration = o.has("duration") ? o.get("duration").getAsInt() : (tr.frames.isEmpty() ? 0 : tr.frames.get(tr.frames.size() - 1).tick);
        return tr;
    }

    static MiyuScene parse(JsonObject root, String name, String fx) {
        return new MiyuScene(name, fx, parseTrack(root.has("npc") ? root.getAsJsonObject("npc") : null),
            parseTrack(root.has("player") ? root.getAsJsonObject("player") : null));
    }

    static final List<MiyuScene> ALL = new ArrayList<>();
    static boolean loaded;

    public static synchronized void ensure() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = MiyuScene.class.getResourceAsStream("/assets/miyu/scenes/index.json")) {
            if (in != null) {
                JsonArray idx = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement el : idx) {
                    JsonObject o = el.getAsJsonObject();
                    try (InputStream s = MiyuScene.class.getResourceAsStream("/assets/miyu/scenes/" + o.get("file").getAsString())) {
                        if (s == null) continue;
                        JsonObject root = JsonParser.parseReader(new InputStreamReader(s, StandardCharsets.UTF_8)).getAsJsonObject();
                        ALL.add(parse(root, o.get("name").getAsString(), o.has("fx") ? o.get("fx").getAsString() : ""));
                    } catch (Exception ignored) { }
                }
            }
        } catch (Exception ignored) { }
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("miyu").resolve("scenes");
            Files.createDirectories(dir);
            List<Path> files = new ArrayList<>();
            try (var st = Files.list(dir)) { st.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add); }
            for (Path p : files) {
                try (var r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                    String n = p.getFileName().toString();
                    ALL.add(parse(root, n.substring(0, n.length() - 5), ""));
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
    }

    public static MiyuScene get(int i) { ensure(); return i >= 0 && i < ALL.size() ? ALL.get(i) : null; }
    public static int count() { ensure(); return Math.min(ALL.size(), 250); }
}
