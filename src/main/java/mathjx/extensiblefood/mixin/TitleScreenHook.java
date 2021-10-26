package mathjx.extensiblefood.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mathjx.extensiblefood.gui.screen.ErrorScreenGadget;
import net.minecraft.client.gui.screen.TitleScreen;

/**
 * This class is a hook to show error messages about food loading
 */
@Mixin(TitleScreen.class)
public final class TitleScreenHook {

	@Inject(method = "init()V", at = @At("HEAD"))
	private void init(final CallbackInfo cb) {
		if (ErrorScreenGadget.shouldDisplay()) {
			ErrorScreenGadget.open((TitleScreen) (Object) this);
		}
	}

}
