package me.mathjx.extensiblefood.block.particle;

import static me.mathjx.extensiblefood.util.JsonUtils.parseBox016;
import static me.mathjx.extensiblefood.util.JsonUtils.parseVec3;
import static net.minecraft.util.JsonHelper.asObject;
import static net.minecraft.util.JsonHelper.getBoolean;
import static net.minecraft.util.JsonHelper.getFloat;
import static net.minecraft.util.JsonHelper.getInt;
import static net.minecraft.util.JsonHelper.getObject;
import static net.minecraft.util.JsonHelper.getString;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public abstract class ParticleEmission {

	protected ParticleEmission() {}

	public abstract void spawn(World world, BlockPos pos, Random random);

	public static ParticleEmission parseParticleEmission(final JsonElement jsonParticles, final RegistryWrapper<ParticleType<?>> registry) throws JsonSyntaxException {
		if (true) // FIXME
			throw new UnsupportedOperationException("particles are not supported yet due to changes in game code");
		
		if (jsonParticles.isJsonArray()) {
			final JsonArray array = jsonParticles.getAsJsonArray();
			final ParticleEmission[] emissions = new ParticleEmission[array.size()];

			for (int i = 0; i < emissions.length; i++) emissions[i] = parseParticleEmission(array.get(i), registry);

			return new CompoundParticleEmission(emissions);
		}

		final JsonObject jsonParticle = asObject(jsonParticles, "particles");

		Box[] boxes;
		if (jsonParticle.has("box")) {
			final JsonElement element = jsonParticle.get("box");

			if (element.isJsonArray()) {
				final JsonArray array = element.getAsJsonArray();
				boxes = new Box[array.size()];

				for (int i = 0; i < boxes.length; i++) boxes[i] = parseBox016(asObject(array.get(i), "box[" + i + ']'));
			} else boxes = new Box[] { parseBox016(asObject(element, "box")) };
		} else throw new JsonSyntaxException("Missing box, expected to find a box or array");

		final float batchChance = getFloat(jsonParticle, "batch_chance", 1f);
		final float chance = getFloat(jsonParticle, "chance", 1f);
		final int perBatchAmount = getInt(jsonParticle, "per_batch_amount", 1);
		final boolean important = getBoolean(jsonParticle, "important", false);
		final boolean alwaysSpawn = getBoolean(jsonParticle, "always_spawn", false);

		ParticleEffect particleEffect;
		try {
			particleEffect = ParticleEffectArgumentType.readParameters(new StringReader(getString(jsonParticle, "particle")), registry);
		} catch (final CommandSyntaxException e) {
			throw new JsonSyntaxException(e.getMessage(), e);
		}

		final Vec3d baseVelocity = jsonParticle.has("velocity_base")
				? parseVec3(getObject(jsonParticle, "velocity_base"), "velocity_base") : Vec3d.ZERO;
		final Vec3d randVelocity = jsonParticle.has("velocity_random")
				? parseVec3(getObject(jsonParticle, "velocity_random"), "velocity_random") : Vec3d.ZERO;

		return new EffectParticleEmission(boxes, particleEffect, batchChance, perBatchAmount, chance, important, alwaysSpawn, baseVelocity, randVelocity);
	}

}
