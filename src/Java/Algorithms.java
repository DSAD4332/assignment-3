import java.util.*;


public final class Algorithms {
    private Algorithms() {}

    private static final class DSU {
        final int[] parent;
        final int[] size;
        int components;
        int largest;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int x) {
            int root = x;
            while (parent[root] != root) root = parent[root];
            while (parent[x] != x) {
                int next = parent[x];
                parent[x] = root;
                x = next;
            }
            return root;
        }

        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;
            if (size[ra] < size[rb]) {
                int t = ra; ra = rb; rb = t;
            }
            parent[rb] = ra;
            size[ra] += size[rb];
            components--;
            largest = Math.max(largest, size[ra]);
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DSU dsu = new DSU(n);
        List<List<Integer>> states = new ArrayList<>(operations.length);
        for (int[] op : operations) {
            dsu.union(op[0], op[1]);
            states.add(Arrays.asList(dsu.components, dsu.largest));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("states", states);
        return result;
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        Integer[] ids = new Integer[edges.length];
        for (int i = 0; i < ids.length; i++) ids[i] = i;
        Arrays.sort(ids, Comparator.comparingInt((Integer id) -> edges[id][2])
                                   .thenComparingInt(id -> id));
        DSU dsu = new DSU(n);
        long cost = 0L;
        List<Integer> chosen = new ArrayList<>(Math.max(0, n - 1));
        for (int id : ids) {
            int[] e = edges[id];
            if (e[0] != e[1] && dsu.union(e[0], e[1])) {
                cost += (long) e[2];
                chosen.add(id);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", chosen);
        result.put("components", dsu.components);
        return result;
    }

    private static final class AdjEdge {
        final int to, id, weight;
        AdjEdge(int to, int id, int weight) {
            this.to = to; this.id = id; this.weight = weight;
        }
    }

    private static final class HeapEdge {
        final int from, to, id, weight;
        HeapEdge(int from, int to, int id, int weight) {
            this.from = from; this.to = to; this.id = id; this.weight = weight;
        }
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<List<AdjEdge>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int id = 0; id < edges.length; id++) {
            int[] e = edges[id];
            if (e[0] == e[1]) continue;
            adj.get(e[0]).add(new AdjEdge(e[1], id, e[2]));
            adj.get(e[1]).add(new AdjEdge(e[0], id, e[2]));
        }
        Comparator<HeapEdge> cmp = Comparator.comparingInt((HeapEdge e) -> e.weight)
                                             .thenComparingInt(e -> e.id)
                                             .thenComparingInt(e -> e.to);
        PriorityQueue<HeapEdge> pq = new PriorityQueue<>(cmp);
        boolean[] visited = new boolean[n];
        List<Integer> chosen = new ArrayList<>(Math.max(0, n - 1));
        long cost = 0L;
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            components++;
            visited[start] = true;
            for (AdjEdge e : adj.get(start))
                pq.add(new HeapEdge(start, e.to, e.id, e.weight));
            while (!pq.isEmpty()) {
                HeapEdge e = pq.poll();
                if (visited[e.to]) continue;
                visited[e.to] = true;
                chosen.add(e.id);
                cost += (long) e.weight;
                for (AdjEdge next : adj.get(e.to)) {
                    if (!visited[next.to])
                        pq.add(new HeapEdge(e.to, next.to, next.id, next.weight));
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", chosen);
        result.put("components", components);
        return result;
    }

    private static List<List<Integer>> adjacency(int n, int[][] edges, boolean reverse) {
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(reverse ? e[1] : e[0]).add(reverse ? e[0] : e[1]);
        return adj;
    }

    private static List<Integer> finishOrder(List<List<Integer>> adj) {
        int n = adj.size();
        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        int[] nextIndex = new int[n];
        List<Integer> order = new ArrayList<>(n);
        for (int root = 0; root < n; root++) {
            if (seen[root]) continue;
            int top = 0;
            stack[0] = root;
            nextIndex[0] = 0;
            seen[root] = true;
            while (top >= 0) {
                int v = stack[top];
                if (nextIndex[top] < adj.get(v).size()) {
                    int to = adj.get(v).get(nextIndex[top]++);
                    if (!seen[to]) {
                        seen[to] = true;
                        stack[++top] = to;
                        nextIndex[top] = 0;
                    }
                } else {
                    order.add(v);
                    top--;
                }
            }
        }
        return order;
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> adj = adjacency(n, edges, false);
        List<List<Integer>> rev = adjacency(n, edges, true);
        List<Integer> order = finishOrder(adj);
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int label = 0;
        int[] stack = new int[n];
        for (int i = order.size() - 1; i >= 0; i--) {
            int root = order.get(i);
            if (component[root] != -1) continue;
            int top = 0;
            stack[0] = root;
            component[root] = label;
            while (top >= 0) {
                int v = stack[top--];
                for (int to : rev.get(v)) {
                    if (component[to] == -1) {
                        component[to] = label;
                        stack[++top] = to;
                    }
                }
            }
            label++;
        }
        Set<Long> seenPairs = new HashSet<>();
        List<List<Integer>> condensation = new ArrayList<>();
        for (int[] e : edges) {
            int a = component[e[0]], b = component[e[1]];
            if (a != b) {
                long key = (((long) a) << 32) | (b & 0xffffffffL);
                if (seenPairs.add(key)) condensation.add(Arrays.asList(a, b));
            }
        }
        List<Integer> labels = new ArrayList<>(n);
        for (int c : component) labels.add(c);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("component", labels);
        result.put("condensation", condensation);
        return result;
    }

    private static final class TopoResult {
        final boolean acyclic;
        final List<Integer> order;
        TopoResult(boolean acyclic, List<Integer> order) {
            this.acyclic = acyclic; this.order = order;
        }
    }

    private static TopoResult topological(List<List<Integer>> adj) {
        int n = adj.size();
        byte[] state = new byte[n]; // 0 unvisited, 1 active, 2 finished
        int[] stack = new int[n];
        int[] nextIndex = new int[n];
        List<Integer> finish = new ArrayList<>(n);
        for (int root = 0; root < n; root++) {
            if (state[root] != 0) continue;
            int top = 0;
            stack[0] = root;
            nextIndex[0] = 0;
            state[root] = 1;
            while (top >= 0) {
                int v = stack[top];
                if (nextIndex[top] < adj.get(v).size()) {
                    int to = adj.get(v).get(nextIndex[top]++);
                    if (state[to] == 1) return new TopoResult(false, Collections.emptyList());
                    if (state[to] == 0) {
                        state[to] = 1;
                        stack[++top] = to;
                        nextIndex[top] = 0;
                    }
                } else {
                    state[v] = 2;
                    finish.add(v);
                    top--;
                }
            }
        }
        Collections.reverse(finish);
        return new TopoResult(true, finish);
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        TopoResult t = topological(adjacency(n, edges, false));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acyclic", t.acyclic);
        result.put("order", t.order);
        return result;
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> adj = adjacency(n, edges, false);
        TopoResult t = topological(adj);
        Map<String, Object> result = new LinkedHashMap<>();
        if (!t.acyclic) {
            result.put("acyclic", false);
            result.put("distance", Collections.emptyList());
            result.put("parent_edge", Collections.emptyList());
            return result;
        }
        List<List<AdjEdge>> weighted = new ArrayList<>(n);
        for (int i = 0; i < n; i++) weighted.add(new ArrayList<>());
        for (int id = 0; id < edges.length; id++) {
            int[] e = edges[id];
            weighted.get(e[0]).add(new AdjEdge(e[1], id, e[2]));
        }
        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int v : t.order) {
            if (distance[v] == null) continue;
            for (AdjEdge e : weighted.get(v)) {
                long candidate = distance[v] + (long) e.weight;
                if (distance[e.to] == null || candidate < distance[e.to]) {
                    distance[e.to] = candidate;
                    parentEdge[e.to] = e.id;
                }
            }
        }
        List<Long> distances = Arrays.asList(distance);
        List<Integer> parents = new ArrayList<>(n);
        for (int p : parentEdge) parents.add(p);
        result.put("acyclic", true);
        result.put("distance", distances);
        result.put("parent_edge", parents);
        return result;
    }

    static List<Integer> reconstructRoute(int source, int target, int[][] edges,
                                          int[] parentEdge, Long[] distance) {
        if (distance[target] == null) return Collections.emptyList();
        List<Integer> reversed = new ArrayList<>();
        boolean[] seen = new boolean[distance.length];
        int current = target;
        reversed.add(current);
        while (current != source) {
            if (current < 0 || current >= distance.length || seen[current])
                throw new IllegalArgumentException("Invalid parent-edge chain");
            seen[current] = true;
            int id = parentEdge[current];
            if (id < 0 || id >= edges.length || edges[id][1] != current)
                throw new IllegalArgumentException("Invalid parent edge for vertex " + current);
            current = edges[id][0];
            reversed.add(current);
        }
        Collections.reverse(reversed);
        return reversed;
    }
}
