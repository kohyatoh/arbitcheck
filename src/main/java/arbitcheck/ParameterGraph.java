package arbitcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ParameterGraph {
    private Map<Class<?>, TypeNode> mTypes;
    private Map<Function, FunctionNode> mFunctions;

    public ParameterGraph(ClassTree classTree) {
        mTypes = new HashMap<Class<?>, ParameterGraph.TypeNode>();
        mFunctions = new HashMap<Function, ParameterGraph.FunctionNode>();
        buildNodes(classTree);
        buildEdges(classTree);
    }

    private void buildNodes(ClassTree classTree) {
        for (Class<?> clazz : classTree.getAllClasses()) {
            addTypeNode(clazz);
            for (Function function : ClassTree.getFunctions(clazz)) {
                if (function.isPublic()) {
                    addFunctionNode(function);
                }
            }
        }
    }

    private void addTypeNode(Class<?> clazz) {
        mTypes.put(clazz, new TypeNode(clazz));
    }

    private void addFunctionNode(Function function) {
        mFunctions.put(function, new FunctionNode(function));
    }

    private void buildEdges(ClassTree classTree) {
        for (Class<?> clazz : classTree.getAllClasses()) {
            for (Function function : ClassTree.getFunctions(clazz)) {
                if (!function.isPublic())
                    continue;
                for (Class<?> c : classTree.getAllParents(
                        function.getOutputType(), true)) {
                    addProviderEdge(function, c);
                }
                List<Class<?>> inputs = function.getInputTypes();
                for (int i = 0; i < inputs.size(); i++) {
                    if (ClassTree.hasLiteral(inputs.get(i))) {
                        mFunctions.get(function).boxes[i] = true;
                        continue;
                    }
                    for (Class<?> c : classTree.getAllChildren(inputs.get(i),
                            true)) {
                        addConsumerEdge(function, c, i);
                    }
                }
            }
        }
    }

    private void addProviderEdge(Function function, Class<?> clazz) {
        mFunctions.get(function).edges.add(mTypes.get(clazz));
        mFunctions.get(function).tobox.add(0);
        mTypes.get(clazz).revedges.add(mFunctions.get(function));
    }

    private void addConsumerEdge(Function function, Class<?> clazz, int index) {
        mTypes.get(clazz).edges.add(mFunctions.get(function));
        mTypes.get(clazz).tobox.add(index);
        mFunctions.get(function).revedges.add(mTypes.get(clazz));
    }

    public void calculateRank() {
        Queue<Node> q = new LinkedList<ParameterGraph.Node>();
        for (FunctionNode node : mFunctions.values()) {
            if (node.isEnabled()) {
                node.rank = 0;
                q.add(node);
            }
        }
        while (!q.isEmpty()) {
            Node node = q.poll();
            for (int i = 0; i < node.edges.size(); i++) {
                Node to = node.edges.get(i);
                if (to.rank == -1) {
                    to.boxes[node.tobox.get(i)] = true;
                    if (to.isEnabled()) {
                        to.rank = node.rank + 1;
                        q.add(to);
                    }
                }
            }
        }
    }

    public int getRank(Class<?> clazz) {
        return mTypes.get(clazz).rank;
    }

    public Set<Class<?>> getDependences(Class<?> clazz) {
        Set<Class<?>> depends = new HashSet<Class<?>>();
        addDepends(clazz, depends);
        return depends;
    }

    public void addDepends(Class<?> clazz, Set<Class<?>> deps) {
        if (ClassTree.hasLiteral(clazz))
            return;
        if (deps.contains(clazz))
            return;
        deps.add(clazz);
        Node node = mTypes.get(clazz);
        if (node == null || node.rank == -1)
            return;
        // add providers
        addDepends(getProvider(clazz, deps), deps);
        // add parameters and mutators
        for (Function function : ClassTree.getFunctions(clazz)) {
            if (!function.isPublic())
                continue;
            addDepends(function, deps);
        }
    }

    public void addDepends(Function function, Set<Class<?>> deps) {
        if (function.getContainingClass().equals(Object.class)) {
            return;
        }
        addDepends(function.getContainingClass(), deps);
        Node node = mFunctions.get(function);
        if (node == null || node.rank == -1)
            return;
        // add parameters
        for (Node n : node.revedges) {
            if (n.rank != -1 && n.rank < node.rank) {
                // if (((TypeNode) n).clazz.isInterface())
                // continue;
                // addDepends(((TypeNode) n).clazz, deps);
            }
        }
        // add mutator
        addDepends(function.getOutputType(), deps);
    }

    public Class<?> getProvider(Class<?> clazz, Set<Class<?>> deps) {
        Node node = mTypes.get(clazz);
        if (node == null || node.rank == -1)
            return null;
        Class<?> provider = null;
        int minRank = Integer.MAX_VALUE;
        for (Node n : node.revedges) {
            Function function = ((FunctionNode) n).function;
            Class<?> fclass = function.getContainingClass();
            Node pnode = mTypes.get(fclass);
            if (pnode != null && pnode.rank != -1) {
                if (pnode.rank < minRank) {
                    provider = fclass;
                    minRank = pnode.rank;
                }
                else if (pnode.rank == minRank && deps.contains(fclass)) {
                    provider = fclass;
                }
            }
        }
        return provider;
    }

    private static class Node {
        public int rank = -1;
        public List<Node> edges = new ArrayList<Node>();
        public List<Node> revedges = new ArrayList<Node>();
        public List<Integer> tobox = new ArrayList<Integer>();
        public boolean[] boxes;

        public Node(int n) {
            boxes = new boolean[n];
        }

        public boolean isEnabled() {
            for (boolean b : boxes)
                if (!b)
                    return false;
            return true;
        }
    }

    private static class TypeNode extends Node {
        // public final Class<?> clazz;

        public TypeNode(Class<?> clazz) {
            super(1);
            // this.clazz = clazz;
        }
    }

    private static class FunctionNode extends Node {
        public final Function function;

        public FunctionNode(Function function) {
            super(function.getInputTypes().size());
            this.function = function;
        }
    }
}
