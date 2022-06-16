package mathjx.extensiblefood.item.tooltip;

import java.util.List;

import mathjx.extensiblefood.ModConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FoodTooltip implements ItemTooltipCallback {

	private static final Text HEADER_TEXT = Text.translatable("tooltip.extensible_food.header").formatted(Formatting.GRAY);
	private static final Text ADVANCED_MORE_TEXT = Text.translatable("tooltip.extensible_food.advanced.more").formatted(Formatting.GRAY);

	private static final Text BOOLEAN_TRUE_TEXT = Text.translatable("tooltip.extensible_food.value.true").formatted(Formatting.GREEN);
	private static final Text BOOLEAN_FALSE_TEXT = Text.translatable("tooltip.extensible_food.value.false").formatted(Formatting.RED);

	public FoodTooltip() {}

	@Override
	public void getTooltip(final ItemStack stack, final TooltipContext context, final List<Text> lines) {
		final Item item = stack.getItem();

		if (item.isFood()) {
			final FoodComponent food = item.getFoodComponent();

			lines.add(Text.empty());

			lines.add(HEADER_TEXT);
			lines.add(Text.translatable("tooltip.extensible_food.hunger", formatFloatingValue(food.getHunger() / 2D)).formatted(Formatting.BLUE));
			lines.add(Text.translatable("tooltip.extensible_food.saturation", formatFloatingValue(food.getSaturationModifier() * food.getHunger() * 2f)).formatted(Formatting.DARK_GREEN));

			if (ModConfig.displayFoodTooltipsTextLevel > 1 || context.isAdvanced()) {
				lines.add(Text.translatable("tooltip.extensible_food.advanced.always_edible", formatBooleanValue(food.isAlwaysEdible())));
				lines.add(Text.translatable("tooltip.extensible_food.advanced.meat", formatBooleanValue(food.isMeat())));
				lines.add(Text.translatable("tooltip.extensible_food.advanced.snack", formatBooleanValue(food.isSnack())));
			}

			if (!food.getStatusEffects().isEmpty()) lines.add(ADVANCED_MORE_TEXT);
		}
	}

	public static Text formatFloatingValue(final double value) {
		if (value < 0D) {
			return Text.translatable("tooltip.extensible_food.value.negative", ItemStack.MODIFIER_FORMAT.format(-value));
		} else return Text.translatable("tooltip.extensible_food.value.positive", ItemStack.MODIFIER_FORMAT.format(value));
	}

	public static Text formatBooleanValue(final boolean value) {
		return value ? BOOLEAN_TRUE_TEXT : BOOLEAN_FALSE_TEXT;
	}

}
