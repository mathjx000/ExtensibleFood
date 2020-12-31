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
	private void inject(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler,
			CallbackInfo cbi) {
		if (ModConfig.overrideFoodAdvancement) {
			final Identifier advancementID = new Identifier("minecraft", "husbandry/balanced_diet");

			JsonElement json = map.get(advancementID);
			if (json != null) {
				JsonObject advancement = json.getAsJsonObject();

				Set<String> names = new HashSet<>();

				JsonObject criteria = new JsonObject();
				JsonArray requirements = new JsonArray();

				advancement.add("criteria", criteria);
				advancement.add("requirements", requirements);

				String triggerName = new Identifier("minecraft", "consume_item").toString();

				Identifier identifier;
				String name;
				for (Entry<RegistryKey<Item>, Item> e : Registry.ITEM.getEntries()) {
					if (!e.getValue().isFood()) continue;

					identifier = e.getKey().getValue();

					if (names.add(name = identifier.getPath()) || names.add(name = identifier.toString())) {
						JsonObject a = new JsonObject();
						a.addProperty("trigger", triggerName);
						JsonObject b = new JsonObject();
						JsonObject c = new JsonObject();
						c.addProperty("item", identifier.toString());
						b.add("item", c);
						a.add("conditions", b);
						criteria.add(name, a);

						JsonArray d = new JsonArray();
						d.add(name);
						requirements.add(d);
					} else
						// this case will never happen (in theory)
						throw new RuntimeException("Same item entry processed twice!");
				}

				LOGGER.info("Replaced advancement {} with {} foods.", advancementID, names.size());
			}
		}
	}

}
