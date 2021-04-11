package mathjx.extensiblefood.gui.screen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mathjx.extensiblefood.ExtensibleFood;
import mathjx.extensiblefood.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Matrix4f;

public final class ErrorScreenGadget extends Screen {

	private static ErrorScreenGadget INSTANCE;

	public static boolean displayed = false;
	private static final int maxLines = 6;
	private static final int maxWidth = 256;

	private static List<ReportEntry> entries = new ArrayList<>();

	private final Screen parent;

	private final OrderedText textMore;

	public ErrorScreenGadget(final Screen parent) {
		super(new TranslatableText("extensible_food.errorScreen.title"));

		this.parent = parent;

		if (entries.size() > maxLines) {
			textMore = new TranslatableText("extensible_food.errorScreen.error.hidden", entries.size() - maxLines).asOrderedText();
		} else textMore = null;
	}

	@Override
	protected void init() {
		this.addButton(new ButtonWidget(width / 2 - 100, height / 6 + 168, 200, 20, ScreenTexts.BACK, btn -> client.openScreen(parent)));
	}

	@Override
	public void render(final MatrixStack matrices, final int mouseX, final int mouseY, final float delta) {
		this.renderBackground(matrices);

		drawCenteredText(matrices, textRenderer, title, width / 2, 15, 0xFFFFFF);

		final float x = width / 2 - maxWidth / 2;
		float y = 45f;
		final int max = Math.min(entries.size(), maxLines);
		ReportEntry entry;

		final Matrix4f matrix4f = matrices.peek().getModel();
		final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

		for (int i = 0; i < max; i++) {
			entry = entries.get(i);

			y = entry.render(matrix4f, immediate, x, y, textRenderer);
		}

		if (textMore != null) {
			y += textRenderer.fontHeight;
			textRenderer.draw(textMore, x, y, 0xFFFFFF, true, matrix4f, immediate, false, 0x000000, 0xF000F0);
		}

		immediate.draw();

		super.render(matrices, mouseX, mouseY, delta);
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
		MinecraftClient.getInstance().openScreen(INSTANCE);
		displayed = true;
	}

	private static final class ReportEntry {

		private final Text generatedMessage;
		private final OrderedText generatedFileLocation;

		ReportEntry(Path file, final Object[] args) {
			generatedMessage = new TranslatableText("extensible_food.errorScreen.error.message", args);

			file = ExtensibleFood.MOD_CONFIG_DIR.relativize(file);
			generatedFileLocation = new TranslatableText("extensible_food.errorScreen.error.location", file.toString().replace(file.getFileSystem().getSeparator(), "/")).setStyle(Style.EMPTY.withItalic(true)).asOrderedText();
		}

		private float render(final Matrix4f matrix4f, final VertexConsumerProvider vertexConsumers, final float x,
				float y, final TextRenderer textRenderer) {
			y += 1f;

			for (final OrderedText line : textRenderer.wrapLines(generatedMessage, maxWidth)) {
				textRenderer.draw(line, x, y, 0xFF_A0_A0, false, matrix4f, vertexConsumers, false, 0x000000, 0xF000F0);
				y += textRenderer.fontHeight;
			}

			y += 1f;

			textRenderer.draw(generatedFileLocation, x, y, 0xA0A0A0, false, matrix4f, vertexConsumers, false, 0x000000, 0xF000F0);
			y += textRenderer.fontHeight;

			return y + 1f;
		}

	}

}
