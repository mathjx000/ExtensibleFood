package me.mathjx.extensiblefood;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.mathjx.extensiblefood.command.FoodInfoCommand;
import me.mathjx.extensiblefood.command.FoodStackArgumentType;
import me.mathjx.extensiblefood.food.FoodLoader;
import me.mathjx.extensiblefood.gui.screen.ErrorScreenGadget;
import me.mathjx.extensiblefood.item.ItemGroupApplier;
import me.mathjx.extensiblefood.item.tooltip.FoodTooltip;
import me.mathjx.extensiblefood.item.tooltip.FoodTooltipComponentCallback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public final class ExtensibleFood implements ModInitializer {

	/**
	 * The mod id
	 */
	public static final String MOD_ID = "extensible_food";
	/**
	 * The mod logger
	 */
	public static final Logger LOGGER = LogManager.getLogger(ExtensibleFood.class);
	/**
	 * <code>true</code> if the game is a client instance
	 */
	public static final boolean IS_CLIENT = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	/**
	 * The mod container
	 */
	public static final ModContainer CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();
	/**
	 * The mod metadata
	 */
	public static final ModMetadata METADATA = CONTAINER.getMetadata();
	/**
	 * The mod configuration directory
	 */
	public static final Path MOD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).toAbsolutePath().normalize();
	/**
	 * The food root directory
	 */
	public static final Path FOOD_DIR = MOD_CONFIG_DIR.resolve("foods");
	/**
	 * The resource pack directory
	 */
	public static final Path COMMON_RESOURCEPACK_DIR = MOD_CONFIG_DIR.resolve("resourcepack").toAbsolutePath();

	@Deprecated(forRemoval = true)
	public static final Set<String> USER_NAMESPACES = new HashSet<>();

	/**
	 * The entry point of the mod
	 */
	@Override
	public void onInitialize() {
		try {
			if (Files.notExists(FOOD_DIR)) Files.createDirectories(FOOD_DIR);
			if (Files.notExists(COMMON_RESOURCEPACK_DIR)) Files.createDirectories(COMMON_RESOURCEPACK_DIR);
		} catch (final IOException e) {
			LOGGER.error("Failed to create directory", e);
		}

		ModConfig.init();

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) LOGGER.warn("Warning: Hosting a server with custom foods can produce unexpected results if the foods are not the same as the client. Use at your own risk.");

		LOGGER.debug("Starting loading cutom foods");
		final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		final long start = System.currentTimeMillis();
		int counter = 0;

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(FOOD_DIR, ExtensibleFood::isValidFoodNamespace)) {
			final ItemGroupApplier groupApplier = new ItemGroupApplier();
			final FoodLoader loader = new FoodLoader(groupApplier);
			for (final Path dir : stream) {
				LOGGER.debug("Searching in namespace directory {}", FOOD_DIR.relativize(dir).toString());
				try {
					counter += readNamespaceDirectory(dir, gson, loader);
				} catch (final IOException e) {
					LOGGER.error("Failed to read files in food directory '" + dir.toString() + "'", e);
				}
			}
			
			groupApplier.apply();
		} catch (final IOException e) {
			LOGGER.error("Error when loading foods in directory {}", FOOD_DIR, e);
		}

		LOGGER.info("Successfully loaded {} custom foods in {}ms.", Integer.toString(counter), Long.toString(System.currentTimeMillis() - start));

		ArgumentTypeRegistry.registerArgumentType(new Identifier(MOD_ID, "food_stack"), FoodStackArgumentType.class, ConstantArgumentSerializer.of(FoodStackArgumentType::new));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess,
				environment) -> FoodInfoCommand.register(dispatcher, registryAccess));

		if (IS_CLIENT && ModConfig.displayFoodTooltipsImagesLevel > 0) {
			TooltipComponentCallback.EVENT.register(new FoodTooltipComponentCallback());
		}
		if (IS_CLIENT && ModConfig.displayFoodTooltipsTextLevel > 0) {
			ItemTooltipCallback.EVENT.register(new FoodTooltip());
		}
	}

	private int readNamespaceDirectory(final Path dir, final Gson gson, final FoodLoader loader) throws IOException {
		final String namespace = dir.getFileName().toString();
		USER_NAMESPACES.add(namespace);

		int counter = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, ExtensibleFood::isValidFoodFile)) {
			for (final Path file : stream) {
				final String fileName = file.getFileName().toString();
				LOGGER.debug("Loading food {}", fileName);

				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					final JsonElement element = gson.fromJson(reader, JsonElement.class);
					final JsonObject object = JsonHelper.asObject(element, "top element");

					final Identifier autoId = new Identifier(namespace, fileName.substring(0, fileName.length() - ".json".length()));
					loader.applyFood(object, autoId);

					counter++;
				} catch (final IOException e) {
					LOGGER.error("Failed to read file '" + file.toString() + '\'', e);
				} catch (final Exception e) {
					LOGGER.error("Error reading file '" + fileName + '\'', e);

					if (
					// ensure client
					FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
					// do not load classes if not required
							ModConfig.showModRelatedErrors) ErrorScreenGadget.report(file, e.getLocalizedMessage());
				}
			}
		}
		return counter;
	}

	private static boolean isValidFoodNamespace(final Path path) throws IOException {
		return Files.isDirectory(path);
	}

	private static boolean isValidFoodFile(final Path path) throws IOException {
		return path.getFileName().toString().endsWith(".json") && Files.isRegularFile(path);
	}

}
