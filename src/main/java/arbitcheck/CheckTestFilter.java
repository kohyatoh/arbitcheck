package arbitcheck;

import randoop.ExecutableSequence;
import randoop.FailureSet;
import randoop.ITestFilter;

public class CheckTestFilter implements ITestFilter {
    @Override
    public boolean outputSequence(ExecutableSequence s, FailureSet f) {
        if (s == null || s.sequence == null) {
            return false;
        }
        if (!SequenceUtil.isChecked(s.sequence)
                || SequenceUtil.didViolateAssumption(s)) {
            return false;
        }
        return !SequenceUtil.isSuccessful(s);
    }
}
