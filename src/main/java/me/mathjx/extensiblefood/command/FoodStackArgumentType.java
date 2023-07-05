package me.mathjx.extensiblefood.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;

public final class FoodStackArgumentType implements ArgumentType<ItemStackArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("potato", "minecraft:sweet_berries");

	private final RegistryWrapper<Item> registryWrapper;

	public FoodStackArgumentType(final CommandRegistryAccess commandRegistryAccess) {
		registryWrapper = commandRegistryAccess.createWrapper(Registries.ITEM.getKey());
	}

	@Override
	public ItemStackArgument parse(final StringReader reader) throws CommandSyntaxException {
		final FoodItemStringReader itemStringReader = new FoodItemStringReader(registryWrapper, reader).consume();
		return new ItemStackArgument(itemStringReader.getItem(), null);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context,
			final SuggestionsBuilder builder) {
		final StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		final FoodItemStringReader stringReader = new FoodItemStringReader(registryWrapper, reader);

		try {
			stringReader.consume();
		} catch (final CommandSyntaxException e) {}

		return stringReader.getSuggestions(builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

}
