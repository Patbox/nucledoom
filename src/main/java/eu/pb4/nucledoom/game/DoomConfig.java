package eu.pb4.nucledoom.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public record DoomConfig(Identifier wadFile, String wadName,
                         List<String> cvars, Map<String, Identifier> resourceMap) {

    public static final MapCodec<DoomConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                Identifier.CODEC.fieldOf("wad").forGetter(DoomConfig::wadFile),
                Codec.STRING.fieldOf("wad_name").forGetter(DoomConfig::wadName),
                Codec.STRING.listOf().optionalFieldOf("cvars", List.of()).forGetter(DoomConfig::cvars),
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("resources", Map.of()).forGetter(DoomConfig::resourceMap)
        ).apply(instance, DoomConfig::new);
    });
}
