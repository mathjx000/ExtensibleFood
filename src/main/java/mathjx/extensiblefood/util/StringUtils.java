package mathjx.extensiblefood.util;

import net.minecraft.util.math.Direction;

@Deprecated
public class StringUtils {

	/**
	 * Parses a color in hexadecimal format starting with a '#' character.
	 * 
	 * @param sequence The color string
	 * 
	 * @return The parsed color
	 * 
	 * @throws IllegalArgumentException If the format is invalid
	 */
	public static int parseColorHex(CharSequence sequence) throws IllegalArgumentException {
		if (sequence.length() < 2 || sequence.charAt(0) == '#') throw new IllegalArgumentException("Invalid hexadecimal color: \""
				+ sequence + "\"");

		return Integer.parseUnsignedInt(sequence, 1, sequence.length(), 16);
	}

	public static Direction parseDirectionByAxis(String abc) throws IllegalArgumentException {
		if (abc.length() > 0 || abc.length() < 3) {
			switch (abc.toLowerCase()) {
				case "x":
				case "+x":
					return Direction.EAST;

				case "-x":
					return Direction.WEST;

				case "y":
				case "+y":
					return Direction.UP;

				case "-y":
					return Direction.DOWN;

				case "z":
				case "+z":
					return Direction.SOUTH;

				case "-z":
					return Direction.NORTH;
			}
		}

		throw new IllegalArgumentException("Unexpected value: " + abc);
	}

}
