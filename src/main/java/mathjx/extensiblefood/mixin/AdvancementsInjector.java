package mathjx.extensiblefood.mixin;

import static mathjx.extensiblefood.ExtensibleFood.LOGGER;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import mathjx.extensiblefood.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

@Mixin(ServerAdvancementLoader.class)
public final class AdvancementsInjector {

	@Inject(method = "apply",
			at = @At(	value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V",
						shift = Shift.BEFORE))
	private void inject(final Map<Identifier, JsonElement> map, final ResourceManager resourceManager,
			final Profiler profiler, final CallbackInfo cbi) {
		if (ModConfig.overrideFoodAdvancement) {
			final Identifier advancementID = new Identifier("minecraft", "husbandry/balanced_diet");

			final JsonElement json = map.get(advancementID);
			if (json != null) {
				final JsonObject advancement = json.getAsJsonObject();

				final Set<String> names = new HashSet<>();

				final JsonObject criteria = new JsonObject();
				final JsonArray requirements = new JsonArray();

				advancement.add("criteria", criteria);
				advancement.add("requirements", requirements);

				final String triggerName = new Identifier("minecraft", "consume_item").toString();

				Identifier identifier;
				String name;
				for (final Entry<RegistryKey<Item>, Item> e : Registry.ITEM.getEntrySet()) {
					if (!e.getValue().isFood()) continue;

					identifier = e.getKey().getValue();

					if (names.add(name = identifier.getPath()) || names.add(name = identifier.toString())) {
						final JsonObject criteriaEntry = new JsonObject();
						criteriaEntry.addProperty("trigger", triggerName);
						final JsonObject criteriaConditions = new JsonObject();
						final JsonArray itemsArray = new JsonArray();
						itemsArray.add(identifier.toString());
						final JsonObject itemCondition = new JsonObject();
						itemCondition.add("items", itemsArray);
						criteriaConditions.add("item", itemCondition);
						criteriaEntry.add("conditions", criteriaConditions);
						criteria.add(name, criteriaEntry);

						final JsonArray requirementsEntry = new JsonArray();
						requirementsEntry.add(name);
						requirements.add(requirementsEntry);
					} else
						// this case will never happen (in theory)
						throw new RuntimeException("Same item entry processed twice!");
				}

				LOGGER.debug("Replaced advancement {} with {} foods.", advancementID, names.size());
			}
		}
	}

}
