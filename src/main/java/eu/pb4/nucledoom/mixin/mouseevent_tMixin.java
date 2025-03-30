package eu.pb4.nucledoom.mixin;

import doom.event_t;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;

import java.awt.*;

@SuppressWarnings("ALL")
@Mixin(value = event_t.mouseevent_t.class, remap = false)
public class mouseevent_tMixin {
    @Overwrite(remap = false)
    public void resetIn(Robot robot, Point windowOffset, int centreX, int centreY) {}
}
