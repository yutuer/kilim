package zhmt.dawn.util;

public abstract class TlsObjPool<T extends PooledObj> {
	private TlsInstance<ObjectPool<T>> pool = new TlsInstance<ObjectPool<T>>() {
		@Override
		protected ObjectPool<T> create() {
			return new ObjectPool<T>() {
				@Override
				protected T create() {
					return TlsObjPool.this.create();
				}
			};
		}
	};

	public ObjectPool<T> getPool() {
		return pool.get();
	}

	public T getOne() {
		T ret = pool.get().getOne();
		return ret;
	}

	public void returnOne(T obj) {
		pool.get().returnOne(obj);
	}

	protected abstract T create();
}
