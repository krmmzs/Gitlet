package gitlet;

import java.util.function.Supplier;

/**
 * Adapted from Guava Suppliers.memoize.
 * @param <T> Type of the value
 */
public class Lazy<T> implements Supplier {

    private volatile Supplier<T> delegate;
    private volatile boolean initialized;
    private T value;

    public Lazy(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        // It means that on this object only and only
        // one thread can excute the enclosed block at one time.
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    T t = delegate.get();
                    value = t;
                    initialized = true;
                    delegate = null;
                    return t;
                }
            }
        }
        return value;
    }
}
