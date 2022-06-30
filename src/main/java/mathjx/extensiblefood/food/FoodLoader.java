package mathjx.extensiblefood.food;

import static mathjx.extensiblefood.ExtensibleFood.IS_CLIENT;
import static mathjx.extensiblefood.ExtensibleFood.LOGGER;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import mathjx.extensiblefood.block.BlockParser;
import mathjx.extensiblefood.block.ConsumableFoodBlock;
import mathjx.extensiblefood.block.CropFoodBlock;
import mathjx.extensiblefood.item.ExtensibleFoodBlockItem;
import mathjx.extensiblefood.item.ExtensibleFoodCropItem;
import mathjx.extensiblefood.item.ExtensibleFoodItem;
import mathjx.extensiblefood.item.FoodConsumptionProgressModelPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.ComposterBlock;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.Rarity;
import net.minecraft.util.UseAction;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;

public final class FoodLoader {

	private final CommandRegistryAccess commandRegistryAccess = new CommandRegistryAccess(DynamicRegistryManager.BUILTIN.get());

	public FoodLoader() { }

	public void applyFood(final JsonObject file, final Identifier autoId) throws JsonParseException {
		final ExtendedFoodComponent foodComponent = parseFoodComponent(JsonHelper.getObject(file, "food"), autoId);

		Pair<Identifier, Block> block;
		if (file.has("block")) {
			final JsonObject jsonBlock = JsonHelper.getObject(file, "block");
			block = BlockParser.parseBlock(jsonBlock, autoId, foodComponent, commandRegistryAccess);

			if (jsonBlock.has("crop_item"))
				throw new JsonSyntaxException("'crop_item' object was moved into the 'item' object as 'additional_crop_item' !");

			doRegister(Registry.BLOCK, block.getLeft(), block.getRight());
		} else block = null;

		if (file.has("item")) {
			JsonObject jsonItem = JsonHelper.getObject(file, "item");
			
			if (jsonItem.has("additional_crop_item")) {
				if (block == null || !(block.getRight() instanceof CropFoodBlock)) throw new JsonSyntaxException("Object 'additional_crop_item' is invalid in this context.");
				
				final JsonObject jsonCropItem = JsonHelper.getObject(jsonItem, "additional_crop_item");
				final Pair<Identifier, BlockItem> pair = parseItemBlock(jsonCropItem, autoId, block.getRight());
				// then parse the crop item as a item block
				doRegister(Registry.ITEM,
						pair.getLeft() == null ? new Identifier(autoId.toString() + "_seeds") : pair.getLeft(),
						pair.getRight());
			}
			
			Pair<Identifier, Item> pair = parseFoodItem(jsonItem, autoId, foodComponent, null);
			doRegister(Registry.ITEM, pair.getLeft(), pair.getRight());
		}
	}

	private static <V, T extends V> void doRegister(Registry<V> registry, Identifier id, T entry) {
		Registry.register(registry, id, entry);
		LOGGER.debug("Registered element with id {} in {}", id, registry.getKey().getValue());
	}

	//
	//// Food
	//

	/**
	 * Parse an extended {@link ExtendedFoodComponent foodComponent} from the given
	 * {@link JsonObject}
	 *
	 * @param foodJson The source JSON
	 * @param foodId   The food file identifier
	 *
	 * @return
	 *
	 * @throws JsonParseException if any JSON syntax error is found
	 */
	ExtendedFoodComponent parseFoodComponent(final JsonObject foodJson, final Identifier foodId) throws JsonParseException {
		final FoodComponent.Builder builder = new FoodComponent.Builder();

		Integer hunger = null;
		Float saturation = null;
		Boolean alwaysEdible = null, meat = null, snack = null;
		Integer itemEatTime;
		SoundEvent itemEatSound;

		if (foodJson.has("base")) { // then we need to copy all parameters of the given item
			final Item baseItem = JsonHelper.getItem(foodJson, "base");

			if (!baseItem.isFood()) throw new JsonParseException("Invalid food base item '"
					+ Registry.ITEM.getId(baseItem) + "', this item exists but it not a food");

			final FoodComponent base = baseItem.getFoodComponent();

			hunger = base.getHunger();
			saturation = base.getSaturationModifier();
			alwaysEdible = base.isAlwaysEdible();
			meat = base.isMeat();
			snack = base.isSnack();

			if (!JsonHelper.getBoolean(foodJson, "effects_override", false)) {
				base.getStatusEffects().forEach(p -> builder.statusEffect(p.getFirst(), p.getSecond()));
			}
		}

		if (foodJson.has("hunger") || hunger == null) hunger = JsonHelper.getInt(foodJson, "hunger");
		if (foodJson.has("saturation") || saturation == null) saturation = JsonHelper.getFloat(foodJson, "saturation");
		if (foodJson.has("always_edible") || alwaysEdible == null) alwaysEdible = JsonHelper.getBoolean(foodJson, "always_edible", false);
		if (foodJson.has("meat") || meat == null) meat = JsonHelper.getBoolean(foodJson, "meat", false);
		if (foodJson.has("snack") || snack == null) snack = JsonHelper.getBoolean(foodJson, "snack", false);
		itemEatTime = foodJson.has("eat_time") ? JsonHelper.getInt(foodJson, "eat_time") : null;
		itemEatSound = foodJson.has("eat_sound")
				? itemEatSound = parseSoundEvent(JsonHelper.getString(foodJson, "eat_sound")) : null;

		builder.hunger(hunger);
		builder.saturationModifier(
				// convert the real saturation value to the internal one
				saturation / (float) hunger / 2f);
		if (alwaysEdible) builder.alwaysEdible();
		if (meat) builder.meat();
		if (snack) builder.snack();

		if (foodJson.has("effects")) {
			final JsonArray array = JsonHelper.getArray(foodJson, "effects");

			for (int s = array.size(), i = 0; i < s; i++) {
				final JsonObject rootEffect = JsonHelper.asObject(array.get(i), "effects[" + i + "]");
				builder.statusEffect(parseEffect(rootEffect), JsonHelper.getFloat(rootEffect, "chance", 1f));
			}
		}

		return new ExtendedFoodComponent(builder.build(), itemEatTime, itemEatSound);
	}

