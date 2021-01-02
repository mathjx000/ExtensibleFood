package mathjx.extensiblefood.block;

import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import mathjx.extensiblefood.block.condition.BlockStayCondition;
import mathjx.extensiblefood.food.ExtendedFoodComponent;
import mathjx.extensiblefood.food.FoodLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.DyeColor;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class BlockDeserializer {

	static IntProperty BRUH;

	public static synchronized ExtensibleFoodBlock deszerializeBlock(JsonObject jsonBlock,
			ExtendedFoodComponent foodComponent) throws JsonSyntaxException {
		final Material material;
		{
			JsonElement e = jsonBlock.get("material");
			if (e == null) throw new JsonSyntaxException("Missing " + "material"
					+ ", expected to find a String or an Object");
			material = parseMaterial(e);
		}
		final AbstractBlock.Settings settings = AbstractBlock.Settings.of(material);

		if (jsonBlock.has("strength")) {
			JsonElement element = jsonBlock.get("strength");

			if (JsonHelper.isNumber(element)) {
				settings.strength(JsonHelper.asFloat(element, "strength"));
			} else {
				JsonObject object = JsonHelper.asObject(element, "strength");

				settings.strength(JsonHelper.getFloat(object, "hardness"), JsonHelper.getFloat(object, "resistance"));
			}
		} else throw new JsonSyntaxException("Missing " + "strength" + ", expected to find an Object or a Float");

		if (jsonBlock.has("slipperiness")) settings.slipperiness(JsonHelper.getFloat(jsonBlock, "slipperiness"));
		if (jsonBlock.has("velocity_multiplier")) settings.velocityMultiplier(JsonHelper.getFloat(jsonBlock, "velocity_multiplier"));
		if (jsonBlock.has("jump_velocity_multiplier")) settings.jumpVelocityMultiplier(JsonHelper.getFloat(jsonBlock, "jump_velocity_multiplier"));

		if (jsonBlock.has("sounds")) {
			JsonObject soundObj = JsonHelper.getObject(jsonBlock, "sounds");
			// @formatter:off
			settings.sounds(new BlockSoundGroup(
					JsonHelper.getFloat(soundObj, "volume", 1f),
					JsonHelper.getFloat(soundObj, "pitch", 1f),
					
					FoodLoader.parseSoundEvent(JsonHelper.getString(soundObj, "break_sound")),
					FoodLoader.parseSoundEvent(JsonHelper.getString(soundObj, "step_sound")),
					FoodLoader.parseSoundEvent(JsonHelper.getString(soundObj, "place_sound")),
					FoodLoader.parseSoundEvent(JsonHelper.getString(soundObj, "hit_sound")),
					FoodLoader.parseSoundEvent(JsonHelper.getString(soundObj, "fall_sound"))
				));
			// @formatter:on
		}

		if (JsonHelper.getBoolean(jsonBlock, "require_tool", false)) settings.requiresTool();

		final int bites = JsonHelper.getInt(jsonBlock, "bites");
		final IntProperty propertyBites = IntProperty.of("bites", 0, bites);

		final VoxelShape[] bitesToShape = new VoxelShape[bites + 1];

		{
			final JsonObject jsonShapes = JsonHelper.getObject(jsonBlock, "shapes");
			final VoxelShape defaultShape = jsonShapes.has("default")
					? BlockShapeDeserializer.deserializeStateShape(JsonHelper.getObject(jsonShapes, "default"), "default")
					: VoxelShapes.empty();

			for (int i = 0; i <= bites; i++) {
				String blockStateName = Integer.toString(i);
				if (jsonShapes.has(blockStateName)) {
					bitesToShape[i] = BlockShapeDeserializer.deserializeStateShape(jsonShapes.get(blockStateName), "shapes["
							+ blockStateName + ']');
				} else bitesToShape[i] = defaultShape;
			}
		}

		if (jsonBlock.has("lights")) {
			final JsonObject jsonLights = JsonHelper.getObject(jsonBlock, "lights");
			final int defaultValue = JsonHelper.getInt(jsonLights, "default", 0);
			final int[] values = new int[bites + 1];

			for (int i = 0; i <= bites; i++) values[i] = JsonHelper.getInt(jsonLights, Integer.toString(i), defaultValue);

			settings.luminance(s -> values[s.get(propertyBites)]);
		}

		BlockStayCondition condition;
		if (jsonBlock.has("placement_conditions")) {
			JsonElement jsonCondition = jsonBlock.get("placement_conditions");

			if (jsonCondition.isJsonArray()) {
				final JsonArray array = jsonCondition.getAsJsonArray();
				final int len = array.size();

				if (len == 0) {
					condition = null;
				} else {
					final BlockStayCondition[] conditions = new BlockStayCondition[len];

					for (int i = 0; i < len; i++) {
						conditions[i] = BlockStayCondition.parseCondition(JsonHelper.asObject(array.get(i), "placement_conditions["
								+ i + '\''));
					}

					condition = BlockStayCondition.createCompoundAND(conditions);
				}
			} else condition = BlockStayCondition.parseCondition(JsonHelper.asObject(jsonCondition, "placement_conditions"));
		} else condition = null;

		return new ExtensibleFoodBlock(settings, bites, BRUH = propertyBites, bitesToShape, JsonHelper.getBoolean(jsonBlock, "comparator_enabled", true), foodComponent, condition);
	}

	private static Material parseMaterial(JsonElement jsonMaterial) throws JsonParseException {
		if (jsonMaterial.isJsonPrimitive()) {
			final String materialName = jsonMaterial.getAsString().toLowerCase(Locale.ROOT);

			// Don't say any thing... Just cry.
			switch (materialName) {
				case "air":
					return Material.AIR;

				case "structure_void":
					return Material.STRUCTURE_VOID;

				case "portal":
					return Material.PORTAL;

				case "carpet":
					return Material.CARPET;

				case "plant":
					return Material.PLANT;

				case "underwater_plant":
					return Material.UNDERWATER_PLANT;

				case "replaceable_plant":
					return Material.REPLACEABLE_PLANT;

				case "nether_shoots":
					return Material.NETHER_SHOOTS;

				case "replaceable_underwater_plant":
					return Material.REPLACEABLE_UNDERWATER_PLANT;

				case "water":
					return Material.WATER;

				case "bubble_column":
					return Material.BUBBLE_COLUMN;

				case "lava":
					return Material.LAVA;

				case "snow_layer":
					return Material.SNOW_LAYER;

				case "fire":
					return Material.FIRE;

				case "supported":
					return Material.SUPPORTED;

				case "cobweb":
					return Material.COBWEB;

				case "redstone_lamp":
					return Material.REDSTONE_LAMP;

				case "organic_product":
					return Material.ORGANIC_PRODUCT;

				case "soil":
					return Material.SOIL;

				case "solid_organic":
					return Material.SOLID_ORGANIC;

				case "dense_ice":
					return Material.DENSE_ICE;

				case "aggregate":
					return Material.AGGREGATE;

				case "sponge":
					return Material.SPONGE;

				case "shulker_box":
					return Material.SHULKER_BOX;

				case "wood":
					return Material.WOOD;

				case "nether_wood":
					return Material.NETHER_WOOD;

				case "bamboo_sapling":
					return Material.BAMBOO_SAPLING;

				case "bamboo":
					return Material.BAMBOO;

				case "wool":
					return Material.WOOL;

				case "tnt":
					return Material.TNT;

				case "leaves":
					return Material.LEAVES;

				case "glass":
					return Material.GLASS;

				case "ice":
					return Material.ICE;

				case "cactus":
					return Material.CACTUS;

				case "stone":
					return Material.STONE;

				case "metal":
					return Material.METAL;

				case "snow_block":
					return Material.SNOW_BLOCK;

				case "repair_station":
					return Material.REPAIR_STATION;

				case "barrier":
					return Material.BARRIER;

				case "piston":
					return Material.PISTON;

				case "unused_plant":
					return Material.UNUSED_PLANT;

				case "ground":
					return Material.GOURD;

				case "egg":
					return Material.EGG;

				case "cake":
					return Material.CAKE;

				default:
					throw new JsonParseException("Unknown material '" + materialName + '\'');
			}
		} else {
			final JsonObject object = JsonHelper.asObject(jsonMaterial, "material");

			MaterialColor color;
			{
				String materialIdentifier = JsonHelper.getString(object, "color");

				final String dyePrefix = "dye//";
				if (materialIdentifier.startsWith(dyePrefix)) {
					materialIdentifier = materialIdentifier.substring(dyePrefix.length());

					DyeColor dye;
					if ((dye = DyeColor.byName(materialIdentifier, null)) == null) throw new JsonParseException("Invalid dye '"
							+ materialIdentifier + '\'');

					color = dye.getMaterialColor();
				} else {
					// TODO
					throw new UnsupportedOperationException("Not yet implement");
				}
			}

			PistonBehavior pistonBehavior = PistonBehavior.NORMAL;
			if (object.has("piston_behavior")) {
				String behavior = JsonHelper.getString(object, "piston_behavior");

				switch (behavior.toLowerCase(Locale.ROOT)) {
					case "normal":
						pistonBehavior = PistonBehavior.NORMAL;
						break;

					case "destroy":
						pistonBehavior = PistonBehavior.DESTROY;
						break;

					case "block":
						pistonBehavior = PistonBehavior.BLOCK;
						break;

					case "ignore":
						pistonBehavior = PistonBehavior.IGNORE;
						break;

					case "push_only":
						pistonBehavior = PistonBehavior.PUSH_ONLY;
						break;

					default:
						throw new JsonParseException("Invalid piston_behavior: '" + pistonBehavior + "'");
				}
			}

			return new Material(color, false, JsonHelper.getBoolean(object, "solid", true), JsonHelper.getBoolean(object, "blocks_movement", true), JsonHelper.getBoolean(object, "block_light", false), JsonHelper.getBoolean(object, "break_by_hand", true), JsonHelper.getBoolean(object, "burnable", false), pistonBehavior);
		}
	}

}
