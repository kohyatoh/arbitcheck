package arbitcheck;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class AConstructor implements Function {
  private final Constructor<?> mConstructor;

  public AConstructor(Constructor<?> constructor) {
    mConstructor = constructor;
  }

  @Override
  public Class<?> getContainingClass() {
    return mConstructor.getDeclaringClass();
  }

  @Override
  public List<Class<?>> getInputTypes() {
    return Arrays.asList(mConstructor.getParameterTypes());
  }

  @Override
  public Class<?> getOutputType() {
    return mConstructor.getDeclaringClass();
  }

  @Override
  public String getName() {
    return mConstructor.getName();
  }

  @Override
  public boolean isPublic() {
    return Modifier.isPublic(mConstructor.getModifiers());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mConstructor == null) ? 0 : mConstructor.hashCode());
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
    AConstructor other = (AConstructor) obj;
    if (mConstructor == null) {
      if (other.mConstructor != null)
        return false;
    } else if (!mConstructor.equals(other.mConstructor))
      return false;
    return true;
  }

}
