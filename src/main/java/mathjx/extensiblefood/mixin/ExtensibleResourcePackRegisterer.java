package mathjx.extensiblefood.mixin;

import static mathjx.extensiblefood.ExtensibleFood.MOD_ID;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mathjx.extensiblefood.resourcepack.UserExtensionResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProfile.InsertionPosition;
import net.minecraft.resource.ResourcePackSource;

@Mixin(ModResourcePackCreator.class)
public final class ExtensibleResourcePackRegisterer {

	private static final ResourcePackSource RESOURCE_PACK_SOURCE = ModResourcePackCreator.RESOURCE_PACK_SOURCE;

	@Inject(method = "register", at = @At(value = "RETURN"))
	public void register(final Consumer<ResourcePackProfile> consumer, final ResourcePackProfile.Factory factory,
			final CallbackInfo cb) {
		@SuppressWarnings("resource")
		final UserExtensionResourcePack pack = new UserExtensionResourcePack();
		final ResourcePackProfile profile = ResourcePackProfile.of("fabric/" + MOD_ID
				+ "/user_extension", true, () -> pack, factory, InsertionPosition.TOP, RESOURCE_PACK_SOURCE);

		consumer.accept(profile);
	}

}
