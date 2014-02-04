package arbitcheck;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TestSupport {

    public static void checkProperty(Class<?> clazz, String propertyName,
            Object receiver, Object... inputs) throws Throwable {
        Method prop = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(propertyName)) {
                prop = m;
                break;
            }
        }
        if (prop == null) {
            throw new IllegalArgumentException("property " + propertyName
                    + " not found");
        }
        // make accessible
        prop.setAccessible(true);
        try {
            prop.invoke(receiver, inputs);
        }
        catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
