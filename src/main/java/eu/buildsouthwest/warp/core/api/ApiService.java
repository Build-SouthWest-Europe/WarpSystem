package eu.buildsouthwest.warp.core.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.buildsouthwest.warp.core.model.GeoMetadata;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ApiService {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final List<String> ADDRESS_KEYS = List.of("civil_parish", "city_district", "village", "town", "city", "municipality", "name");
//    private final String baseUrl;
//    private final String apiKey;

    public ApiService() {
//        this.baseUrl = baseUrl;
//        this.apiKey = apiKey;
    }

    public CompletableFuture<Optional<GeoMetadata>> fetchCoords(double x, double z) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://smybteapi.buildtheearth.net/projection/toGeo?mcpos="
                        + URLEncoder.encode(String.format(Locale.ENGLISH, "%.2f", x), StandardCharsets.UTF_8)
                        + ","
                        + URLEncoder.encode(String.format(Locale.ENGLISH, "%.2f", z), StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(8))
                .GET().build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if(response.statusCode() == 404) return Optional.<GeoMetadata>empty();
                    if (response.statusCode() != 200) {
                        throw new CompletionException(
                                new IOException("BTE API returned status code " + response.statusCode())
                        );
                    }

                    return parseGeo(JsonParser.parseString(response.body()).getAsJsonObject());
                });
    }

    public CompletableFuture<Optional<String>> fetchLocation(GeoMetadata geo) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://nominatim.openstreetmap.org/reverse?lat="
                        + URLEncoder.encode(String.valueOf(geo.latitude()), StandardCharsets.UTF_8)
                        + "&lon=" + URLEncoder.encode(String.valueOf(geo.longitude()), StandardCharsets.UTF_8)
                        + "&format=json&addressdetails=1&zoom=13&accept-language=en"))
                .header("User-Agent", "BuildTheEarth, SouthWest Europe Team (marinvilchesmartin@gmail.com)")
                .timeout(Duration.ofSeconds(8))
                .GET().build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if(res.statusCode() == 404) return Optional.<String>empty();
                    if (res.statusCode() != 200) {
                        throw new CompletionException(
                                new IOException("BTE API returned status code " + res.statusCode())
                        );
                    }

                    JsonObject address = JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonObject("address");
                    for(String key : ADDRESS_KEYS) {
                        if(address.has(key) && !address.get(key).isJsonNull()) {
                            return Optional.of(address.get(key).getAsString());
                        }
                    }

                    return Optional.empty();
                });
    }

    private Optional<GeoMetadata> parseGeo(JsonObject body){
        JsonArray pos = body.getAsJsonArray("geo_positions");

        if(pos == null || pos.isEmpty()) return Optional.empty();

        JsonArray coords = pos.get(0).getAsJsonArray();
        return Optional.of(new GeoMetadata(coords.get(0).getAsDouble(), coords.get(1).getAsDouble(), ""));
    }
}
