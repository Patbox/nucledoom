package eu.pb4.nucledoom.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

public record DoomConfig(Identifier wadFile, String wadName,
                         List<Identifier> pwads,
                         List<String> cvars, Map<String, Identifier> resourceMap,
                         boolean saves,
                         Optional<Identifier> saveName) {

    public static final MapCodec<DoomConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                Identifier.CODEC.fieldOf("wad").forGetter(DoomConfig::wadFile),
                Codec.STRING.fieldOf("wad_name").forGetter(DoomConfig::wadName),
                ExtraCodecs.compactListCodec(Identifier.CODEC).optionalFieldOf("pwad", List.of()).forGetter(DoomConfig::pwads),
                Codec.STRING.listOf().optionalFieldOf("cvars", List.of()).forGetter(DoomConfig::cvars),
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("resources", Map.of()).forGetter(DoomConfig::resourceMap),
                Codec.BOOL.optionalFieldOf("saves", true).forGetter(DoomConfig::saves),
                Identifier.CODEC.optionalFieldOf("save_name").forGetter(DoomConfig::saveName)
        ).apply(instance, DoomConfig::new);
    });
}
