package mathjx.extensiblefood.item;

import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * This class is a generic item model predicate provider for consumption
 * progress of food items.
 * 
 * It provides a value from <code>0f</code> to <code>1f</code>.
 * 
 * This class act as a singleton since no specific data needs to be specified.
 * Only the {@link Identifier} key may change.
 */
public final class FoodConsumptionProgressModelPredicate implements UnclampedModelPredicateProvider {

	public static final FoodConsumptionProgressModelPredicate INSTANCE = new FoodConsumptionProgressModelPredicate();

	private FoodConsumptionProgressModelPredicate() {}

	@Override
	public float unclampedCall(ItemStack stack, ClientWorld world, /* @Nullable */ LivingEntity entity, int seed) {
		// Ensure this is the item being used
		if (entity != null && entity.isUsingItem() && entity.getActiveItem() == stack) {
			return 1f - ((float) entity.getItemUseTimeLeft() / (float) stack.getMaxUseTime());
		} else return 0f;
	}

}
