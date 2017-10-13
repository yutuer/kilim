package zhmt.dawn.util;

public class SimpleLinkedList<E> {
	protected Node<E> head;
	protected Node<E> tail;
	private int _size = 0;
	private NodePool nodePool;
	
	@SuppressWarnings("unchecked")
	public SimpleLinkedList() {
		if(nodePool==null)
			nodePool = NodePool.tls.get();
		head = nodePool.getOne();
		tail = head;
	}

	public void remove(E e) {
		Node<E> idx = head.next;
		while (idx != null) {
			if (idx.item.equals(e)) {
				removeNode(idx);
				return;
			}
			idx = head.next;
		}
	}

	public int size() {
		return _size;
	}

	@SuppressWarnings("unchecked")
	public void add(E e) {
		Node<E> tmp = nodePool.getOne();
		tmp.item = e;
		tail.next = tmp;
		tmp.prev = tail;
		tail = tmp;
		_size++;
	}

	public E getFirst() {
		if (tail == head) {
			return null;
		}

		return head.next.item;
	}

	public E removeFirst() {
		if (tail == head) {
			return null;
		}

		Node<E> node = head.next;
		E ret = node.item;
		removeNode(node);
		return ret;
	}

	private void removeNode(Node<E> node) {
		node.prev.next = node.next;
		if (node.next != null) {
			node.next.prev = node.prev;
		}
		if (tail == node) {
			tail = node.prev;
		}
		nodePool.returnOne(node);
		_size--;
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
