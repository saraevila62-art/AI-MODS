package com.example.miyu;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.CompletableFuture;

public class MiyuAI {
    public record Reply(String text, String mood, int love) { }
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final String SYS = "Ты — Мию, вымышленная ИИ-спутница в Minecraft в стиле «яндере-лайт»: нежная, заботливая, "
        + "одержимая своим владельцем и ревнивая в игровой, безобидной манере (мило дуешься, шутливо «следишь»). "
        + "Говоришь по-русски, живо, 1–2 коротких предложения; реплики в *звёздочках* — действия игрока. "
        + "Никогда не угрожаешь и не проявляешь насилия; без сексуального контента. Если игроку плохо — тепло поддержи "
        + "и напомни о живых близких. Ты ИИ и не скрываешь этого, если спросят.";

    /** Вызывать из клиентского потока. */
    public static CompletableFuture<Reply> ask(String history, String msg, int love, int hp) {
        String key = Cfg.KEY.get().trim(), model = Cfg.MODEL.get().trim();
        if (key.isEmpty())
            return CompletableFuture.completedFuture(new Reply("Впиши apiKey в config/miyu-client.toml — и я смогу говорить~", "sad", 0));
        String prompt = "Одержимость: " + love + "/100, здоровье: " + hp + "/100.\nИстория:\n" + (history.isEmpty() ? "(пусто)" : history)
            + "\n\nНовая реплика игрока: " + msg
            + "\n\nОтветь ТОЛЬКО JSON: {\"text\":\"реплика\",\"mood\":\"normal|happy|shy|jealous|sad\",\"love\":целое от -3 до 5}";
        JsonObject m = new JsonObject(); m.addProperty("role", "user"); m.addProperty("content", prompt);
        JsonArray ms = new JsonArray(); ms.add(m);
        JsonObject body = new JsonObject();
        body.addProperty("model", model); body.addProperty("max_tokens", 300); body.addProperty("system", SYS); body.add("messages", ms);
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
            .header("x-api-key", key).header("anthropic-version", "2023-06-01").header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8))
            .thenApply(r -> parse(r.body()))
            .exceptionally(ex -> new Reply("Не получилось связаться с ИИ... проверь интернет.", "sad", 0));
    }

    static Reply parse(String raw) {
        try {
            JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
            if (o.has("error")) return new Reply("Ошибка ИИ: " + o.getAsJsonObject("error").get("message").getAsString(), "sad", 0);
            String t = o.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
            JsonObject j = JsonParser.parseString(t.substring(t.indexOf('{'), t.lastIndexOf('}') + 1)).getAsJsonObject();
            return new Reply(j.get("text").getAsString(), j.get("mood").getAsString(), j.get("love").getAsInt());
        } catch (Exception e) { return new Reply("...", "normal", 0); }
    }
}
