package mathjx.extensiblefood.food;

import static mathjx.extensiblefood.ExtensibleFood.IS_CLIENT;
import static mathjx.extensiblefood.ExtensibleFood.LOGGER;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.ComposterBlock;
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
import net.minecraft.util.Rarity;
import net.minecraft.util.UseAction;
import net.minecraft.util.registry.Registry;

public final class FoodLoader {

//	private final Gson gson;

	public FoodLoader() {
//		final GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
//		this.gson = builder.create();
	}

	public void applyFood(final JsonObject file, final Identifier autoId) throws JsonParseException {
		final ExtendedFoodComponent foodComponent = parseFoodComponent(JsonHelper.getObject(file, "food"), autoId);

		Item theItem;
		if (file.has("block")) {
			final JsonObject jsonBlock = JsonHelper.getObject(file, "block");
			final Block block = BlockParser.parseBlock(jsonBlock, foodComponent);

			if (block instanceof CropFoodBlock) {
				if (jsonBlock.has("crop_item")) {
					// then parse the crop item as a item block
					Registry.register(Registry.ITEM, new Identifier(autoId.toString()
							+ "_seeds"), parseItemBlock(JsonHelper.getObject(jsonBlock, "crop_item"), block));
					theItem = null;
				} else theItem = parseFoodItem(JsonHelper.getObject(file, "item"), foodComponent, block);
			} else theItem = parseItemBlock(JsonHelper.getObject(file, "item"), block);

			Registry.register(Registry.BLOCK, autoId, block);
			LOGGER.debug("Registered block with id {}", autoId.toString());
		} else theItem = null;

		if (theItem == null) theItem = parseFoodItem(JsonHelper.getObject(file, "item"), foodComponent, null);

		Registry.register(Registry.ITEM, autoId, theItem);
		LOGGER.debug("Registered item with id {}", autoId.toString());
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
	ExtendedFoodComponent parseFoodComponent(final JsonObject foodJson,
			final Identifier foodId) throws JsonParseException {
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

//		if (saturation > hunger) LOGGER.warn("The food '{}' has a saturation of {} which is greater than the maximum imposed by its hunger ({}). Saturation will be clamped to {}.", foodId.toString(), saturation, hunger, hunger);

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
					: null
			);
		// @formatter:on
	}

	//
	//// #Food
	//

	//
	//// FoodItem
	//

	Item parseFoodItem(final JsonObject jsonItem, final ExtendedFoodComponent foodComponent,
			final Block block) throws JsonParseException {
		final Item.Settings settings = new Item.Settings();

		final UseAction itemUseAction;
		final AtomicReference<Text> itemName_ref = new AtomicReference<Text>(),
				itemDescription_ref = new AtomicReference<>(null);
		final AtomicBoolean itemGlint_ref = new AtomicBoolean(false);
		final AtomicReference<Float> composterValue_ref = new AtomicReference<>(null);
		final Item itemConsumeRemainder;
		final Identifier itemModelPredicate;

		parseCommonItemProperties(jsonItem, settings, itemName_ref, itemDescription_ref, itemGlint_ref, composterValue_ref);

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

		settings.food(foodComponent.food);

		// Build the item
		final Item item = block == null
				? new ExtensibleFoodItem(settings, itemName_ref.get(), itemDescription_ref.get(), itemUseAction, itemGlint_ref.get(), itemConsumeRemainder, foodComponent)
				: new ExtensibleFoodCropItem(block, settings, itemName_ref.get(), itemDescription_ref.get(), itemGlint_ref.get(), itemUseAction, itemConsumeRemainder, foodComponent);

		// register it to composter map if required
		{
			final Float f = composterValue_ref.get();
			if (f != null) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(item, f.floatValue());
		}
		// register the model predicate provider if specified
		if (itemModelPredicate != null) {
			FabricModelPredicateProviderRegistry.register(item, itemModelPredicate, FoodConsumptionProgressModelPredicate.INSTANCE);
		}

		return item;
	}

	private BlockItem parseItemBlock(final JsonObject jsonItem, final Block block) {
		final Item.Settings settings = new Item.Settings();

		final AtomicReference<Text> name_ref = new AtomicReference<Text>(),
				descriptionRef = new AtomicReference<>(null);
		final AtomicBoolean glint_ref = new AtomicBoolean(false);
		final AtomicReference<Float> composterValue_ref = new AtomicReference<>(null);

		parseCommonItemProperties(jsonItem, settings, name_ref, descriptionRef, glint_ref, composterValue_ref);

		final BlockItem blockItem;
		if (block instanceof ConsumableFoodBlock) blockItem = new ExtensibleFoodBlockItem((ConsumableFoodBlock) block, settings, name_ref.get(), descriptionRef.get(), glint_ref.get());
		else if (block instanceof CropFoodBlock) {
			blockItem = new ExtensibleFoodCropItem(block, settings, name_ref.get(), descriptionRef.get(), glint_ref.get(), null, null, null);
			((CropFoodBlock) block).seedItem = blockItem;
		} else throw new RuntimeException("Illeeegaaaal!");

		// register to composter map if required
		{
			final Float f = composterValue_ref.get();
			if (f != null) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(blockItem, f.floatValue());
		}

		return blockItem;
	}

	private void parseCommonItemProperties(final JsonObject jsonItem, final Item.Settings settings,
			final AtomicReference<Text> name_ref, final AtomicReference<Text> description_ref,
			final AtomicBoolean glint_ref, final AtomicReference<Float> composterValue_ref) throws JsonParseException {
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
		} else settings.group(ItemGroup.FOOD);

		if (jsonItem.has("max_count")) settings.maxCount(JsonHelper.getInt(jsonItem, "max_count")); // maxCount key
		if (!jsonItem.has("name")) throw new JsonSyntaxException("Missing name, expected to find a json text element");
		name_ref.set(Text.Serializer.fromJson(jsonItem.get("name"))); // name
		description_ref.set(jsonItem.has("description")
				? Text.Serializer.fromJson(jsonItem.get("description")).formatted(Formatting.GRAY) : null); // description

		if (jsonItem.has("rarity")) settings.rarity(parseRarity(jsonItem, "rarity")); // rarity

		glint_ref.set(JsonHelper.getBoolean(jsonItem, "glint", false)); // glint
		if (JsonHelper.getBoolean(jsonItem, "fireproof", false)) settings.fireproof(); // fire resistant

		// remainder item after used in a craft
		if (jsonItem.has("recipe_remainder")) settings.recipeRemainder(JsonHelper.getItem(jsonItem, "recipe_remainder"));

		if (jsonItem.has("composter")) composterValue_ref.set(JsonHelper.getFloat(jsonItem, "composter"));
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
