package arbitcheck;

public interface Monitoring {
    public void collect(Object o);

    public void classify(boolean b, String name);
}
