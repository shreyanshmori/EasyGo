package datastructures.hashmap;

/**
 * Generic HashMap — custom implementation using array of linked-list buckets.
 * Collision resolution: Separate Chaining.
 * Features: Dynamic resizing (load factor 0.75), put, get, remove,
 *           containsKey, keys array, values array, size, print.
 *
 * Used for: Seat availability map, fare cache, route lookup,
 *           session/login token store.
 */
public class CustomHashMap<K, V> {

    // ── Default constants ─────────────────────────────────────────────────────
    private static final int    DEFAULT_CAPACITY   = 16;
    private static final double LOAD_FACTOR_LIMIT  = 0.75;

    // ── Entry (bucket node) ───────────────────────────────────────────────────
    private static class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value) {
            this.key   = key;
            this.value = value;
            this.next  = null;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Entry<K, V>[] buckets;
    private int size;
    private int capacity;

    // ── Constructors ──────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public CustomHashMap() {
        this.capacity = DEFAULT_CAPACITY;
        this.buckets  = new Entry[capacity];
        this.size     = 0;
    }

    @SuppressWarnings("unchecked")
    public CustomHashMap(int initialCapacity) {
        this.capacity = initialCapacity;
        this.buckets  = new Entry[capacity];
        this.size     = 0;
    }

    // ── Hash function ─────────────────────────────────────────────────────────
    private int hash(K key) {
        if (key == null) return 0;
        int h = key.hashCode();
        // spread high bits to reduce clustering
        h = h ^ (h >>> 16);
        return Math.abs(h % capacity);
    }

    // ── Put ───────────────────────────────────────────────────────────────────
    public void put(K key, V value) {
        if ((double) size / capacity >= LOAD_FACTOR_LIMIT) resize();

        int idx = hash(key);
        Entry<K, V> curr = buckets[idx];

        // update if key exists
        while (curr != null) {
            if (keysEqual(curr.key, key)) {
                curr.value = value;
                return;
            }
            curr = curr.next;
        }

        // insert at head of bucket chain
        Entry<K, V> newEntry = new Entry<>(key, value);
        newEntry.next = buckets[idx];
        buckets[idx]  = newEntry;
        size++;
    }

    // ── Get ───────────────────────────────────────────────────────────────────
    public V get(K key) {
        int idx = hash(key);
        Entry<K, V> curr = buckets[idx];
        while (curr != null) {
            if (keysEqual(curr.key, key)) return curr.value;
            curr = curr.next;
        }
        return null;
    }

    // ── Get or default ────────────────────────────────────────────────────────
    public V getOrDefault(K key, V defaultValue) {
        V val = get(key);
        return (val != null) ? val : defaultValue;
    }

    // ── Remove ────────────────────────────────────────────────────────────────
    public boolean remove(K key) {
        int idx = hash(key);
        Entry<K, V> curr = buckets[idx];
        Entry<K, V> prev = null;

        while (curr != null) {
            if (keysEqual(curr.key, key)) {
                if (prev == null) buckets[idx] = curr.next;
                else              prev.next    = curr.next;
                size--;
                return true;
            }
            prev = curr;
            curr = curr.next;
        }
        return false;
    }

    // ── Contains key ─────────────────────────────────────────────────────────
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    // ── Keys as array ─────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Object[] keys() {
        Object[] keyArr = new Object[size];
        int idx = 0;
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> curr = buckets[i];
            while (curr != null) {
                keyArr[idx++] = curr.key;
                curr = curr.next;
            }
        }
        return keyArr;
    }

    // ── Values as array ───────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Object[] values() {
        Object[] valArr = new Object[size];
        int idx = 0;
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> curr = buckets[i];
            while (curr != null) {
                valArr[idx++] = curr.value;
                curr = curr.next;
            }
        }
        return valArr;
    }

    // ── Resize (double capacity, rehash all entries) ──────────────────────────
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Entry<K, V>[] newBuckets = new Entry[newCapacity];

        for (int i = 0; i < capacity; i++) {
            Entry<K, V> curr = buckets[i];
            while (curr != null) {
                Entry<K, V> next = curr.next;
                // rehash
                int newIdx = Math.abs(curr.key.hashCode() ^ (curr.key.hashCode() >>> 16)) % newCapacity;
                curr.next = newBuckets[newIdx];
                newBuckets[newIdx] = curr;
                curr = next;
            }
        }
        buckets  = newBuckets;
        capacity = newCapacity;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        for (int i = 0; i < capacity; i++) buckets[i] = null;
        size = 0;
    }

    public void print() {
        System.out.println("CustomHashMap [size=" + size + ", capacity=" + capacity + "]");
        for (int i = 0; i < capacity; i++) {
            if (buckets[i] == null) continue;
            System.out.print("  [" + i + "]: ");
            Entry<K, V> curr = buckets[i];
            while (curr != null) {
                System.out.print("(" + curr.key + " -> " + curr.value + ")");
                if (curr.next != null) System.out.print(" -> ");
                curr = curr.next;
            }
            System.out.println();
        }
    }

    // ── Internal key equality (handles null) ──────────────────────────────────
    private boolean keysEqual(K a, K b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}