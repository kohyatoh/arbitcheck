package arbitcheck;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import randoop.ExecutableSequence;
import randoop.IEventListener;
import randoop.NormalExecution;
import randoop.RMethod;
import randoop.StatementKind;

public class CheckStat implements IEventListener {
    private int mMaxCheckCount;
    private Map<Method, Integer> mCheckCount;
    private Map<Method, Integer> mFailCount;
    private Map<Method, Map<String, Integer>> mLabelsForMethod;

    public CheckStat(int maxCheckCount, List<StatementKind> model) {
        mMaxCheckCount = maxCheckCount;
        mCheckCount = new HashMap<Method, Integer>();
        mFailCount = new HashMap<Method, Integer>();
        mLabelsForMethod = new HashMap<Method, Map<String, Integer>>();
        for (StatementKind s : model) {
            if (s instanceof RMethod) {
                Method m = ((RMethod) s).getMethod();
                if (m.getAnnotation(Check.class) != null) {
                    mCheckCount.put(m, 0);
                    mFailCount.put(m, 0);
                    mLabelsForMethod.put(m, new HashMap<String, Integer>());
                }
            }
        }
    }

    @Override
    public void explorationStart() {
    }

    @Override
    public void explorationEnd() {
        report(System.out);
    }

    @Override
    public void generationStepPre() {
    }

    @Override
    public void generationStepPost(ExecutableSequence s) {
        if (s == null || s.sequence == null) {
            return;
        }
        if (!SequenceUtil.isChecked(s.sequence)
                || SequenceUtil.didViolateAssumption(s)) {
            return;
        }

        Method m = ((RMethod) s.sequence.getLastStatement()).getMethod();
        mCheckCount.put(m, mCheckCount.get(m) + 1);
        if (!SequenceUtil.isSuccessful(s)) {
            mFailCount.put(m, mFailCount.get(m) + 1);
        }
        for (int i = 0; i < s.sequence.size(); i++) {
            if (s.sequence.getStatementKind(i) instanceof RMonitoring) {
                NormalExecution r = (NormalExecution) s.getResult(i);
                CheckMonitor monitoring = (CheckMonitor) r.getRuntimeValue();
                String label = monitoring.getLabel();
                Map<String, Integer> labels = mLabelsForMethod.get(m);
                if (labels.containsKey(label)) {
                    labels.put(label, labels.get(label) + 1);
                }
                else {
                    labels.put(label, 1);
                }
            }
        }
    }

    @Override
    public void progressThreadUpdate() {
    }

    @Override
    public boolean stopGeneration() {
        for (int c : mCheckCount.values()) {
            if (c < mMaxCheckCount)
                return false;
        }
        return true;
    }

    public void report(OutputStream s) {
        PrintStream ps = new PrintStream(s);
        ps.println("");
        ps.println("# Results ");
        for (Method m : mCheckCount.keySet()) {
            int total = mCheckCount.get(m);
            int failed = mFailCount.get(m);
            if (failed != 0) {
                ps.println(m.getName() + ": Failed " + failed + " tests out of " + total + " tests.");
            }
            else {
                ps.println(m.getName() + ": OK, passed " + total + " tests.");
            }

            final Map<String, Integer> labels = mLabelsForMethod.get(m);
            List<String> labelsList = new ArrayList<String>(labels.keySet());
            Collections.sort(labelsList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return labels.get(o2).compareTo(labels.get(o1));
                }
            });
            for (String label : labelsList) {
                ps.println(label
                        + ": "
                        + Math.floor(100.0 * mLabelsForMethod.get(m).get(label)
                                / total));
            }
        }
    }
}
