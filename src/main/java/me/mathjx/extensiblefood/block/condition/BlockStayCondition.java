package me.mathjx.extensiblefood.block.condition;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import me.mathjx.extensiblefood.util.UnsafeCommandRegistryAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.SideShapeType;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

/**
 * Base condition class for placing blocks
 */
public abstract class BlockStayCondition {

	/**
	 * Test this condition
	 *
	 * @param state the state about to be placed
	 * @param world the world
	 * @param pos   the position
	 *
	 * @return <code>true</code> if the test is passed, <code>false</code>
	 *             otherwise.
	 */
	public abstract boolean test(BlockState state, World world, BlockPos pos);

	//
	//

	private static final Map<String, ConditionDeserializer> DESERIALIZERS = new HashMap<>();

	static {
		DESERIALIZERS.put("block_predicate", (json, commandRegistryAccess) -> {
			final JsonArray offsetArray = JsonHelper.getArray(json, "offset");
			if (offsetArray.size() != 3) throw new JsonParseException("invalid offset array: length must be equals to 3");

			// @formatter:off
			final Vec3i offset = new Vec3i(	JsonHelper.asInt(offsetArray.get(0), "offset[0]"),
										JsonHelper.asInt(offsetArray.get(1), "offset[1]"),
										JsonHelper.asInt(offsetArray.get(2), "offset[2]"));
			// @formatter:on

			final String predicateStr = JsonHelper.getString(json, "predicate");

			return new BlockPredicateCondition(offset, BlockPredicateArgumentType.blockPredicate(
					/*
					 * SAFETY: under normal circumstances, tags are the only part that may be
					 * troublesome but since only equality checks are performed on tag entries in
					 * the worst case no entries are matched at all.
					 * 
					 * This DOES means however that save specific tags wont be taken into account.
					 */
					commandRegistryAccess.unsafeAccess()).parse(new StringReader(predicateStr)));
		});
		DESERIALIZERS.put("is_side_solid", (json, commandRegistryAccess) -> {
			final String sideName = JsonHelper.getString(json, "side", Direction.DOWN.getName());
			final Direction side = Direction.byName(sideName);
			if (side == null) throw new JsonSyntaxException("Unexpected side: " + sideName);

			final String faceShapeTypeName = JsonHelper.getString(json, "shape_type");
			final SideShapeType sideShapeType = switch (faceShapeTypeName.toLowerCase()) {
				case "full" -> SideShapeType.FULL;
				case "center" -> SideShapeType.CENTER;
				case "rigid" -> SideShapeType.RIGID;

				default -> throw new JsonSyntaxException("Unexpected side_shape_type value: " + faceShapeTypeName);
			};

			return new IsBlockSideSolidCondition(side, sideShapeType);
		});
		DESERIALIZERS.put("compound", (json, commandRegistryAccess) -> {
			String type = JsonHelper.getString(json, "type", "or").toLowerCase();
			
			JsonArray jsonComponents = JsonHelper.getArray(json, "components");
			
			BlockStayCondition[] conditions = new BlockStayCondition[jsonComponents.size()];
			
			for (int i = 0; i < conditions.length; i++) {
				conditions[i] =  parseCondition(JsonHelper.asObject(jsonComponents.get(i), "components[" + Integer.toString(i) + ']'), commandRegistryAccess);
			}
			
			return switch (type) {
				case "and" -> createCompoundAND(conditions);
				case "or" -> createCompoundOR(conditions);
				case "xor" -> createCompoundXOR(conditions);
				case "nand" -> createCompoundNAND(conditions);
				
				default -> throw new JsonSyntaxException("Unexpected 'type' value: " + type);
			};
		});
	}

	/**
	 * Converts the given {@link JsonObject} into a {@link BlockStayCondition}
	 *
	 * @param jsonCondition         the serialized condition
	 * @param commandRegistryAccess the registry access
	 *
	 * @return a {@link BlockStayCondition} condition
	 *
	 * @throws JsonParseException if any syntax errors occurs
	 */
	public static BlockStayCondition parseCondition(final JsonObject jsonCondition,
			final UnsafeCommandRegistryAccess commandRegistryAccess) throws JsonParseException {
		final String conditionName = JsonHelper.getString(jsonCondition, "condition");
		final ConditionDeserializer deserializer = DESERIALIZERS.get(conditionName);

		if (deserializer == null) {
			throw new JsonParseException("Unknown condition '" + conditionName + "'");
		} else {
			try {
				return deserializer.deserialize(jsonCondition, commandRegistryAccess);
			} catch (final CommandSyntaxException e) {
				throw new JsonSyntaxException(e);
			}
		}
	}

	//

	/**
	 * Creates a compound AND condition
	 *
	 * @param conditions the conditions
	 *
	 * @return
	 */
	public static BlockStayCondition createCompoundAND(final BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.AND_Condition(conditions);
	}

	/**
	 * Creates a compound OR condition
	 *
	 * @param conditions the conditions
	 *
	 * @return
	 */
	public static BlockStayCondition createCompoundOR(final BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.OR_Condition(conditions);
	}

	/**
	 * Creates a compound XOR condition
	 *
	 * @param conditions the conditions
	 *
	 * @return
	 */
	public static BlockStayCondition createCompoundXOR(final BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.XOR_Condition(conditions);
	}

	/**
	 * Creates a compound NAND condition
	 *
	 * @param conditions the conditions
	 *
	 * @return
	 */
	public static BlockStayCondition createCompoundNAND(final BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.NAND_Condition(conditions);
	}

	/**
	 * The functional interface for deserializing {@link BlockStayCondition}
	 */
	@FunctionalInterface
	interface ConditionDeserializer {

		BlockStayCondition deserialize(JsonObject json, UnsafeCommandRegistryAccess access)
				throws JsonParseException, InvalidIdentifierException, CommandSyntaxException;

	}

}
