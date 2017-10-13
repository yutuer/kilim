package zhmt.dawn.util;

public class TlsInstance<T> {
	private ThreadLocal<T> instance = new ThreadLocal<>();
	private Class<T> cls;

	public TlsInstance() {

	}

	@SuppressWarnings("rawtypes")
	public TlsInstance(Class cls) {
		this.cls = cls;
	}

	public T get() {
		T ret = instance.get();
		if (ret != null) {
			return ret;
		}
		ret = create();
		instance.set(ret);
		return ret;
	}

	protected T create() {
		try {
			return cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
