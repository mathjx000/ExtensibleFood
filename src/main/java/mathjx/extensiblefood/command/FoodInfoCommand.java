package mathjx.extensiblefood.command;

import java.util.LinkedList;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;

import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public final class FoodInfoCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		// sadly we can't easily suggest only food items :/
		dispatcher.register(CommandManager.literal("foodinfo").then(CommandManager.argument("item", ItemStackArgumentType.itemStack()).executes(ctx -> {
			ServerCommandSource source = (ServerCommandSource) ctx.getSource();
			ItemStackArgument arg = ItemStackArgumentType.getItemStackArgument(ctx, "item");
			Item item = arg.getItem();

			if (item.isFood()) {
				FoodComponent component = item.getFoodComponent();
				LinkedList<Text> lines = new LinkedList<>();

				lines.add(new TranslatableText("commands.extensible_food.foodinfo.info_header", arg.createStack(1, false).toHoverableText()));
				lines.add(new TranslatableText("commands.extensible_food.foodinfo.entry_hunger", createNumberText(component.getHunger())));
				lines.add(new TranslatableText("commands.extensible_food.foodinfo.entry_saturation", createNumberText(component.getSaturationModifier())));
				lines.add(new TranslatableText("commands.extensible_food.foodinfo.entry_meat", createBooleanText(component.isMeat())));
				lines.add(new TranslatableText("commands.extensible_food.foodinfo.entry_alwaysEdible", createBooleanText(component.isAlwaysEdible())));
				lines.add(new TranslatableText("commands.extensible_food.foodinfo.entry_snack", createBooleanText(component.isSnack())));

				for (Pair<StatusEffectInstance, Float> pair : component.getStatusEffects()) {
					StatusEffectInstance effect = pair.getFirst();
					lines.add(new TranslatableText("commands.extensible_food.foodinfo.entry_effect", new TranslatableText(effect.getTranslationKey()), createNumberText(pair.getSecond()), createNumberText(effect.getDuration()), createNumberText(effect.getAmplifier()), createBooleanText(effect.shouldShowParticles()), createBooleanText(effect.shouldShowIcon())));
				}

				for (Text line : lines) source.sendFeedback(line, false);

				return Command.SINGLE_SUCCESS;
			} else {
				source.sendError(new TranslatableText("commands.extensible_food.foodinfo.invalid_item", new ItemStack(item, 1).toHoverableText()));

				return 0;
			}
		})));
	}

	private static Text createNumberText(Object arg) {
		return new LiteralText(arg.toString()).setStyle(Style.EMPTY.withColor(Formatting.AQUA));
	}

	public static Text createBooleanText(boolean bool) {
		return new TranslatableText(bool ? "commands.extensible_food.foodinfo.text.boolean.true"
				: "commands.extensible_food.foodinfo.text.boolean.false").setStyle(Style.EMPTY.withColor(bool
						? Formatting.GREEN : Formatting.RED));
	}

}
