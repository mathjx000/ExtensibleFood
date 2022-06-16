package mathjx.extensiblefood.block.condition;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.block.SideShapeType;
import net.minecraft.command.CommandRegistryAccess;
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

			return new BlockPredicateCondition(offset, BlockPredicateArgumentType.blockPredicate(commandRegistryAccess).parse(new StringReader(predicateStr)));
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

				default -> throw new IllegalArgumentException("Unexpected side_shape_type value: " + faceShapeTypeName);
			};

			return new IsBlockSideSolidCondition(side, sideShapeType);
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
			CommandRegistryAccess commandRegistryAccess) throws JsonParseException {
		final String conditionName = JsonHelper.getString(jsonCondition, "condition");
		final ConditionDeserializer deserializer = DESERIALIZERS.get(conditionName);

		if (deserializer == null) {
			throw new JsonParseException("Unknown condition '" + conditionName + "'");
		} else {
			int len = 1;
			// all condition including null if no extra conditions
			final BlockStayCondition[] conditions = new BlockStayCondition[5];

			try {
				conditions[0] = deserializer.deserialize(jsonCondition, commandRegistryAccess);
			} catch (final CommandSyntaxException e) {
				throw new JsonSyntaxException(e);
			}

			BlockStayCondition[] subConditions;
			JsonArray array;

			if (jsonCondition.has("and")) {
				array = JsonHelper.getArray(jsonCondition, "and");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "and[" + i
								+ ']'), commandRegistryAccess);
					}

					conditions[len++] = subConditions.length == 1 ? subConditions[0] : createCompoundAND(subConditions);
				}
			}

			if (jsonCondition.has("or")) {
				array = JsonHelper.getArray(jsonCondition, "or");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "or[" + i
								+ ']'), commandRegistryAccess);
					}

					conditions[len++] = subConditions.length == 1 ? subConditions[0] : createCompoundOR(subConditions);
				}
			}

			if (jsonCondition.has("xor")) {
				array = JsonHelper.getArray(jsonCondition, "xor");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "xor[" + i
								+ ']'), commandRegistryAccess);
					}

					conditions[len++] = subConditions.length == 1 ? subConditions[0] : createCompoundXOR(subConditions);
				}
			}

			if (jsonCondition.has("nand")) {
				array = JsonHelper.getArray(jsonCondition, "nand");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "nand[" + i
								+ ']'), commandRegistryAccess);
					}

					conditions[len++] = createCompoundNAND(subConditions);
				}
			}

			if (len == 1) {
				return conditions[0];
			} else return createCompoundAND(conditions);
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

		BlockStayCondition deserialize(JsonObject json,
				CommandRegistryAccess access) throws JsonParseException, InvalidIdentifierException, CommandSyntaxException;

	}

}
