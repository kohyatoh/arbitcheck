package arbitcheck;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AMethod implements Function {
    private final Method mMethod;

    public AMethod(Method method) {
        mMethod = method;
    }

    @Override
    public Class<?> getContainingClass() {
        return mMethod.getDeclaringClass();
    }

    @Override
    public List<Class<?>> getInputTypes() {
        List<Class<?>> types = new ArrayList<Class<?>>();
        if (!Modifier.isStatic(mMethod.getModifiers())) {
            types.add(mMethod.getDeclaringClass());
        }
        types.addAll(Arrays.asList(mMethod.getParameterTypes()));
        return types;
    }

    @Override
    public Class<?> getOutputType() {
        return mMethod.getReturnType();
    }

    @Override
    public String getName() {
        return mMethod.getName();
    }

    @Override
    public boolean isPublic() {
        return Modifier.isPublic(mMethod.getModifiers());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mMethod == null) ? 0 : mMethod.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AMethod other = (AMethod) obj;
        if (mMethod == null) {
            if (other.mMethod != null)
                return false;
        }
        else if (!mMethod.equals(other.mMethod))
            return false;
        return true;
    }

}
