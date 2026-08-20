package datastructures.queue;

public class CustomQueue<T> {

    // ── Node ──────────────────────────────────────────────────────────────────
    private static class Node<T> {
        T data;
        Node<T> next;
        Node(T data) { this.data = data; this.next = null; }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Node<T> front;
    private Node<T> rear;
    private int size;

    public CustomQueue() {
        front = null;
        rear  = null;
        size  = 0;
    }

    // ── Enqueue (add to rear) ─────────────────────────────────────────────────
    public void enqueue(T data) {
        Node<T> newNode = new Node<>(data);
        if (rear == null) {
            front = rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }
        size++;
    }

    // ── Dequeue (remove from front) ───────────────────────────────────────────
    public T dequeue() {
        if (front == null) throw new RuntimeException("Queue is empty");
        T data = front.data;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return data;
    }

    // ── Peek front ────────────────────────────────────────────────────────────
    public T peek() {
        if (front == null) throw new RuntimeException("Queue is empty");
        return front.data;
    }

    // ── Contains ──────────────────────────────────────────────────────────────
    public boolean contains(T data) {
        Node<T> curr = front;
        while (curr != null) {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        }
        return false;
    }

    // ── Remove specific element (cancel from waiting list) ───────────────────
    public boolean removeElement(T data) {
        if (front == null) return false;
        if (front.data.equals(data)) { dequeue(); return true; }
        Node<T> curr = front;
        while (curr.next != null) {
            if (curr.next.data.equals(data)) {
                if (curr.next == rear) rear = curr;
                curr.next = curr.next.next;
                size--;
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    // ── Get position in queue (1-based) ──────────────────────────────────────
    public int getPosition(T data) {
        Node<T> curr = front;
        int pos = 1;
        while (curr != null) {
            if (curr.data.equals(data)) return pos;
            curr = curr.next;
            pos++;
        }
        return -1;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    public void print() {
        Node<T> curr = front;
        System.out.print("FRONT -> ");
        while (curr != null) {
            System.out.print("[" + curr.data + "]");
            if (curr.next != null) System.out.print(" -> ");
            curr = curr.next;
        }
        System.out.println(" <- REAR");
    }
}
