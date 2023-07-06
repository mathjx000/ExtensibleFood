package me.mathjx.extensiblefood.food;

import static me.mathjx.extensiblefood.ExtensibleFood.IS_CLIENT;
import static me.mathjx.extensiblefood.ExtensibleFood.LOGGER;

import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import me.mathjx.extensiblefood.block.BlockParser;
import me.mathjx.extensiblefood.block.ConsumableFoodBlock;
import me.mathjx.extensiblefood.block.CropFoodBlock;
import me.mathjx.extensiblefood.item.ExtensibleFoodBlockItem;
import me.mathjx.extensiblefood.item.ExtensibleFoodCropItem;
import me.mathjx.extensiblefood.item.ExtensibleFoodItem;
import me.mathjx.extensiblefood.item.FoodConsumptionProgressModelPredicate;
import me.mathjx.extensiblefood.item.ItemGroupApplier;
import me.mathjx.extensiblefood.util.FoodMathUtils;
import me.mathjx.extensiblefood.util.UnsafeCommandRegistryAccess;
import net.minecraft.block.Block;
import net.minecraft.block.ComposterBlock;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.Rarity;
import net.minecraft.util.UseAction;

public final class FoodLoader {

	public static final int FORMAT_VERSION = 3;
	
	private final UnsafeCommandRegistryAccess commandRegistryAccess = new UnsafeCommandRegistryAccess(
			CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup()));
	private final ItemGroupApplier groupApplier;

	public FoodLoader(ItemGroupApplier groupApplier) {
		this.groupApplier = groupApplier;
	}

	public void applyFood(final JsonObject file, final Identifier autoId) throws JsonParseException {
		{
			int fileVersion = JsonHelper.getInt(file, "format_version", 1);
			
			if (fileVersion < FORMAT_VERSION) {
				throw new JsonSyntaxException("This file use an older (version " + fileVersion + "), incompatible format. Current: "+ FORMAT_VERSION);
			} else if (fileVersion > FORMAT_VERSION) {
				throw new JsonSyntaxException("This file use a newer (version " + fileVersion + ") format that is not supported by this version of the mod. Please update the mod in order to load this file.");
			}
		}
		
		
		final ExtendedFoodComponent foodComponent = parseFoodComponent(JsonHelper.getObject(file, "food"), autoId);

		Pair<Optional<Identifier>, Block> block;
		if (file.has("block")) {
			final JsonObject jsonBlock = JsonHelper.getObject(file, "block");
			block = BlockParser.parseBlock(jsonBlock, foodComponent, commandRegistryAccess);

			if (jsonBlock.has("crop_item"))
				throw new JsonSyntaxException("'crop_item' object was moved into the 'item' object as 'additional_crop_item' !");

			doRegister(Registries.BLOCK, block.getLeft().orElse(autoId), block.getRight());
		} else block = null;

		if (file.has("item")) {
			JsonObject jsonItem = JsonHelper.getObject(file, "item");
			
			Pair<Identifier, ? extends Item> item; 
			if (jsonItem.has("additional_crop_item")) {
				if (block == null || !(block.getRight() instanceof CropFoodBlock)) throw new JsonSyntaxException("Object 'additional_crop_item' is invalid in this context.");
				
				final JsonObject jsonCropItem = JsonHelper.getObject(jsonItem, "additional_crop_item");
				final Pair<Identifier, BlockItem> cropItem = parseItemBlock(jsonCropItem, block.getRight(), ItemGroups.NATURAL, null);
				// then parse the crop item as a item block
				doRegister(Registries.ITEM,
						cropItem.getLeft() == null ?
								new Identifier(autoId.toString() + "_seeds") :
								cropItem.getLeft(),
						cropItem.getRight());
				
				item = parseFoodItem(jsonItem, foodComponent);
			} else if (block != null) {
				// no seeds are defined so either the block is a consumable block or a a comestible crop like potato
				item = parseItemBlock(jsonItem, block.getRight(), ItemGroups.FOOD_AND_DRINK, foodComponent);
			} else {
				item = parseFoodItem(jsonItem, foodComponent);
			}
			
			doRegister(Registries.ITEM, item.getLeft() == null ? autoId : item.getLeft(), item.getRight());
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
					+ Registries.ITEM.getId(baseItem) + "', this item exists but it not a food");

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
		if (foodJson.has("saturation") || saturation == null) saturation = FoodMathUtils.humanReadableSaturationPointsToSaturationRatio(hunger, JsonHelper.getFloat(foodJson, "saturation"));
		if (foodJson.has("saturation_ratio") || saturation == null) {
			if (foodJson.has("saturation"))
				throw new JsonSyntaxException("Conflit between properties 'saturation' and 'saturation_ratio'. Use only one of them.");
			saturation = JsonHelper.getFloat(foodJson, "saturation_ratio");
		}

		if (foodJson.has("always_edible") || alwaysEdible == null) alwaysEdible = JsonHelper.getBoolean(foodJson, "always_edible", false);
		if (foodJson.has("meat") || meat == null) meat = JsonHelper.getBoolean(foodJson, "meat", false);
		if (foodJson.has("snack") || snack == null) snack = JsonHelper.getBoolean(foodJson, "snack", false);
		itemEatTime = foodJson.has("eat_time") ? JsonHelper.getInt(foodJson, "eat_time") : null;
		itemEatSound = foodJson.has("eat_sound")
				? itemEatSound = parseSoundEvent(JsonHelper.getString(foodJson, "eat_sound")) : null;

		builder.hunger(hunger);
		builder.saturationModifier(saturation);
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
		final StatusEffect effect = Registries.STATUS_EFFECT.getOrEmpty(new Identifier(id)).orElseThrow(() -> new JsonParseException("Expected id to be an item, was unknown string '"
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

	private Pair<Identifier, Item> parseFoodItem(final JsonObject jsonItem, final ExtendedFoodComponent foodComponent) throws JsonParseException {
		final CommonItemProperties props =  parseCommonItemProperties(jsonItem);
		final CommonFoodItemProperties foodProps = parseCommonFoodItemProperties(jsonItem);

		props.settings.food(foodComponent.food);

		// Build the item
		final Item item = new ExtensibleFoodItem(props.settings, props.name, props.description, foodProps.itemUseAction, props.glint, foodProps.itemConsumeRemainder, foodComponent);

		props.registerComposterValue(item);
		foodProps.applyItemModelPredicateProvider(item);
		groupApplier.addToExisting(item, props.itemGroup == null ?
				ItemGroups.FOOD_AND_DRINK :
				props.itemGroup);

		return new Pair<>(props.id, item);
	}

	private Pair<Identifier, BlockItem> parseItemBlock(final JsonObject jsonItem, final Block block, RegistryKey<ItemGroup> defaultGroup, ExtendedFoodComponent foodComponent) {
		final CommonItemProperties props = parseCommonItemProperties(jsonItem);

		final BlockItem blockItem;
		if (block instanceof ConsumableFoodBlock) {
			blockItem = new ExtensibleFoodBlockItem((ConsumableFoodBlock) block, props.settings, props.name, props.description, props.glint);
		} else if (block instanceof CropFoodBlock) {
			if (foodComponent == null) {
				blockItem = new ExtensibleFoodCropItem(block, props.settings, props.name, props.description, props.glint, null, null, null);
			} else {
				CommonFoodItemProperties foodProps = parseCommonFoodItemProperties(jsonItem);
				props.settings.food(foodComponent.food);
				blockItem = new ExtensibleFoodCropItem(block, props.settings, props.name, props.description, props.glint, foodProps.itemUseAction, foodProps.itemConsumeRemainder, foodComponent);
				foodProps.applyItemModelPredicateProvider(blockItem);
			}
			((CropFoodBlock) block).seedItem = blockItem;
		} else throw new RuntimeException();

		props.registerComposterValue(blockItem);
		groupApplier.addToExisting(blockItem, props.itemGroup == null ? defaultGroup : props.itemGroup);

		return new Pair<>(props.id, blockItem);
	}

	private CommonFoodItemProperties parseCommonFoodItemProperties(JsonObject jsonItem) {
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
		
		return new CommonFoodItemProperties(itemUseAction, itemConsumeRemainder, itemModelPredicate);
	}

	private CommonItemProperties parseCommonItemProperties(final JsonObject jsonItem) throws JsonParseException {
		Identifier itemId = jsonItem.has("id") ? new Identifier(JsonHelper.getString(jsonItem, "id")) : null;

		final Item.Settings settings = new Item.Settings();
		final Text name;
		final Text description;
		final boolean glint;
		final Float composterValue;

		// group
		final RegistryKey<ItemGroup> group;
		if (jsonItem.has("group")) {
			final Identifier id; 
			try {
				id = new Identifier(JsonHelper.getString(jsonItem, "group"));
			} catch (InvalidIdentifierException e) {
				throw new JsonSyntaxException(e);
			}

			if (Registries.ITEM_GROUP.containsId(id)) {
				group = RegistryKey.of(Registries.ITEM_GROUP.getKey(), id);
			} else {
				final StringBuilder builder = new StringBuilder();
				builder.append("invalid item group '")
						.append(id)
						.append("\' valid values are : ( ")
						.append(Registries.ITEM_GROUP.getKeys()
								.stream()
								.map(RegistryKey::getValue)
								.map(Identifier::toString)
								.collect(Collectors.joining(" | ")))
						.append(" )");
				throw new JsonParseException(builder.toString());
			}
		} else
			group = null;

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

		return new CommonItemProperties(itemId, group, settings, name, description, glint, composterValue);
	}

	private static record CommonItemProperties(Identifier id, RegistryKey<ItemGroup> itemGroup, Item.Settings settings, Text name, Text description, boolean glint, Float composterValue) {
		
		/** register to composter map if required */
		void registerComposterValue(Item item) {
			if (composterValue != null) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(item, composterValue.floatValue());
		}
		
	}
	
	private record CommonFoodItemProperties(UseAction itemUseAction, Item itemConsumeRemainder, Identifier itemModelPredicate) {
		void applyItemModelPredicateProvider(Item item) {
			// register the model predicate provider if specified
			if (itemModelPredicate != null) {
				ModelPredicateProviderRegistry.register(item, itemModelPredicate, FoodConsumptionProgressModelPredicate.INSTANCE);
			}
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

		SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
		if (sound == null) sound = Registry.register(Registries.SOUND_EVENT, soundId, SoundEvent.of(soundId));

		return sound;
	}

}
