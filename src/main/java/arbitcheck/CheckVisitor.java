package arbitcheck;

import randoop.ExecutableSequence;
import randoop.ExecutionVisitor;

public class CheckVisitor implements ExecutionVisitor {
    @Override
    public void initialize(ExecutableSequence executableSequence) {
    }

    @Override
    public void visitBefore(ExecutableSequence sequence, int i) {
    }

    @Override
    public void visitAfter(ExecutableSequence sequence, int i) {
        if (i != sequence.sequence.size() - 1) {
            return;
        }
        if (SequenceUtil.isChecked(sequence.sequence)) {
            if (SequenceUtil.didViolateAssumption(sequence))
                return;
            if (!SequenceUtil.isSuccessful(sequence)) {
                PropertyCheck check = new PropertyCheck();
                sequence.addCheck(sequence.sequence.size() - 1, check, false);
            }
        }
    }
}
