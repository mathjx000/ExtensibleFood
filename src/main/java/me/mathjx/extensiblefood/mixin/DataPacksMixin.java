package me.mathjx.extensiblefood.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.mathjx.extensiblefood.util.CustomResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.SaveLoading;

@Mixin(SaveLoading.DataPacks.class)
public class DataPacksMixin {
	
	@ModifyVariable(method = "load()Lcom/mojang/datafixers/util/Pair;",
					ordinal = 0, name = "list",
					at = @At(value = "STORE", ordinal = 0))
	private List<ResourcePack> onLoad(List<ResourcePack> list) {
		return CustomResourcePack.addTo(list, ResourceType.SERVER_DATA);
	}
	
}
