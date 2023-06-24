package mathjx.extensiblefood.block;

import static mathjx.extensiblefood.ExtensibleFood.IS_CLIENT;
import static mathjx.extensiblefood.ExtensibleFood.LOGGER;
import static net.minecraft.util.JsonHelper.asFloat;
import static net.minecraft.util.JsonHelper.asObject;
import static net.minecraft.util.JsonHelper.getBoolean;
import static net.minecraft.util.JsonHelper.getFloat;
import static net.minecraft.util.JsonHelper.getInt;
import static net.minecraft.util.JsonHelper.getObject;
import static net.minecraft.util.JsonHelper.getString;
import static net.minecraft.util.JsonHelper.isNumber;

import java.util.Locale;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import mathjx.extensiblefood.block.collision.CollisionEffect;
import mathjx.extensiblefood.block.condition.BlockStayCondition;
import mathjx.extensiblefood.block.particle.ParticleEmission;
import mathjx.extensiblefood.food.ExtendedFoodComponent;
import mathjx.extensiblefood.food.FoodLoader;
import mathjx.extensiblefood.mixin.RenderLayerAccess;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.shape.VoxelShape;

public final class BlockParser {

	/**
	 * Dirty workaround for dynamic block state properties creation
	 */
	@Deprecated
	static IntProperty BRUH;

	public static synchronized Pair<Optional<Identifier>, Block> parseBlock(final JsonObject jsonBlock, final ExtendedFoodComponent foodComponent,
			final CommandRegistryAccess commandRegistryAccess) throws JsonSyntaxException {
		Identifier blockId = jsonBlock.has("id") ? new Identifier(JsonHelper.getString(jsonBlock, "id")) : null;
		
		final BlockType type;
		{
			if (!jsonBlock.has("type")) {
				LOGGER.warn("Missing " + "type" + ", expected to find String 'consumable' or 'crop'");
				type = BlockType.CONSUMABLE;
			} else type = switch (getString(jsonBlock, "type")) {
				case "consumable" -> BlockType.CONSUMABLE;
				case "crop" -> BlockType.CROP;

				default -> throw new JsonSyntaxException("Unexpected Block type expected to be \"consumable\" or \"crop\", got something invalid");
			};
		}

		// parse block settings
		final AbstractBlock.Settings settings;
		{
			final JsonElement e = jsonBlock.get("material");
			if (e != null) {
				settings = parseMaterial(e);
			} else if (type == BlockType.CROP) {
				settings = AbstractBlock.Settings.create().mapColor(MapColor.DARK_GREEN).noCollision().ticksRandomly()
						.breakInstantly().pistonBehavior(PistonBehavior.DESTROY);
			} else
				throw new JsonSyntaxException("Missing " + "material" + ", expected to find a String or an Object");
		}

		if (jsonBlock.has("sounds")) {
			final JsonObject soundObj = getObject(jsonBlock, "sounds");
			// @formatter:off
			settings.sounds(new BlockSoundGroup(
					getFloat(soundObj, "volume", 1f),
					getFloat(soundObj, "pitch", 1f),

					FoodLoader.parseSoundEvent(getString(soundObj, "break_sound")),
					FoodLoader.parseSoundEvent(getString(soundObj, "step_sound")),
					FoodLoader.parseSoundEvent(getString(soundObj, "place_sound")),
					FoodLoader.parseSoundEvent(getString(soundObj, "hit_sound")),
					FoodLoader.parseSoundEvent(getString(soundObj, "fall_sound"))
				));
			// @formatter:on
		} else if (type == BlockType.CROP) settings.sounds(BlockSoundGroup.CROP);

		if (jsonBlock.has("placement_conditions"))
			throw new JsonSyntaxException("Property 'placement_conditions' is no longer accepted, use 'placement_condition' instead");
		
		BlockStayCondition stayCondition = jsonBlock.has("placement_condition")
				? BlockStayCondition.parseCondition(JsonHelper.getObject(jsonBlock, "placement_condition"), commandRegistryAccess)
						: null;

		final ParticleEmission particleEmission = jsonBlock.has("particles")
				? ParticleEmission.parseParticleEmission(jsonBlock.get("particles")) : null;

		final CollisionEffect collisionEffect = jsonBlock.has("collision_effects")
				? CollisionEffect.parseCollisionEffect(jsonBlock.get("collision_effects")) : null;

		RenderLayer renderLayer;
		if (IS_CLIENT && jsonBlock.has("render_mode")) {
			renderLayer = switch (getString(jsonBlock, "render_mode").toLowerCase(Locale.ROOT)) {
				case "cutout" -> RenderLayer.getCutout();
				case "cutout_mipped" -> RenderLayer.getCutoutMipped();
				case "translucent" -> RenderLayer.getTranslucent();
				case "solid" -> RenderLayer.getSolid();

				default -> throw new JsonSyntaxException("Unexpected render_mode, expected string 'cutout', 'cutout_mipped', 'translucent' and 'solid'");
			};
		} else renderLayer = null;

		Block constructed;
		// parse type specific properties
		switch (type) {
			case CONSUMABLE:
				constructed = parseConsumableFoodBlock(jsonBlock, settings, foodComponent, stayCondition, particleEmission, collisionEffect);
				break;

			case CROP:
				constructed = parseCropFoodBlock(jsonBlock, settings.noCollision().ticksRandomly(), foodComponent, stayCondition, particleEmission, collisionEffect);
				if (IS_CLIENT && renderLayer == null) renderLayer = RenderLayer.getCutout();

				break;

			default:
				throw new RuntimeException("Illeeegaaaal!");
		}

		if (renderLayer != null) RenderLayerAccess.getBlocksMappedLayers().put(constructed, renderLayer);

		return new Pair<>(Optional.ofNullable(blockId), constructed);
	}

