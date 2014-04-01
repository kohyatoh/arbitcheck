package arbitcheck;

import randoop.Execution;

public class PropertyCheck implements randoop.Check {
    private static final long serialVersionUID = 1L;

    @Override
    public String toCodeStringPreStatement() {
        return "";
    }

    @Override
    public String toCodeStringPostStatement() {
        return "";
    }

    @Override
    public String get_value() {
        return "";
    }

    @Override
    public String get_id() {
        return "";
    }

    @Override
    public int get_stmt_no() {
        return 0;
    }

    @Override
    public boolean evaluate(Execution execution) {
        return false;
    }

    @Override
    public int hashCode() {
        return 7;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PropertyCheck))
            return false;
        return true;
    }
}
