package fun.ogi.util.funevents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import fun.ogi.util.ClientLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class FunEventsUtil {
    private static final String BASE_URL = "https://api.funtime.su/method/";
    private static final String TOKEN = "3e4f95a.320252b03bb05e4d3675aca64437e66b";
    private static final int MAX_SERVERS_PER_REQUEST = 30;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(7))
            .build();

    private final Map<String, String> params = new LinkedHashMap<>();

    public FunEventsUtil() {
    }

    public List<String> getServers() throws IOException {
        String body = get("servers-info", Collections.emptyMap());
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("response") || !root.get("response").isJsonArray()) {
                return Collections.emptyList();
            }
            List<String> servers = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("response")) {
                if (element.isJsonPrimitive()) {
                    servers.add(element.getAsString());
                }
            }
            return servers;
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new IOException("Не удалось разобрать servers-info: " + body, e);
        }
    }

    public List<FunEvent> getEvents(String eventType, List<String> servers) throws IOException {
        if (servers == null || servers.isEmpty()) {
            return Collections.emptyList();
        }
        if (servers.size() > MAX_SERVERS_PER_REQUEST) {
            throw new IOException("Максимум " + MAX_SERVERS_PER_REQUEST + " серверов за запрос");
        }
        Map<String, String> query = new LinkedHashMap<>();
        query.put("event-type", eventType);
        query.put("server-type", String.join(",", servers));

        String body = get("events-info", query);
        List<FunEvent> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("response") || !root.get("response").isJsonArray()) {
                return result;
            }
            for (JsonElement element : root.getAsJsonArray("response")) {
                if (!element.isJsonObject()) continue;
                JsonObject serverBlock = element.getAsJsonObject();
                String server = getString(serverBlock, "server");
                JsonArray events = getArray(serverBlock, "events");
                for (JsonElement eventElement : events) {
                    if (!eventElement.isJsonObject()) continue;
                    JsonObject event = eventElement.getAsJsonObject();
                    result.add(new FunEvent(
                            server,
                            getString(event, "event-type"),
                            getString(event, "id"),
                            getInt(event, "time-seconds-left"),
                            getString(event, "phase"),
                            getString(event, "loot"),
                            getBoolean(event, "location-announced"),
                            parseLocation(event.get("location-event"))
                    ));
                }
            }
            return result;
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new IOException("Не удалось разобрать events-info: " + body, e);
        }
    }

    public List<FunMine> getMines(List<String> servers) throws IOException {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("server-types", servers == null || servers.isEmpty() ? "all" : String.join(",", servers));

        String body = get("mines-info", query);
        List<FunMine> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("servers") || !root.get("servers").isJsonObject()) {
                return result;
            }
            JsonObject serversObj = root.getAsJsonObject("servers");
            for (Map.Entry<String, JsonElement> entry : serversObj.entrySet()) {
                if (!entry.getValue().isJsonArray()) continue;
                for (JsonElement mineElement : entry.getValue().getAsJsonArray()) {
                    if (!mineElement.isJsonObject()) continue;
                    JsonObject mine = mineElement.getAsJsonObject();
                    result.add(new FunMine(
                            entry.getKey(),
                            getString(mine, "server-ru-name"),
                            getString(mine, "mine-name"),
                            getString(mine, "mine-rarity"),
                            getString(mine, "next-mine-rarity"),
                            getInt(mine, "reset-seconds-left")
                    ));
                }
            }
            return result;
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new IOException("Не удалось разобрать mines-info: " + body, e);
        }
    }

    public List<FunEvent> getAllEvents(String eventType) throws IOException {
        List<String> servers = getServers();
        if (servers.isEmpty()) {
            return Collections.emptyList();
        }
        List<FunEvent> result = new ArrayList<>();
        for (int i = 0; i < servers.size(); i += MAX_SERVERS_PER_REQUEST) {
            int end = Math.min(i + MAX_SERVERS_PER_REQUEST, servers.size());
            result.addAll(getEvents(eventType, servers.subList(i, end)));
        }
        return result;
    }

    public List<FunMine> getAllMines() throws IOException {
        return getMines(Collections.emptyList());
    }

    public FunEvent getRandomEvent(String eventType) throws IOException {
        List<FunEvent> events = getAllEvents(eventType);
        if (events.isEmpty()) {
            return null;
        }
        return events.get(ThreadLocalRandom.current().nextInt(events.size()));
    }

    public void logRandomEventDebug() {
        try {
            FunEvent event = getRandomEvent("all");
            if (event != null) {
                ClientLogger.info("[FunEvents Debug] " + event);
            } else {
                ClientLogger.info("[FunEvents Debug] Активных ивентов сейчас нет");
            }
        } catch (IOException e) {
            ClientLogger.warn("[FunEvents Debug] Не удалось получить ивенты: " + e.getMessage());
        }

        try {
            List<FunMine> mines = getAllMines();
            if (!mines.isEmpty()) {
                FunMine mine = mines.get(ThreadLocalRandom.current().nextInt(mines.size()));
                ClientLogger.info("[FunEvents Debug] " + mine);
            }
        } catch (IOException e) {
            ClientLogger.warn("[FunEvents Debug] Не удалось получить шахты: " + e.getMessage());
        }
    }

    public static String translateEventId(String id) {
        if (id == null) return "";
        String key = normalize(id);
        if (key.isEmpty()) return id;
        return EVENT_NAMES_RU.getOrDefault(key, id);
    }

    public static String translatePhase(String phase) {
        if (phase == null) return "";
        String key = normalize(phase);
        if (key.isEmpty()) return phase;
        return PHASES_RU.getOrDefault(key, phase);
    }

    public static String translateRarity(String rarity) {
        if (rarity == null) return "";
        String key = normalize(rarity);
        if (key.isEmpty()) return rarity;
        return RARITIES_RU.getOrDefault(key, rarity);
    }

    private String get(String path, Map<String, String> queryParams) throws IOException {
        try {
            StringBuilder url = new StringBuilder(BASE_URL).append(path);
            if (queryParams != null && !queryParams.isEmpty()) {
                url.append('?');
                boolean first = true;
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    if (!first) url.append('&');
                    first = false;
                    url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization-Token", TOKEN)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String body = new String(response.body(), StandardCharsets.UTF_8);
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return body;
            }
            String reason = switch (status) {
                case 401 -> "Неверный токен (Unauthorized)";
                case 402 -> "Превышен лимит запросов по токену (402)";
                case 403 -> "Ошибка обработки запроса (403)";
                case 404 -> "Не найдено (404)";
                default -> "HTTP " + status;
            };
            throw new IOException(path + ": " + reason + " — " + body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Прерван запрос к FunTime API", e);
        }
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }

    private static int getInt(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return 0;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean getBoolean(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return false;
        }
        if (element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        String value = element.getAsString();
        return value.equalsIgnoreCase("true") || value.equals("1");
    }

    private static JsonArray getArray(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonArray()) {
            return new JsonArray();
        }
        return element.getAsJsonArray();
    }

    private static FunEventLocation parseLocation(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject location = element.getAsJsonObject();
        return new FunEventLocation(
                getInt(location, "x"),
                getInt(location, "y"),
                getInt(location, "z")
        );
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9а-яё]", "");
    }

    private static Map<String, String> buildEventNames() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("geyser", "Гейзер");
        map.put("geiser", "Гейзер");
        map.put("airdrop", "Аирдроп");
        map.put("airdrops", "Аирдроп");
        map.put("mystbeacon", "Загадочный маяк");
        map.put("mysticbeacon", "Загадочный маяк");
        map.put("mysteriousbeacon", "Загадочный маяк");
        map.put("mystic", "Мистик");
        map.put("mystik", "Мистик");
        map.put("vulkan", "Вулкан");
        map.put("volcano", "Вулкан");
        map.put("vulcan", "Вулкан");
        map.put("meteor", "Метеоритный дождь");
        map.put("meteorite", "Метеоритный дождь");
        map.put("meteorrain", "Метеоритный дождь");
        map.put("meteoriterain", "Метеоритный дождь");
        map.put("meteordozhd", "Метеоритный дождь");
        map.put("deathchest", "Сундук смерти");
        map.put("sunduksmerti", "Сундук смерти");
        map.put("beaconkiller", "Маяк Убийца");
        map.put("mayakubiica", "Маяк Убийца");
        map.put("killerbeacon", "Маяк Убийца");
        map.put("beacon", "Маяк");
        map.put("altarundead", "Алтарь Нежити");
        map.put("altarnezhity", "Алтарь Нежити");
        map.put("deathvagon", "Смертельный вагон");
        map.put("smertelnyivagon", "Смертельный вагон");
        map.put("adrezna", "Адская резня");
        map.put("adskayareznya", "Адская резня");
        map.put("stashfortune", "Тайник удачи");
        map.put("tainikudachi", "Тайник удачи");
        map.put("tainik", "Тайник удачи");
        return map;
    }

    private static Map<String, String> buildPhases() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("starting", "Начинается");
        map.put("announcing", "Анонсируется");
        map.put("running", "Активен");
        map.put("activating", "Активируется");
        map.put("giving", "Раздача лута");
        map.put("looting", "Лутится");
        map.put("waiting", "Ожидание");
        map.put("closed", "Закрыт");
        map.put("finished", "Завершён");
        return map;
    }

    private static Map<String, String> buildRarities() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("default", "Обычная");
        map.put("rare", "Редкая");
        map.put("epic", "Эпическая");
        map.put("legendary", "Легендарная");
        map.put("mythical", "Мифическая");
        map.put("unique", "Уникальная");
        map.put("godly", "Божественная");
        return map;
    }

    private static final Map<String, String> EVENT_NAMES_RU = buildEventNames();
    private static final Map<String, String> PHASES_RU = buildPhases();
    private static final Map<String, String> RARITIES_RU = buildRarities();
}

