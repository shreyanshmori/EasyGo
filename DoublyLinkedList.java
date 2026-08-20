package datastructures.linkedlist;

/**
 * Generic Doubly Linked List — custom implementation.
 * Used in Railway System for Previous/Next date navigation.
 * Supports: addFirst, addLast, addAt, removeFirst, removeLast,
 *           removeAt, get, traverseForward, traverseBackward, reverse.
 */
public class DoublyLinkedList<T> {

    // ── Node ──────────────────────────────────────────────────────────────────
    public static class Node<T> {
        public T data;
        public Node<T> prev;
        public Node<T> next;

        public Node(T data) {
            this.data = data;
            this.prev = null;
            this.next = null;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public DoublyLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    // ── Add at head ───────────────────────────────────────────────────────────
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        size++;
    }

    // ── Add at tail ───────────────────────────────────────────────────────────
    public void addLast(T data) {
        Node<T> newNode = new Node<>(data);
        if (tail == null) {
            head = tail = newNode;
        } else {
            newNode.prev = tail;
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    // ── Add at index ──────────────────────────────────────────────────────────
    public void addAt(int index, T data) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        if (index == 0)    { addFirst(data); return; }
        if (index == size) { addLast(data);  return; }

        Node<T> newNode = new Node<>(data);
        Node<T> curr = getNode(index);
        Node<T> prevNode = curr.prev;

        newNode.next = curr;
        newNode.prev = prevNode;
        prevNode.next = newNode;
        curr.prev = newNode;
        size++;
    }

    // ── Remove head ───────────────────────────────────────────────────────────
    public T removeFirst() {
        if (head == null) throw new RuntimeException("List is empty");
        T data = head.data;
        if (head == tail) { head = tail = null; }
        else {
            head = head.next;
            head.prev = null;
        }
        size--;
        return data;
    }

    // ── Remove tail ───────────────────────────────────────────────────────────
    public T removeLast() {
        if (tail == null) throw new RuntimeException("List is empty");
        T data = tail.data;
        if (head == tail) { head = tail = null; }
        else {
            tail = tail.prev;
            tail.next = null;
        }
        size--;
        return data;
    }

    // ── Remove by index ───────────────────────────────────────────────────────
    public T removeAt(int index) {
        checkIndex(index);
        if (index == 0)        return removeFirst();
        if (index == size - 1) return removeLast();

        Node<T> curr = getNode(index);
        curr.prev.next = curr.next;
        curr.next.prev = curr.prev;
        size--;
        return curr.data;
    }

    // ── Remove by value ───────────────────────────────────────────────────────
    public boolean remove(T data) {
        Node<T> curr = head;
        while (curr != null) {
            if (curr.data.equals(data)) {
                if (curr == head) { removeFirst(); return true; }
                if (curr == tail) { removeLast();  return true; }
                curr.prev.next = curr.next;
                curr.next.prev = curr.prev;
                size--;
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    // ── Get by index ──────────────────────────────────────────────────────────
    public T get(int index) {
        checkIndex(index);
        return getNode(index).data;
    }

    // ── Peek head / tail ──────────────────────────────────────────────────────
    public T peekFirst() {
        if (head == null) throw new RuntimeException("List is empty");
        return head.data;
    }

    public T peekLast() {
        if (tail == null) throw new RuntimeException("List is empty");
        return tail.data;
    }

    // ── Navigation helpers (for date navigation in Railway) ───────────────────
    public Node<T> getHead() { return head; }
    public Node<T> getTail() { return tail; }

    /** Move one step forward from a given node. */
    public Node<T> next(Node<T> node) {
        if (node == null) throw new RuntimeException("Node is null");
        return node.next;
    }

    /** Move one step backward from a given node. */
    public Node<T> previous(Node<T> node) {
        if (node == null) throw new RuntimeException("Node is null");
        return node.prev;
    }

    // ── Reverse in-place ──────────────────────────────────────────────────────
    public void reverse() {
        Node<T> curr = head;
        Node<T> temp = null;
        while (curr != null) {
            temp = curr.prev;
            curr.prev = curr.next;
            curr.next = temp;
            curr = curr.prev;          // move forward (was next)
        }
        if (temp != null) {
            head = temp.prev;
        }
        // swap head and tail
        temp = head;
        head = tail;
        tail = temp;
    }

    // ── Traverse ──────────────────────────────────────────────────────────────
    public void traverseForward() {
        Node<T> curr = head;
        System.out.print("HEAD <-> ");
        while (curr != null) {
            System.out.print(curr.data);
            if (curr.next != null) System.out.print(" <-> ");
            curr = curr.next;
        }
        System.out.println(" <-> TAIL");
    }

    public void traverseBackward() {
        Node<T> curr = tail;
        System.out.print("TAIL <-> ");
        while (curr != null) {
            System.out.print(curr.data);
            if (curr.prev != null) System.out.print(" <-> ");
            curr = curr.prev;
        }
        System.out.println(" <-> HEAD");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()       { return size; }
    public boolean isEmpty() { return size == 0; }

    public boolean contains(T data) {
        Node<T> curr = head;
        while (curr != null) {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        }
        return false;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────
    private Node<T> getNode(int index) {
        Node<T> curr;
        if (index < size / 2) {
            curr = head;
            for (int i = 0; i < index; i++) curr = curr.next;
        } else {
            curr = tail;
            for (int i = size - 1; i > index; i--) curr = curr.prev;
        }
        return curr;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
}