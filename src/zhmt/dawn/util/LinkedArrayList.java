package zhmt.dawn.util;

/**
 * It is more cpu-cache-friedly than SimpleLinkedList.
 * 
 * @author zhmt
 * @createdate 2015年7月3日 上午6:54:09
 * @param <T>
 */
class LinkedArrayList<T> {
	private SimpleLinkedList<Object[]> arrList = new SimpleLinkedList<>();
	private Object[] head;
	private int ri;
	private Object[] tail;
	private int wi;
	private int arrLen;
	private int size;

	public LinkedArrayList() {
		head = ObjectArrayPool.tls.get().getOne();
		tail = head;
		ri = 0;
		wi = 0;
		size = 0;
		arrLen = head.length;
		arrList.add(head);
	}

	public void add(T t) {
		tail[wi] = t;
		wi++;
		size++;
		if (wi >= arrLen) {
			wi = 0;
			tail = ObjectArrayPool.tls.get().getOne();
			arrList.add(tail);
		}
	}

	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	public T getFirst() {
		return (T) head[ri];
	}

	@SuppressWarnings("unchecked")
	public T removeFirst() {
		T ret = (T) head[ri];
		if (ret == null) {
			return null;
		}

		size--;
		head[ri] = null;
		ri++;
		if (ri >= arrLen) {
			ri = 0;
			ObjectArrayPool.tls.get().returnOne(head);
			arrList.removeFirst();
			head = arrList.getFirst();
		}

		return ret;
	}

	@Override
	protected void finalize() throws Throwable {
		while (size > 0) {
			removeFirst();
		}
	}
}
