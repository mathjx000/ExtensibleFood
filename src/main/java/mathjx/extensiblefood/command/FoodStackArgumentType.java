package mathjx.extensiblefood.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.argument.ItemStackArgument;

final class FoodStackArgumentType implements ArgumentType<ItemStackArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("potato", "minecraft:sweet_berries");

	@Override
	public ItemStackArgument parse(StringReader reader) throws CommandSyntaxException {
		FoodItemStringReader itemStringReader = new FoodItemStringReader(reader).consume();
		return new ItemStackArgument(itemStringReader.getItem(), null);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		FoodItemStringReader stringReader = new FoodItemStringReader(reader);

		try {
			stringReader.consume();
		} catch (CommandSyntaxException e) {}

		return stringReader.getSuggestions(builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

}
