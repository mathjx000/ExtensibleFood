package mathjx.extensiblefood.mixin;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mathjx.extensiblefood.ExtensibleFood;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.minecraft.resource.ResourceType;

@Mixin(ModResourcePackUtil.class)
public final class ExtensibleResourcePackRegisterer {

//	private static final ResourcePackSource RESOURCE_PACK_SOURCE = ModResourcePackCreator.RESOURCE_PACK_SOURCE;

	@Inject(method = "appendModResourcePacks", at = @At(value = "RETURN"), remap = false)
	private static void register(List<ModResourcePack> packs, ResourceType type, @Nullable String subPath,
			final CallbackInfo cb) {
		if (subPath == null || subPath == "user_extension") {
			ModResourcePack pack = ModNioResourcePackInvoker.ModNioResourcePack("extension_pack", ExtensibleFood.METADATA, Collections.singletonList(ExtensibleFood.COMMON_RESOURCEPACK_DIR), type, null, ResourcePackActivationType.ALWAYS_ENABLED);

//			UserExtensionResourcePack pack = new UserExtensionResourcePack();

			if (!pack.getNamespaces(type).isEmpty()) {
				packs.add(pack);
			}
		}

//		@SuppressWarnings("resource")
//		final UserExtensionResourcePack pack = new UserExtensionResourcePack();
//		final ResourcePackProfile profile = ResourcePackProfile.of("fabric/" + MOD_ID
//				+ "/user_extension", true, () -> pack, factory, InsertionPosition.TOP, RESOURCE_PACK_SOURCE);
//
//		consumer.accept(profile);
	}

}
