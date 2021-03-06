package mathjx.extensiblefood.command;

import static net.minecraft.command.argument.ItemStringReader.ID_INVALID_EXCEPTION;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class FoodItemStringReader {

	public static final DynamicCommandExceptionType NOT_FOOD_EXCEPTION = new DynamicCommandExceptionType((object) -> {
		return new TranslatableText("argument.extensible_food.food_item.invalid", new Object[] { object });
	});

	private final StringReader reader;
	private Item item;

	private boolean suggest = true;

	public FoodItemStringReader(StringReader reader) {
		this.reader = reader;
	}

	public Item getItem() {
		return this.item;
	}

	public void readItem() throws CommandSyntaxException {
		int i = this.reader.getCursor();
		Identifier identifier = Identifier.fromCommandInput(this.reader);

		Item item = Registry.ITEM.getOrEmpty(identifier).orElseThrow(() -> {
			this.reader.setCursor(i);
			return ID_INVALID_EXCEPTION.createWithContext(this.reader, identifier.toString());
		});
		if (item.isFood()) {
			this.item = item;
		} else {
			this.reader.setCursor(i);
			throw NOT_FOOD_EXCEPTION.createWithContext(this.reader, identifier.toString());
		}
	}

	public FoodItemStringReader consume() throws CommandSyntaxException {
		suggest = true; // suggest until item reading fail
		readItem();
		suggest = false; // item is valid -> no need to suggest

		return this;
	}

	public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder) {
		if (suggest) {
			Stream<Identifier> stream = Registry.ITEM.getEntries().stream().filter(e -> e.getValue().isFood()).map(e -> e.getKey().getValue());
			return CommandSource.suggestIdentifiers(stream, builder);
		} else return builder.buildFuture();
	}

}
