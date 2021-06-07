package ca.kieve.sologjm.debug;

public class DebugUtils {
    private DebugUtils() {
        // Do not instantiate
    }

    /**
     * Print the properties of an object.
     */
    public static void p(Object object) {
        if (object == null) return;
        System.out.println(new Dumper.Column(object, ""));
    }
}
