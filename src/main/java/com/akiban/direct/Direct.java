package com.akiban.direct;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO - Total hack that this is static - need to a way to get this into the
 * context for JDBCResultSet.
 * 
 * @author peter
 * 
 */
public class Direct {

    private final static Map<Class<?>, Class<? extends AbstractDirectObject>> classMap = new HashMap<>();
    private final static ThreadLocal<Map<Class<?>, AbstractDirectObject>> instanceMap = new ThreadLocal<Map<Class<?>, AbstractDirectObject>>() {

        @Override
        protected Map<Class<?>, AbstractDirectObject> initialValue() {
            return new HashMap<Class<?>, AbstractDirectObject>();
        }
    };
    
    private final static ThreadLocal<DirectContextImpl> contextThreadLocal = new ThreadLocal<>();

    public static void registerDirectObjectClass(final Class<?> iface, final Class<? extends AbstractDirectObject> impl) {
        classMap.put(iface, impl);
    }

    /**
     * TODO - for now this clears everything!
     */

    public static void unregisterDirectObjectClasses() {
        classMap.clear();
        instanceMap.remove();

    }

    /**
     * Return a thread-private instance of an entity object of the registered
     * for a given Row, or null if there is none.
     */
    public static AbstractDirectObject objectForRow(final Class<?> c) {
        AbstractDirectObject o = instanceMap.get().get(c);
        if (o == null) {
            try {
                Class<? extends AbstractDirectObject> cl = classMap.get(c);
                o = cl.newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassCastException e) {
                throw new RuntimeException(e);
            }
            if (o != null) {
                instanceMap.get().put(c, o);
            }
        }
        return o;
    }
    
    public static void enter(DirectContextImpl dc) {
        contextThreadLocal.set(dc);
        dc.enter();
    }
    
    public static DirectContextImpl getContext() {
        return contextThreadLocal.get();
    }
    
    public static void leave() {
        DirectContextImpl dc = contextThreadLocal.get();
        contextThreadLocal.remove();
        assert dc != null : "enter() was not called before leave()";
        dc.leave();
    }

}
