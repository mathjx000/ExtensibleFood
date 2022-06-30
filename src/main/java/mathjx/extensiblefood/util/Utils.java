package mathjx.extensiblefood.util;


public final class Utils {
	
	public static float saturationRatioToHumanReadableSaturationPoints(int hunger, float ratio) {
		return ratio * hunger * 2f;
	}
	
	public static float humanReadableSaturationPointsToSaturationRatio(int hunger, float satPoints) {
		return satPoints / (float) hunger / 2f;
	}
	
}
