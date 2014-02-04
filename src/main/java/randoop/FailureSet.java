package randoop;

import java.util.LinkedHashSet;
import java.util.Set;

public class FailureSet {

    private Set<Failure> failures = new LinkedHashSet<Failure>();

    public static class Failure {
        public final StatementKind st;
        public final Class<?> viocls;

        public Failure(StatementKind st, Class<?> viocls) {
            this.st = st;
            this.viocls = viocls;
        }

        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o == this)
                return true;
            Failure other = (Failure) o;
            if (!st.equals(other.st))
                return false;
            if (!viocls.equals(other.viocls))
                return false;
            return true;
        }

        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + st.hashCode();
            hash = hash * 31 + viocls.hashCode();
            return hash;
        }
    }

    public FailureSet(ExecutableSequence es) {
        int idx = es.getFailureIndex();

        if (idx < 0) {
            return;
        }

        for (Check obs : es.getFailures(idx)) {
            Class<?> vioCls = obs.getClass();
            StatementKind st = es.sequence.getStatementKind(idx);

            assert st != null;

            failures.add(new Failure(st, vioCls));
        }
    }

    public Set<Failure> getFailures() {
        return failures;
    }

}
