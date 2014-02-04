package arbitcheck;

import java.util.ArrayList;
import java.util.List;

public final class Main {
    private final List<String> mArgs;
    private final TestTargeter mTestTargeter;
    private final List<String> mTargetClasses;

    private Main(List<String> args, String userClasspath) {
        mArgs = args;
        mTestTargeter = new TestTargeter(ClassTree.fromClasspath(userClasspath));
        mTargetClasses = new ArrayList<String>();
    }

    private void runCheck(String property) {
        mTargetClasses.addAll(mTestTargeter.determineTarget(SequenceUtil
                .getMethod(property)));
        invokeRandoop(property);
    }

    private void invokeRandoop(String property) {
        List<String> args = new ArrayList<String>();
        args.add("gentests");
        args.addAll(mArgs);
        args.add("--property");
        args.add(property);
        args.add("--junit_output_dir");
        args.add("gentests");
        args.add("--junit_classname");
        args.add(getSimpleClassName(property) + "Test");
        args.add("--junit_package_name");
        args.add(getPackageName(property));
        args.add("--junit_methodname");
        args.add("test"
                + upperCamel(property.substring(property.lastIndexOf('.') + 1)));
        for (String className : mTargetClasses) {
            System.err.println(className);
            args.add("--testclass");
            args.add(className);
        }
        randoop.main.Main.main(args.toArray(new String[0]));
    }

    private String getPackageName(String property) {
        String full = property.substring(0, property.lastIndexOf('.'));
        if (full.indexOf('.') == -1)
            return "";
        return full.substring(0, full.lastIndexOf('.'));
    }

    private String getSimpleClassName(String property) {
        String full = property.substring(0, property.lastIndexOf('.'));
        if (full.indexOf('.') == -1)
            return full;
        return full.substring(full.lastIndexOf('.') + 1);
    }

    private String upperCamel(String s) {
        if (s.length() <= 1)
            return s.toUpperCase();
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err
                    .println("usage: java arbitcheck.Main classname userclasspath");
            System.err
                    .println("  Note: the target class must be on classpath!");
            System.exit(1);
        }
        String property = args[0];
        String userClasspath = args[1];
        List<String> newargs = new ArrayList<String>();
        for (int i = 2; i < args.length; i++) {
            newargs.add(args[i]);
        }
        new Main(newargs, userClasspath).runCheck(property);
    }
}
