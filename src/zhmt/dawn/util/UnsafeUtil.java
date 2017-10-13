package zhmt.dawn.util;

import java.lang.reflect.Field;
import java.nio.ByteOrder;

public class UnsafeUtil {
	public static final sun.misc.Unsafe unsafe;
	public static final boolean isBigEndian = ByteOrder.nativeOrder().equals(
			ByteOrder.BIG_ENDIAN);

	static {
		sun.misc.Unsafe tmp = null;
		try {
			Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			tmp = (sun.misc.Unsafe) f.get(null);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		unsafe = tmp;
	}

	public static short shortToBigEndian(short data) {
		return data;
	}


//	public static int intToBigEndian(int data) {
//		return data;
//	}


	public static void main(String[] args) {
		System.out.println(0xFF &0xf0);
	}
}
