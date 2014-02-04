package arbitcheck;

import java.util.List;

public interface Function {
    public Class<?> getContainingClass();

    public List<Class<?>> getInputTypes();

    public Class<?> getOutputType();

    public String getName();

    public boolean isPublic();
}
