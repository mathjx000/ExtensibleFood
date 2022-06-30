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

	public static VoxelShape[] parseShapes(final JsonObject jsonShapes,
			final int expectedShapeCount) throws JsonSyntaxException {
		if (jsonShapes.has("base")) {
			final VoxelShape base = parseStateShape(jsonShapes.get("base"), "base");
			final Direction dir = JsonUtils.getDirection(jsonShapes, "direction");

			return generateSlicedShapes(base, expectedShapeCount, dir);
		}

		final VoxelShape[] shapes = new VoxelShape[expectedShapeCount];
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

	/**
	 * Generates slices shapes based on <code>baseShape</code> like Minecraft Cake.
	 * 
	 * @param baseShape The base {@link VoxelShape shape}
	 * @param sliceCount    The number of slices to cut
	 * @param direction The direction of the slicing process
	 * 
	 * @return Progressively sliced shapes
	 */
	private static VoxelShape[] generateSlicedShapes(final VoxelShape baseShape, final int sliceCount, final Direction direction) {
		final VoxelShape[] shapes = new VoxelShape[sliceCount];
		final Box bb = baseShape.getBoundingBox();

		// direction vector
		final Vec3i vec3i = direction.getVector();
		// inverted direction vector mask
		final Vec3i dirMaskInv = new Vec3i(1 - Math.abs(vec3i.getX()), 1 - Math.abs(vec3i.getY()), 1 - Math.abs(vec3i.getZ()));

		final double bx = direction == Direction.WEST ? bb.maxX : bb.minX,
				by = direction == Direction.DOWN ? bb.maxY : bb.minY,
				bz = direction == Direction.NORTH ? bb.maxZ : bb.minZ;

		// vec3i gives the masking and the direction
		final double dx = bb.getXLength() / sliceCount * vec3i.getX(),
				dy = bb.getYLength() / sliceCount * vec3i.getY(),
				dz = bb.getZLength() / sliceCount * vec3i.getZ();

		double mx = bb.maxX * dirMaskInv.getX() + bx * Math.abs(vec3i.getX()) + dx,
				my = bb.maxY * dirMaskInv.getY() + by * Math.abs(vec3i.getY()) + dy,
				mz = bb.maxZ * dirMaskInv.getZ() + bz * Math.abs(vec3i.getZ()) + dz;

		shapes[0] = baseShape; // the first shape is the full shape
		for (int i = 1; i < sliceCount; i++) {
			shapes[i] = VoxelShapes.combine(baseShape, VoxelShapes.cuboid(Math.min(bx, mx), Math.min(by, my), Math.min(bz, mz), Math.max(bx, mx), Math.max(by, my), Math.max(bz, mz)), BooleanBiFunction.ONLY_FIRST);
			mx += dx;
			my += dy;
			mz += dz;
		}

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
		return VoxelShapes.cuboid(JsonUtils.parseBox016(jsonShape));
	}

}
