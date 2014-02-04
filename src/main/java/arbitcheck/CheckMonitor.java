package arbitcheck;

public class CheckMonitor implements Monitoring {
    private final StringBuilder mLabel;

    public CheckMonitor() {
        mLabel = new StringBuilder();
    }

    public String getLabel() {
        return mLabel.toString();
    }

    @Override
    public void collect(Object o) {
        addLabel(o.toString());
    }

    @Override
    public void classify(boolean b, String name) {
        if (b) {
            addLabel(name);
        }
    }

    private void addLabel(String s) {
        if (mLabel.length() != 0) {
            mLabel.append(' ');
        }
        mLabel.append(s);
    }
}
