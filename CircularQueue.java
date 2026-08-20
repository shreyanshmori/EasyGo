package datastructures.queue;

public class CircularQueue<T> {

    // ── Fields ────────────────────────────────────────────────────────────────
    private final Object[] buffer;
    private int front;
    private int rear;
    private int size;
    private final int capacity;

    public CircularQueue(int capacity) {
        this.capacity = capacity;
        this.buffer   = new Object[capacity];
        this.front    = 0;
        this.rear     = 0;
        this.size     = 0;
    }

    // ── Enqueue ───────────────────────────────────────────────────────────────
    public void enqueue(T data) {
        if (isFull()) throw new RuntimeException("Circular Queue is full");
        buffer[rear] = data;
        rear = (rear + 1) % capacity;
        size++;
    }

    // ── Dequeue ───────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) throw new RuntimeException("Circular Queue is empty");
        T data = (T) buffer[front];
        buffer[front] = null;
        front = (front + 1) % capacity;
        size--;
        return data;
    }

    // ── Peek ──────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new RuntimeException("Circular Queue is empty");
        return (T) buffer[front];
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public boolean isFull()  { return size == capacity; }
    public boolean isEmpty() { return size == 0; }
    public int size()        { return size; }
    public int capacity()    { return capacity; }

    @SuppressWarnings("unchecked")
    public void print() {
        if (isEmpty()) { System.out.println("CircularQueue: []"); return; }
        System.out.print("CircularQueue: [");
        int idx = front;
        for (int i = 0; i < size; i++) {
            System.out.print(buffer[idx]);
            if (i < size - 1) System.out.print(", ");
            idx = (idx + 1) % capacity;
        }
        System.out.println("]  (front=" + front + ", rear=" + rear + ")");
    }
}