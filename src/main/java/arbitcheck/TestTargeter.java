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
        Check check = method.getAnnotation(Check.class);
        boolean shouldIgnoreMonitoring =
                check == null ? false : check.monitoring();
        for (Class<?> clazz : f.getInputTypes()) {
            if (shouldIgnoreMonitoring && Monitoring.class.equals(clazz)) {
                shouldIgnoreMonitoring = false;
                continue;
            }
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
