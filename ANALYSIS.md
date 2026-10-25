# Assignment 3 — Graph Algorithms for a Campus Festival

## Build and run

Requirements: Java 17 or newer. All classes are in the default package, as required by the template.

```bash
javac src/Java/*.java
java -cp src/Java Main < tests/cases.jsonl
java -cp src/Java RouteHelperTest
java -Xmx1g -cp src/Java ScaleTest
java -cp src/Java Benchmark
```

The JSON Lines interface is unchanged: `Main` reads one JSON object per line and prints one result object per line. `Input.java`, `Json.java`, and `Main.java` are included as supplied support files. Algorithm state is allocated inside each method call; consecutive requests do not share graph state.

## 1. DSU

**Steps and invariant.** Initially each vertex is its own root and component size is one. `find` first locates the root and then compresses the traversed path. `union` attaches the smaller root tree below the larger one, updates its size and decrements the component count only when the roots differ. The largest component size is updated after a successful merge. Therefore, repeated unions and self-unions do not change the partition.

**Boundary cases.** `n=0` gives no operations and `largest=0`; with `n>0`, the largest component starts at one. A state is emitted only after each operation.

**Complexity.** `O(n + q α(n))` amortized time and `O(n+q)` memory including output.

## 2. Kruskal minimum spanning forest

**Steps and invariant.** Original edge IDs are stored separately from the edge array and sorted by `(weight, ID)`. In that order, an edge is selected only if it joins two different DSU components. Thus every selected edge preserves a forest, and Kruskal's cut property ensures minimum total weight. The final DSU component count includes isolated vertices.

**Boundary cases.** Self-loops are skipped; parallel edges, equal and negative weights are allowed. Cost uses `long`, so sums can exceed 32-bit integer range. For `n=0`, the result is cost zero, no IDs and zero components.

**Complexity.** `O(n + m log(m+1))` time and `O(n+m)` memory.

## 3. Prim minimum spanning forest

**Steps and invariant.** The undirected adjacency list stores each non-loop edge in both directions and retains its original ID. For every unvisited root, Prim marks the root and pushes its outgoing edges into a min-priority queue. The lightest edge crossing from the visited set to an unvisited vertex is accepted; edges ending at already visited vertices are discarded. When the queue empties, that component is complete and the outer loop starts another component if needed.

**Correctness and boundary cases.** The cut property justifies every accepted edge. Starting a new search at every unvisited vertex counts isolated vertices and disconnected components. Self-loops are ignored; negative weights and parallel edges remain valid. `cost` is a `long`.

**Complexity.** `O(n + m log(m+1))` time and `O(n+m)` memory with a lazy priority queue.

## 4. Kosaraju strongly connected components

**Steps and invariant.** The first pass runs iterative DFS on the original graph and appends each vertex only after all outgoing neighbours have been processed. The explicit stack stores a per-frame next-neighbour index, so each adjacency entry is scanned once. The second pass processes vertices in decreasing first-pass finish time on the transposed graph; each traversal marks exactly one SCC. Component labels are nonnegative integers and do not need to be consecutive in any particular order.

The condensation is built by scanning original edges. Edges internal to one component are skipped; a 64-bit key for each `(fromLabel,toLabel)` pair prevents duplicate condensation edges.

**Boundary cases.** Empty graphs return empty arrays. Isolated vertices become singleton SCCs. Self-loops and duplicate input edges do not create extra condensation edges.

**Complexity.** `O(n+m)` expected time and `O(n+m)` memory.

## 5. DFS topological ordering

**Steps and invariant.** Every vertex has state 0 (unvisited), 1 (active on the DFS stack) or 2 (finished). The explicit stack and per-frame next-neighbour index simulate recursive DFS without using the Java call stack. An edge to an active vertex is a back-edge and proves a directed cycle. Edges to finished vertices are not treated as cycles. If no cycle is found, reversing finish order yields a topological ordering.

**Boundary cases.** Every component is checked, including components unreachable from vertex 0. A self-loop is detected as a cycle. An empty graph is acyclic with an empty order.

**Complexity.** `O(n+m)` time and `O(n+m)` memory.

## 6. DAG shortest paths

**Steps and invariant.** The complete graph is checked for cycles by the same three-state DFS before any shortest-path relaxation. A cycle in any component returns `acyclic=false` and empty distance and parent arrays, even if the source cannot reach that component. For a DAG, vertices are processed in topological order. Only reachable vertices are relaxed; the candidate distance is the current `long` distance plus the edge weight. `parent_edge[v]` records the original edge ID that last improved the shortest distance to `v`.

