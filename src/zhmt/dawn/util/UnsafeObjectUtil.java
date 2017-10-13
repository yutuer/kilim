package zhmt.dawn.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sun.misc.Unsafe;

public class UnsafeObjectUtil {

	public final static int objHeadSize;
	public final static int arrayHeadSize;
	public final static int arrayLengthOffset;
	public final static int arrayBaseOffset;

	//jvm layout will do padding to 8bytes*N
	public final static int longSize = 8;
	public final static int doubleSize = 8;
	public final static int floatSize = 4;
	public final static int intSize = 4;
	public final static int shortSize = 2;
	public final static int charSize = 2;
	public final static int boolSize = 1;
	public final static int byteSize = 1;
	public final static int refSize;

	public final static int addressSize;

	private final static ObjAddr objAddrUtil;

	static {
		addressSize = Unsafe.ADDRESS_SIZE;
		objHeadSize = objFieldOffset(OneField.class, "a");
		refSize = Math.abs(objFieldOffset(ObjField.class, "b")
				- objFieldOffset(ObjField.class, "a"));
		arrayBaseOffset = Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;
		arrayHeadSize = arrayBaseOffset;
		arrayLengthOffset = arrayBaseOffset - 4;
		if (addressSize == 4)
			objAddrUtil = new ObjAddrI32();
		else if (addressSize == 8 && refSize == 8)
			objAddrUtil = new ObjAddrI64();
		else
			objAddrUtil = new ObjAddrI64CompressOops();
	}

