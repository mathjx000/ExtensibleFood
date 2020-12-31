package mathjx.extensiblefood.block;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class BlockShapeDeserializer {

	/**
	 * Parse a combined and simplified shape for a {@link BlockState}
	 * 
	 * @param  jsonShape          the serialized shape / shape collection
	 * @param  stateName          the name of the state
	 * 
	 * @return                    a simplified {@link VoxelShape}
	 * 
	 * @throws JsonParseException if any syntax error occurs
	 */
	public static VoxelShape deserializeStateShape(JsonElement jsonShape, String stateName) throws JsonParseException {
		if (jsonShape.isJsonArray()) {
			final JsonArray array = jsonShape.getAsJsonArray();
			final int len;

			// no need to compute a shape if empty
			if ((len = array.size()) == 0) return VoxelShapes.empty();

			final VoxelShape[] shapes = new VoxelShape[len];
			for (int i = 0; i < len; i++) {
				shapes[i] = parseSingleShape(JsonHelper.asObject(array.get(i), "array[" + i + "]"));
			}

			return Arrays.stream(shapes).reduce(VoxelShapes::union).get();
		} else if (jsonShape.isJsonObject()) {
			return parseSingleShape(jsonShape.getAsJsonObject());
		} else throw new JsonParseException("expected shape " + stateName + " to be an array or an object");
	}

	/**
	 * Deserialize a single {@link VoxelShape} element
	 * 
	 * @param  jsonShape           the serialized shape
	 * 
	 * @return                     a {@link VoxelShape}
	 * 
	 * @throws JsonSyntaxException if any syntax error occurs
	 */
	private static VoxelShape parseSingleShape(JsonObject jsonShape) throws JsonSyntaxException {
		final double x1, y1, z1, x2, y2, z2;

		JsonArray vectorArray = JsonHelper.getArray(jsonShape, "from");
		if (vectorArray.size() != 3) throw new JsonSyntaxException("Expected array 'from' to be of length 3");

		x1 = (double) JsonHelper.asFloat(vectorArray.get(0), "from[0]");
		y1 = (double) JsonHelper.asFloat(vectorArray.get(1), "from[1]");
		z1 = (double) JsonHelper.asFloat(vectorArray.get(2), "from[2]");

		vectorArray = JsonHelper.getArray(jsonShape, "to");
		if (vectorArray.size() != 3) throw new JsonSyntaxException("Expected array 'to' to be of length 3");

		x2 = (double) JsonHelper.asFloat(vectorArray.get(0), "to[0]");
		y2 = (double) JsonHelper.asFloat(vectorArray.get(1), "to[1]");
		z2 = (double) JsonHelper.asFloat(vectorArray.get(2), "to[2]");

		return VoxelShapes.cuboid(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
	}

}
