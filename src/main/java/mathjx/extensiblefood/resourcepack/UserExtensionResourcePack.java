package mathjx.extensiblefood.resourcepack;

import static mathjx.extensiblefood.ExtensibleFood.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

public final class UserExtensionResourcePack extends AbstractFileResourcePack {

	private final LinkOption[] linkOptions = { LinkOption.NOFOLLOW_LINKS };

	public UserExtensionResourcePack() {
		super(null);
	}

	private Path getPath(String name) {
		Path p = COMMON_RESOURCEPACK_DIR.resolve(name).normalize();
		if (!p.startsWith(COMMON_RESOURCEPACK_DIR)) return null;
		return p;
	}

	@Override
	public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth,
			Predicate<String> pathFilter) {
		Path relativiser = getPath(type.getDirectory() + '/' + namespace);
		Path path = relativiser.resolve(prefix);
		Collection<Identifier> identifiers = new ArrayList<>();

		String systemSeparator = path.getFileSystem().getSeparator();

		try {
			if (path != null && Files.isDirectory(path, linkOptions)) {
				Files.walk(path, maxDepth)
						// the path must be a file
						.filter(Files::isRegularFile)
						// ensure the file is not an metadata file and the filter accept this path
						.filter(p -> {
							String filename = p.getFileName().toString();
							return !filename.endsWith(".mcmeta") && pathFilter.test(filename);
						})
						// make paths relative to the namespace
						.map(relativiser::relativize)
						// replace native path separators
						.map(p -> p.toString().replace(systemSeparator, "/"))
						// finally convert and append identifiers
						.forEach(p -> {
							try {
								identifiers.add(new Identifier(namespace, p));
							} catch (InvalidIdentifierException e) {
								LOGGER.error(e.getMessage());
							}
						});
			}
		} catch (IOException e) {
			LOGGER.warn("findResources custom at " + path + " in namespace " + namespace + ", mod " + MOD_ID
					+ " failed!", e);
		}

		return identifiers;
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		return USER_NAMESPACES;
	}

	@Override
	protected InputStream openFile(String name) throws IOException {
		InputStream stream = ModResourcePackUtil.openDefault(METADATA, name);
		if (stream != null) return stream;

		Path path = getPath(name);
		if (path == null || Files.notExists(path, linkOptions)) {
//			Matcher matcher = FAKEABLE_ITEM.matcher(name);
//			if (matcher.matches()) {
//				String s = matcher.group(2);
//				Identifier textureId = new Identifier(matcher.group(1), "item/"
//						+ s.substring(0, s.length() - ".json".length()));
//				JsonObject jsonModel = this.generateBasicItemModel(textureId);
//
//				// return the generated text as an input stream
//				return new ByteArrayInputStream(new GsonBuilder().create().toJson(jsonModel).getBytes(StandardCharsets.UTF_8));
//			} else 
			throw new FileNotFoundException(name);
		}

		return Files.newInputStream(path, linkOptions);
	}

	@Override
	protected boolean containsFile(String name) {
		if (ModResourcePackUtil.containsDefault(METADATA, name)) return true;
//		if (FAKEABLE_ITEM.matcher(name).matches()) return true;

		Path path = getPath(name);
		return path != null && Files.exists(path, linkOptions);
	}

	@Override
	public void close() {}

	@Override
	public String getName() {
		return MOD_ID + "/extension_pack";
	}

//	@Deprecated
//	private static final Pattern FAKEABLE_ITEM = Pattern.compile("^assets\\/(" + "[a-z0-9-_]+"
//			+ ")\\/models\\/item\\/(.*)(?<!\\.mcmeta)$");

//	/**
//	 * Generate a basic item model based on the texture of the food
//	 * 
//	 * @param  textureID the {@link Identifier} of the food
//	 * 
//	 * @return           a JSON object ready to be injected with other game models
//	 */
//	private JsonObject generateBasicItemModel(Identifier textureID) {
//		JsonObject jsonModel = new JsonObject();
//
//		// the parent model used to generate the model geometry based on the texture
//		jsonModel.addProperty("parent", "minecraft:item/generated");
//
//		// the textures object that contains every textures
//		JsonObject texturesObject = new JsonObject();
//		// the layer0 texture for generated models
//		texturesObject.addProperty("layer0", textureID.toString());
//		jsonModel.add("textures", texturesObject);
//
//		return jsonModel;
//	}

}
