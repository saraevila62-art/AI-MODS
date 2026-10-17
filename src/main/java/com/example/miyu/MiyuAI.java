package com.example.miyu;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class MiyuAI {
    public record Reply(String text, String mood, int love) { }
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String sys() {
        boolean m = Cfg.male();
        String who = m
            ? "вымышленный ИИ-спутник в Minecraft в стиле «яндере-лайт»: нежный, заботливый, одержимый своим владельцем и ревнивый"
            : "вымышленная ИИ-спутница в Minecraft в стиле «яндере-лайт»: нежная, заботливая, одержимая своим владельцем и ревнивая";
        return "Ты — " + Cfg.name() + ", " + who + " в игровой, безобидной манере (мило дуешься, шутливо «следишь»). "
            + "О себе говоришь в " + (m ? "мужском" : "женском") + " роде. Говоришь по-русски, живо, 1–2 коротких предложения; "
            + "реплики в *звёздочках* — действия игрока. Никогда не угрожаешь и не проявляешь насилия; без сексуального контента. "
            + "Если игроку плохо — тепло поддержи и напомни о живых близких. Ты ИИ и не скрываешь этого, если спросят.";
    }

    /** Вызывать из клиентского потока (читает конфиг). */
    public static CompletableFuture<Reply> ask(String history, String msg, int love, int hp) {
        String key = Cfg.KEY.get().trim(), model = Cfg.MODEL.get().trim(), prov = Cfg.PROVIDER.get().trim().toLowerCase();
        String base = Cfg.BASE.get().trim().replaceAll("/+$", ""), extra = Cfg.PERSONA.get().trim();
        if (key.isEmpty())
            return CompletableFuture.completedFuture(new Reply("Впиши apiKey в config/miyu-client.toml — и я смогу говорить~", "error", 0));
        String sys = sys() + (extra.isEmpty() ? "" : "\nДополнительно от игрока: " + extra);
        String prompt = "Одержимость: " + love + "/100, здоровье: " + hp + "/100.\nИстория:\n" + (history.isEmpty() ? "(пусто)" : history)
            + "\n\nНовая реплика игрока: " + msg
            + "\n\nОтветь ТОЛЬКО JSON: {\"text\":\"реплика\",\"mood\":\"normal|happy|shy|jealous|sad\",\"love\":целое от -3 до 5}";
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", 400);
        JsonArray ms = new JsonArray();
        HttpRequest.Builder rb;
        if (prov.equals("anthropic")) {
            body.addProperty("system", sys);
            ms.add(msgObj("user", prompt));
            rb = HttpRequest.newBuilder(URI.create(base.isEmpty() ? "https://api.anthropic.com/v1/messages" : base + "/messages"))
                .header("x-api-key", key).header("anthropic-version", "2023-06-01");
        } else {
            ms.add(msgObj("system", sys));
            ms.add(msgObj("user", prompt));
            rb = HttpRequest.newBuilder(URI.create(base + "/chat/completions")).header("Authorization", "Bearer " + key);
        }
        body.add("messages", ms);
        HttpRequest req = rb.header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(r -> parse(r.body()))
            .exceptionally(ex -> new Reply("Не получилось связаться с ИИ... проверь интернет и baseUrl.", "error", 0));
    }

    static JsonObject msgObj(String role, String content) {
        JsonObject m = new JsonObject(); m.addProperty("role", role); m.addProperty("content", content); return m;
    }

    static Reply parse(String raw) {
        try {
            JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
            if (o.has("error")) {
                JsonElement er = o.get("error");
                String msg = er.isJsonObject() && er.getAsJsonObject().has("message") ? er.getAsJsonObject().get("message").getAsString() : er.toString();
                return new Reply("Ошибка ИИ: " + msg, "error", 0);
            }
            String t = o.has("choices")
                ? o.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString()
                : o.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
            JsonObject j = JsonParser.parseString(t.substring(t.indexOf('{'), t.lastIndexOf('}') + 1)).getAsJsonObject();
            return new Reply(j.get("text").getAsString(), j.get("mood").getAsString(), j.get("love").getAsInt());
        } catch (Exception e) { return new Reply("...", "normal", 0); }
    }
}
