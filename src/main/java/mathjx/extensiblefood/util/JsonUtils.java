package mathjx.extensiblefood.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class JsonUtils {

	public static Vec3d parseVec3(final JsonElement jsonVec, final String name) throws JsonSyntaxException {
		if (jsonVec.isJsonObject()) {
			final JsonObject jo = jsonVec.getAsJsonObject();
			return new Vec3d(JsonHelper.getFloat(jo, "x"), JsonHelper.getFloat(jo, "y"), JsonHelper.getFloat(jo, "z"));
		} else {
			final JsonArray ja = JsonHelper.asArray(jsonVec, name);

			if (ja.size() != 3) throw new JsonSyntaxException("Expected array '" + name + "' to be of length 3");

			return new Vec3d(JsonHelper.asFloat(ja.get(0), name + "[0]"), JsonHelper.asFloat(ja.get(1), name
					+ "[1]"), JsonHelper.asFloat(ja.get(2), name + "[2]"));
		}
	}

	public static Box parseBox01(final JsonObject jsonBox) throws JsonSyntaxException {
		final JsonElement from = jsonBox.get("from");
		if (from == null) throw new JsonSyntaxException("Missing from, expected to find a object or array");

		final JsonElement to = jsonBox.get("to");
		if (to == null) throw new JsonSyntaxException("Missing to, expected to find a object or array");

		return new Box(parseVec3(from, "from"), parseVec3(to, "to"));
	}

	public static Box parseBox016(final JsonObject jsonBox) throws JsonSyntaxException {
		final JsonElement from = jsonBox.get("from");
		if (from == null) throw new JsonSyntaxException("Missing from, expected to find a object or array");

		final JsonElement to = jsonBox.get("to");
		if (to == null) throw new JsonSyntaxException("Missing to, expected to find a object or array");

		return new Box(parseVec3(from, "from").multiply(1D / 16d), parseVec3(to, "to").multiply(1d / 16d));
	}

}
