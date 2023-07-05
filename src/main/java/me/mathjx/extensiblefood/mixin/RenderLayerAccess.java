package me.mathjx.extensiblefood.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;

@Environment(EnvType.CLIENT)
@Mixin(RenderLayers.class)
public interface RenderLayerAccess {

	@Accessor("BLOCKS")
	public static Map<Block, RenderLayer> getBlocksMappedLayers() {
		throw new AssertionError(); // should never happen
	}

}
