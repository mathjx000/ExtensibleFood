package mathjx.extensiblefood.item;

import java.util.List;

import mathjx.extensiblefood.food.ExtendedFoodComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

	public ExtensibleFoodItem(final Settings settings, final Text name, final Text description, final UseAction action,
			final boolean glint, final Item foodRemainder, final ExtendedFoodComponent foodComponent) {
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
	public int getMaxUseTime(final ItemStack stack) {
		return foodComponent.eatTime == null ? super.getMaxUseTime(stack) : foodComponent.eatTime;
	}

	@Override
	public UseAction getUseAction(final ItemStack stack) {
		return action;
	}

	@Override
	public boolean hasGlint(final ItemStack stack) {
		return glint;
	}

	@Override
	public ItemStack finishUsing(final ItemStack stack, final World world, final LivingEntity user) {
		final ItemStack remaining = super.finishUsing(stack, world, user);
		final boolean p = user instanceof PlayerEntity;

		if ((!p || !((PlayerEntity) user).abilities.creativeMode) && foodRemainder != null) {
			if (stack.isEmpty()) return new ItemStack(foodRemainder, 1);
			else if (p) {
				remaining.increment(1);
				return ItemUsage.method_30012(remaining, (PlayerEntity) user, new ItemStack(foodRemainder, 1));
			} else user.dropStack(new ItemStack(foodRemainder, 1));
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

}