	private static ConsumableFoodBlock parseConsumableFoodBlock(final JsonObject jsonBlock,
			final AbstractBlock.Settings settings, final ExtendedFoodComponent foodComponent,
			final BlockStayCondition stayCondition, final ParticleEmission particleEmission,
			final CollisionEffect collisionEffect) throws JsonSyntaxException {
		if (jsonBlock.has("strength")) {
			parseBlockStrength(settings, jsonBlock.get("strength"));
		} else throw new JsonSyntaxException("Missing " + "strength" + ", expected to find an Object or a Float");

		if (jsonBlock.has("slipperiness")) settings.slipperiness(getFloat(jsonBlock, "slipperiness"));
		if (jsonBlock.has("velocity_multiplier")) settings.velocityMultiplier(getFloat(jsonBlock, "velocity_multiplier"));
		if (jsonBlock.has("jump_velocity_multiplier")) settings.jumpVelocityMultiplier(getFloat(jsonBlock, "jump_velocity_multiplier"));

		if (getBoolean(jsonBlock, "require_tool", false)) settings.requiresTool();

		final int bites = getInt(jsonBlock, "bites");
		final IntProperty propertyBites = IntProperty.of("bites", 0, bites);

		final VoxelShape[] bitesToShape = BlockShapeParser.parseShapes(getObject(jsonBlock, "shapes"), bites + 1);

		if (jsonBlock.has("lights")) parseLightingValues(settings, bites + 1, getObject(jsonBlock, "lights"), propertyBites);

		BRUH = propertyBites;
		return new ConsumableFoodBlock(settings, bites, propertyBites, bitesToShape, getBoolean(jsonBlock, "comparator_enabled", true), foodComponent, stayCondition, particleEmission, collisionEffect);
	}

	private static CropFoodBlock parseCropFoodBlock(final JsonObject jsonBlock, final AbstractBlock.Settings settings,
			final ExtendedFoodComponent foodComponent, final BlockStayCondition stayCondition,
			final ParticleEmission particleEmission, final CollisionEffect collisionEffect) throws JsonSyntaxException {
		if (jsonBlock.has("strength")) {
			parseBlockStrength(settings, jsonBlock.get("strength"));
		} else settings.breakInstantly();

		final int maxAge = getInt(jsonBlock, "max_age");
		final IntProperty propertyAge = IntProperty.of("age", 0, maxAge);

		final int minLight = checkLightRange(getInt(jsonBlock, "minimum_light", 9), "minimum_light");
		final int maxLight = checkLightRange(getInt(jsonBlock, "maximum_light", 15), "maximum_light");

		final VoxelShape[] agesToShape = BlockShapeParser.parseShapes(getObject(jsonBlock, "shapes"), maxAge + 1);

		if (jsonBlock.has("lights")) parseLightingValues(settings, maxAge + 1, getObject(jsonBlock, "lights"), propertyAge);
		BRUH = propertyAge;
		return new CropFoodBlock(settings, maxAge, getBoolean(jsonBlock, "fertilizable", true), minLight, maxLight, propertyAge, agesToShape, stayCondition, particleEmission, collisionEffect);
	}

