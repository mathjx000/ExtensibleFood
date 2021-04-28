package mathjx.extensiblefood.item;

import java.util.List;

import mathjx.extensiblefood.ModConfig;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

@Deprecated
public final class FoodTooltip {

	private static final Text HEADER_TEXT = new TranslatableText("tooltip.extensible_food.header").formatted(Formatting.GRAY);
	private static final Text ADVANCED_MORE_TEXT = new TranslatableText("tooltip.extensible_food.advanced.more").formatted(Formatting.GRAY);

	private static final Text BOOLEAN_TRUE_TEXT = new TranslatableText("tooltip.extensible_food.value.true").formatted(Formatting.GREEN);
	private static final Text BOOLEAN_FALSE_TEXT = new TranslatableText("tooltip.extensible_food.value.false").formatted(Formatting.RED);

	public static void append(final ItemStack stack, final TooltipContext context, final List<Text> list) {
		final Item item = stack.getItem();

		if (item.isFood()) {
			final FoodComponent food = item.getFoodComponent();

			list.add(LiteralText.EMPTY);

			list.add(HEADER_TEXT);
			list.add(new TranslatableText("tooltip.extensible_food.hunger", formatFloatingValue(food.getHunger() / 2D)).formatted(Formatting.BLUE));
			list.add(new TranslatableText("tooltip.extensible_food.saturation", formatFloatingValue(food.getSaturationModifier() * food.getHunger() * 2f)).formatted(Formatting.DARK_GREEN));

			if (ModConfig.displayFoodTooltipsBehavior > 1 || context.isAdvanced()) {
				list.add(new TranslatableText("tooltip.extensible_food.advanced", formatBooleanValue(food.isAlwaysEdible()), formatBooleanValue(food.isMeat()), formatBooleanValue(food.isSnack())));

				if (!food.getStatusEffects().isEmpty()) list.add(ADVANCED_MORE_TEXT);
			}
		}
	}

	public static Text formatFloatingValue(final double value) {
		if (value < 0D) {
			return new TranslatableText("tooltip.extensible_food.value.negative", ItemStack.MODIFIER_FORMAT.format(-value));
		} else return new TranslatableText("tooltip.extensible_food.value.positive", ItemStack.MODIFIER_FORMAT.format(value));
	}

	public static Text formatBooleanValue(final boolean value) {
		return value ? BOOLEAN_TRUE_TEXT : BOOLEAN_FALSE_TEXT;
	}

}
