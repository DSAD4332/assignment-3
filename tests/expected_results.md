# Expected-result notes for `cases.jsonl`

These are semantic expectations; valid alternative forests and tied shortest-path parents are acceptable.

1. `ping`: `{"status":"ready"}`.
2. DSU sample: `states=[[3,2],[2,3],[2,3],[2,3]]`.
3. Empty DSU: `states=[]`.
4. Kruskal sample: cost `5`, components `2`, selected IDs `{1,2}`.
5. Prim sample: cost `5`, components `2`, selected IDs `{1,2}` in either order.
6. Negative-weight triangle: cost `-2000000000`, one component, IDs `{0,1}`.
7. SCC sample: vertices 0 and 1 share a component; 2 and 3 are separate; condensation has exactly the arcs SCC(0/1) -> SCC(2) -> SCC(3).
8. Topological sample: acyclic; every vertex occurs once and edges 0->2, 1->2, 2->3 point forward.
9. Three-vertex directed cycle: `acyclic=false`, `order=[]`.
10. DAG sample: `distance=[0,3,-4,0,null]`, `parent_edge=[-1,0,2,3,-1]`.
11. DAG with a cycle in a component unreachable from source: `acyclic=false`, empty distance and parent arrays.
12. Tied parallel edges: `distance=[0,1,3,null]`; parent edge for vertex 1 may be either parallel edge ID 0 or 1, and the witness must end at vertex 1.