	/**
	 * Parses block strength properties.
	 *
	 * @param settings The block settings
	 * @param element  The element to read from
	 *
	 * @throws JsonSyntaxException If any parsing error occurs
	 */
	private static void parseBlockStrength(final AbstractBlock.Settings settings,
			final JsonElement element) throws JsonSyntaxException {
		if (isNumber(element)) settings.strength(asFloat(element, "strength"));
		else {
			final JsonObject object = asObject(element, "strength");

			settings.strength(getFloat(object, "hardness"), getFloat(object, "resistance"));
		}
	}

	/**
	 * Parses block lightning values
	 *
	 * @param settings                 The block settings
	 * @param expectedValuesCount      The number of expected lighting values
	 * @param jsonLights               The lighting data to read from
	 * @param stateProperty2LightValue The property used to perform mapping from
	 *                                     {@link BlockState} to light value
	 *
	 * @throws JsonSyntaxException If any parsing error occurs
	 */
	private static void parseLightingValues(final AbstractBlock.Settings settings, final int expectedValuesCount,
			final JsonObject jsonLights, final IntProperty stateProperty2LightValue) throws JsonSyntaxException {

		final int defaultValue = getInt(jsonLights, "default", 0);
		final int[] values = new int[expectedValuesCount];

		for (int i = 0; i < expectedValuesCount; i++) {
			final String name = Integer.toString(i);

			values[i] = checkLightRange(getInt(jsonLights, name, defaultValue), name);
		}

		settings.luminance(s -> values[s.get(stateProperty2LightValue)]);
	}

	private static int checkLightRange(final int value, final String name) throws JsonSyntaxException {
		if (value < 0 || value > 15) throw new JsonSyntaxException("Expected " + name
				+ " to be between 0 and 15 (inclusives)");
		return value;
	}

	private static AbstractBlock.Settings parseMaterial(final JsonElement jsonMaterial)
			throws JsonParseException {
		final AbstractBlock.Settings settings;
		final boolean fullSpec;

		final JsonObject object = asObject(jsonMaterial, "material");

		if (object.has("copy_from_block")) {
			final Identifier blockId = new Identifier(JsonHelper.getString(object, "copy_from_block"));

			final Block block = Registries.BLOCK.get(blockId);

			if (block == null) throw new JsonParseException("No block found with the id: \"" + blockId + '"');

			settings = AbstractBlock.Settings.copy(block);
			fullSpec = false;
		} else {
			settings = AbstractBlock.Settings.create();
			fullSpec = true;
		}

		if (object.has("color") || fullSpec) {
			String materialIdentifier = getString(object, "color");

			final String dyePrefix = "dye//";
			if (materialIdentifier.startsWith(dyePrefix)) {
				materialIdentifier = materialIdentifier.substring(dyePrefix.length());

				DyeColor dye;
				if ((dye = DyeColor.byName(materialIdentifier, null)) == null) throw new JsonParseException("Invalid dye '"
						+ materialIdentifier + '\'');

				settings.mapColor(dye.getMapColor());
			} else {
				// StringUtils.parseColorHex(materialIdentifier);

				throw new JsonSyntaxException("Material colors other than dye colors are not allowed.");
			}
		}

		if (object.has("piston_behavior")) {
			final String behavior = getString(object, "piston_behavior");

			settings.pistonBehavior(switch (behavior.toLowerCase(Locale.ROOT)) {
				case "normal" -> PistonBehavior.NORMAL;
				case "destroy" -> PistonBehavior.DESTROY;
				case "block" -> PistonBehavior.BLOCK;
				case "ignore" -> PistonBehavior.IGNORE;
				case "push_only" -> PistonBehavior.PUSH_ONLY;
	
				default -> throw new JsonParseException("Invalid piston_behavior: '" + behavior + "'");
			});
		}
		

		if (getBoolean(object, "solid", true))
			settings.solid();

		if (!getBoolean(object, "blocks_movement", true))
			settings.noCollision();

		if (!getBoolean(object, "block_light", false))
			settings.nonOpaque();

		if (!getBoolean(object, "break_by_hand", true))
			settings.requiresTool();

		if (getBoolean(object, "burnable", false))
			settings.burnable();

		return settings;
	}

	private enum BlockType {

		CONSUMABLE,
		CROP;

	}

}
