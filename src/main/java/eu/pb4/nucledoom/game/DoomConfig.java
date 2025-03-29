package eu.pb4.nucledoom.game;

import com.mojang.serialization.MapCodec;

public record DoomConfig() {

	public static final MapCodec<DoomConfig> CODEC = MapCodec.unit(new DoomConfig());/*RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("game").forGetter(DoomConfig::game),
			Codecs.VECTOR_3F.xmap(Vec3d::new, Vec3d::toVector3f).optionalFieldOf("spectator_spawn_offset", DEFAULT_SPECTATOR_SPAWN_OFFSET).forGetter(DoomConfig::spectatorSpawnOffset),
			Codec.intRange(1, 4).optionalFieldOf("players", 1).forGetter(DoomConfig::playerCount),
			Codec.BOOL.optionalFieldOf("swap_x_z", false).forGetter(DoomConfig::swapXZ),
			Codec.BOOL.optionalFieldOf("save", false).forGetter(DoomConfig::save)
		).apply(instance, DoomConfig::new);
	});*/
}
