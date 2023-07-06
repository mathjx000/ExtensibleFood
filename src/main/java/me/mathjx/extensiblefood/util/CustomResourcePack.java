package me.mathjx.extensiblefood.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import me.mathjx.extensiblefood.ExtensibleFood;
import net.minecraft.SharedConstants;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;

public class CustomResourcePack extends DirectoryResourcePack {
	
	public static List<ResourcePack> addTo(List<ResourcePack> original,
			ResourceType type) {
		// ensure the list is modifiable
		final var modified = new ArrayList<>(original);
		modified
			.add(new CustomResourcePack(
				ExtensibleFood.COMMON_RESOURCEPACK_DIR, type));
		return modified;
	}
	
	private final ResourceType type;
	
	private CustomResourcePack(Path path, ResourceType type) {
		// boolean - after diving in code it seems that non-vanilla packs are set to false
		super("ExtensibleFood - External Pack", path, false);
		
		this.type = type;
	}
	
	@Override
	public InputSupplier<InputStream> openRoot(String... segments) {
		if (segments.length == 1 && segments[0].equals("pack.mcmeta")) {
			return this::createMetaStream;
		} else
			return super.openRoot(segments);
	}
	
	private InputStream createMetaStream() {
		return IOUtils.toInputStream(String.format("{\"pack\":{\"pack_format\":"
				+ SharedConstants.getGameVersion().getResourceVersion(type) + ",\"description\":\"\"}}"),
				StandardCharsets.UTF_8);
	}
	
}