	private static int objFieldOffset(Class cls, String field) {
		try {
			return (int) UnsafeUtil.unsafe.objectFieldOffset(cls
					.getDeclaredField(field));
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private static int objFieldOffset(Field field) {
		try {
			return (int) UnsafeUtil.unsafe.objectFieldOffset(field);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private static int getFieldTypeSize(Field f) {
		if (f.getType().isArray() || !f.getType().isPrimitive()) {
			return refSize;
		}

		return getTypeSize(f.getType());
	}

	private static int getTypeSize(Class cls) {
		if (cls == long.class || cls == double.class) {
			return 8;
		}
		if (cls == int.class || cls == float.class) {
			return 4;
		}
		if (cls == char.class || cls == short.class) {
			return 2;
		}
		if (cls == byte.class || cls == boolean.class) {
			return 1;
		}
		throw new RuntimeException(cls + " is not a primitive type.");
	}

	private static int objFieldEndOffset(Field field) {
		try {
			return (int) UnsafeUtil.unsafe.objectFieldOffset(field)
					+ getFieldTypeSize(field);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	//http://www.importnew.com/1305.html
	//TODO padding \ if cls is array
	public static int objectShadowSize(Class cls) {
		int max = 0;
		for (Field f : cls.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			max = Math.max(max, objFieldEndOffset(f));
		}
		return max;
	}

	private static class FieldMemoryInfo {
		String name;
		int offset;
		int size;

		@Override
		public String toString() {
			return "FieldMemoryInfo [" + "offset=" + offset + ", \tsize="
					+ size + ",\tname=" + name + "]";
		}

		public void print() {
			for (int i = 0; i < size; i++) {
				System.out.println(toString());
			}
		}
	}

	public static void printClsObjMemLayout(Class cls) {
		List<FieldMemoryInfo> ret = new ArrayList<>();
		getClsObjMemLayout(cls, ret);
		Collections.sort(ret, new Comparator<FieldMemoryInfo>() {
			@Override
			public int compare(FieldMemoryInfo o1, FieldMemoryInfo o2) {
				return o1.offset - o2.offset;
			}
		});

		FieldMemoryInfo last = null;
		for (FieldMemoryInfo info : ret) {
			if (last != null && last.offset + last.size != info.offset) {
				FieldMemoryInfo tmp = new FieldMemoryInfo();
				tmp.offset = last.offset + last.size;
				tmp.size = info.offset - tmp.offset;
				tmp.name = "gap";
				tmp.print();
			}
			last = info;
			info.print();
		}
	}

	private static void getClsObjMemLayout(Class cls, List<FieldMemoryInfo> ret) {
		if (cls.getSuperclass() != null) {
			getClsObjMemLayout(cls.getSuperclass(), ret);
		}

		for (Field f : cls.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			FieldMemoryInfo one = new FieldMemoryInfo();
			one.offset = objFieldOffset(f);
			one.name = cls.getName() + "." + f.getName();
			one.size = getFieldTypeSize(f);
			ret.add(one);
		}
	}

	public static Object fromAddress(long address) {
		return objAddrUtil.fromAddr(address);
	}

	public static long addressOf(Object o) {
		return objAddrUtil.addressOf(o);
	}

	private static interface ObjAddr {
		public long addressOf(Object obj);

		public Object fromAddr(long addr);
	}

	private static class ObjAddrI32 implements ObjAddr {
		@Override
		public long addressOf(Object obj) {
			Object helperArray[] = new Object[1];
			helperArray[0] = obj;
			long addr = UnsafeUtil.unsafe.getInt(helperArray,
					(long) arrayBaseOffset) & 0xFFFFFFFFL;
			return addr;
		}

		@Override
		public Object fromAddr(long addr) {
			Object[] array = new Object[] { null };
			UnsafeUtil.unsafe.putInt(array, (long) arrayBaseOffset,
					(int) (addr));
			return array[0];
		}
	}

	private static class ObjAddrI64 implements ObjAddr {
		@Override
		public long addressOf(Object obj) {
			System.out.println("aa");
			Object helperArray[] = new Object[1];
			helperArray[0] = obj;
			long addr = UnsafeUtil.unsafe.getLong(helperArray,
					(long) arrayBaseOffset);
			return addr;
		}

		@Override
		public Object fromAddr(long addr) {
			System.out.println("bb");
			Object[] array = new Object[] { null };
			UnsafeUtil.unsafe.putLong(array, (long) arrayBaseOffset, addr);
			return array[0];
		}
	}

	private static class ObjAddrI64CompressOops implements ObjAddr {
		@Override
		public long addressOf(Object obj) {
			Object helperArray[] = new Object[1];
			helperArray[0] = obj;
			long addr = UnsafeUtil.unsafe.getInt(helperArray,
					(long) arrayBaseOffset) & 0xFFFFFFFFL;
			addr = addr << 3;
			return addr;
		}

		@Override
		public Object fromAddr(long addr) {
			Object[] array = new Object[] { null };
			UnsafeUtil.unsafe.putInt(array, (long) arrayBaseOffset,
					(int) (addr >> 3));
			return array[0];
		}
	}

	private static final ObjAddr objAddr = null;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("addressSize\t\t" + addressSize);
		System.out.println("refSize\t\t" + refSize);

		System.out.println("objHeadSize\t\t" + objHeadSize);

		System.out.println("arrayHeadSize\t\t" + arrayHeadSize);
		System.out.println("arrayLengthOffset\t\t" + arrayLengthOffset);
		System.out.println("arrayBaseOffset\t\t" + arrayBaseOffset);

		//		printClsObjMemLayout( S1.class);

		//		long a = UnsafeUtil.unsafe.allocateMemory(100);

	}

	public static void printBytes(long objectAddress, int num) {
		for (long i = 0; i < num; i++) {
			int cur = UnsafeUtil.unsafe.getByte(objectAddress + i);
			System.out.print((char) cur);
		}
		System.out.println();
	}

}

class OneField {
	int a;
}

class ObjField {
	String a;
	String b;
}

//=====================test
class S1 {
	byte d;

	@Override
	protected void finalize() throws Throwable {
		System.out.println("gc S1");
	}
}

class S2 extends S1 {
	byte b;
	//	int c;
}

class Test extends S2 {
	int a;
	byte e;
	short f;
	char g;
	long h;
	String i;

	class Test2 {

	}
}
