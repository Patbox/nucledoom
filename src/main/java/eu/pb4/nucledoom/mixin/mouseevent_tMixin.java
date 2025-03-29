package eu.pb4.nucledoom.mixin;

import doom.event_t;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;

import java.awt.*;

@SuppressWarnings("ALL")
@Mixin(event_t.mouseevent_t.class)
public class mouseevent_tMixin {
    @Overwrite
    public void resetIn(Robot robot, Point windowOffset, int centreX, int centreY) {}
}
