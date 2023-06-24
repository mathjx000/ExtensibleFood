package mathjx.extensiblefood.block.collision;

import static net.minecraft.util.JsonHelper.asObject;
import static net.minecraft.util.JsonHelper.asString;
import static net.minecraft.util.JsonHelper.getFloat;
import static net.minecraft.util.JsonHelper.getString;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import mathjx.extensiblefood.util.JsonUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class CollisionEffect {

	protected CollisionEffect() {}

	public abstract void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity);

	public static CollisionEffect parseCollisionEffect(
			final JsonElement jsonCollisionEffect) throws JsonSyntaxException {
		if (jsonCollisionEffect.isJsonArray()) {
			final JsonArray ja = jsonCollisionEffect.getAsJsonArray();
			final CollisionEffect[] compound = new CollisionEffect[ja.size()];

			for (int i = 0; i < compound.length; i++) compound[i] = parseCollisionEffect(ja.get(i));

			return new CompoundCollisionEffect(compound);
		} else {
			final JsonObject jo = asObject(jsonCollisionEffect, "collision_effects");

			final EntityType<?>[] exclusions = jo.has("exclude") ? parseEntityTypeArray(jo.get("exclude"), "exclude")
					: new EntityType[0];

			switch (getString(jo, "type")) {
				case "thorns": {
					final float damagesAmount = getFloat(jo, "damages", 1f);
					final double movementThreshold = getFloat(jo, "movement_threshold", -1f);

					return new ThornsCollisionEffect(exclusions, damagesAmount, movementThreshold);
				}

				case "slow_movement": {
					Vec3d multiplier;
					if (jo.has("multiplier")) {
						if (jo.isJsonPrimitive()) {
							final float _f = getFloat(jo, "multiplier");
							multiplier = new Vec3d(_f, _f, _f);
						} else multiplier = JsonUtils.parseVec3(jo.get("multiplier"), "multiplier");
					} else multiplier = new Vec3d(0.800000011920929D, 0.75D, 0.800000011920929D);

					return new SlowMovementCollisionEffect(exclusions, multiplier);
				}

				default:
					throw new JsonSyntaxException("Unexpected type, expected 'thorns' or 'slow_movement'");
			}
		}
	}

	private static EntityType<?>[] parseEntityTypeArray(final JsonElement jsonEntities, final String name) {
		if (jsonEntities.isJsonArray()) {
			final JsonArray ja = jsonEntities.getAsJsonArray();
			final EntityType<?>[] types = new EntityType[ja.size()];

			for (int i = 0; i < types.length; i++) types[i] = parseEntityType(ja.get(i), name + '[' + i + ']');

			return types;
		} else return new EntityType[] { parseEntityType(jsonEntities, name) };
	}

	private static EntityType<?> parseEntityType(final JsonElement jsonEntity,
			final String name) throws JsonSyntaxException {
		return Registries.ENTITY_TYPE.getOrEmpty(new Identifier(asString(jsonEntity, name))).orElseThrow(() -> new JsonSyntaxException("Unexpected "
				+ name + ", expected to be an entity id"));
	}

}
