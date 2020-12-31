package mathjx.extensiblefood.item;

import java.util.List;

import mathjx.extensiblefood.block.ExtensibleFoodBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class ExtensibleFoodItemBlock extends BlockItem {

	private final Text name, description;
	private final boolean glint;

	public ExtensibleFoodItemBlock(ExtensibleFoodBlock block, Settings settings, Text name, Text description,
			boolean glint) {
		super(block, settings);

		this.name = name;
		this.description = description;
		this.glint = glint;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Text getName() {
		return this.name;
	}

	@Override
	public Text getName(ItemStack stack) {
		return this.name;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		if (this.description != null) tooltip.add(this.description);

		super.appendTooltip(stack, world, tooltip, context);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return this.glint;
	}

}
