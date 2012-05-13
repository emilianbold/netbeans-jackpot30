package test;

public class App  {
    private static Object LOCK = new Object();

    public static void main( String[] args ) {
        synchronized (LOCK) {
            System.err.println("underLock");
        }
    }
}
