package mathjx.extensiblefood.block;

import static net.minecraft.util.JsonHelper.getObject;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import mathjx.extensiblefood.util.JsonUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class BlockShapeParser {

	/**
	 * Parse a combined and simplified shape for a {@link BlockState}
	 *
	 * @param jsonShape the serialized shape / shape collection
	 * @param stateName the name of the state
	 *
	 * @return a simplified {@link VoxelShape}
	 *
	 * @throws JsonParseException if any syntax error occurs
	 */
	public static VoxelShape parseStateShape(final JsonElement jsonShape,
			final String stateName) throws JsonSyntaxException {
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
		} else throw new JsonSyntaxException("expected shape " + stateName + " to be an array or an object");
	}

	public static VoxelShape[] parseShapes(JsonObject jsonShapes, int expectedShapeCount) throws JsonSyntaxException {
		if (jsonShapes.has("base")) {
			VoxelShape base = parseStateShape(jsonShapes.get("base"), "base");
			Direction dir = JsonUtils.getDirection(jsonShapes, "direction");

			return msliceBaseShape(base, expectedShapeCount, dir);
		}

		VoxelShape[] shapes = new VoxelShape[expectedShapeCount];
		final VoxelShape defaultShape = jsonShapes.has("default")
				? BlockShapeParser.parseStateShape(getObject(jsonShapes, "default"), "default") : VoxelShapes.empty();

		for (int i = 0; i < expectedShapeCount; i++) {
			final String blockStateName = Integer.toString(i);
			if (jsonShapes.has(blockStateName)) {
				shapes[i] = BlockShapeParser.parseStateShape(jsonShapes.get(blockStateName), "shapes[" + blockStateName
						+ ']');
			} else shapes[i] = defaultShape;
		}

		return shapes;
	}

	private static VoxelShape[] msliceBaseShape(VoxelShape shape, int slices, Direction direction) {
		direction = direction.getOpposite();

		VoxelShape[] shapes = new VoxelShape[slices];
		Box bb = shape.getBoundingBox();

		// direction vector
		Vec3i vec3i = direction.getVector();
		// inverted direction vector mask
		Vec3i dirMaskInv = new Vec3i(1 - Math.abs(vec3i.getX()), 1 - Math.abs(vec3i.getY()), 1 - Math.abs(vec3i.getZ()));

		double bx = direction == Direction.WEST ? bb.maxX : bb.minX,
				by = direction == Direction.DOWN ? bb.maxY : bb.minY,
				bz = direction == Direction.NORTH ? bb.maxZ : bb.minZ;

		// vec3i gives the masking and the direction
		double dx = (bb.getXLength() / (double) slices) * vec3i.getX(),
				dy = (bb.getYLength() / (double) slices) * vec3i.getY(),
				dz = (bb.getZLength() / (double) slices) * vec3i.getZ();

		double mx = bb.maxX * dirMaskInv.getX() + bx * Math.abs(vec3i.getX()) + dx,
				my = bb.maxY * dirMaskInv.getY() + by * Math.abs(vec3i.getY()) + dy,
				mz = bb.maxZ * dirMaskInv.getZ() + bz * Math.abs(vec3i.getZ()) + dz;

		slices--;
		for (int i = slices - 1; i >= 0; i--) {
			shapes[i] = VoxelShapes.combine(shape, VoxelShapes.cuboid(bx, by, bz, mx, my, mz), BooleanBiFunction.ONLY_FIRST);
			mx += dx;
			my += dy;
			mz += dz;
		}
		shapes[slices] = shape; // the last shape is the full shape

		return shapes;
	}

	/**
	 * Deserialize a single {@link VoxelShape} element
	 *
	 * @param jsonShape the serialized shape
	 *
	 * @return a {@link VoxelShape}
	 *
	 * @throws JsonSyntaxException if any syntax error occurs
	 */
	private static VoxelShape parseSingleShape(final JsonObject jsonShape) throws JsonSyntaxException {
//		final double x1, y1, z1, x2, y2, z2;
//
//		JsonArray vectorArray = JsonHelper.getArray(jsonShape, "from");
//		if (vectorArray.size() != 3) throw new JsonSyntaxException("Expected array 'from' to be of length 3");
//
//		x1 = (double) JsonHelper.asFloat(vectorArray.get(0), "from[0]");
//		y1 = (double) JsonHelper.asFloat(vectorArray.get(1), "from[1]");
//		z1 = (double) JsonHelper.asFloat(vectorArray.get(2), "from[2]");
//
//		vectorArray = JsonHelper.getArray(jsonShape, "to");
//		if (vectorArray.size() != 3) throw new JsonSyntaxException("Expected array 'to' to be of length 3");
//
//		x2 = (double) JsonHelper.asFloat(vectorArray.get(0), "to[0]");
//		y2 = (double) JsonHelper.asFloat(vectorArray.get(1), "to[1]");
//		z2 = (double) JsonHelper.asFloat(vectorArray.get(2), "to[2]");

//		return VoxelShapes.cuboid(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
		return VoxelShapes.cuboid(JsonUtils.parseBox016(jsonShape));
	}

}
