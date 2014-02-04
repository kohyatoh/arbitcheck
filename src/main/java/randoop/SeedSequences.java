package randoop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import randoop.util.PrimitiveTypes;
import randoop.util.Reflection;

/**
 * Provides functionality for creating a set of sequences that create a set of
 * primitive values. Used by sequence generators.
 */
public final class SeedSequences {
    private SeedSequences() {
        throw new IllegalStateException("no instance");
    }

    public static final List<Object> primitiveSeeds = Arrays.<Object> asList(
            (byte) (-1), (byte) 0, (byte) 1, (byte) 10, (byte) 100,
            (short) (-1), (short) 0, (short) 1, (short) 10, (short) 100, (-1),
            0, 1, 10, 100, (-1L), 0L, 1L, 10L, 100L, (float) -1.0, (float) 0.0,
            (float) 1.0, (float) 10.0, (float) 100.0, -1.0, 0.0, 1.0, 10.0,
            100.0, '#', ' ', '4', 'a', true, false, "", "hi!");

    /**
     * A set of sequences that create primitive values, e.g. int i = 0; or
     * String s = "hi";
     */
    public static Set<Sequence> defaultSeeds() {
        List<Object> seeds = new ArrayList<Object>(primitiveSeeds);
        return SeedSequences.objectsToSeeds(seeds);
    }

    /**
     * Precondition: objs consists exclusively of boxed primitives and strings.
     * Returns a set of sequences that create the given objects.
     */
    public static Set<Sequence> objectsToSeeds(Collection<Object> objs) {
        Set<Sequence> retval = new LinkedHashSet<Sequence>();
        for (Object o : objs) {
            retval.add(PrimitiveOrStringOrNullDecl.sequenceForPrimitive(o));
        }
        return retval;
    }

    public static Set<Object> getSeeds(Class<?> c) {
        Set<Object> result = new LinkedHashSet<Object>();
        for (Object seed : primitiveSeeds) {
            boolean seedOk = isOk(c, seed);
            if (seedOk)
                result.add(seed);
        }
        return result;
    }

    private static boolean isOk(Class<?> c, Object seed) {
        if (PrimitiveTypes.isBoxedPrimitiveTypeOrString(c)) {
            c = PrimitiveTypes.getUnboxType(c);
        }
        return Reflection.canBePassedAsArgument(seed, c);
    }

    /**
     * Inspects the declared fields of the given classes. If it finds fields
     * with a @TestValue annotation, ensures that the fields are static, public,
     * and declare a primitive type (or String), or an array of such types. It
     * returns a set of statement sequences corresponding to the values
     * collected from the annotated fields.
     * 
     * @param classes
     *            A collection of classes containing @TestValue annotation on
     *            one or more static fields.
     * @return A set of Sequences representing primitive values collected frome
     *         @TestValue-annotated fields in the given classes
     */
    public static Set<Sequence> getSeedsFromAnnotatedFields(Class<?>... classes) {

        // This list will store the primitive values (or Strings) obtained from
        // @TestValue fields.
        List<Object> primitives = new ArrayList<Object>();

        // Now we convert the values collected to sequences. We do this by
        // calling
        // the objectsToSeeds(List<Object>) method.
        //
        // There is a small wrinkle left: method objectsToSeeds(List<Object>)
        // doesn't admit null values.
        // Note that if there was a null value in the values we collected, it
        // must have comes from a
        // String field. In this case, we remove the null value, and it
        // afterwards.
        boolean nullString = primitives.remove(null);
        Set<Sequence> retval = objectsToSeeds(primitives);
        if (nullString) {
            // Add "String x = null" statement.
            retval.add(Sequence.create(PrimitiveOrStringOrNullDecl
                    .nullOrZeroDecl(String.class)));
        }
        return retval;
    }

}