	/**
	 * Parse a {@link StatusEffectInstance} from a JSON object
	 *
	 * @param object serialized effect
	 *
	 * @return an instance of the serialized effect
	 */
	private StatusEffectInstance parseEffect(final JsonObject object) {
		final String id = JsonHelper.getString(object, "id");
		final StatusEffect effect = Registry.STATUS_EFFECT.getOrEmpty(new Identifier(id)).orElseThrow(() -> new JsonParseException("Expected id to be an item, was unknown string '"
				+ id + '\''));

		// @formatter:off
		return new StatusEffectInstance(effect,
				JsonHelper.getInt(object, "duration"),
				JsonHelper.getInt(object, "amplifier", 0),
				JsonHelper.getBoolean(object, "ambient", false),
				JsonHelper.getBoolean(object, "show_particles", true),
				JsonHelper.getBoolean(object, "show_icon", true),
				object.has("hidden_effect")
					? parseEffect(JsonHelper.getObject(object, "hidden_effect"))
					: null,
				effect.getFactorCalculationDataSupplier()		// TODO: custom calculation data ?
			);
		// @formatter:on
	}

	//
	//// #Food
	//

	//
	//// FoodItem
	//

	private Pair<Identifier, Item> parseFoodItem(final JsonObject jsonItem, final Identifier autoId, final ExtendedFoodComponent foodComponent,
			final Block block) throws JsonParseException {
		final CommonItemProperties props =  parseCommonItemProperties(jsonItem, autoId, ItemGroup.FOOD);

		final UseAction itemUseAction;
		final Item itemConsumeRemainder;
		final Identifier itemModelPredicate;

		itemUseAction = jsonItem.has("action") ? parseAction(jsonItem, "action") : UseAction.EAT; // action

		// remainder item after eaten
		// TODO item specific data support
		if (jsonItem.has("consume_remainder")) {
			itemConsumeRemainder = JsonHelper.getItem(jsonItem, "consume_remainder");
		} else itemConsumeRemainder = null;

		if (IS_CLIENT && jsonItem.has("item_model_predicate")) {
			// This is a client only property
			itemModelPredicate = new Identifier(JsonHelper.getString(jsonItem, "item_model_predicate"));
		} else itemModelPredicate = null;

		props.settings.food(foodComponent.food);

		// Build the item
		final Item item = block == null
				? new ExtensibleFoodItem(props.settings, props.name, props.description, itemUseAction, props.glint, itemConsumeRemainder, foodComponent)
				: new ExtensibleFoodCropItem(block, props.settings, props.name, props.description, props.glint, itemUseAction, itemConsumeRemainder, foodComponent);

		props.registerComposterValue(item);
		
		// register the model predicate provider if specified
		if (itemModelPredicate != null) {
			ModelPredicateProviderRegistry.register(item, itemModelPredicate, FoodConsumptionProgressModelPredicate.INSTANCE);
		}

		return new Pair<>(props.id, item);
	}

	private Pair<Identifier, BlockItem> parseItemBlock(final JsonObject jsonItem, final Identifier autoId, final Block block) {
		final CommonItemProperties props = parseCommonItemProperties(jsonItem, autoId, block == null ? ItemGroup.FOOD : ItemGroup.MISC);

		final BlockItem blockItem;
		if (block instanceof ConsumableFoodBlock) blockItem = new ExtensibleFoodBlockItem((ConsumableFoodBlock) block, props.settings, props.name, props.description, props.glint);
		else if (block instanceof CropFoodBlock) {
			blockItem = new ExtensibleFoodCropItem(block, props. settings, props.name, props.description, props.glint, null, null, null);
			((CropFoodBlock) block).seedItem = blockItem;
		} else throw new RuntimeException();

		props.registerComposterValue(blockItem);

		return new Pair<>(props.id, blockItem);
	}

