package me.mathjx.extensiblefood.command;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class FoodItemStringReader {

	private static final DynamicCommandExceptionType ID_INVALID_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("argument.item.id.invalid", id));
	private static final DynamicCommandExceptionType NOT_FOOD_EXCEPTION = new DynamicCommandExceptionType(object -> Text.translatable("argument.extensible_food.food_item.invalid", object));

	private final RegistryWrapper<Item> registryWrapper;
	private final StringReader reader;
	private RegistryEntry<Item> item;

	private boolean suggest = true;

	public FoodItemStringReader(final RegistryWrapper<Item> registryWrapper, final StringReader reader) {
		this.registryWrapper = registryWrapper;
		this.reader = reader;
	}

	public RegistryEntry<Item> getItem() {
		return item;
	}

	private void readItem() throws CommandSyntaxException {
		final int i = reader.getCursor();
		final Identifier identifier = Identifier.fromCommandInput(reader);

		final RegistryEntry<Item> item = registryWrapper
				.getOptional(RegistryKey.of(Registries.ITEM.getKey(), identifier))
				.orElseThrow(() -> {
					reader.setCursor(i);
					return ID_INVALID_EXCEPTION.createWithContext(reader, identifier.toString());
				});
		if (item.value().isFood()) {
			this.item = item;
		} else {
			reader.setCursor(i);
			throw NOT_FOOD_EXCEPTION.createWithContext(reader, identifier.toString());
		}
	}

	public FoodItemStringReader consume() throws CommandSyntaxException {
		suggest = true; // suggest until item reading fail
		readItem();
		suggest = false; // item is valid -> no need to suggest

		return this;
	}

	public CompletableFuture<Suggestions> getSuggestions(final SuggestionsBuilder builder) {
		if (suggest) {
			final Stream<Identifier> stream = Registries.ITEM.getEntrySet().stream().filter(e -> e.getValue().isFood()).map(e -> e.getKey().getValue());
			return CommandSource.suggestIdentifiers(stream, builder);
		} else return builder.buildFuture();
	}

}
