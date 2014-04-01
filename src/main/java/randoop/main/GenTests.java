package randoop.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import plume.Option;
import plume.OptionGroup;
import plume.Options;
import plume.Options.ArgException;
import plume.SimpleLog;
import plume.Unpublicized;
import randoop.AbstractGenerator;
import randoop.BugInRandoopException;
import randoop.ComponentManager;
import randoop.ExecutableSequence;
import randoop.ExecutionVisitor;
import randoop.ForwardGenerator;
import randoop.ITestFilter;
import randoop.JunitFileWriter;
import randoop.RConstructor;
import randoop.RMethod;
import randoop.RandoopListenerManager;
import randoop.SeedSequences;
import randoop.Sequence;
import randoop.StatementKind;
import randoop.Variable;
import randoop.experimental.GreedySequenceSimplifier;
import randoop.util.DefaultReflectionFilter;
import randoop.util.Log;
import randoop.util.Randomness;
import randoop.util.Reflection;
import randoop.util.ReflectionExecutor;
import randoop.util.TimeoutExceededException;
import arbitcheck.CheckStat;
import arbitcheck.CheckTestFilter;
import arbitcheck.CheckVisitor;
import arbitcheck.SequenceUtil;

public class GenTests extends GenInputsAbstract {

    private static final String command = "gentests";

    private static final String pitch = "Generates unit tests for a set of classes.";

    private static final String commandGrammar = "gentests OPTIONS";

    private static final String where = "At least one class is specified via `--testclass' or `--classlist'.";

    private static final String summary = "Attempts to generate JUnit tests that "
            + "capture the behavior of the classes under test and/or find contract violations. "
            + "Randoop generates tests using feedback-directed random test generation. ";

    private static final String input = "One or more names of classes to test. A class to test can be specified "
            + "via the `--testclass=<CLASSNAME>' or `--classlist=<FILENAME>' options.";

    private static final String output = "A JUnit test suite (as one or more Java source files). The "
            + "tests in the suite will pass when executed using the classes under test.";

    private static final String example = "java randoop.main.Main gentests --testclass=java.util.Collections "
            + " --testclass=java.util.TreeSet";

    private static final List<String> notes;

    public static Method property;

    static {

        notes = new ArrayList<String>();
        notes.add("Randoop executes the code under test, with no mechanisms to protect your system from harm resulting from arbitrary code execution. If random execution of your code could have undesirable effects (e.g. deletion of files, opening network connections, etc.) make sure you execute Randoop in a sandbox machine.");
        notes.add("Randoop will only use methods from the classes that you specify for testing. If Randoop is not generating tests for a particular method, make sure that you are including classes for the types that the method requires. Otherwise, Randoop may fail to generate tests due to missing input parameters.");
        notes.add("Randoop is designed to be deterministic when the code under test is itself deterministic. This means that two runs of Randoop will generate the same tests. To get variation across runs, use the --randomseed option.");

    }

    @OptionGroup(value = "GenTests unpublicized options", unpublicized = true)
    @Unpublicized
    @Option("Signals that this is a run in the context of a system test. (Slower)")
    public static boolean system_test_run = false;

    public static SimpleLog progress = new SimpleLog(true);

    private static Options options = new Options(GenTests.class,
            GenInputsAbstract.class, ReflectionExecutor.class,
            ForwardGenerator.class, AbstractGenerator.class);

