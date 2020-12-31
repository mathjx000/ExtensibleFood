package mathjx.extensiblefood.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import mathjx.extensiblefood.ModConfig;
import mathjx.extensiblefood.item.FoodTooltip;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Mixin(ItemStack.class)
public final class FoodStatsTooltipHook {

	@Inject(method = "getTooltip",
			at = @At(	value = "INVOKE",
						target = "Lnet/minecraft/item/Item;appendTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/client/item/TooltipContext;)V",
						shift = Shift.AFTER),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void getTooltipHook(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir,
			List<Text> list, int i) {
		if (ModConfig.displayFoodTooltipsBehavior > 0 || context.isAdvanced()) FoodTooltip.append((ItemStack) (Object) this, context, list);
	}

}
