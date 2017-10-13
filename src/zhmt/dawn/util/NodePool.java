package zhmt.dawn.util;

@SuppressWarnings({"rawtypes","unchecked"})
public class NodePool {
	public static final TlsInstance<NodePool> tls = new TlsInstance<NodePool>(
			NodePool.class);

	private Node head;

	public Node getOne() {
		if (head == null) {
			return new Node();
		}
		Node tmp = head;
		head = head.next;
		tmp.next = null;
		return tmp;
	}

	public void returnOne(Node node) {
		node.item = null;
		node.prev = null;
		node.next = head;
		head = node;
	}
}
