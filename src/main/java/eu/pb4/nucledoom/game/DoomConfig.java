package eu.pb4.nucledoom.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.nucledoom.NucleDoom;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public record DoomConfig(List<String> cvars, Map<String, Identifier> resourceMap) {

    public static final MapCodec<DoomConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                Codec.STRING.listOf().optionalFieldOf("cvars", List.of()).forGetter(DoomConfig::cvars),
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("resources", createDefaultResourceMap()).forGetter(DoomConfig::resourceMap)
        ).apply(instance, DoomConfig::new);
    });

    private static Map<String, Identifier> createDefaultResourceMap() {
        return Map.of(
                "doom1.wad", Identifier.of(NucleDoom.MOD_ID, "wads/doomshareware/doom1.wad"),
                "mochadoom.cfg", Identifier.of(NucleDoom.MOD_ID, "default_config.cfg")
        );
    }
}
