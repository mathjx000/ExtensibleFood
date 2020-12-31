package mathjx.extensiblefood.food;

import net.minecraft.item.FoodComponent;
import net.minecraft.sound.SoundEvent;

public final class ExtendedFoodComponent {

	public final FoodComponent food;

	public final Integer eatTime;
	public final SoundEvent eatSound;

	ExtendedFoodComponent(FoodComponent foodComponent, Integer eatTime, SoundEvent eatSound) {
		this.food = foodComponent;
		this.eatTime = eatTime;
		this.eatSound = eatSound;
	}

}
