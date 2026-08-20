package datastructures.graph;

/**
 * Weighted Directed Graph — Adjacency List implementation (no Java Collections).
 * Uses custom arrays and linked-list edge chains.
 *
 * Used for:
 *   - Railway / Flight / Bus route management
 *   - Shortest path (Dijkstra) for fare calculation
 *   - Reachability / connectivity checks
 *
 * Supports: addVertex, addEdge, removeEdge, BFS, DFS,
 *           Dijkstra shortest path, print adjacency list.
 */
public class Graph {

    // ── Edge node (adjacency list entry) ─────────────────────────────────────
    private static class Edge {
        int    dest;
        double weight;   // distance / cost
        Edge   next;

        Edge(int dest, double weight) {
            this.dest   = dest;
            this.weight = weight;
            this.next   = null;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Edge[]   adjList;      // array of edge-chain heads
    private String[] vertexNames;  // city / station name per vertex
    private int      numVertices;
    private int      edgeCount;

    // ── Constructor ───────────────────────────────────────────────────────────
    public Graph(int maxVertices) {
        this.numVertices  = maxVertices;
        this.adjList      = new Edge[maxVertices];
        this.vertexNames  = new String[maxVertices];
        this.edgeCount    = 0;
    }

    // ── Set vertex name ───────────────────────────────────────────────────────
    public void setVertexName(int vertex, String name) {
        checkVertex(vertex);
        vertexNames[vertex] = name;
    }

    public String getVertexName(int vertex) {
        checkVertex(vertex);
        return vertexNames[vertex] != null ? vertexNames[vertex] : "V" + vertex;
    }

    // ── Add directed edge (src → dest with weight) ────────────────────────────
    public void addEdge(int src, int dest, double weight) {
        checkVertex(src);
        checkVertex(dest);
        Edge newEdge   = new Edge(dest, weight);
        newEdge.next   = adjList[src];
        adjList[src]   = newEdge;
        edgeCount++;
    }

    // ── Add undirected edge (both directions) ─────────────────────────────────
    public void addUndirectedEdge(int src, int dest, double weight) {
        addEdge(src, dest, weight);
        addEdge(dest, src, weight);
    }

    // ── Remove directed edge ──────────────────────────────────────────────────
    public boolean removeEdge(int src, int dest) {
        checkVertex(src);
        Edge curr = adjList[src], prev = null;
        while (curr != null) {
            if (curr.dest == dest) {
                if (prev == null) adjList[src] = curr.next;
                else              prev.next    = curr.next;
                edgeCount--;
                return true;
            }
            prev = curr;
            curr = curr.next;
        }
        return false;
    }

    // ── Check if edge exists ──────────────────────────────────────────────────
    public boolean hasEdge(int src, int dest) {
        Edge curr = adjList[src];
        while (curr != null) {
            if (curr.dest == dest) return true;
            curr = curr.next;
        }
        return false;
    }

    // ── Get edge weight ───────────────────────────────────────────────────────
    public double getWeight(int src, int dest) {
        Edge curr = adjList[src];
        while (curr != null) {
            if (curr.dest == dest) return curr.weight;
            curr = curr.next;
        }
        return -1;   // edge not found
    }

    // ── BFS (manual array-based queue, no java.util) ──────────────────────────
    public void bfs(int start) {
        checkVertex(start);
        boolean[] visited = new boolean[numVertices];
        int[] queue = new int[numVertices];
        int front = 0, rear = 0;

        visited[start] = true;
        queue[rear++]  = start;

        System.out.print("BFS from " + getVertexName(start) + ": ");
        while (front < rear) {
            int curr = queue[front++];
            System.out.print(getVertexName(curr) + " ");
            Edge edge = adjList[curr];
            while (edge != null) {
                if (!visited[edge.dest]) {
                    visited[edge.dest] = true;
                    queue[rear++]      = edge.dest;
                }
                edge = edge.next;
            }
        }
        System.out.println();
    }

    // ── DFS (recursive, uses custom stack via call stack) ─────────────────────
    public void dfs(int start) {
        checkVertex(start);
        boolean[] visited = new boolean[numVertices];
        System.out.print("DFS from " + getVertexName(start) + ": ");
        dfsRec(start, visited);
        System.out.println();
    }

    private void dfsRec(int vertex, boolean[] visited) {
        visited[vertex] = true;
        System.out.print(getVertexName(vertex) + " ");
        Edge edge = adjList[vertex];
        while (edge != null) {
            if (!visited[edge.dest]) dfsRec(edge.dest, visited);
            edge = edge.next;
        }
    }

    // ── Dijkstra's Shortest Path ─────────────────────────────────────────────
    /**
     * Returns the shortest distance array from `src` to all vertices.
     * Path reconstruction: use getPath(src, dest, prev).
     */
    public double[] dijkstra(int src) {
        checkVertex(src);
        double[] dist    = new double[numVertices];
        boolean[] visited = new boolean[numVertices];
        int[]   prev     = new int[numVertices];

        // initialise
        for (int i = 0; i < numVertices; i++) {
            dist[i] = Double.MAX_VALUE;
            prev[i] = -1;
        }
        dist[src] = 0;

        // O(V²) using simple min-scan (replace with heap for O((V+E)logV))
        for (int count = 0; count < numVertices - 1; count++) {
            int u = minDistVertex(dist, visited);
            if (u == -1) break;
            visited[u] = true;

            Edge edge = adjList[u];
            while (edge != null) {
                int v = edge.dest;
                if (!visited[v] && dist[u] + edge.weight < dist[v]) {
                    dist[v] = dist[u] + edge.weight;
                    prev[v] = u;
                }
                edge = edge.next;
            }
        }
        return dist;
    }

    /** Returns the path from src to dest as an int array, using prev[] from dijkstra. */
    public int[] getPath(int src, int dest) {
        double[] dist    = new double[numVertices];
        boolean[] visited = new boolean[numVertices];
        int[]   prev     = new int[numVertices];
        for (int i = 0; i < numVertices; i++) { dist[i] = Double.MAX_VALUE; prev[i] = -1; }
        dist[src] = 0;

        for (int count = 0; count < numVertices - 1; count++) {
            int u = minDistVertex(dist, visited);
            if (u == -1) break;
            visited[u] = true;
            Edge edge = adjList[u];
            while (edge != null) {
                int v = edge.dest;
                if (!visited[v] && dist[u] + edge.weight < dist[v]) {
                    dist[v] = dist[u] + edge.weight;
                    prev[v] = u;
                }
                edge = edge.next;
            }
        }

        // reconstruct path using a manual stack (array)
        int[] stack = new int[numVertices];
        int top = 0, curr = dest;
        while (curr != -1) { stack[top++] = curr; curr = prev[curr]; }

        // reverse
        int[] path = new int[top];
        for (int i = 0; i < top; i++) path[i] = stack[top - 1 - i];
        return path;
    }

    /** Print shortest path between src and dest. */
    public void printShortestPath(int src, int dest) {
        int[] path = getPath(src, dest);
        double[] dist = dijkstra(src);
        System.out.print("Shortest path " + getVertexName(src) + " -> " + getVertexName(dest) + ": ");
        for (int i = 0; i < path.length; i++) {
            System.out.print(getVertexName(path[i]));
            if (i < path.length - 1) System.out.print(" -> ");
        }
        System.out.println("  (Total cost: " + dist[dest] + ")");
    }

    // ── Print Adjacency List ──────────────────────────────────────────────────
    public void printGraph() {
        System.out.println("Graph [vertices=" + numVertices + ", edges=" + edgeCount + "]");
        for (int i = 0; i < numVertices; i++) {
            System.out.print("  " + getVertexName(i) + " -> ");
            Edge edge = adjList[i];
            if (edge == null) { System.out.println("(none)"); continue; }
            while (edge != null) {
                System.out.print(getVertexName(edge.dest) + "(" + edge.weight + ")");
                if (edge.next != null) System.out.print(", ");
                edge = edge.next;
            }
            System.out.println();
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    public int getNumVertices() { return numVertices; }
    public int getEdgeCount()   { return edgeCount; }

    // ── Internal helpers ──────────────────────────────────────────────────────
    private int minDistVertex(double[] dist, boolean[] visited) {
        double min = Double.MAX_VALUE;
        int minIdx = -1;
        for (int v = 0; v < numVertices; v++) {
            if (!visited[v] && dist[v] <= min) { min = dist[v]; minIdx = v; }
        }
        return minIdx;
    }

    private void checkVertex(int v) {
        if (v < 0 || v >= numVertices)
            throw new IllegalArgumentException("Invalid vertex: " + v);
    }
}