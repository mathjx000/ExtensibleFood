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
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

/**
 * Base condition class for placing blocks
 */
public abstract class BlockStayCondition {

	/**
	 * Test this condition
	 * 
	 * @param  state the state about to be placed
	 * @param  world the world
	 * @param  pos   the position
	 * 
	 * @return       <code>true</code> if the test is passed, <code>false</code>
	 *                   otherwise.
	 */
	public abstract boolean test(BlockState state, World world, BlockPos pos);

	//
	//

	/**
	 * This map contains a reference to every registered deserializers
	 */
	private static final Map<String, ConditionDeserializer> UNAMED_MAP = new HashMap<>();

	static {
		UNAMED_MAP.put("block_predicate", json -> {
			JsonArray offsetArray = JsonHelper.getArray(json, "offset");
			if (offsetArray.size() != 3) throw new JsonParseException("invalid offset array: length must be equals to 3");

			// @formatter:off
			Vec3i offset = new Vec3i(	JsonHelper.asInt(offsetArray.get(0), "offset[0]"),
										JsonHelper.asInt(offsetArray.get(1), "offset[1]"),
										JsonHelper.asInt(offsetArray.get(2), "offset[2]"));
			// @formatter:on

			String predicateStr = JsonHelper.getString(json, "predicate");

			return new BlockPredicateCondition(offset, BlockPredicateArgumentType.blockPredicate().parse(new StringReader(predicateStr)));
		});
	}

	/**
	 * Converts the given {@link JsonObject} into a {@link BlockStayCondition}
	 * 
	 * @param  jsonCondition      the serialized condition
	 * 
	 * @return                    a {@link BlockStayCondition} condition
	 * 
	 * @throws JsonParseException if any syntax errors occurs
	 */
	public static BlockStayCondition parseCondition(JsonObject jsonCondition) throws JsonParseException {
		final String conditionName = JsonHelper.getString(jsonCondition, "condition");
		final ConditionDeserializer deserializer = UNAMED_MAP.get(conditionName);

		if (deserializer == null) {
			throw new JsonParseException("Unknown condition '" + conditionName + "'");
		} else {
			int len = 1;
			// all condition including null if no extra conditions
			BlockStayCondition[] conditions = new BlockStayCondition[4];

			try {
				conditions[0] = deserializer.deserialize(jsonCondition);
			} catch (CommandSyntaxException e) {
				throw new JsonSyntaxException(e);
			}

			BlockStayCondition[] subConditions;
			JsonArray array;

			if (jsonCondition.has("and")) {
				array = JsonHelper.getArray(jsonCondition, "and");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "and[" + i + "]"));
					}

					conditions[len++] = subConditions.length == 1 ? subConditions[0] : createCompoundAND(subConditions);
				}
			}

			if (jsonCondition.has("or")) {
				array = JsonHelper.getArray(jsonCondition, "or");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "or[" + i + "]"));
					}

					conditions[len++] = subConditions.length == 1 ? subConditions[0] : createCompoundOR(subConditions);
				}
			}

			if (jsonCondition.has("xor")) {
				array = JsonHelper.getArray(jsonCondition, "xor");

				if (array.size() > 0) {
					subConditions = new BlockStayCondition[array.size()];

					for (int i = 0; i < subConditions.length; i++) {
						subConditions[i] = parseCondition(JsonHelper.asObject(array.get(i), "xor[" + i + "]"));
					}

					conditions[len++] = subConditions.length == 1 ? subConditions[0] : createCompoundXOR(subConditions);
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
	 * @param  conditions the conditions
	 * 
	 * @return
	 */
	public static BlockStayCondition createCompoundAND(BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.AND_Condition(conditions);
	}

	/**
	 * Creates a compound OR condition
	 * 
	 * @param  conditions the conditions
	 * 
	 * @return
	 */
	public static BlockStayCondition createCompoundOR(BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.OR_Condition(conditions);
	}

	/**
	 * Creates a compound XOR condition
	 * 
	 * @param  conditions the conditions
	 * 
	 * @return
	 */
	public static BlockStayCondition createCompoundXOR(BlockStayCondition... conditions) {
		return new CompoundBlockStayCondition.XOR_Condition(conditions);
	}

	/**
	 * The functional interface for deserializing {@link BlockStayCondition}
	 */
	@FunctionalInterface
	static interface ConditionDeserializer {

		BlockStayCondition deserialize(
				JsonObject json) throws JsonParseException, InvalidIdentifierException, CommandSyntaxException;

	}

}
