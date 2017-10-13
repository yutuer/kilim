package zhmt.dawn;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;

import zhmt.dawn.util.ObjectPool;
import zhmt.dawn.util.PooledObj;

/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * A red-black tree based set. Elements is unique, but Comparable values need
 * not to be unique.
 * 
 * <p>
 * method compareTo is just used to sort elements. element.equals is used to
 * define obj1 equals obj2.
 * 
 * <p>
 * A Red-Black tree based {@link NavigableMap} implementation. The map is sorted
 * according to the {@linkplain Comparable natural ordering} of its keys, or by
 * a {@link Comparator} provided at map creation time, depending on which
 * constructor is used.
 *
 * <p>
 * This implementation provides guaranteed log(n) time cost for the
 * {@code containsKey}, {@code get}, {@code put} and {@code remove} operations.
 * Algorithms are adaptations of those in Cormen, Leiserson, and Rivest's
 * <em>Introduction to Algorithms</em>.
 *
 * <p>
 * Note that the ordering maintained by a tree map, like any sorted map, and
 * whether or not an explicit comparator is provided, must be <em>consistent
 * with {@code equals}</em> if this sorted map is to correctly implement the
 * {@code Map} interface. (See {@code Comparable} or {@code Comparator} for a
 * precise definition of <em>consistent with equals</em>.) This is so because
 * the {@code Map} interface is defined in terms of the {@code equals}
 * operation, but a sorted map performs all key comparisons using its
 * {@code compareTo} (or {@code compare}) method, so two keys that are deemed
 * equal by this method are, from the standpoint of the sorted map, equal. The
 * behavior of a sorted map <em>is</em> well-defined even if its ordering is
 * inconsistent with {@code equals}; it just fails to obey the general contract
 * of the {@code Map} interface.
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a map concurrently, and at least one of the threads
 * modifies the map structurally, it <em>must</em> be synchronized externally.
 * (A structural modification is any operation that adds or deletes one or more
 * mappings; merely changing the value associated with an existing key is not a
 * structural modification.) This is typically accomplished by synchronizing on
 * some object that naturally encapsulates the map. If no such object exists,
 * the map should be "wrapped" using the
 * {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap}
 * method. This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:
 * 
 * <pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));
 * </pre>
 *
 * <p>
 * The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's "collection view methods" are
 * <em>fail-fast</em>: if the map is structurally modified at any time after the
 * iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a
 * {@link ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
 * is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators throw
 * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
 * would be wrong to write a program that depended on this exception for its
 * correctness: <em>the fail-fast behavior of iterators
 * should be used only to detect bugs.</em>
 *
 * <p>
 * All {@code Entry} pairs returned by methods in this class and its views
 * represent snapshots of mappings at the time they were produced. They do
 * <strong>not</strong> support the {@code Entry.setValue} method. (Note however
 * that it is possible to change mappings in the associated map using
 * {@code put}.)
 *
 * <p>
 * This class is a member of the <a href="{@docRoot}
 * /../technotes/guides/collections/index.html"> Java Collections Framework</a>.
 *
 * @param <K>
 *            the type of keys maintained by this map
 * @param <V>
 *            the type of mapped values
 *
 * @author Josh Bloch and Doug Lea
 * @see Map
 * @see HashMap
 * @see Hashtable
 * @see Comparable
 * @see Comparator
 * @see Collection
 * @since 1.2
 */

public class BrTreeSet<K> {
	/**
	 * The comparator used to maintain order in this tree map, or null if it
	 * uses the natural ordering of its keys.
	 *
	 * @serial
	 */
	private final Comparator<? super K> comparator;

	private transient Entry<K> root = null;
	private final HashMap<K, Entry<K>> map = new HashMap<>();

	/**
	 * The number of entries in the tree
	 */
	private transient int size = 0;

	/**
	 * The number of structural modifications to the tree.
	 */
	private transient int modCount = 0;

	/**
	 * Constructs a new, empty tree map, using the natural ordering of its keys.
	 * All keys inserted into the map must implement the {@link Comparable}
	 * interface. Furthermore, all such keys must be
	 * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw a
	 * {@code ClassCastException} for any keys {@code k1} and {@code k2} in the
	 * map. If the user attempts to put a key into the map that violates this
	 * constraint (for example, the user attempts to put a string key into a map
	 * whose keys are integers), the {@code put(Object key, Object value)} call
	 * will throw a {@code ClassCastException}.
	 */
	public BrTreeSet() {
		comparator = null;
	}

	/**
	 * Constructs a new, empty tree map, ordered according to the given
	 * comparator. All keys inserted into the map must be <em>mutually
	 * comparable</em> by the given comparator: {@code comparator.compare(k1,
	 * k2)} must not throw a {@code ClassCastException} for any keys {@code k1}
	 * and {@code k2} in the map. If the user attempts to put a key into the map
	 * that violates this constraint, the {@code put(Object
	 * key, Object value)} call will throw a {@code ClassCastException}.
	 *
	 * @param comparator
	 *            the comparator that will be used to order this map. If
	 *            {@code null}, the {@linkplain Comparable natural ordering} of
	 *            the keys will be used.
	 */
	public BrTreeSet(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}

	// Query Operations

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified
	 * key.
	 *
	 * @param key
	 *            key whose presence in this map is to be tested
	 * @return {@code true} if this map contains a mapping for the specified key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared with the keys
	 *             currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null and this map uses natural
	 *             ordering, or its comparator does not permit null keys
	 */
	public boolean contains(K key) {
		return getEntry(key) != null;
	}

	public Comparator<? super K> comparator() {
		return comparator;
	}

	//	/**
	//	 * @throws NoSuchElementException
	//	 *             {@inheritDoc}
	//	 */
	//	public K firstKey() {
	//		Entry<K> e = getFirstEntry();
	//		return e == null ? null : e.key.getFirst();
	//	}

	//	/**
	//	 * @throws NoSuchElementException
	//	 *             {@inheritDoc}
	//	 */
	//	public K lastKey() {
	//		return key(getLastEntry());
	//	}

	/**
	 * Returns this map's entry for the given key, or {@code null} if the map
	 * does not contain an entry for the key.
	 *
	 * @return this map's entry for the given key, or {@code null} if the map
	 *         does not contain an entry for the key
	 * @throws ClassCastException
	 *             if the specified key cannot be compared with the keys
	 *             currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null and this map uses natural
	 *             ordering, or its comparator does not permit null keys
	 */
	final Entry<K> getEntry(Object key) {
		// Offload comparator-based version for sake of performance
		//		if (comparator != null)
		//			return getEntryUsingComparator(key);
		//		if (key == null)
		//			throw new NullPointerException();
		//		Comparable<? super K> k = (Comparable<? super K>) key;
		//		Entry<K> p = root;
		//		while (p != null) {
		//			int cmp = k.compareTo(p.key.getFirst());
		//			if (cmp < 0)
		//				p = p.left;
		//			else if (cmp > 0)
		//				p = p.right;
		//			else {
		//				if (p.key.contains(key))
		//					return p;
		//			}
		//		}
		return map.get(key);
	}


	/**
	 * Associates the specified value with the specified key in this map. If the
	 * map previously contained a mapping for the key, the old value is
	 * replaced.
	 *
	 * @param element
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 *
	 * @return the previous value associated with {@code key}, or {@code null}
	 *         if there was no mapping for {@code key}. (A {@code null} return
	 *         can also indicate that the map previously associated {@code null}
	 *         with {@code key}.)
	 * @throws ClassCastException
	 *             if the specified key cannot be compared with the keys
	 *             currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null and this map uses natural
	 *             ordering, or its comparator does not permit null keys
	 */
	public K add(K element) {
		if (element == null)
			throw new NullPointerException();

		Entry<K> oldEntry = map.get(element);
		if (oldEntry != null) {
			remove(element);
		}

		Entry<K> t = root;
		if (t == null) {
			compare(element, element); // type (and possibly null) check

			root = entryPool.getOne();
			root.key.add(element);
			size = 1;
			modCount++;
			map.put(element, root);
			return null;
		}
		int cmp;
		Entry<K> parent;
		// split comparator and comparable paths
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			do {
				parent = t;
				cmp = cpr.compare(element, t.key.getFirst());
				if (cmp < 0)
					t = t.left;
				else if (cmp > 0)
					t = t.right;
				else {
					t.key.add(element);
					map.put(element, t);
					return element;
				}
			} while (t != null);
		} else {
			Comparable<? super K> k = (Comparable<? super K>) element;
			do {
				parent = t;
				cmp = k.compareTo(t.key.getFirst());
				if (cmp < 0)
					t = t.left;
				else if (cmp > 0)
					t = t.right;
				else {
					t.key.add(element);
					map.put(element, t);
					return element;
				}
			} while (t != null);
		}
		Entry<K> e = entryPool.getOne();
		e.key.add(element);
		e.parent = parent;
		map.put(element, e);
		if (cmp < 0)
			parent.left = e;
		else
			parent.right = e;
		fixAfterInsertion(e);
		size++;
		modCount++;
		return null;
	}

	/**
	 * Removes the mapping for this key from this TreeMap if present.
	 *
	 * @param key
	 *            key for which mapping should be removed
	 * @return the previous value associated with {@code key}, or {@code null}
	 *         if there was no mapping for {@code key}. (A {@code null} return
	 *         can also indicate that the map previously associated {@code null}
	 *         with {@code key}.)
	 * @throws ClassCastException
	 *             if the specified key cannot be compared with the keys
	 *             currently in the map
	 * @throws NullPointerException
	 *             if the specified key is null and this map uses natural
	 *             ordering, or its comparator does not permit null keys
	 */
	public K remove(K key) {
		Entry<K> p = map.remove(key);
		if (p == null)
			return null;

		K oldValue = key;
		if (p.key.size() <= 1)
			deleteEntry(p);
		else
			p.key.remove(key);
		return oldValue;
	}

	/**
	 * Removes all of the mappings from this map. The map will be empty after
	 * this call returns.
	 */
	public void clear() {
		modCount++;
		size = 0;
		root = null;
	}

	// NavigableMap API methods

	/**
	 * @since 1.6
	 */
	public K first() {
		Entry<K> e = getFirstEntry();
		return e == null ? null : e.key.getFirst();
	}

	/**
	 * @since 1.6
	 */
	public K lastEntry() {
		Entry<K> e = getLastEntry();
		return e == null ? null : e.key.getLast();
	}

	/**
	 * @since 1.6
	 */
	public K pollFirst() {
		Entry<K> p = getFirstEntry();
		if (p != null) {
			K ret = p.key.getFirst();

			if (p.key.size() <= 1)
				deleteEntry(p);
			else
				p.key.remove(ret);
			map.remove(ret);

			return ret;
		}
		return null;
	}

	/**
	 * @since 1.6
	 */
	public K pollLast() {
		Entry<K> p = getLastEntry();
		if (p != null) {
			K ret = p.key.removeLast();

			if (p.key.size() <= 1)
				deleteEntry(p);
			else
				p.key.remove(ret);
			map.remove(ret);

			return ret;
		}
		return null;
	}

	// Views

	// Little utilities

	/**
	 * Compares two keys using the correct comparison method for this TreeMap.
	 */
	final int compare(Object k1, Object k2) {
		return comparator == null ? ((Comparable<? super K>) k1)
				.compareTo((K) k2) : comparator.compare((K) k1, (K) k2);
	}

	/**
	 * Test two values for equality. Differs from o1.equals(o2) only in that it
	 * copes with {@code null} o1 properly.
	 */
	static final boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	// Red-black mechanics

	private static final boolean RED = false;
	private static final boolean BLACK = true;

	/**
	 * Node in the Tree. Doubles as a means to pass key-value pairs back to user
	 * (see Entry).
	 */

	static final class Entry<K> implements PooledObj {
		LinkedList<K> key;
		Entry<K> left = null;
		Entry<K> right = null;
		Entry<K> parent;
		boolean color = BLACK;

		/**
		 * Make a new cell with given key, value, and parent, and with
		 * {@code null} child links, and BLACK color.
		 */
		Entry() {
			this.key = new LinkedList<>();
			this.parent = parent;
		}

		/**
		 * Returns the key.
		 *
		 * @return the key
		 */
		public LinkedList<K> getKey() {
			return key;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Entry))
				return false;
			Entry<?> e = (Entry<?>) o;

			return valEquals(key, e.getKey());
		}

		public int hashCode() {
			int keyHash = (key == null ? 0 : key.hashCode());
			return keyHash;
		}

		public String toString() {
			return key.toString();
		}

		@Override
		public void reset() {
			key.clear();

			left = null;
			right = null;
			parent = null;
			color = BLACK;
		}
	}

	/**
	 * Returns the first Entry in the TreeMap (according to the TreeMap's
	 * key-sort function). Returns null if the TreeMap is empty.
	 */
	final Entry<K> getFirstEntry() {
		Entry<K> p = root;
		if (p != null)
			while (p.left != null)
				p = p.left;
		return p;
	}

	/**
	 * Returns the last Entry in the TreeMap (according to the TreeMap's
	 * key-sort function). Returns null if the TreeMap is empty.
	 */
	final Entry<K> getLastEntry() {
		Entry<K> p = root;
		if (p != null)
			while (p.right != null)
				p = p.right;
		return p;
	}

	/**
	 * Returns the successor of the specified Entry, or null if no such.
	 */
	static <K> Entry<K> successor(Entry<K> t) {
		if (t == null)
			return null;
		else if (t.right != null) {
			Entry<K> p = t.right;
			while (p.left != null)
				p = p.left;
			return p;
		} else {
			Entry<K> p = t.parent;
			Entry<K> ch = t;
			while (p != null && ch == p.right) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Returns the predecessor of the specified Entry, or null if no such.
	 */
	static <K, V> Entry<K> predecessor(Entry<K> t) {
		if (t == null)
			return null;
		else if (t.left != null) {
			Entry<K> p = t.left;
			while (p.right != null)
				p = p.right;
			return p;
		} else {
			Entry<K> p = t.parent;
			Entry<K> ch = t;
			while (p != null && ch == p.left) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Balancing operations.
	 *
	 * Implementations of rebalancings during insertion and deletion are
	 * slightly different than the CLR version. Rather than using dummy
	 * nilnodes, we use a set of accessors that deal properly with null. They
	 * are used to avoid messiness surrounding nullness checks in the main
	 * algorithms.
	 */

	private static <K, V> boolean colorOf(Entry<K> p) {
		return (p == null ? BLACK : p.color);
	}

	private static <K, V> Entry<K> parentOf(Entry<K> p) {
		return (p == null ? null : p.parent);
	}

	private static <K, V> void setColor(Entry<K> p, boolean c) {
		if (p != null)
			p.color = c;
	}

	private static <K, V> Entry<K> leftOf(Entry<K> p) {
		return (p == null) ? null : p.left;
	}

	private static <K, V> Entry<K> rightOf(Entry<K> p) {
		return (p == null) ? null : p.right;
	}

	/** From CLR */
	private void rotateLeft(Entry<K> p) {
		if (p != null) {
			Entry<K> r = p.right;
			p.right = r.left;
			if (r.left != null)
				r.left.parent = p;
			r.parent = p.parent;
			if (p.parent == null)
				root = r;
			else if (p.parent.left == p)
				p.parent.left = r;
			else
				p.parent.right = r;
			r.left = p;
			p.parent = r;
		}
	}

	/** From CLR */
	private void rotateRight(Entry<K> p) {
		if (p != null) {
			Entry<K> l = p.left;
			p.left = l.right;
			if (l.right != null)
				l.right.parent = p;
			l.parent = p.parent;
			if (p.parent == null)
				root = l;
			else if (p.parent.right == p)
				p.parent.right = l;
			else
				p.parent.left = l;
			l.right = p;
			p.parent = l;
		}
	}

	/** From CLR */
	private void fixAfterInsertion(Entry<K> x) {
		x.color = RED;

		while (x != null && x != root && x.parent.color == RED) {
			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry<K> y = rightOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == rightOf(parentOf(x))) {
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateRight(parentOf(parentOf(x)));
				}
			} else {
				Entry<K> y = leftOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == leftOf(parentOf(x))) {
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		root.color = BLACK;
	}

	/**
	 * Delete node p, and then rebalance the tree.
	 */
	private void deleteEntry(Entry<K> p) {
		modCount++;
		size--;

		// If strictly internal, copy successor's element to p and then make p
		// point to successor.
		if (p.left != null && p.right != null) {
			Entry<K> s = successor(p);
			LinkedList<K> tmp = p.key;
			p.key = s.key;
			s.key = tmp;
			p = s;
		}

		//		System.out.println("del entry" + p.key);

		// Start fixup at replacement node, if it exists.
		Entry<K> replacement = (p.left != null ? p.left : p.right);

		if (replacement != null) {
			// Link replacement to parent
			replacement.parent = p.parent;
			if (p.parent == null)
				root = replacement;
			else if (p == p.parent.left)
				p.parent.left = replacement;
			else
				p.parent.right = replacement;

			// Null out links so they are OK to use by fixAfterDeletion.
			p.left = p.right = p.parent = null;

			// Fix replacement
			if (p.color == BLACK)
				fixAfterDeletion(replacement);
		} else if (p.parent == null) { // return if we are the only node.
			root = null;
		} else { //  No children. Use self as phantom replacement and unlink.
			if (p.color == BLACK)
				fixAfterDeletion(p);

			if (p.parent != null) {
				if (p == p.parent.left)
					p.parent.left = null;
				else if (p == p.parent.right)
					p.parent.right = null;
				p.parent = null;
			}
		}

		entryPool.returnOne(p);
	}

	/** From CLR */
	private void fixAfterDeletion(Entry<K> x) {
		while (x != root && colorOf(x) == BLACK) {
			if (x == leftOf(parentOf(x))) {
				Entry<K> sib = rightOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if (colorOf(leftOf(sib)) == BLACK
						&& colorOf(rightOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(rightOf(sib)) == BLACK) {
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = root;
				}
			} else { // symmetric
				Entry<K> sib = leftOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if (colorOf(rightOf(sib)) == BLACK
						&& colorOf(leftOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(leftOf(sib)) == BLACK) {
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = root;
				}
			}
		}

		setColor(x, BLACK);
	}

	/**
	 * Find the level down to which to assign all nodes BLACK. This is the last
	 * `full' level of the complete binary tree produced by buildTree. The
	 * remaining nodes are colored RED. (This makes a `nice' set of color
	 * assignments wrt future insertions.) This level number is computed by
	 * finding the number of splits needed to reach the zeroeth node. (The
	 * answer is ~lg(N), but in any case must be computed by same quick O(lg(N))
	 * loop.)
	 */
	private static int computeRedLevel(int sz) {
		int level = 0;
		for (int m = sz - 1; m >= 0; m = m / 2 - 1)
			level++;
		return level;
	}

	private ObjectPool<Entry> entryPool = new ObjectPool<BrTreeSet.Entry>() {
		@Override
		protected Entry create() {
			return new Entry();
		}
	};

	public static void main(String[] args) {
		BrTreeSet<Integer> set = new BrTreeSet<>();
		int N = 100;
		for (int i = 0; i < 100; i++)
			set.add(i);
		for (int i = 0; i < 100; i++)
			set.remove(i);
		for (int i = 0; i < 100; i++) {
			set.add(i);
			set.add(i);
			set.add(i);
			set.add(i);
			set.add(i);
			set.remove(i);

			set.add(i);
			set.remove(i);

			set.add(i);
			set.remove(i);

			set.add(i);
			set.remove(i);
			set.add(i);
		}


		while (set.size() > 0) {
			System.out.println(set.pollFirst());
		}
	}
}
