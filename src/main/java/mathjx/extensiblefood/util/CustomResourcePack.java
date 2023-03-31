package mathjx.extensiblefood.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import mathjx.extensiblefood.ExtensibleFood;
import net.minecraft.SharedConstants;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;

public class CustomResourcePack extends DirectoryResourcePack {
	
	public static List<ResourcePack> addTo(List<ResourcePack> original,
			ResourceType type) {
		// ensure the list is modifiable
		final var modified = new ArrayList<>(original);
		modified
			.add(new CustomResourcePack(
				ExtensibleFood.COMMON_RESOURCEPACK_DIR.toFile(), type));
		return modified;
	}
	
	private final ResourceType type;
	
	private CustomResourcePack(File file, ResourceType type) {
		super(file);
		
		this.type = type;
	}
	
	@Override
	protected InputStream openFile(String name) throws IOException {
		if (name.equals("pack.mcmeta")) {
			return IOUtils
				.toInputStream(
						String
							.format("{\"pack\":{\"pack_format\":"
									+ type
										.getPackVersion(SharedConstants
											.getGameVersion())
									+ ",\"description\":\"\"}}"),
						StandardCharsets.UTF_8);
		}
		
		return super.openFile(name);
	}
	
}
