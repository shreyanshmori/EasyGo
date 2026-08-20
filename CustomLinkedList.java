package datastructures.linkedlist;

/**
 * Generic Singly Linked List — custom implementation.
 * Supports: add, addAt, remove, removeAt, get, contains, size, reverse, print.
 */
public class CustomLinkedList<T> {

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
    private int size;

    public CustomLinkedList() {
        head = null;
        size = 0;
    }

    // ── Add at tail ───────────────────────────────────────────────────────────
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> curr = head;
            while (curr.next != null) curr = curr.next;
            curr.next = newNode;
        }
        size++;
    }

    // ── Add at head ───────────────────────────────────────────────────────────
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        newNode.next = head;
        head = newNode;
        size++;
    }

    // ── Add at index ──────────────────────────────────────────────────────────
    public void addAt(int index, T data) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        if (index == 0) { addFirst(data); return; }
        Node<T> newNode = new Node<>(data);
        Node<T> curr = head;
        for (int i = 0; i < index - 1; i++) curr = curr.next;
        newNode.next = curr.next;
        curr.next = newNode;
        size++;
    }

    // ── Get by index ──────────────────────────────────────────────────────────
    public T get(int index) {
        checkIndex(index);
        Node<T> curr = head;
        for (int i = 0; i < index; i++) curr = curr.next;
        return curr.data;
    }

    // ── Remove head ───────────────────────────────────────────────────────────
    public T removeFirst() {
        if (head == null) throw new RuntimeException("List is empty");
        T data = head.data;
        head = head.next;
        size--;
        return data;
    }

    // ── Remove by value (first occurrence) ───────────────────────────────────
    public boolean remove(T data) {
        if (head == null) return false;
        if (head.data.equals(data)) { head = head.next; size--; return true; }
        Node<T> curr = head;
        while (curr.next != null) {
            if (curr.next.data.equals(data)) {
                curr.next = curr.next.next;
                size--;
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    // ── Remove by index ───────────────────────────────────────────────────────
    public T removeAt(int index) {
        checkIndex(index);
        if (index == 0) return removeFirst();
        Node<T> curr = head;
        for (int i = 0; i < index - 1; i++) curr = curr.next;
        T data = curr.next.data;
        curr.next = curr.next.next;
        size--;
        return data;
    }

    // ── Contains ──────────────────────────────────────────────────────────────
    public boolean contains(T data) {
        Node<T> curr = head;
        while (curr != null) {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        }
        return false;
    }

    // ── Reverse in-place ──────────────────────────────────────────────────────
    public void reverse() {
        Node<T> prev = null, curr = head, next;
        while (curr != null) {
            next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        head = prev;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int size()      { return size; }
    public boolean isEmpty() { return size == 0; }
    public Node<T> getHead() { return head; }

    // ── Print ─────────────────────────────────────────────────────────────────
    public void print() {
        Node<T> curr = head;
        System.out.print("[");
        while (curr != null) {
            System.out.print(curr.data);
            if (curr.next != null) System.out.print(" -> ");
            curr = curr.next;
        }
        System.out.println("]");
    }

    // ── Internal helper ───────────────────────────────────────────────────────
    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
}