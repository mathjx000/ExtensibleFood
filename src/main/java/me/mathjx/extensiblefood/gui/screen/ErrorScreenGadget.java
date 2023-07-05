package me.mathjx.extensiblefood.gui.screen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import me.mathjx.extensiblefood.ExtensibleFood;
import me.mathjx.extensiblefood.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class ErrorScreenGadget extends Screen {

	private static ErrorScreenGadget INSTANCE;

	public static boolean displayed = false;
	private static final int maxLines = 6;
	private static final int maxWidth = 256;

	private static List<ReportEntry> entries = new ArrayList<>();

	private final Screen parent;

	private final OrderedText textMore;

	public ErrorScreenGadget(final Screen parent) {
		super(Text.translatable("extensible_food.errorScreen.title"));

		this.parent = parent;

		if (entries.size() > maxLines) {
			textMore = Text.translatable("extensible_food.errorScreen.error.hidden", entries.size() - maxLines).asOrderedText();
		} else textMore = null;
	}

	@Override
	protected void init() {
		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, btn -> client.setScreen(parent)).dimensions(width / 2 - 100, height / 6 + 168, 200, 20).build());
	}

	@Override
	public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
		this.renderBackground(context);

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);

		final int x = (int) (width / 2 - maxWidth / 2f);
		float y = 45f;
		final int max = Math.min(entries.size(), maxLines);
		ReportEntry entry;

		for (int i = 0; i < max; i++) {
			entry = entries.get(i);

			y = entry.render(context, x, y, textRenderer);
		}

		if (textMore != null) {
			y += textRenderer.fontHeight;
			context.drawText(textRenderer, textMore, x, (int) y, 0xFFFFFF, true);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void removed() {}

	public static void report(final Path file, final Object... args) {
		entries.add(new ReportEntry(file, args));
	}

	public static boolean shouldDisplay() {
		return !displayed && ModConfig.showModRelatedErrors && !entries.isEmpty();
	}

	public static void open(final Screen parent) {
		INSTANCE = new ErrorScreenGadget(parent);
		MinecraftClient.getInstance().setScreen(INSTANCE);
		displayed = true;
	}

	private static final class ReportEntry {

		private final Text generatedMessage;
		private final OrderedText generatedFileLocation;

		ReportEntry(Path file, final Object[] args) {
			generatedMessage = Text.translatable("extensible_food.errorScreen.error.message", args);

			file = ExtensibleFood.MOD_CONFIG_DIR.relativize(file);
			generatedFileLocation = Text.translatable("extensible_food.errorScreen.error.location", file.toString().replace(file.getFileSystem().getSeparator(), "/")).setStyle(Style.EMPTY.withItalic(true)).asOrderedText();
		}

		private float render(final DrawContext context, final int x, float y, final TextRenderer textRenderer) {
			y += 1f;

			for (final OrderedText line : textRenderer.wrapLines(generatedMessage, maxWidth)) {
				context.drawText(textRenderer, line, x, (int) y, 0xFF_A0_A0, false);
				y += textRenderer.fontHeight;
			}

			y += 1f;

			context.drawText(textRenderer, generatedFileLocation, x, (int) y, 0xA0A0A0, false);
			y += textRenderer.fontHeight;

			return y + 1f;
		}

	}

}
