package zhmt.dawn.util;

/**
 * Cpu cache friedly Queue , to replace SimpleLinkedList
 * @author zhmt
 * @createdate 2015年7月2日 下午10:51:28
 * @param <T>
 */
public class CycleArrayQueue<T> {
	private Object[] sink;
	private int head;
	private int tail;
	private int sinkLen;
	private boolean overflow;
	private int size;
	private SimpleLinkedList<T> ultraQ;
	
//	public static int count = 0;

	public CycleArrayQueue(int size) {
//		count++;
//		System.out.println(count);
		sink = new Object[size];
		head = 0;
		tail = 0;
		size = 0;
		sinkLen = sink.length;
		ultraQ = new SimpleLinkedList<>();
		overflow = false;
	}

	public boolean isEmpty() {
		return head == tail;
	}

	public void add(T node) {
		size++;

		if (overflow) {
			ultraQ.add(node);
			return;
		}

		int nextT = nextTail();
		if (nextT == head) {
			overflow = true;
			ultraQ.add(node);
		} else {
			sink[tail] = node;
			tail = nextT;
		}

	}

	private int nextTail() {
		return (tail + 1) % sinkLen;
	}

	private int nextHead() {
		return (head + 1) % sinkLen;
	}

	public int size() {
		return this.size;
	}

	@SuppressWarnings("unchecked")
	public T getFirst() {
		if (isEmpty()) {
			return null;
		}
		T ret = (T) sink[head];
		return ret;
	}

	@SuppressWarnings("unchecked")
	public T removeFirst() {
		if (isEmpty()) {
			return null;
		}

		size--;
		T ret = (T) sink[head];
		sink[head] = null;
		head = nextHead();
		if (overflow) {
			T tmp = ultraQ.removeFirst();
			int nextT = nextTail();
			sink[tail] = tmp;
			tail = nextT;
			if (ultraQ.size() <= 0) {
				overflow = false;
			}
		}

		return ret;
	}
}
