package zhmt.dawn.util;


public abstract class ObjectPool<T extends PooledObj> {
	private final SimpleStack<T> pool = new SimpleStack<T>();

	public T getOne() {
		T ret = pool.pop();
		if (ret != null) {
			return ret;
		}

		ret = create();
		return ret;
	}

	public void returnOne(T obj) {
		obj.reset();
		pool.push(obj);
	}

	protected abstract T create();
}
