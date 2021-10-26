package mathjx.extensiblefood;

import static mathjx.extensiblefood.ExtensibleFood.LOGGER;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class ModConfig {

	private static final Path MOD_CONFIG_FILE = ExtensibleFood.MOD_CONFIG_DIR.resolve("config.properties");

	// UI behaviors
	public static final boolean showModRelatedErrors;
	public static final int displayFoodTooltipsBehavior;

	// Game features
	public static final boolean overrideFoodAdvancement;

	static {
		final AtomicBoolean dirtyFlag = new AtomicBoolean(false);
		final Properties props = new Properties();

//		boolean bShowModRelatedErrors = true;
//		int iDisplayFoodTooltips = 0;
//		boolean bOverrideFoodAdvancement = true;
//		Path pFoodAdvancementOverrideTemplate = null;

		if (Files.exists(MOD_CONFIG_FILE)) {
			try (Reader reader = Files.newBufferedReader(MOD_CONFIG_FILE, StandardCharsets.UTF_8)) {
				props.load(reader);
			} catch (final IOException e) {
				LOGGER.error("Failed to load configuration file '" + MOD_CONFIG_FILE.toString() + "'", e);
//				dirtyFlag.set(true);
//				initDefaults(props);
			}
		} else {
//			dirtyFlag.set(true);
//			initDefaults(props);
		}

		final boolean server = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;

		if (!server) {
			showModRelatedErrors = parseProperty(props, "show_mod_related_errors", Boolean::parseBoolean, () -> Boolean.toString(true), dirtyFlag);
			displayFoodTooltipsBehavior = parseProperty(props, "display_food_tooltips_behavior", Integer::parseInt, () -> Integer.toString(0), dirtyFlag);
		} else {
			showModRelatedErrors = false;
			displayFoodTooltipsBehavior = 0;
		}

		overrideFoodAdvancement = parseProperty(props, "override_food_advancement", Boolean::parseBoolean, () -> Boolean.toString(true), dirtyFlag);

//		showModRelatedErrors = bShowModRelatedErrors;
//		displayFoodTooltipsBehavior = iDisplayFoodTooltips;

		if (dirtyFlag.get()) {
			try (Writer writer = Files.newBufferedWriter(MOD_CONFIG_FILE, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				props.store(writer, "ExtensibleFood Mod Settings\nUse with care.\nWarning: Hosting a server with custom foods can produce unexpected results if the foods are not the same as the client.");
			} catch (final IOException e) {
				LOGGER.error("Failed to save configuration file '" + MOD_CONFIG_FILE.toString() + "'", e);
			}
		}
	}

	private static <T> T parseProperty(final Properties props, final String propertyName,
			final Function<String, T> parser, final Supplier<String> ifAbsent, final AtomicBoolean dirtyFlag) {
		String value;

		if (!props.containsKey(propertyName)) {
			dirtyFlag.set(true);
			if (ifAbsent == null || (value = ifAbsent.get()) == null) {
				value = null;
				props.put(propertyName, "");
			} else props.put(propertyName, value);
		} else {
			value = props.getProperty(propertyName);

			try {
				return parser.apply(value);
			} catch (final Exception e) {
				LOGGER.error("Failed to parse config property '" + propertyName + "' with value '" + value
						+ "', falling back to default: '" + (value = ifAbsent.get()) + "'", e);
			}
		}

		return parser.apply(value);
	}

	static void init() {
		LOGGER.debug("Configuration loaded");
	}

//	@Deprecated
//	private static void initDefaults(Properties props) {
//		props.putIfAbsent("show_mod_related_errors", Boolean.toString(true));
//		props.putIfAbsent("display_food_tooltips_behavior", Integer.toString(0));
////		props.putIfAbsent("custom_default_item_model", "");
//	}

}
