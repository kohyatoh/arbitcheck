package randoop;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import randoop.experimental.SequencePrettyPrinter;
import randoop.main.GenInputsAbstract;
import randoop.util.CollectionsExt;
import randoop.util.Log;

/**
 * Outputs a collection of sequences as Java files, using the JUnit framework,
 * with one method per sequence.
 */
public class JunitFileWriter {

    // The class of the main JUnit suite, and the prefix of the subsuite names.
    public String junitDriverClassName;

    // The prefix of test method names
    public String junitMethodName;

    // The package name of the main JUnit suite
    public String packageName;

    // The directory where the JUnit files should be written to.
    private String dirName;

    public static boolean includeParseableString = false;

    private int testsPerFile;

    private Map<String, List<List<ExecutableSequence>>> createdSequencesAndClasses = new LinkedHashMap<String, List<List<ExecutableSequence>>>();

    public JunitFileWriter(String junitDirName, String packageName,
            String junitDriverClassName, String junitMethodName,
            int testsPerFile) {
        this.dirName = junitDirName;
        this.packageName = packageName;
        this.junitDriverClassName = junitDriverClassName;
        this.junitMethodName = junitMethodName;
        this.testsPerFile = testsPerFile;
    }

    public static File createJunitTestFile(String junitOutputDir,
            String packageName, ExecutableSequence es, String className,
            String methodName) {
        JunitFileWriter writer = new JunitFileWriter(junitOutputDir,
                packageName, "dummy", "dummy", 1);
        writer.createOutputDir();
        return writer.writeSubSuite(Collections.singletonList(es), className,
                methodName);
    }

    /**
     * Creates Junit tests for the faults. Output is a set of .java files.
     */
    public List<File> createJunitTestFiles(List<ExecutableSequence> sequences,
            String junitTestsClassName, String junitTestsMethodName) {
        if (sequences.size() == 0) {
            System.out
                    .println("No sequences given to createJunitFiles. No Junit class created.");
            return new ArrayList<File>();
        }

        createOutputDir();

        List<File> ret = new ArrayList<File>();
        List<List<ExecutableSequence>> subSuites = CollectionsExt
                .<ExecutableSequence> chunkUp(
                        new ArrayList<ExecutableSequence>(sequences),
                        testsPerFile);
        for (int i = 0; i < subSuites.size(); i++) {
            ret.add(writeSubSuite(subSuites.get(i), junitTestsClassName + i,
                    junitTestsMethodName));
        }
        createdSequencesAndClasses.put(junitTestsClassName, subSuites);
        return ret;
    }

    private void createOutputDir() {
        File dir = getDir();
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new Error("Unable to create directory: "
                        + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Creates Junit tests for the faults. Output is a set of .java files.
     * 
     * the default junit class name is the driver class name + index
     */
    public List<File> createJunitTestFiles(List<ExecutableSequence> sequences) {
        return createJunitTestFiles(sequences, junitDriverClassName,
                junitMethodName);
    }

    /** create both the test files and the drivers for convenience **/
    public List<File> createJunitFiles(List<ExecutableSequence> sequences,
            List<Class<?>> allClasses) {
        List<File> ret = new ArrayList<File>();
        ret.addAll(createJunitTestFiles(sequences));
        ret.add(writeDriverFile(allClasses));
        return ret;
    }

    /** create both the test files and the drivers for convinience **/
    public List<File> createJunitFiles(List<ExecutableSequence> sequences) {
        List<File> ret = new ArrayList<File>();
        ret.addAll(createJunitTestFiles(sequences));
        ret.add(writeDriverFile());
        return ret;
    }

    private File writeSubSuite(List<ExecutableSequence> sequencesForOneFile,
            String junitTestsClassName, String junitTestsMethodName) {
        if (GenInputsAbstract.pretty_print) {
            SequencePrettyPrinter printer = new SequencePrettyPrinter(
                    sequencesForOneFile, packageName, junitTestsClassName,
                    junitTestsMethodName);
            return printer.createFile(getDir().getAbsolutePath());
        }
        throw new UnsupportedOperationException("use pretty_print");
    }

    // TODO document and move to util directory.
    public static String indent(String codeString) {
        StringBuilder indented = new StringBuilder();
        String[] lines = codeString.split(Globals.lineSep);
        for (String line : lines) {
            indented.append("    " + line + Globals.lineSep);
        }
        return indented.toString();
    }

    private static void outputPackageName(PrintStream out, String packageName) {
        boolean isDefaultPackage = packageName.length() == 0;
        if (!isDefaultPackage)
            out.println("package " + packageName + ";");
    }

    public File writeDriverFile() {
        return writeDriverFile(junitDriverClassName);
    }

    public File writeDriverFile(List<Class<?>> allClasses) {
        return writeDriverFile(junitDriverClassName);
    }

    /**
     * Creates Junit tests for the faults. Output is a set of .java files.
     */
    public File writeDriverFile(String driverClassName) {
        return writeDriverFile(getDir(), packageName, driverClassName,
                getJunitTestSuiteNames());
    }

    public List<String> getJunitTestSuiteNames() {
        List<String> junitTestSuites = new LinkedList<String>();
        for (String junitTestsClassName : createdSequencesAndClasses.keySet()) {
            int numSubSuites = createdSequencesAndClasses.get(
                    junitTestsClassName).size();
            for (int i = 0; i < numSubSuites; i++) {
                junitTestSuites.add(junitTestsClassName + i);
            }
        }
        return junitTestSuites;
    }

    public static File writeDriverFile(File dir, String packageName,
            String driverClassName, List<String> junitTestSuiteNames) {
        File file = new File(dir, driverClassName + ".java");
        PrintStream out = createTextOutputStream(file);
        try {
            outputPackageName(out, packageName);
            out.println("import org.junit.runner.RunWith;");
            out.println("import org.junit.runners.Suite;");
            out.println("import org.junit.runners.Suite.SuiteClasses;");
            out.println("");
            out.println("@RunWith(Suite.class)");
            out.print("@SuiteClasses(");
            for (int i = 0; i < junitTestSuiteNames.size(); i++) {
                if (i > 0)
                    out.println(", ");
                out.print(junitTestSuiteNames.get(i) + ".class");
            }
            out.println(")");
            out.println("public class " + driverClassName + " {");
            out.println("}");
        }
        finally {
            if (out != null)
                out.close();
        }
        return file;
    }

    public File getDir() {
        File dir = null;
        if (dirName == null || dirName.length() == 0)
            dir = new File(System.getProperty("user.dir"));
        else
            dir = new File(dirName);
        if (packageName == null)
            return dir;
        packageName = packageName.trim(); // Just in case.
        if (packageName.length() == 0)
            return dir;
        String[] split = packageName.split("\\.");
        for (String s : split) {
            dir = new File(dir, s);
        }
        return dir;
    }

    private static PrintStream createTextOutputStream(File file) {
        try {
            return new PrintStream(file);
        }
        catch (IOException e) {
            Log.out.println("Exception thrown while creating text print stream:"
                    + file.getName());
            e.printStackTrace();
            System.exit(1);
            throw new Error("This can't happen");
        }
    }

}
