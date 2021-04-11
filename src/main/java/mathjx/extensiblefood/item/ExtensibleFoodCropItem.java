package mathjx.extensiblefood.item;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class ExtensibleFoodCropItem extends AliasedBlockItem {

	private final Text name, description;
	private final boolean glint;

	public ExtensibleFoodCropItem(final Block block, final Settings settings, final Text name, final Text description,
			final boolean glint) {
		super(block, settings);

		this.name = name;
		this.description = description;
		this.glint = glint;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Text getName() {
		return name;
	}

	@Override
	public Text getName(final ItemStack stack) {
		return name;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(final ItemStack stack, final World world, final List<Text> tooltip,
			final TooltipContext context) {
		if (description != null) tooltip.add(description);

		super.appendTooltip(stack, world, tooltip, context);
	}

	@Override
	public boolean hasGlint(final ItemStack stack) {
		return glint;
	}

}
