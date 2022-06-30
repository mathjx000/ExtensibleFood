package mathjx.extensiblefood.mixin;

import java.nio.file.Path;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resource.ResourceType;

/**
 * ඞ
 */
@Mixin(ModNioResourcePack.class)
public interface ModNioResourcePackInvoker {

	@Invoker(value = "<init>")
	public static ModNioResourcePack ModNioResourcePack(final String name, final ModMetadata modInfo, final List<Path> paths,
			final ResourceType type, final AutoCloseable closer, final ResourcePackActivationType activationType) {
		throw new AssertionError();
	}

}
