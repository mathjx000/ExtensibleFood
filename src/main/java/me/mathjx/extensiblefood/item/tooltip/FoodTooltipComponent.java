package me.mathjx.extensiblefood.item.tooltip;

import me.mathjx.extensiblefood.ExtensibleFood;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.FoodComponent;
import net.minecraft.util.Identifier;

public final class FoodTooltipComponent implements TooltipData, TooltipComponent {

	private static final Identifier ICONS = new Identifier(ExtensibleFood.MOD_ID, "textures/gui/tooltip_icons.png");
	private static final int FULL_UNIT_WIDTH = 9;
	private static final int HEIGHT = FULL_UNIT_WIDTH + 2;

	private final FoodComponent food;

	public FoodTooltipComponent(final FoodComponent food) {
		this.food = food;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public int getWidth(final TextRenderer textRenderer) {
		// Utils.saturationRatioToHumanReadableSaturationPoints(food.getHunger(), food.getSaturationModifier())
		return Math.max((food.getHunger() + 1) / 2 * FULL_UNIT_WIDTH, (int) (food.getHunger() * food.getSaturationModifier() * FULL_UNIT_WIDTH));
	}

	@Override
	public void drawItems(final TextRenderer textRenderer, final int x, final int y, final DrawContext context) {
		// TODO: support of mixing with others components
		// TODO: support of effect display

		// RenderSystem.setShaderTexture(0, ICONS);

		int i, j;
		// First draw the hunger background
		for (i = 0, j = (food.getHunger() + 1) / 2; i < j; i++) context.drawTexture(ICONS, x + i * FULL_UNIT_WIDTH, y, 0, 0, FULL_UNIT_WIDTH, FULL_UNIT_WIDTH, 32, 32);

		// Then draw the saturation background
		final float saturationWidth = food.getHunger() * food.getSaturationModifier();
		for (i = 0; i < saturationWidth; i++) {
			int w = FULL_UNIT_WIDTH;
			if (saturationWidth - i < 1f) w = Math.round(w * (saturationWidth - i));
			context.drawTexture(ICONS, x + i * FULL_UNIT_WIDTH, y, FULL_UNIT_WIDTH, 0, w, FULL_UNIT_WIDTH, 32, 32);
		}

		// Finally draw the hunger foreground
		for (i = 0, j = food.getHunger() / 2; i < j; i++) context.drawTexture(ICONS, x + i * FULL_UNIT_WIDTH, y, 0, FULL_UNIT_WIDTH, FULL_UNIT_WIDTH, FULL_UNIT_WIDTH, 32, 32);
		if (food.getHunger() % 2 == 1) context.drawTexture(ICONS, x + food.getHunger() / 2 * FULL_UNIT_WIDTH, y, FULL_UNIT_WIDTH, FULL_UNIT_WIDTH, FULL_UNIT_WIDTH, FULL_UNIT_WIDTH, 32, 32);
	}

}
