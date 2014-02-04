package randoop.experimental;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import arbitcheck.DummyMonitoring;
import arbitcheck.Monitoring;
import arbitcheck.RMonitoring;

import plume.UtilMDE;
import randoop.ArrayDeclaration;
import randoop.Check;
import randoop.ExecutableSequence;
import randoop.Globals;
import randoop.PrimitiveOrStringOrNullDecl;
import randoop.RConstructor;
import randoop.RMethod;
import randoop.StatementKind;
import randoop.Variable;
import randoop.main.GenInputsAbstract;
import randoop.util.PrimitiveTypes;
import randoop.util.Util;

/**
 * This class prints a sequence with all variables renamed
 * */
class SequenceDumper {
    /**
     * The sequence to be printed
     * */
    public final ExecutableSequence sequenceToPrint;
    /**
     * The class for renaming all output variables
     * */
    public final VariableRenamer renamer;

    public SequenceDumper(ExecutableSequence sequenceToPrint,
            VariableRenamer renamer) {
        this.sequenceToPrint = sequenceToPrint;
        this.renamer = renamer;
    }

    /**
     * Print a sequence statement by statement, with variables renamed.
     * */
    public String printSequenceAsCodeString() {
        StringBuilder sb = new StringBuilder();
        Set<Integer> usedIndexes = new HashSet<Integer>();
        for (int i = 0; i < this.sequenceToPrint.sequence.size(); i++) {
            for (Variable v : this.sequenceToPrint.sequence.getInputs(i)) {
                usedIndexes.add(v.getDeclIndex());
            }
        }
        for (int i = 0; i < this.sequenceToPrint.sequence.size(); i++) {
            StatementKind statement = this.sequenceToPrint.sequence
                    .getStatementKind(i);
            Variable outputVar = this.sequenceToPrint.sequence.getVariable(i);
            List<Variable> inputVars = this.sequenceToPrint.sequence
                    .getInputs(i);
            // store the code text of the current statement
            StringBuilder oneStatement = new StringBuilder();
            // print the current statement
            this.appendCode(oneStatement, statement, outputVar, inputVars,
                    usedIndexes.contains(i));

            for (Check d : this.sequenceToPrint.getChecks(i)) {
                oneStatement.insert(0, d.toCodeStringPreStatement());
                oneStatement.append(d.toCodeStringPostStatement());
                oneStatement.append(Globals.lineSep);
            }

            sb.append(oneStatement);

        }
        return sb.toString();
    }

    /**********************************************************
     * The following code prints different types of statements.
     **********************************************************/
    private void appendCode(StringBuilder sb, StatementKind statement,
            Variable newVar, List<Variable> inputVars, boolean varUsed) {
        if (statement instanceof PrimitiveOrStringOrNullDecl) {
            if (GenInputsAbstract.long_format) {
                PrimitiveOrStringOrNullDecl primiveStatement = (PrimitiveOrStringOrNullDecl) statement;
                this.printPrimitiveType(primiveStatement, newVar, inputVars, sb);
            }
        }
        else if (statement instanceof RMethod) {
            this.printRMethod((RMethod) statement, newVar, inputVars, sb,
                    varUsed);
        }
        else if (statement instanceof RConstructor) {
            this.printRConstructor((RConstructor) statement, newVar, inputVars,
                    sb, varUsed);
        }
        else if (statement instanceof ArrayDeclaration) {
            ArrayDeclaration arrayDeclaration = (ArrayDeclaration) statement;
            this.printArrayDeclaration(arrayDeclaration, newVar, inputVars, sb,
                    varUsed);
        }
        else if (statement instanceof RMonitoring) {
            this.printRMonitoring((RMonitoring) statement, newVar, inputVars,
                    sb, varUsed);
        }
        else {
            throw new Error("Wrong type of statement: " + statement);
        }
    }

    private void printRMethod(RMethod rmethod, Variable newVar,
            List<Variable> inputVars, StringBuilder sb, boolean varUsed) {
        if (varUsed && !rmethod.isVoid()) {
            sb.append(getSimpleCompilableName(rmethod.getMethod()
                    .getReturnType()));
            String cast = "";
            sb.append(" " + this.renamer.getRenamedVar(newVar.index) + " = "
                    + cast);
        }

        if (Modifier.isPrivate(rmethod.getMethod().getModifiers())) {
            String receiverString = rmethod.isStatic() ? "null" : this.renamer
                    .getRenamedVar(inputVars.get(0).index);
            sb.append("checkProperty(");
            sb.append(getSimpleCompilableName(rmethod.getMethod()
                    .getDeclaringClass()));
            sb.append(".class, \"");
            sb.append(rmethod.getMethod().getName());
            sb.append("\", ");
            sb.append(receiverString);
            String inputs = toArguments(rmethod, inputVars);
            if (inputs.length() > 0) {
                sb.append(", ");
                sb.append(inputs);
            }
        }
        else {
            String receiverString = rmethod.isStatic() ? null : this.renamer
                    .getRenamedVar(inputVars.get(0).index);
            appendReceiverOrClassForStatics(rmethod, receiverString, sb);
            sb.append(".");
            sb.append(rmethod.getTypeArguments());
            sb.append(rmethod.getMethod().getName() + "(");
            sb.append(toArguments(rmethod, inputVars));
        }
        sb.append(");" + Globals.lineSep);
    }

