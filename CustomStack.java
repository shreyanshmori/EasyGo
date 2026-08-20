package datastructures.stack;

/**
 * Generic Stack — custom linked-node implementation (LIFO).
 * Used for: Undo/redo booking operations, cancellation history,
 *           backtracking in route graph DFS.
 * Supports: push, pop, peek, contains, size, print.
 */
public class CustomStack<T> {

    // ── Node ──────────────────────────────────────────────────────────────────
    private static class Node<T> {
        T data;
        Node<T> next;
        Node(T data) { this.data = data; this.next = null; }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Node<T> top;
    private int size;

    public CustomStack() {
        top  = null;
        size = 0;
    }

    // ── Push ──────────────────────────────────────────────────────────────────
    public void push(T data) {
        Node<T> newNode = new Node<>(data);
        newNode.next = top;
        top = newNode;
        size++;
    }

    // ── Pop ───────────────────────────────────────────────────────────────────
    public T pop() {
        if (top == null) throw new RuntimeException("Stack underflow — stack is empty");
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    // ── Peek (top without removal) ────────────────────────────────────────────
    public T peek() {
        if (top == null) throw new RuntimeException("Stack is empty");
        return top.data;
    }

    // ── Contains ──────────────────────────────────────────────────────────────
    public boolean contains(T data) {
        Node<T> curr = top;
        while (curr != null) {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        }
        return false;
    }

    // ── Search (returns 1-based depth from top, -1 if not found) ─────────────
    public int search(T data) {
        Node<T> curr = top;
        int depth = 1;
        while (curr != null) {
            if (curr.data.equals(data)) return depth;
            curr = curr.next;
            depth++;
        }
        return -1;
    }

    // ── Drain to array (top → bottom order) ──────────────────────────────────
    @SuppressWarnings("unchecked")
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node<T> curr = top;
        for (int i = 0; i < size; i++) {
            arr[i] = curr.data;
            curr = curr.next;
        }
        return arr;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    public void print() {
        Node<T> curr = top;
        System.out.println("--- TOP ---");
        while (curr != null) {
            System.out.println("  | " + curr.data + " |");
            curr = curr.next;
        }
        System.out.println("-----------");
    }
}