package eu.pb4.nucledoom.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicLong;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.levelgen.LegacyRandomSource.class)
public interface LegacyRandomSourceAccessor {
    @Accessor
    AtomicLong getSeed();
}
