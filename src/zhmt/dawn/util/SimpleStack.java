package zhmt.dawn.util;

public class SimpleStack<E> {
	private Node<E> head;
	private NodePool nodePool;

	public SimpleStack() {
		nodePool = NodePool.tls.get();
	}

	@SuppressWarnings("unchecked")
	public void push(E e) {
		Node<E> tmp = nodePool.getOne();
		tmp.item = e;
		tmp.next = head;
		head = tmp;
	}

	public E peekTop() {
		return head == null ? null : head.item;
	}

	public E pop() {
		if (head == null) {
			return null;
		}

		Node<E> node = head;
		head = head.next;
		E ret = node.item;
		nodePool.returnOne(node);
		return ret;
	}

	@Override
	public String toString() {
		String ret = "[";
		Node<E> e = head.next;
		while (e != null) {
			ret += e.item.toString() + ",";
			e = e.next;
		}
		ret += "]";
		return ret;
	}

}