**Boundary cases.** Negative weights are valid in a DAG. The source has distance zero and parent `-1`; unreachable vertices have Java `null` (serialized as JSON `null`) and parent `-1`. A finite distance of `-1` is not confused with unreachability. Parallel edges are supported.

**Complexity.** `O(n+m)` time and memory.

### Route-reconstruction helper

`reconstructRoute(source, target, edges, parentEdge, distance)` follows original parent edge IDs backward, checks that each edge ends at the current vertex, detects a malformed loop, then reverses the collected vertices. The helper returns `[source]` for the source itself and an empty list for an unreachable target. It is a helper only, not a JSON task.

Actual helper test output from `RouteHelperTest`:

```text
reachable target 3 route=[0, 1, 2, 3]
source target 0 route=[0]
unreachable target 4 route=[]
parallel/tied edge route route=[0, 1]
parallel selected parent edge ID=1, weight=5
```

The example route uses edge IDs `0, 2, 3` with weights `3, -7, 4`, total `0`. The parallel-edge case deliberately chooses edge ID 1 (one of two equal `0 -> 1` edges), weight 5; the route is `[0,1]`. Both are valid parent witnesses.

## Tests and results

Commands run in the supplied working environment:

```bash
javac src/Java/*.java
java -cp src/Java Main < tests/cases.jsonl > tests/actual_outputs.jsonl
java -cp src/Java RouteHelperTest
java -Xmx1g -cp src/Java ScaleTest
```

`tests/cases.jsonl` contains mixed tasks in one process, the ping setup check, empty DSU, repeated and self unions, disconnected MST with an isolate, negative weights and a total below -32-bit range, duplicate condensation edges, a DAG, a directed cycle, unreachable vertices, and tied parallel edges. `tests/actual_outputs.jsonl` records the actual JSON Lines output. Expected results are justified by the README examples and graph invariants: for example, the DSU example yields component/largest pairs `(3,2),(2,3),(2,3),(2,3)`; the disconnected MST selects weights 2 and 3 for total 5 and leaves vertex 3 isolated; the DAG example has distances `[0,3,-4,0,null]` and parents `[-1,0,2,3,-1]`. Alternative valid MST IDs and valid tied-edge parents are accepted.

`ScaleTest` passed with a 100,000-vertex deep chain for topological sorting, SCC and DAG shortest paths, and 250,000 DSU operations. This specifically checks the iterative DFS implementation against stack overflow and the maximum DSU operation count. The source code supports the published edge limits; benchmark graphs below exercise up to 59,999 edges.

## Kruskal versus Prim benchmark

The benchmark is implemented in `src/Java/Benchmark.java`. It generates one seeded graph per size and gives that same graph to both algorithms. The graph has a chain plus random extra edges, so it is connected and has multiple candidate edges. Seed is `20261021 + n`. The benchmark checks that both results have the same optimum cost and validates each returned forest: no selected cycle, sum of selected edge weights equals reported cost, component count agrees, and selected edge count equals `n - components`.

**Environment:** OpenJDK 21.0.12.1 (Java 17-compatible source), Linux x86_64, AMD EPYC 9V74 virtualized CPU environment, 3 CPUs visible. Timer: `System.nanoTime()`. One warm-up call per method per graph; three measured repetitions; median is the middle of the three sorted timings. Compilation is excluded. Graph generation and creation of the input `List` are excluded; conversion of input rows and each algorithm's internal sorting/adjacency/priority-queue work are included. JSON parsing and console output are excluded.

Measured results (milliseconds; timings are environment-specific):

| n | m | Kruskal runs | Kruskal median | Prim runs | Prim median |
|---:|---:|---|---:|---|---:|
| 1,000 | 3,999 | 1.945, 2.034, 3.031 | 2.034 | 3.029, 3.674, 3.921 | 3.674 |
| 5,000 | 19,999 | 12.370, 13.972, 17.230 | 13.972 | 12.463, 17.425, 17.737 | 17.425 |
| 15,000 | 59,999 | 50.033, 50.060, 55.847 | 50.060 | 38.066, 38.961, 39.450 | 38.961 |

Kruskal was faster at the first two sizes in these runs, while Prim was faster at the largest size. This is not a universal ranking: allocation, sorting, heap operations, graph shape, JVM warm-up and shared virtualized hardware affect observed timings. The asymptotic bounds for both implementations are `O(n + m log(m+1))`; the benchmark is evidence for these particular generated inputs only, not a proof that one algorithm is generally faster.

## AI/tool acknowledgement

OpenAI ChatGPT was used to help explain and review the Java implementation, and make "ANALYSIS.md".