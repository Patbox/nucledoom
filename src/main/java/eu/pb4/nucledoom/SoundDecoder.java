package eu.pb4.nucledoom;

import com.google.gson.JsonParser;
import eu.pb4.nucledoom.mixin.LegacyRandomSourceAccessor;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import eu.pb4.polymer.soundpatcher.impl.VanillaSoundJson;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SoundDecoder {
    private static final String URL = "https://piston-meta.mojang.com/v1/packages/6c7ffa1ae18c954e57a94f67e3bbd9031d8d588c/29.json";
    private static Map<String, String> hash2file = Map.of();
    private static Map<String, Sound> path2Sound = Map.of();

    @Nullable
    public static Sound decode(String sound) {
        sound = hash2file.getOrDefault(sound, sound);

        return path2Sound.get(sound);
    }


    public static void load() {
        CompletableFuture.runAsync(SoundDecoder::loadAsync);
    }

    public static void loadAsync() {
        try {
            var file = JsonParser.parseString(HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(URL)).build(), HttpResponse.BodyHandlers.ofString()).body());

            var hashMap = new HashMap<String, String>();
            for (var entry : file.getAsJsonObject().getAsJsonObject("objects").entrySet()) {
                if (!entry.getKey().endsWith(".ogg")) {
                    continue;
                }

                hashMap.put(entry.getValue().getAsJsonObject().getAsJsonPrimitive("hash").getAsString(), entry.getKey());
            }

            hash2file = hashMap;

            var sounds = new HashMap<String, Sound>();

            for (var entry : SoundPatcher.getVanillaSoundAsset().sounds().entrySet()) {
                int weight = 0;
                for (var def : entry.getValue().sounds()) {
                    weight += def.weight();
                }

                for (var def : entry.getValue().sounds()) {
                    if (sounds.containsKey(def.name().getPath())) continue;

                    long seed = 0;

                    var randomSource = RandomSource.create(0);

                    if (entry.getValue().sounds().size() > 1) {
                        outer:
                        while (true) {
                            seed = ((LegacyRandomSourceAccessor) randomSource).getSeed().get();
                            int j = randomSource.nextInt(weight);

                            for (var weighted : entry.getValue().sounds()) {
                                j -= weighted.weight();
                                if (j < 0) {
                                    if (weighted == def) {
                                        break outer;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    sounds.put("minecraft/sounds/" + def.name().getPath() + ".ogg",
                            new Sound(BuiltInRegistries.SOUND_EVENT.getValue(Identifier.withDefaultNamespace(entry.getKey())), def.volume(), def.pitch(), seed));
                }
            }

            path2Sound = sounds;

            System.out.println("Finished decoding sounds!");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public record Sound(SoundEvent event, float volume, float pitch, long seed) {}
}