    public GenTests() {
        super(command, pitch, commandGrammar, where, summary, notes, input,
                output, example, options);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handle(String[] args) throws RandoopTextuiException {

        // RandoopSecurityManager randoopSecurityManager = new
        // RandoopSecurityManager(
        // RandoopSecurityManager.Status.OFF);
        // System.setSecurityManager(randoopSecurityManager);

        try {
            String[] nonargs = options.parse(args);
            if (nonargs.length > 0)
                throw new ArgException("Unrecognized arguments: "
                        + Arrays.toString(nonargs));
        }
        catch (ArgException ae) {
            usage("while parsing command-line arguments: %s", ae.getMessage());
        }

        checkOptionsValid();

        property = SequenceUtil.getMethod(GenInputsAbstract.property);

        // load assumption classses
        for (String className : assumption_classes.split(",")) {
            try {
                Class<?> clazz = Class.forName(className);
                assumption_class_list.add(clazz);
            }
            catch (ClassNotFoundException e) {
                // not found
                System.err.println("assumption class " + className
                        + " not found");
            }
        }
        // TODO: current implementation of Randoop does not handle Generics
        // properly.
        // Temporally ignore ClassCastException to ignore false positives by
        // this.
        assumption_class_list.add(ClassCastException.class);

        Randomness.reset(randomseed);

        java.security.Policy policy = java.security.Policy.getPolicy();

        if (!GenInputsAbstract.noprogressdisplay) {
            System.out.printf("policy = %s%n", policy);
        }

        // If some properties were specified, set them
        for (String prop : GenInputsAbstract.system_props) {
            String[] pa = prop.split("=", 2);
            if (pa.length != 2)
                usage("invalid property definition: %s%n", prop);
            System.setProperty(pa[0], pa[1]);
        }

        // If an initializer method was specified, execute it
        execute_init_routine(1);

        // Find classes to test.
        if (classlist == null && methodlist == null && testclass.size() == 0) {
            System.out
                    .println("You must specify some classes or methods to test.");
            System.out
                    .println("Use the --classlist, --testclass, or --methodlist options.");
            System.exit(1);
        }

        List<Class<?>> allClasses = findClassesFromArgs(options);

        // Remove private (non-.isVisible) classes and abstract classes
        // and interfaces.
        List<Class<?>> classes = new ArrayList<Class<?>>(allClasses.size());
        for (Class<?> c : allClasses) {
            // if (Reflection.isAbstract (c)) {
            // System.out.println("Ignoring abstract " + c +
            // " specified via --classlist or --testclass.");
            // } else if (! Reflection.isVisible (c)) {
            if (!Reflection.isVisible(c)) {
                System.out.println("Ignoring non-visible " + c
                        + " specified via --classlist or --testclass.");
            }
            else {
                classes.add(c);
            }
        }

        // Make sure each of the classes is visible. Should really make sure
        // there is at least one visible constructor/factory in each class as
        // well.
        for (Class<?> c : classes) {
            if (!Reflection.isVisible(c)) {
                throw new Error("Specified class " + c + " is not visible");
            }
        }

        DefaultReflectionFilter reflectionFilter = new DefaultReflectionFilter(
                omitmethods);
        List<StatementKind> model = Reflection.getStatements(classes,
                reflectionFilter);

        // Always add Object constructor (it's often useful).
        RConstructor objectConstructor = null;
        try {
            objectConstructor = RConstructor.getRConstructor(Object.class
                    .getConstructor());
            if (!model.contains(objectConstructor))
                model.add(objectConstructor);
        }
        catch (Exception e) {
            throw new BugInRandoopException(e); // Should never reach here!
        }

        if (methodlist != null) {
            Set<StatementKind> statements = new LinkedHashSet<StatementKind>();
            try {
                for (Member m : Reflection
                        .loadMethodsAndCtorsFromFile(new File(methodlist))) {
                    if (m instanceof Method) {
                        if (reflectionFilter.canUse((Method) m)) {
                            statements.add(RMethod.getRMethod((Method) m));
                        }
                    }
                    else {
                        assert m instanceof Constructor<?>;
                        if (reflectionFilter.canUse((Constructor<?>) m)) {
                            statements.add(RConstructor
                                    .getRConstructor((Constructor<?>) m));
                        }
                        statements.add(RConstructor
                                .getRConstructor((Constructor<?>) m));
                    }
                }
            }
            catch (IOException e) {
                System.out.println("Error while reading method list file "
                        + methodlist);
                System.exit(1);
            }
            for (StatementKind st : statements) {
                if (!model.contains(st))
                    model.add(st);
            }
        }

        if (model.size() == 0) {
            Log.out.println("There are no methods to test. Exiting.");
            System.exit(1);
        }
        if (!GenInputsAbstract.noprogressdisplay) {
            System.out.println("PUBLIC MEMBERS=" + model.size());
        }

        // Initialize components.
        Set<Sequence> components = new LinkedHashSet<Sequence>();

        // Add default seeds.
        components.addAll(SeedSequences
                .objectsToSeeds(SeedSequences.primitiveSeeds));

        // Add user-specified seeds.
        components.addAll(SeedSequences.getSeedsFromAnnotatedFields(classes
                .toArray(new Class<?>[0])));

        ComponentManager componentMgr = new ComponentManager(components);

        RandoopListenerManager listenerMgr = new RandoopListenerManager();
        CheckStat checkStat = new CheckStat(GenInputsAbstract.check_count,
                model);
        listenerMgr.addListener(checkStat);

        AbstractGenerator explorer = null;

        LinkedList<ITestFilter> outputTestFilters = new LinkedList<ITestFilter>();
        // outputTestFilters.add(new DefaultTestFilter());
        outputTestFilters.add(new CheckTestFilter());

        // ///////////////////////////////////////
        // Create the generator for this session.
        explorer = new ForwardGenerator(model, timelimit * 1000, inputlimit,
                componentMgr, null, listenerMgr, outputTestFilters);
        // ///////////////////////////////////////

        if (!GenInputsAbstract.noprogressdisplay) {
            System.out.printf("Explorer = %s\n", explorer);
        }

        // Determine what visitors to install.
        // NOTE that order matters! Regression capture visitor
        // should come after contract-violating visitor.
        List<ExecutionVisitor> visitors = new ArrayList<ExecutionVisitor>();
        visitors.add(new CheckVisitor());

        // Install any user-specified visitors.
        if (!GenInputsAbstract.visitor.isEmpty()) {
            for (String visitorClsName : GenInputsAbstract.visitor) {
                try {
                    Class<ExecutionVisitor> cls = (Class<ExecutionVisitor>) Class
                            .forName(visitorClsName);
                    ExecutionVisitor vis = cls.newInstance();
                    visitors.add(vis);
                }
                catch (Exception e) {
                    System.out.println("Error while loading visitor class "
                            + visitorClsName);
                    System.out.println("Exception message: " + e.getMessage());
                    System.out.println("Stack trace:");
                    e.printStackTrace(System.out);
                    System.out.println("Randoop will exit with code 1.");
                    System.exit(1);
                }
            }
        }

        explorer.executionVisitor.visitors.addAll(visitors);

        explorer.explore();

        // dump_seqs ("after explore", explorer.outSeqs);

        if (dont_output_tests)
            return true;

        // Create JUnit files containing faults.
        if (!GenInputsAbstract.noprogressdisplay) {
            System.out.println();
            // System.out.print("Creating Junit tests (" +
            // explorer.outSeqs.size() + " tests)...");
            // the number of resulting tests are unknown here
            System.out.print("Creating JUnit tests...");
        }
        List<ExecutableSequence> sequences = new ArrayList<ExecutableSequence>();
        for (ExecutableSequence p : explorer.outSeqs) {
            sequences.add(p);
        }

        // Remove any sequences that throw
        // randoop.util.TimeoutExceededException.
        // It would be nicer for Randoop to output a test suite that detects
        // long-running tests and generates a TimeoutExceededException, as
        // documented in Issue 11:
        // http://code.google.com/p/randoop/issues/detail?id=11 .
        {
            List<ExecutableSequence> non_timeout_seqs = new ArrayList<ExecutableSequence>();
            boolean keep = true;
            for (ExecutableSequence es : sequences) {
                if (keep)
                    non_timeout_seqs.add(es);
                // This test suggests a shorter way to implement this method.
                assert keep == !es
                        .throwsException(TimeoutExceededException.class);
            }
            sequences = non_timeout_seqs;
        }

        // If specified, remove any sequences that don't include the target
        // class
        // System.out.printf ("test_classes regex = %s%n",
        // GenInputsAbstract.test_classes);
        if (GenInputsAbstract.test_classes != null) {
            List<ExecutableSequence> tc_seqs = new ArrayList<ExecutableSequence>();
            for (ExecutableSequence es : sequences) {
                boolean keep = false;
                for (Variable v : es.sequence.getAllVariables()) {
                    if (GenInputsAbstract.test_classes.matcher(
                            v.getType().getName()).matches()) {
                        keep = true;
                        break;
                    }
                }
                if (keep)
                    tc_seqs.add(es);
            }
            sequences = tc_seqs;
            System.out.printf("%n%d sequences include %s%n", sequences.size(),
                    GenInputsAbstract.test_classes);
        }

        // If specified remove any sequences that are used as inputs in other
        // tests. These sequences are redundant.
        //
        // While we're at it, remove the useless sequence "new Object()".
        Sequence newObj = new Sequence().extend(objectConstructor);
        if (GenInputsAbstract.remove_subsequences) {
            List<ExecutableSequence> unique_seqs = new ArrayList<ExecutableSequence>();
            Set<Sequence> subsumed_seqs = explorer.subsumed_sequences();
            for (ExecutableSequence es : sequences) {
                if (!subsumed_seqs.contains(es.sequence)
                        && !es.sequence.equals(newObj)) {
                    unique_seqs.add(es);
                }
            }
            // if (!GenInputsAbstract.noprogressdisplay) {
            // System.out.printf("%d subsumed tests removed%n", sequences.size()
            // - unique_seqs.size());
            // }
            sequences = unique_seqs;
        }

        // Write out junit tests
        if (GenInputsAbstract.outputlimit < sequences.size()) {
            List<ExecutableSequence> seqs = new ArrayList<ExecutableSequence>();
            for (int ii = 0; ii < GenInputsAbstract.outputlimit; ii++)
                seqs.add(sequences.get(ii));
            sequences = seqs;
        }

        if (GenInputsAbstract.simplify_failed_tests) {
            List<ExecutableSequence> failedSequences = new LinkedList<ExecutableSequence>();
            for (ExecutableSequence sequence : sequences) {
                if (sequence.hasFailure()
                        && !sequence.hasNonExecutedStatements()) {
                    failedSequences.add(sequence);
                }
            }
            // simplify each failed statement, and replace the original
            // sequences with the
            // simplified one
            System.out.println("Start to simplify: " + failedSequences.size()
                    + " sequences.");
            for (ExecutableSequence failedSequence : failedSequences) {
                GreedySequenceSimplifier simplifier = new GreedySequenceSimplifier(
                        failedSequence.sequence, explorer.executionVisitor);
                ExecutableSequence simplified_sequence = simplifier
                        .simplfy_sequence();
                // System.out.println("Simplified a failed sequence, original length: "
                // + failedSequence.sequence.size()
                // + ", length after simplification: " +
                // simplified_sequence.sequence.size());
                int index = sequences.indexOf(failedSequence);
                assert index != -1 : "The index should not be -1";
                // replace the failed sequence with the simplified one
                sequences.remove(index);
                sequences.add(index, simplified_sequence);
            }
            // remove duplicated tests
            Set<ExecutableSequence> sets = new HashSet<ExecutableSequence>(
                    sequences);
            sequences.clear();
            sequences.addAll(sets);
        }

        // sort sequences
        Collections.sort(sequences, new Comparator<ExecutableSequence>() {
            @Override
            public int compare(ExecutableSequence o1, ExecutableSequence o2) {
                Integer v1 = o1.sequence.size();
                Integer v2 = o2.sequence.size();
                return v1.compareTo(v2);
            }
        });

        if (!sequences.isEmpty()) {
            write_junit_tests(junit_output_dir, sequences, null);
        }

        return true;
    }

    /**
     * Writes the sequences as JUnit files to the specified directory.
     * 
     * additionalJUnitCLasses can be null.
     **/
    public static List<File> write_junit_tests(String output_dir,
            List<ExecutableSequence> seq, List<String> additionalJUnitClasses) {

        if (!GenInputsAbstract.noprogressdisplay) {
            System.out.printf("Writing %d junit tests%n", seq.size());
        }
        JunitFileWriter jfw = new JunitFileWriter(output_dir,
                junit_package_name, junit_classname, junit_methodname,
                testsperfile);
        List<File> ret = new ArrayList<File>();
        ret.addAll(jfw.createJunitTestFiles(seq));
        List<String> junitTestSuiteNames = new LinkedList<String>();
        junitTestSuiteNames.addAll(jfw.getJunitTestSuiteNames());
        junitTestSuiteNames.addAll(additionalJUnitClasses == null ? Collections
                .<String> emptyList() : additionalJUnitClasses);
        ret.add(JunitFileWriter.writeDriverFile(jfw.getDir(), jfw.packageName,
                jfw.junitDriverClassName, junitTestSuiteNames));
        // ret.add(jfw.writeDriverFile());
        List<File> files = ret;
        if (!GenInputsAbstract.noprogressdisplay) {
            System.out.println();
        }

        for (File f : files) {
            if (!GenInputsAbstract.noprogressdisplay) {
                System.out.println("Created file: " + f.getAbsolutePath());
            }
        }
        return files;
    }

    /**
     * Execute the init routine (if user specified one)
     */
    public static void execute_init_routine(int phase) {

        if (GenInputsAbstract.init_routine == null)
            return;

        String full_name = GenInputsAbstract.init_routine;
        int lastdot = full_name.lastIndexOf(".");
        if (lastdot == -1)
            usage("invalid init routine: %s\n", full_name);
        String classname = full_name.substring(0, lastdot);
        String methodname = full_name.substring(lastdot + 1);
        methodname = methodname.replaceFirst("[()]*$", "");
        System.out.printf("%s - %s\n", classname, methodname);
        Class<?> iclass = null;
        try {
            iclass = Class.forName(classname);
        }
        catch (Exception e) {
            usage("Can't load init class %s: %s", classname, e.getMessage());
        }
        Method imethod = null;
        try {
            imethod = iclass.getDeclaredMethod(methodname, int.class);
        }
        catch (Exception e) {
            usage("Can't find init method %s: %s", methodname, e);
        }
        if (!Modifier.isStatic(imethod.getModifiers()))
            usage("init method %s.%s must be static", classname, methodname);
        try {
            imethod.invoke(null, phase);
        }
        catch (Exception e) {
            usage(e, "problem executing init method %s.%s: %s", classname,
                    methodname, e);
        }
    }

    /** Read a list of sequences from a serialized file **/
    public static List<ExecutableSequence> read_sequences(String filename) {

        // Read the list of sequences from the serialized file
        List<ExecutableSequence> seqs = null;
        try {
            FileInputStream fileis = new FileInputStream(filename);
            ObjectInputStream objectis = new ObjectInputStream(
                    new GZIPInputStream(fileis));
            @SuppressWarnings("unchecked")
            List<ExecutableSequence> seqs_tmp = (List<ExecutableSequence>) objectis
                    .readObject();
            seqs = seqs_tmp;
            objectis.close();
            fileis.close();
        }
        catch (Exception e) {
            throw new Error(e);
        }

        return seqs;
    }

    /** Write out a serialized file of sequences **/
    public static void write_sequences(List<ExecutableSequence> seqs,
            String outfile) {

        // dump_seqs ("write_sequences", seqs);

        try {
            FileOutputStream fileos = new FileOutputStream(outfile);
            ObjectOutputStream objectos = new ObjectOutputStream(
                    new GZIPOutputStream(fileos));
            System.out.printf(" Saving %d sequences to %s%n", seqs.size(),
                    outfile);
            objectos.writeObject(seqs);
            objectos.close();
            fileos.close();
        }
        catch (Exception e) {
            throw new Error(e);
        }
        System.out.printf("Finished saving sequences%n");
    }

    /** Print out usage error and stack trace and then exit **/
    static void usage(Throwable t, String format, Object... args) {

        System.out.print("ERROR: ");
        System.out.printf(format, args);
        System.out.println();
        System.out.println(options.usage());
        if (t != null)
            t.printStackTrace();
        System.exit(-1);
    }

    static void usage(String format, Object... args) {
        usage(null, format, args);
    }

    public static void dump_seqs(String msg, List<ExecutableSequence> seqs) {

        /*
         * if (false) { System.out.printf ("Sequences at %s\n", msg); for (int
         * seq_no = 0; seq_no < seqs.size(); seq_no++) System.out.printf
         * ("seq %d [%08X]:\n %s\n", seq_no, seqs.get(seq_no).seq_id(),
         * seqs.get(seq_no).toCodeString());
         * 
         * }
         */
    }
}
