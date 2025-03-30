package eu.pb4.nucledoom.mixin;

import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import w.WadLoader;

@Mixin(WadLoader.class)
public class WadLoaderMixin {
    @Redirect(  method = "<init>()V", at = @At(value = "INVOKE", target = "Ljava/lang/Runtime;addShutdownHook(Ljava/lang/Thread;)V"))
    private void doNotAddThat(Runtime instance, Thread hook) {}
}
