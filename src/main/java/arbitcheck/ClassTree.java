package arbitcheck;

import java.io.IOException;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassTree {
    private final Map<Class<?>, Node> mNodes;

    public ClassTree(List<String> classNames) {
        mNodes = new HashMap<Class<?>, ClassTree.Node>();
        read(classNames);
        build();
    }

    private void read(List<String> classNames) {
        for (String name : classNames) {
            try {
                Class<?> clazz = Class.forName(name);
                addClass(clazz);
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void addClass(Class<?> clazz) {
        if (clazz == null)
            return;
        if (!mNodes.containsKey(clazz)) {
            mNodes.put(clazz, new Node(clazz));
            addClass(clazz.getSuperclass());
            for (Class<?> i : clazz.getInterfaces())
                addClass(i);
            for (Constructor<?> m : clazz.getConstructors()) {
                for (Class<?> c : m.getParameterTypes())
                    addClass(c);
                for (Class<?> c : m.getExceptionTypes())
                    addClass(c);
            }
            for (Method m : clazz.getMethods()) {
                addClass(m.getReturnType());
                for (Class<?> c : m.getParameterTypes())
                    addClass(c);
                for (Class<?> c : m.getExceptionTypes())
                    addClass(c);
            }
        }
    }

    private void build() {
        for (Node node : mNodes.values()) {
            Class<?> clazz = node.getClazz();
            if (clazz.getSuperclass() != null) {
                Node parent = mNodes.get(clazz.getSuperclass());
                parent.addChild(node);
                node.addParent(parent);
            }
            for (Class<?> ifx : clazz.getInterfaces()) {
                Node parent = mNodes.get(ifx);
                parent.addChild(node);
                node.addParent(parent);
            }
        }
    }

    public Set<Class<?>> getAllClasses() {
        return mNodes.keySet();
    }

    public Set<Class<?>> getAllChildren(Class<?> clazz, boolean containsThis) {
        if (!mNodes.containsKey(clazz))
            throw new IllegalArgumentException("Unknown class "
                    + clazz.getName());
        return mNodes.get(clazz).getAllChildren(containsThis);
    }

    public Set<Class<?>> getAllParents(Class<?> clazz, boolean containsThis) {
        if (!mNodes.containsKey(clazz))
            throw new IllegalArgumentException("Unknown class "
                    + clazz.getName());
        return mNodes.get(clazz).getAllParents(containsThis);
    }

    public static List<Function> getFunctions(Class<?> clazz) {
        List<Function> functions = new ArrayList<Function>();
        for (Constructor<?> constructor : clazz.getConstructors()) {
            functions.add(new AConstructor(constructor));
        }
        for (Method method : clazz.getMethods()) {
            functions.add(new AMethod(method));
        }
        return functions;
    }

    public static boolean isInstantiatable(Class<?> clazz) {
        if (!Modifier.isPublic(clazz.getModifiers()))
            return false;
        if (clazz.isInterface())
            return false;
        if (Modifier.isAbstract(clazz.getModifiers()))
            return false;
        return true;
    }

    public static boolean hasLiteral(Class<?> clazz) {
        if (clazz.isArray()) {
            return hasLiteral(clazz.getComponentType());
        }
        if (clazz.isPrimitive())
            return true;
        if (Boolean.class.equals(clazz))
            return true;
        if (Byte.class.equals(clazz))
            return true;
        if (Short.class.equals(clazz))
            return true;
        if (Integer.class.equals(clazz))
            return true;
        if (Long.class.equals(clazz))
            return true;
        if (Character.class.equals(clazz))
            return true;
        if (Float.class.equals(clazz))
            return true;
        if (Double.class.equals(clazz))
            return true;
        // not true primitive, but randoop knows how to create them
        if (String.class.equals(clazz))
            return true;
        if (Object.class.equals(clazz))
            return true;
        return false;
    }

    public static ClassTree fromClasspath(String classpath) {
        List<String> classNames = new ArrayList<String>();
        for (String path : classpath.split(":")) {
            classNames.addAll(listClasses(path));
        }
        return new ClassTree(classNames);
    }

    public static List<String> listClasses(String path) {
        try {
            if (path.endsWith(".jar")) {
                return listClasses(new JarFile(path));
            }
            else {
                return listClasses(new File(path), null);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static List<String> listClasses(JarFile jar) throws IOException {
        List<String> classes = new ArrayList<String>();
        Enumeration<JarEntry> en = jar.entries();
        while (en.hasMoreElements()) {
            JarEntry entry = en.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                classes.add(name.substring(0,
                        name.length() - ".class".length())
                        .replace('/', '.'));
            }
        }
        return classes;
    }

    public static List<String> listClasses(File file, String name) throws IOException {
        List<String> classes = new ArrayList<String>();
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                String childName =
                    (name == null ? "" : name + ".") + child.getName();
                classes.addAll(listClasses(child, childName));
            }
        }
        else {
            if (name.endsWith(".class")) {
                classes.add(name.substring(0, name.length() - ".class".length()));
            }
        }
        return classes;
    }

    private static class Node {
        private final Class<?> mClass;
        private final Set<Node> mChildren;
        private final Set<Node> mParents;

        public Node(Class<?> cls) {
            mClass = cls;
            mChildren = new HashSet<ClassTree.Node>();
            mParents = new HashSet<ClassTree.Node>();
        }

        public Class<?> getClazz() {
            return mClass;
        }

        public void addChild(Node node) {
            mChildren.add(node);
        }

        public void addParent(Node node) {
            mParents.add(node);
        }

        public Set<Class<?>> getAllChildren(boolean containsThis) {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            if (containsThis)
                classes.add(mClass);
            for (Node node : mChildren)
                classes.addAll(node.getAllChildren(true));
            return classes;
        }

        public Set<Class<?>> getAllParents(boolean containsThis) {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            if (containsThis)
                classes.add(mClass);
            for (Node node : mParents)
                classes.addAll(node.getAllParents(true));
            return classes;
        }
    }
}
