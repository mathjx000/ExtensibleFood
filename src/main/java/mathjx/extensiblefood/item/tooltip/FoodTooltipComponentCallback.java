package mathjx.extensiblefood.item.tooltip;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;

@Environment(EnvType.CLIENT)
public final class FoodTooltipComponentCallback implements TooltipComponentCallback {

	public FoodTooltipComponentCallback() {}

	@Override
	public @Nullable TooltipComponent getComponent(final TooltipData data) {
		if (data instanceof FoodTooltipComponent) return (TooltipComponent) data;
		return null;
	}

}
