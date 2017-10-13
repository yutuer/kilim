package zhmt.dawn.nio.buffer;


public class EzMath {
	public static boolean isEven(int data) {
		return (data & 1) == 0;
	}

	public static boolean isEven(long data) {
		return (data & 1) == 0;
	}

	public static int ceilingToPowerOfTwo(int data) {
		for (int i = 0;; i++) {
			int power = 1 << i;
			if (power >= data) {
				return power;
			}
		}
	}

	/**
	 * Round up to the next highest power of 2.
	 * 
	 * @param data
	 * @return
	 */
	public static long upperPowerOfTwo(long data) {
		if (data <= 0) {
			return 1;
		}

		data--;
		data |= data >> 1;
		data |= data >> 2;
		data |= data >> 4;
		data |= data >> 8;
		data |= data >> 16;

		data++;
		return data;
	}

	/**
	 * Round up to the next highest power of 2.
	 * 
	 * @param data
	 * @return
	 */
	public static int upperPowerOfTwo(int data) {
		if (data <= 0) {
			return 1;
		}

		data--;
		data |= data >> 1;
		data |= data >> 2;
		data |= data >> 4;
		data |= data >> 8;
		data |= data >> 16;

		data++;
		return data;
	}

	private static final int MultiplyDeBruijnBitPosition[] = { 0, 9, 1, 10, 13,
			21, 2, 29, 11, 14, 16, 18, 22, 25, 3, 30, 8, 12, 20, 28, 15, 17, 24, 7,
			19, 27, 23, 6, 26, 5, 4, 31 };

	/**
	 * @param v
	 *          v must be power of 2.
	 * @return
	 */
	public static int log2(int v) {
		int r; // result goes here
		v |= v >> 1; // first round down to one less than a power of 2
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;

		r = MultiplyDeBruijnBitPosition[(((v * 0x07C4ACDD) >> 27) + 32) & 31];
		return r;
	}

	private static final int[] tab64 = { 63, 0, 58, 1, 59, 47, 53, 2, 60, 39, 48,
			27, 54, 33, 42, 3, 61, 51, 37, 40, 49, 18, 28, 20, 55, 30, 34, 11, 43,
			14, 22, 4, 62, 57, 46, 52, 38, 26, 32, 41, 50, 36, 17, 19, 29, 10, 13,
			21, 56, 45, 25, 31, 35, 16, 9, 12, 44, 24, 15, 8, 23, 7, 6, 5 };

	public static int log2(long value) {
		value |= value >> 1;
		value |= value >> 2;
		value |= value >> 4;
		value |= value >> 8;
		value |= value >> 16;
		value |= value >> 32;
		long idx = ((long) ((value - (value >> 1)) * 0x07EDD5E59A4E28C2L)) >> 58;
		return tab64[(int) idx];
	}

	public static boolean isPowerofTwo(int data) {
		return (data & (data - 1)) == 0;
	}

}

