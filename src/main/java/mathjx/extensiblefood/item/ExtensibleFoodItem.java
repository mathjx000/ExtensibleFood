package mathjx.extensiblefood.item;

import java.util.List;

import mathjx.extensiblefood.food.ExtendedFoodComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ComposterBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public final class ExtensibleFoodItem extends Item {

	private final Text name, description;
	private final UseAction action;
	private final boolean glint;
	private final Item foodRemainder;

	/*
	 * Item food properties
	 * 
	 * Used for eat time and sound
	 */
	private final ExtendedFoodComponent foodComponent;

	public ExtensibleFoodItem(Settings settings, Text name, Text description, UseAction action, boolean glint,
			Item foodRemainder, ExtendedFoodComponent foodComponent) {
		super(settings);

		this.name = name;
		this.description = description;
		this.action = action;
		this.glint = glint;
		this.foodRemainder = foodRemainder;
		this.foodComponent = foodComponent;
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
	public int getMaxUseTime(ItemStack stack) {
		return foodComponent.eatTime == null ? super.getMaxUseTime(stack) : foodComponent.eatTime;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return this.action;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return this.glint;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack remaining = super.finishUsing(stack, world, user);
		boolean p = user instanceof PlayerEntity;

		if (p && ((PlayerEntity) user).abilities.creativeMode) {
			return remaining;
		} else if (this.foodRemainder != null) {
			if (stack.isEmpty()) {
				return new ItemStack(this.foodRemainder, 1);
			} else if (p) {
				remaining.increment(1);
				return ItemUsage.method_30012(remaining, (PlayerEntity) user, new ItemStack(this.foodRemainder, 1));
			} else user.dropStack(new ItemStack(this.foodRemainder, 1));
		}

		return remaining;
	}

	@Override
	public SoundEvent getDrinkSound() {
		return foodComponent.eatSound == null ? super.getDrinkSound() : foodComponent.eatSound;
	}

	@Override
	public SoundEvent getEatSound() {
		return foodComponent.eatSound == null ? super.getEatSound() : foodComponent.eatSound;
	}

	@Deprecated
	void clear() {
		// cleared = true;

		ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.computeFloatIfPresent(this, (i, f) -> null);

		// TODO clear the item
	}

}
