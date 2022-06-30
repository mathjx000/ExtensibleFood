package mathjx.extensiblefood.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mathjx.extensiblefood.ModConfig;
import mathjx.extensiblefood.item.tooltip.FoodTooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;

@Mixin(ItemStack.class)
public final class FoodStatsTooltipHook {

	/**
	 * Here we inject our food tooltip.
	 */
	@Inject(method = "getTooltipData", at = @At(value = "RETURN"), cancellable = true)
	private void injectTooltipData(final CallbackInfoReturnable<Optional<TooltipData>> cir) {
		if (ModConfig.displayFoodTooltipsImagesLevel > 0 && cir.getReturnValue().isEmpty()) {
			final ItemStack thiz = (ItemStack) (Object) this;

			if (thiz.isFood()) {
				final FoodComponent component = thiz.getItem().getFoodComponent();
				cir.setReturnValue(Optional.of(new FoodTooltipComponent(component)));
			}
		}
	}

}