	private CommonItemProperties parseCommonItemProperties(final JsonObject jsonItem, Identifier itemId, ItemGroup defaultGroup) throws JsonParseException {
		if (jsonItem.has("id")) itemId = new Identifier(JsonHelper.getString(jsonItem, "id"));

		final Item.Settings settings = new Item.Settings();
		final Text name;
		final Text description;
		final boolean glint;
		final Float composterValue;

		// group
		group: if (jsonItem.has("group")) {
			final String groupName = JsonHelper.getString(jsonItem, "group");

			for (final ItemGroup group : ItemGroup.GROUPS) {
				if (group.getName().equalsIgnoreCase(groupName)) {
					settings.group(group);
					break group;
				}
			}

			final StringBuilder builder = new StringBuilder();
			builder.append("invalid item group '").append(groupName).append("\' valid values are : ( ");

			final String s = " | ";
			builder.append(ItemGroup.GROUPS[0].getName());
			for (int i = 1; i < ItemGroup.GROUPS.length; i++) builder.append(s).append(ItemGroup.GROUPS[i].getName());
			builder.append(" )");

			throw new JsonParseException(builder.toString());
		} else settings.group(defaultGroup);

		if (jsonItem.has("max_count")) settings.maxCount(JsonHelper.getInt(jsonItem, "max_count")); // maxCount key
		if (!jsonItem.has("name")) throw new JsonSyntaxException("Missing name, expected to find a json text element");
		name = Text.Serializer.fromJson(jsonItem.get("name")); // name
		description = jsonItem.has("description")
				? Text.Serializer.fromJson(jsonItem.get("description")).formatted(Formatting.GRAY) : null; // description

		if (jsonItem.has("rarity")) settings.rarity(parseRarity(jsonItem, "rarity")); // rarity

		glint = JsonHelper.getBoolean(jsonItem, "glint", false); // glint
		if (JsonHelper.getBoolean(jsonItem, "fireproof", false)) settings.fireproof(); // fire resistant

		// remainder item after used in a craft
		if (jsonItem.has("recipe_remainder")) settings.recipeRemainder(JsonHelper.getItem(jsonItem, "recipe_remainder"));

		composterValue = jsonItem.has("composter") ? JsonHelper.getFloat(jsonItem, "composter") : null;

		return new CommonItemProperties(itemId, settings, name, description, glint, composterValue);
	}

	private record CommonItemProperties(Identifier id, Item.Settings settings, Text name, Text description, boolean glint, Float composterValue) {
		
		/** register to composter map if required */
		void registerComposterValue(Item blockItem) {
			if (composterValue != null) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(blockItem, composterValue);
		}
		
	}

	/**
	 * Converts the string representation of an {@link UseAction}.
	 *
	 * @param container the object containing the property to parse
	 * @param fieldName the property name
	 *
	 * @return an {@link UseAction}
	 *
	 * @throws JsonParseException if the action name is invalid
	 */
	private UseAction parseAction(final JsonObject container, final String fieldName) throws JsonParseException {
		final String val = JsonHelper.getString(container, fieldName);

		switch (val) {
			case "none":
				return UseAction.NONE;

			case "eat":
				return UseAction.EAT;

			case "drink":
				return UseAction.DRINK;

			case "block":
				return UseAction.BLOCK;

			case "bow":
				return UseAction.BOW;

			case "spear":
				return UseAction.SPEAR;

			case "crossbow":
				return UseAction.CROSSBOW;

			default:
				throw new JsonParseException("Expected " + fieldName
						+ " to be a string ( none | eat | drink | block | bow | spear | crossbow ), got '" + val
						+ '\'');
		}
	}

	/**
	 * parse the string representation to a {@link Rarity} object
	 *
	 * @param container
	 * @param fieldName
	 *
	 * @return the rarity object
	 *
	 * @throws JsonParseException if the rarity name is invalid
	 */
	private Rarity parseRarity(final JsonObject container, final String fieldName) throws JsonParseException {
		final String val = JsonHelper.getString(container, fieldName);

		switch (val) {
			case "common":
				return Rarity.COMMON;

			case "uncommon":
				return Rarity.UNCOMMON;

			case "rare":
				return Rarity.RARE;

			case "epic":
				return Rarity.EPIC;

			default:
				throw new JsonParseException("Expected " + fieldName
						+ " to be ( common | uncommon | rare | epic ), got '" + val + '\'');
		}
	}

	//
	//// #FoodItem
	//

	public static SoundEvent parseSoundEvent(final String string) {
		final Identifier soundId = new Identifier(string);

		SoundEvent sound = Registry.SOUND_EVENT.get(soundId);
		if (sound == null) sound = Registry.register(Registry.SOUND_EVENT, soundId, new SoundEvent(soundId));

		return sound;
	}

}
