package datastructures.linkedlist;

/**
 * Generic Circular Singly Linked List — custom implementation.
 * Last node's next points back to head (circular structure).
 * Supports: add, addFirst, remove, removeFirst, get, contains, print.
 */
public class CircularLinkedList<T> {

    // ── Node ──────────────────────────────────────────────────────────────────
    public static class Node<T> {
        public T data;
        public Node<T> next;

        public Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Node<T> head;
    private Node<T> tail;   // keep tail reference for O(1) addLast
    private int size;

    public CircularLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    // ── Add at tail ───────────────────────────────────────────────────────────
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
            newNode.next = head;        // points to itself
        } else {
            tail.next = newNode;
            tail = newNode;
            tail.next = head;           // maintain circular link
        }
        size++;
    }

    // ── Add at head ───────────────────────────────────────────────────────────
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
            newNode.next = head;
        } else {
            newNode.next = head;
            head = newNode;
            tail.next = head;           // update tail's link to new head
        }
        size++;
    }

    // ── Remove head ───────────────────────────────────────────────────────────
    public T removeFirst() {
        if (head == null) throw new RuntimeException("List is empty");
        T data = head.data;
        if (head == tail) {             // only one node
            head = null;
            tail = null;
        } else {
            head = head.next;
            tail.next = head;
        }
        size--;
        return data;
    }

    // ── Remove by value (first occurrence) ───────────────────────────────────
    public boolean remove(T data) {
        if (head == null) return false;

        if (head.data.equals(data)) {
            removeFirst();
            return true;
        }

        Node<T> curr = head;
        do {
            if (curr.next != null && curr.next.data.equals(data)) {
                if (curr.next == tail) tail = curr;
                curr.next = curr.next.next;
                size--;
                return true;
            }
            curr = curr.next;
        } while (curr != head);

        return false;
    }

    // ── Get by index ──────────────────────────────────────────────────────────
    public T get(int index) {
        checkIndex(index);
        Node<T> curr = head;
        for (int i = 0; i < index; i++) curr = curr.next;
        return curr.data;
    }

    // ── Contains ──────────────────────────────────────────────────────────────
    public boolean contains(T data) {
        if (head == null) return false;
        Node<T> curr = head;
        do {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        } while (curr != head);
        return false;
    }

    // ── Rotate: move head to tail (one step forward) ─────────────────────────
    public void rotate() {
        if (size <= 1) return;
        tail = head;
        head = head.next;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }
    public Node<T> getHead() { return head; }
    public Node<T> getTail() { return tail; }

    // ── Print (stops after full cycle) ────────────────────────────────────────
    public void print() {
        if (head == null) { System.out.println("[]"); return; }
        Node<T> curr = head;
        System.out.print("[");
        do {
            System.out.print(curr.data);
            curr = curr.next;
            if (curr != head) System.out.print(" -> ");
        } while (curr != head);
        System.out.println(" -> (head)]");
    }

    // ── Internal helper ───────────────────────────────────────────────────────
    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
}