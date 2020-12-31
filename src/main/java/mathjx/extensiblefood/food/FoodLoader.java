package mathjx.extensiblefood.food;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import mathjx.extensiblefood.block.BlockDeserializer;
import mathjx.extensiblefood.block.ExtensibleFoodBlock;
import mathjx.extensiblefood.item.ExtensibleFoodItem;
import mathjx.extensiblefood.item.ExtensibleFoodItemBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
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

import static mathjx.extensiblefood.ExtensibleFood.LOGGER;

public final class FoodLoader {

//	private final Gson gson;

	public FoodLoader() {
//		final GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
//		this.gson = builder.create();
	}

	public void applyFood(JsonObject file, Identifier autoId) throws JsonParseException {
		final ExtendedFoodComponent foodComponent = parseFoodComponent(JsonHelper.getObject(file, "food"));

		final Item theItem;
		if (file.has("block")) {
			// then the item should be a BlockItem that can't be eaten directly like cake

			final JsonObject jsonBlock = JsonHelper.getObject(file, "block");
			final ExtensibleFoodBlock block = BlockDeserializer.deszerializeBlock(jsonBlock, foodComponent);

			theItem = parseItemBlock(JsonHelper.getObject(file, "item"), block);

			Registry.register(Registry.BLOCK, autoId, block);
			LOGGER.debug("Registered block with id {}", autoId.toString());
		} else {
			theItem = parseFoodItem(JsonHelper.getObject(file, "item"), foodComponent);
		}

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
	 * @param  foodJson           the source JSON
	 * 
	 * @return
	 * 
	 * @throws JsonParseException if any JSON syntax error is found
	 */
	ExtendedFoodComponent parseFoodComponent(JsonObject foodJson) throws JsonParseException {
		FoodComponent.Builder builder = new FoodComponent.Builder();

		Integer hunger = null;
		Float saturation = null;
		Boolean alwaysEdible = null, meat = null, snack = null;
		Integer itemEatTime;
		SoundEvent itemEatSound;

		if (foodJson.has("base")) { // then we need to copy all parameters of the given item
			Item baseItem = JsonHelper.getItem(foodJson, "base");

			if (!baseItem.isFood()) throw new JsonParseException("Invalid food base item '"
					+ Registry.ITEM.getId(baseItem) + "', this item exists but it not a food");

			FoodComponent base = baseItem.getFoodComponent();

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
	 * @param  object serialized effect
	 * 
	 * @return        an instance of the serialized effect
	 */
	private StatusEffectInstance parseEffect(JsonObject object) {
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

	ExtensibleFoodItem parseFoodItem(JsonObject jsonItem,
			ExtendedFoodComponent foodComponent) throws JsonParseException {
		final Item.Settings settings = new Item.Settings();

		final UseAction itemUseAction;
		final AtomicReference<Text> itemNameRef = new AtomicReference<Text>(),
				itemDescriptionRef = new AtomicReference<>(null);
		final AtomicBoolean itemGlintRef = new AtomicBoolean(false);
		final AtomicReference<Float> composterValueRef = new AtomicReference<>(null);
		final Item itemConsumeRemainder;

		parseCommonItemProperties(jsonItem, settings, itemNameRef, itemDescriptionRef, itemGlintRef, composterValueRef);

		itemUseAction = jsonItem.has("action") ? parseAction(jsonItem, "action") : UseAction.EAT; // action

		// remainder item after eaten
		// TODO specification support
		if (jsonItem.has("consume_remainder")) {
			itemConsumeRemainder = JsonHelper.getItem(jsonItem, "consume_remainder");
		} else itemConsumeRemainder = null;

		settings.food(foodComponent.food);

		// Build the item
		final ExtensibleFoodItem item = new ExtensibleFoodItem(settings, itemNameRef.get(), itemDescriptionRef.get(), itemUseAction, itemGlintRef.get(), itemConsumeRemainder, foodComponent);

		// register it to composter map if required
		{
			Float f = composterValueRef.get();
			if (f != null) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(item, f.floatValue());
		}

		return item;
	}

	private ExtensibleFoodItemBlock parseItemBlock(JsonObject jsonItem, ExtensibleFoodBlock block) {
		final Item.Settings settings = new Item.Settings();

		final AtomicReference<Text> nameRef = new AtomicReference<Text>(), descriptionRef = new AtomicReference<>(null);
		final AtomicBoolean glintRef = new AtomicBoolean(false);
		final AtomicReference<Float> composterValueRef = new AtomicReference<>(null);

		parseCommonItemProperties(jsonItem, settings, nameRef, descriptionRef, glintRef, composterValueRef);

		final ExtensibleFoodItemBlock itemBlock = new ExtensibleFoodItemBlock(block, settings, nameRef.get(), descriptionRef.get(), glintRef.get());

		// register to composter map if required
		{
			Float f = composterValueRef.get();
			if (f != null) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(itemBlock, f.floatValue());
		}

		return itemBlock;
	}

	private void parseCommonItemProperties(JsonObject jsonItem, Item.Settings settings, AtomicReference<Text> nameRef,
			AtomicReference<Text> descriptionRef, AtomicBoolean glintRef,
			AtomicReference<Float> composterValueRef) throws JsonParseException {
		// group
		if (jsonItem.has("group")) {
			final String groupName = JsonHelper.getString(jsonItem, "group");

			for (ItemGroup group : ItemGroup.GROUPS) {
				if (group.getName().equals(groupName)) {
					settings.group(group);
					break;
				}
			}

			StringBuilder builder = new StringBuilder();
			builder.append("invalid item group '").append(groupName).append("\' valid values are : ( ");

			final String s = " | ";
			builder.append(ItemGroup.GROUPS[0].getName());
			for (int i = 1; i < ItemGroup.GROUPS.length; i++) builder.append(s).append(ItemGroup.GROUPS[i].getName());
			builder.append(" )");

			throw new JsonParseException(builder.toString());
		} else settings.group(ItemGroup.FOOD);

		if (jsonItem.has("max_count")) settings.maxCount(JsonHelper.getInt(jsonItem, "max_count")); // maxCount key
		if (!jsonItem.has("name")) throw new JsonSyntaxException("Missing name, expected to find a json text element");
		nameRef.set(Text.Serializer.fromJson(jsonItem.get("name"))); // name
		descriptionRef.set(jsonItem.has("description")
				? Text.Serializer.fromJson(jsonItem.get("description")).formatted(Formatting.GRAY) : null); // description

		if (jsonItem.has("rarity")) settings.rarity(parseRarity(jsonItem, "rarity")); // rarity

		glintRef.set(JsonHelper.getBoolean(jsonItem, "glint", false)); // glint
		if (JsonHelper.getBoolean(jsonItem, "fireproof", false)) settings.fireproof(); // fire resistant

		// remainder item after used in a craft
		if (jsonItem.has("recipe_remainder")) settings.recipeRemainder(JsonHelper.getItem(jsonItem, "recipe_remainder"));

		if (jsonItem.has("composter")) composterValueRef.set(JsonHelper.getFloat(jsonItem, "composter"));
	}

	/**
	 * Converts the string representation of an {@link UseAction}.
	 * 
	 * @param  container          the object containing the property to parse
	 * @param  fieldName          the property name
	 * 
	 * @return                    an {@link UseAction}
	 * 
	 * @throws JsonParseException if the action name is invalid
	 */
	private UseAction parseAction(JsonObject container, String fieldName) throws JsonParseException {
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
	 * @param  container
	 * @param  fieldName
	 * 
	 * @return                    the rarity object
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

	public static SoundEvent parseSoundEvent(String string) {
		Identifier soundId = new Identifier(string);

		SoundEvent sound = Registry.SOUND_EVENT.get(soundId);
		if (sound == null) sound = Registry.register(Registry.SOUND_EVENT, soundId, new SoundEvent(soundId));

		return sound;
	}

}