    private String toArguments(RMethod rmethod, List<Variable> inputVars) {
        StringBuilder sb = new StringBuilder();
        int startIndex = (rmethod.isStatic() ? 0 : 1);
        for (int i = startIndex; i < inputVars.size(); i++) {
            if (i > startIndex)
                sb.append(", ");

            // CASTING.
            // We cast whenever the variable and input types are not identical.
            // We also cast if input type is a primitive, because Randoop uses
            // boxed primitives, and need to convert back to primitive.
            if (PrimitiveTypes.isPrimitive(rmethod.getInputTypes().get(i))
                    && GenInputsAbstract.long_format) {
                sb.append("(" + rmethod.getInputTypes().get(i).getSimpleName()
                        + ")");
            }
            else if (!inputVars.get(i).getType()
                    .equals(rmethod.getInputTypes().get(i))) {
                sb.append("(" + rmethod.getInputTypes().get(i).getSimpleName()
                        + ")");
            }

            // In the short output format, statements like "int x = 3" are not
            // added to a sequence; instead,
            // the value (e.g. "3") is inserted directly added as arguments to
            // method calls.
            StatementKind statementCreatingVar = inputVars.get(i)
                    .getDeclaringStatement();
            if (!GenInputsAbstract.long_format
                    && statementCreatingVar instanceof PrimitiveOrStringOrNullDecl) {
                sb.append(PrimitiveTypes
                        .toCodeString(((PrimitiveOrStringOrNullDecl) statementCreatingVar)
                                .getValue()));
            }
            else {
                sb.append(this.renamer.getRenamedVar(inputVars.get(i).index)/*
                                                                             * inputVars
                                                                             * .
                                                                             * get
                                                                             * (
                                                                             * i
                                                                             * )
                                                                             * .
                                                                             * getName
                                                                             * (
                                                                             * )
                                                                             */);
            }
        }
        return sb.toString();
    }

    private void printRConstructor(RConstructor rconstructor, Variable newVar,
            List<Variable> inputVars, StringBuilder sb, boolean varUsed) {

        assert inputVars.size() == rconstructor.getInputTypes().size();

        Class<?> declaringClass = rconstructor.getConstructor()
                .getDeclaringClass();
        boolean isNonStaticMember = !Modifier.isStatic(declaringClass
                .getModifiers()) && declaringClass.isMemberClass();
        assert Util.implies(isNonStaticMember, inputVars.size() > 0);

        // Note on isNonStaticMember: if a class is a non-static member class,
        // the
        // runtime signature of the constructor will have an additional argument
        // (as the first argument) corresponding to the owning object. When
        // printing
        // it out as source code, we need to treat it as a special case: instead
        // of printing "new Foo(x,y.z)" we have to print "x.new Foo(y,z)".

        // TODO the last replace is ugly. There should be a method that does it.
        String declaringClassStr = getSimpleCompilableName(declaringClass);

        if (varUsed) {
            sb.append(declaringClassStr + " "
                    + this.renamer.getRenamedVar(newVar.index)/*
                                                               * newVar.getName()
                                                               */+ " = ");
        }
        sb.append((isNonStaticMember ? this.renamer.getRenamedVar(inputVars
                .get(0).index) /* inputVars.get(0) */+ "." : "")
                + "new "
                + (isNonStaticMember ? declaringClass.getSimpleName()
                        : declaringClassStr) + "(");
        for (int i = (isNonStaticMember ? 1 : 0); i < inputVars.size(); i++) {
            if (i > (isNonStaticMember ? 1 : 0))
                sb.append(", ");

            // We cast whenever the variable and input types are not identical.
            if (!inputVars.get(i).getType()
                    .equals(rconstructor.getInputTypes().get(i)))
                sb.append("("
                        + rconstructor.getInputTypes().get(i).getSimpleName()
                        + ")");

            // In the short output format, statements like "int x = 3" are not
            // added to a sequence; instead,
            // the value (e.g. "3") is inserted directly added as arguments to
            // method calls.
            StatementKind statementCreatingVar = inputVars.get(i)
                    .getDeclaringStatement();
            if (!GenInputsAbstract.long_format
                    && statementCreatingVar instanceof PrimitiveOrStringOrNullDecl) {
                sb.append(PrimitiveTypes
                        .toCodeString(((PrimitiveOrStringOrNullDecl) statementCreatingVar)
                                .getValue()));
            }
            else {
                sb.append(this.renamer.getRenamedVar(inputVars.get(i).index)/*
                                                                             * inputVars
                                                                             * .
                                                                             * get
                                                                             * (
                                                                             * i
                                                                             * )
                                                                             * .
                                                                             * getName
                                                                             * (
                                                                             * )
                                                                             */);
            }
        }
        sb.append(");");
        sb.append(Globals.lineSep);
    }

