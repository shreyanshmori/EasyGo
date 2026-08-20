package datastructures.trees;

/**
 * Generic Binary Search Tree — custom implementation.
 * Elements must implement Comparable<T>.
 *
 * Used for:
 *   - Fast booking search by PNR / booking ID
 *   - Sorted passenger lookup
 *   - Train/Bus/Flight schedule indexing
 *
 * Supports: insert, search, delete, min, max,
 *           inOrder, preOrder, postOrder, height, size.
 */
public class BinarySearchTree<T extends Comparable<T>> {

    // ── Node ──────────────────────────────────────────────────────────────────
    public static class Node<T> {
        public T data;
        public Node<T> left;
        public Node<T> right;

        public Node(T data) {
            this.data  = data;
            this.left  = null;
            this.right = null;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Node<T> root;
    private int size;

    public BinarySearchTree() {
        root = null;
        size = 0;
    }

    // ── Insert ────────────────────────────────────────────────────────────────
    public void insert(T data) {
        root = insertRec(root, data);
    }

    private Node<T> insertRec(Node<T> node, T data) {
        if (node == null) { size++; return new Node<>(data); }
        int cmp = data.compareTo(node.data);
        if      (cmp < 0) node.left  = insertRec(node.left,  data);
        else if (cmp > 0) node.right = insertRec(node.right, data);
        // duplicate: ignore (or handle per use case)
        return node;
    }

    // ── Search ────────────────────────────────────────────────────────────────
    public boolean search(T data) {
        return searchRec(root, data);
    }

    private boolean searchRec(Node<T> node, T data) {
        if (node == null) return false;
        int cmp = data.compareTo(node.data);
        if      (cmp == 0) return true;
        else if (cmp < 0)  return searchRec(node.left,  data);
        else               return searchRec(node.right, data);
    }

    // ── Get node (returns null if not found) ──────────────────────────────────
    public Node<T> getNode(T data) {
        return getNodeRec(root, data);
    }

    private Node<T> getNodeRec(Node<T> node, T data) {
        if (node == null) return null;
        int cmp = data.compareTo(node.data);
        if      (cmp == 0) return node;
        else if (cmp < 0)  return getNodeRec(node.left,  data);
        else               return getNodeRec(node.right, data);
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public void delete(T data) {
        root = deleteRec(root, data);
    }

    private Node<T> deleteRec(Node<T> node, T data) {
        if (node == null) return null;
        int cmp = data.compareTo(node.data);
        if (cmp < 0) {
            node.left  = deleteRec(node.left,  data);
        } else if (cmp > 0) {
            node.right = deleteRec(node.right, data);
        } else {
            // node to delete found
            size--;
            if (node.left == null)  return node.right;
            if (node.right == null) return node.left;
            // two children: replace with in-order successor (min of right sub-tree)
            Node<T> successor = minNode(node.right);
            node.data  = successor.data;
            size++;   // deleteRec will decrement again
            node.right = deleteRec(node.right, successor.data);
        }
        return node;
    }

    // ── Min / Max ─────────────────────────────────────────────────────────────
    public T min() {
        if (root == null) throw new RuntimeException("BST is empty");
        return minNode(root).data;
    }

    public T max() {
        if (root == null) throw new RuntimeException("BST is empty");
        Node<T> curr = root;
        while (curr.right != null) curr = curr.right;
        return curr.data;
    }

    private Node<T> minNode(Node<T> node) {
        while (node.left != null) node = node.left;
        return node;
    }

    // ── Height ────────────────────────────────────────────────────────────────
    public int height() { return heightRec(root); }

    private int heightRec(Node<T> node) {
        if (node == null) return 0;
        int lh = heightRec(node.left);
        int rh = heightRec(node.right);
        return 1 + (lh > rh ? lh : rh);
    }

    // ── Traversals (print to console) ─────────────────────────────────────────
    public void inOrder() {
        System.out.print("InOrder   : ");
        inOrderRec(root);
        System.out.println();
    }

    private void inOrderRec(Node<T> node) {
        if (node == null) return;
        inOrderRec(node.left);
        System.out.print(node.data + " ");
        inOrderRec(node.right);
    }

    public void preOrder() {
        System.out.print("PreOrder  : ");
        preOrderRec(root);
        System.out.println();
    }

    private void preOrderRec(Node<T> node) {
        if (node == null) return;
        System.out.print(node.data + " ");
        preOrderRec(node.left);
        preOrderRec(node.right);
    }

    public void postOrder() {
        System.out.print("PostOrder : ");
        postOrderRec(root);
        System.out.println();
    }

    private void postOrderRec(Node<T> node) {
        if (node == null) return;
        postOrderRec(node.left);
        postOrderRec(node.right);
        System.out.print(node.data + " ");
    }

    // ── Level-order (BFS) print using a manual array-based queue ──────────────
    public void levelOrder() {
        if (root == null) { System.out.println("BST is empty"); return; }
        // manual array-based queue (no java.util.Queue)
        Object[] q = new Object[size + 1];
        int front = 0, rear = 0;
        q[rear++] = root;
        System.out.print("LevelOrder: ");
        while (front < rear) {
            @SuppressWarnings("unchecked")
            Node<T> curr = (Node<T>) q[front++];
            System.out.print(curr.data + " ");
            if (curr.left  != null) q[rear++] = curr.left;
            if (curr.right != null) q[rear++] = curr.right;
        }
        System.out.println();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int size()        { return size; }
    public boolean isEmpty() { return root == null; }
    public Node<T> getRoot() { return root; }
}