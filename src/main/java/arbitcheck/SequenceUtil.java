package arbitcheck;

import java.lang.reflect.Method;

import randoop.ExceptionalExecution;
import randoop.ExecutableSequence;
import randoop.ExecutionOutcome;
import randoop.NormalExecution;
import randoop.RMethod;
import randoop.Sequence;
import randoop.StatementKind;
import randoop.main.GenInputsAbstract;

public final class SequenceUtil {
    // returns the first method with the given name
    public static Method getMethod(String name) {
        String className = name.substring(0, name.lastIndexOf('.'));
        String methodName = name.substring(name.lastIndexOf('.') + 1);
        try {
            Class<?> cls = Class.forName(className);
            for (Method m : cls.getDeclaredMethods()) {
                if (methodName.equals(m.getName())) {
                    // make method accessible (even if it is private)
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isChecked(Sequence s) {
        StatementKind stmt = s.getLastStatement();
        if (stmt instanceof RMethod) {
            Method m = ((RMethod) stmt).getMethod();
            return m.getAnnotation(Check.class) != null;
        }
        return false;
    }

    public static boolean didViolateAssumption(ExecutableSequence s) {
        for (int i = 0; i < s.sequence.size(); i++) {
            ExecutionOutcome outcome = s.getResult(i);
            if (outcome instanceof ExceptionalExecution) {
                // check if the exception is the one thrown by assume*()
                for (Class<?> assumptionClass : GenInputsAbstract.assumption_class_list) {
                    if (assumptionClass
                            .isInstance(((ExceptionalExecution) outcome)
                                    .getException())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSuccessful(ExecutableSequence s) {
        if (s.hasNonExecutedStatements()) {
            return false;
        }
        ExecutionOutcome outcome = s.getResult(s.sequence.size() - 1);
        StatementKind stmt = s.sequence.getLastStatement();
        Class<? extends Throwable> expected = Check.None.class;
        if (stmt instanceof RMethod) {
            Method m = ((RMethod) stmt).getMethod();
            Check check = (Check) m.getAnnotation(Check.class);
            if (check != null) {
                expected = check.expected();
            }
        }
        if (Check.None.class.equals(expected)) {
            return outcome instanceof NormalExecution;
        }
        else {
            if (outcome instanceof ExceptionalExecution) {
                Throwable exception = ((ExceptionalExecution) outcome)
                        .getException();
                return expected.isInstance(exception);
            }
            return false;
        }
    }
}