    private void printPrimitiveType(PrimitiveOrStringOrNullDecl statement,
            Variable newVar, List<Variable> inputVars, StringBuilder sb) {
        Class<?> type = statement.getType();
        // print primitive type
        if (type.isPrimitive()) {
            sb.append(PrimitiveTypes.boxedType(type).getSimpleName());
            sb.append(" ");
            sb.append(this.renamer.getRenamedVar(newVar.index));
            sb.append(" = new ");
            sb.append(PrimitiveTypes.boxedType(type).getSimpleName());
            sb.append("(");
            sb.append(PrimitiveTypes.toCodeString(statement.getValue()));
            sb.append(");");
            sb.append(Globals.lineSep);

        }
        else {
            sb.append(getSimpleCompilableName(type));
            sb.append(" ");
            sb.append(this.renamer.getRenamedVar(newVar.index));
            sb.append(" = ");
            sb.append(PrimitiveTypes.toCodeString(statement.getValue()));
            sb.append(";");
            sb.append(Globals.lineSep);
        }
    }

    private void printArrayDeclaration(ArrayDeclaration statement,
            Variable newVar, List<Variable> inputVars, StringBuilder sb,
            boolean varUsed) {
        if (!varUsed) {
            return;
        }
        int length = statement.getLength();
        if (inputVars.size() > length)
            throw new IllegalArgumentException("Too many arguments:"
                    + inputVars.size() + " capacity:" + length);
        String declaringClass = statement.getElementType().getSimpleName();// .getCanonicalName();
        sb.append(declaringClass + "[] "
                + this.renamer.getRenamedVar(newVar.index) + " = new "
                + declaringClass + "[] { ");
        for (int i = 0; i < inputVars.size(); i++) {
            if (i > 0)
                sb.append(", ");

            // In the short output format, statements like "int x = 3" are not
            // added to a sequence; instead,
            // the value (e.g. "3") is inserted directly added as arguments to
            // method calls.
            StatementKind statementCreatingVar = inputVars.get(i)
                    .getDeclaringStatement();
            if (!GenInputsAbstract.long_format
                    && statementCreatingVar instanceof PrimitiveOrStringOrNullDecl) {
                sb.append(PrimitiveTypes
                        .toCodeString(((PrimitiveOrStringOrNullDecl) statementCreatingVar)
                                .getValue()));
            }
            else {
                sb.append(/* inputVars.get(i).getName() */this.renamer
                        .getRenamedVar(inputVars.get(i).index));
            }
        }
        sb.append("};");
        sb.append(Globals.lineSep);
    }

    private void printRMonitoring(RMonitoring monitoring, Variable newVar,
            List<Variable> inputVars, StringBuilder sb, boolean varUsed) {
        if (!varUsed) {
            return;
        }
        sb.append(getSimpleCompilableName(Monitoring.class));

        sb.append(" " + this.renamer.getRenamedVar(newVar.index) + " = "
                + "new " + getSimpleCompilableName(DummyMonitoring.class)
                + "();");
        sb.append(Globals.lineSep);
    }

    private static String getSimpleCompilableName(Class<?> cls) {
        String retval = cls.getSimpleName();

        // If it's an array, it starts with "[".
        if (retval.charAt(0) == '[') {
            // Class.getName() returns a a string that is almost in JVML
            // format, except that it slashes are periods. So before calling
            // classnameFromJvm, we replace the period with slashes to
            // make the string true JVML.
            retval = UtilMDE.classnameFromJvm(retval.replace('.', '/'));
        }

        // If inner classes are involved, Class.getName() will return
        // a string with "$" characters. To make it compilable, must replace
        // with
        // dots.
        retval = retval.replace('$', '.');

        return retval;
    }

    private static void appendReceiverOrClassForStatics(RMethod rmethod,
            String receiverString, StringBuilder b) {
        if (rmethod.isStatic()) {
            String s2 = rmethod.getMethod().getDeclaringClass().getSimpleName()
                    .replace('$', '.'); // TODO combine this with last if clause
            b.append(s2);
        }
        else {
            Class<?> expectedType = rmethod.getInputTypes().get(0);
            String className = expectedType.getSimpleName();// expectedType.getCanonicalName();
            boolean mustCast = className != null
                    && PrimitiveTypes
                            .isBoxedPrimitiveTypeOrString(expectedType)
                    && !expectedType.equals(String.class);
            if (mustCast) {
                // this is a little paranoid but we need to cast primitives in
                // order to get them boxed.
                b.append("((" + className + ")" + receiverString + ")");
            }
            else {
                b.append(receiverString);
            }
        }
    }
}
