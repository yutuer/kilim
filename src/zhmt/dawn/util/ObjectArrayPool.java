package zhmt.dawn.util;

class ObjectArrayPool {
	public static final int ARRAYSIZE = 5000;
	public static final TlsInstance<ObjectArrayPool> tls = new TlsInstance<ObjectArrayPool>(
			ObjectArrayPool.class);

	private final SimpleStack<Object[]> pool = new SimpleStack<Object[]>();

	public Object[] getOne() {
		Object[] ret = pool.pop();
		if (ret != null) {
			return ret;
		}

		ret = new Object[ARRAYSIZE];
		return ret;
	}

	public void returnOne(Object[] obj) {
		pool.push(obj);
	}
}
