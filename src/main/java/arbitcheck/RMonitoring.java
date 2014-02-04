package arbitcheck;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import randoop.ExecutionOutcome;
import randoop.Globals;
import randoop.NormalExecution;
import randoop.StatementKind;
import randoop.Variable;

public class RMonitoring implements StatementKind {

    @Override
    public List<Class<?>> getInputTypes() {
        return Collections.emptyList();
    }

    @Override
    public Class<?> getOutputType() {
        return Monitoring.class;
    }

    @Override
    public ExecutionOutcome execute(Object[] statementInput, PrintStream out) {
        return new NormalExecution(new CheckMonitor(), 1);
    }

    @Override
    public void appendCode(Variable newVar, List<Variable> inputVars,
            StringBuilder b, boolean varUsed) {
        if (!varUsed)
            return;
        b.append("Monitoring ");
        b.append(newVar.getName());
        b.append(" = new arbitcheck.DummyMonitoring();");
        b.append(Globals.lineSep);
    }

    @Override
    public String toParseableString() {
        return "";
    }
}
