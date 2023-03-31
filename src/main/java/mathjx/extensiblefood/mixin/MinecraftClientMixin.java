package mathjx.extensiblefood.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mathjx.extensiblefood.util.CustomResourcePack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	
	@ModifyVariable(method = "<init>", ordinal = 0, name = "list",
					at = @At(value = "STORE", ordinal = 0))
	private List<ResourcePack> onInit(List<ResourcePack> original) {
		return CustomResourcePack
			.addTo(original, ResourceType.CLIENT_RESOURCES);
	}
	
	@ModifyVariable(method = "reloadResources(Z)Ljava/util/concurrent/CompletableFuture;",
					ordinal = 0, name = "list",
					at = @At(value = "STORE", ordinal = 0))
	private List<ResourcePack> onReload(List<ResourcePack> original) {
		return CustomResourcePack
			.addTo(original, ResourceType.CLIENT_RESOURCES);
	}
	
}
