package datastructures.heap;

import java.util.Comparator;

/**
 * Generic Binary Heap (Min-Heap by default; Max-Heap via comparator).
 * Uses a dynamic array (manual resizing — no ArrayList).
 *
 * Used for:
 *   - Flight dynamic pricing priority queue
 *   - RAC/Waiting list priority (e.g. senior citizens first)
 *   - Dijkstra's shortest-path in route graph
 *
 * Supports: insert, extractMin/Max, peek, heapify, size, print.
 */
public class CustomHeap<T> {

    // ── Fields ────────────────────────────────────────────────────────────────
    private Object[]    heap;
    private int         size;
    private int         capacity;
    private Comparator<T> comparator;

    private static final int DEFAULT_CAPACITY = 16;

    // ── Constructors ──────────────────────────────────────────────────────────
    /** Min-Heap: elements must implement Comparable. */
    public CustomHeap() {
        this(DEFAULT_CAPACITY, null);
    }

    /** Heap with custom comparator (pass reversed comparator for Max-Heap). */
    public CustomHeap(int initialCapacity, Comparator<T> comparator) {
        this.capacity   = initialCapacity;
        this.heap       = new Object[capacity];
        this.size       = 0;
        this.comparator = comparator;
    }

    // ── Insert ────────────────────────────────────────────────────────────────
    public void insert(T data) {
        if (size == capacity) resize();
        heap[size] = data;
        siftUp(size);
        size++;
    }

    // ── Extract root (min or max depending on comparator) ─────────────────────
    @SuppressWarnings("unchecked")
    public T extract() {
        if (size == 0) throw new RuntimeException("Heap is empty");
        T root = (T) heap[0];
        heap[0] = heap[size - 1];
        heap[size - 1] = null;
        size--;
        if (size > 0) siftDown(0);
        return root;
    }

    // ── Peek root ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0) throw new RuntimeException("Heap is empty");
        return (T) heap[0];
    }

    // ── Build heap from array (heapify — O(n)) ────────────────────────────────
    public void buildHeap(T[] arr) {
        capacity = arr.length * 2;
        heap     = new Object[capacity];
        size     = arr.length;
        for (int i = 0; i < arr.length; i++) heap[i] = arr[i];
        // start from last non-leaf and sift down
        for (int i = (size / 2) - 1; i >= 0; i--) siftDown(i);
    }

    // ── Heap Sort (returns sorted array — ascending for min-heap) ─────────────
    @SuppressWarnings("unchecked")
    public Object[] heapSort() {
        Object[] sorted = new Object[size];
        // copy current heap state
        Object[] backup = new Object[size];
        int savedSize   = size;
        for (int i = 0; i < size; i++) backup[i] = heap[i];

        for (int i = 0; i < savedSize; i++) sorted[i] = extract();

        // restore
        heap = backup;
        size = savedSize;
        for (int i = (size / 2) - 1; i >= 0; i--) siftDown(i);
        return sorted;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    public void print() {
        System.out.print("Heap [size=" + size + "]: ");
        for (int i = 0; i < size; i++) System.out.print(heap[i] + (i < size - 1 ? ", " : ""));
        System.out.println();
    }

    // ── Sift Up (bubble newly inserted element up) ────────────────────────────
    private void siftUp(int idx) {
        while (idx > 0) {
            int parent = (idx - 1) / 2;
            if (compare(idx, parent) < 0) {
                swap(idx, parent);
                idx = parent;
            } else break;
        }
    }

    // ── Sift Down (push root down after extraction) ───────────────────────────
    private void siftDown(int idx) {
        while (true) {
            int left  = 2 * idx + 1;
            int right = 2 * idx + 2;
            int target = idx;

            if (left  < size && compare(left,  target) < 0) target = left;
            if (right < size && compare(right, target) < 0) target = right;

            if (target != idx) {
                swap(idx, target);
                idx = target;
            } else break;
        }
    }

    // ── Compare two indices using comparator or Comparable ────────────────────
    @SuppressWarnings("unchecked")
    private int compare(int i, int j) {
        T a = (T) heap[i];
        T b = (T) heap[j];
        if (comparator != null) return comparator.compare(a, b);
        return ((Comparable<T>) a).compareTo(b);
    }

    // ── Swap ──────────────────────────────────────────────────────────────────
    private void swap(int i, int j) {
        Object tmp = heap[i];
        heap[i]    = heap[j];
        heap[j]    = tmp;
    }

    // ── Resize (double capacity) ──────────────────────────────────────────────
    private void resize() {
        capacity *= 2;
        Object[] newHeap = new Object[capacity];
        for (int i = 0; i < size; i++) newHeap[i] = heap[i];
        heap = newHeap;
    }
}