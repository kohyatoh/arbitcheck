package arbitcheck;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class TestTargeter {
    private final ParameterGraph mParameterGraph;

    public TestTargeter(ClassTree classTree) {
        mParameterGraph = new ParameterGraph(classTree);
        mParameterGraph.calculateRank();
    }

    public Set<String> determineTarget(Method method) {
        Set<String> targets = new HashSet<String>();
        targets.add(method.getDeclaringClass().getName());
        Set<Class<?>> params = new HashSet<Class<?>>();
        Function f = new AMethod(method);
        for (Class<?> clazz : f.getInputTypes()) {
            params.add(clazz);
        }
        for (Class<?> clazz : params) {
            for (Class<?> c : mParameterGraph.getDependences(clazz)) {
                targets.add(c.getName());
            }
        }
        return targets;
    }
}
