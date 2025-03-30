package eu.pb4.nucledoom.mixin;

import org.spongepowered.asm.mixin.Mixin;
import utils.ResourceIO;

@Mixin(value = ResourceIO.class, remap = false)
public class ResourceIOMixin {
}
