package zhmt.dawn.util;

import java.util.HashMap;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class TlsClassifiedObjectPool {

	private static final ThreadLocal<HashMap<Class, ObjectPool>> pool = new ThreadLocal<>();

	public static <T extends PooledObj> T getOne(Class<T> cls) {
		return (T) getClassifiedPool(cls).getOne();
	}

	public static <T extends PooledObj> void returnOne(T obj) {
		getClassifiedPool(obj.getClass()).returnOne(obj);
	}

	private static HashMap<Class, ObjectPool> getPool() {
		HashMap<Class, ObjectPool> ret = pool.get();
		if (ret != null) {
			return ret;
		}
		ret = new HashMap<>();
		pool.set(ret);
		return ret;
	}

	private static ObjectPool getClassifiedPool(final Class cls) {
		HashMap<Class, ObjectPool> all = getPool();
		ObjectPool ret = all.get(cls);
		if (ret != null) {
			return ret;
		}

		ret = new ObjectPool() {
			@Override
			protected PooledObj create() {
				try {
					return (PooledObj) cls.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		all.put(cls, ret);
		return ret;
	}
}
