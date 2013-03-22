
package com.akiban.server.service.servicemanager;

import com.akiban.server.error.CircularDependencyException;
import com.akiban.server.service.servicemanager.configuration.ServiceBinding;
import com.akiban.util.ArgumentValidation;
import com.akiban.util.Exceptions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.grapher.GrapherModule;
import com.google.inject.grapher.InjectorGrapher;
import com.google.inject.grapher.graphviz.GraphvizModule;
import com.google.inject.grapher.graphviz.GraphvizRenderer;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public final class Guicer {
    // Guicer interface

    public Collection<Class<?>> directlyRequiredClasses() {
        return directlyRequiredClasses;
    }

    public void stopAllServices(ServiceLifecycleActions<?> withActions) {
        try {
            stopServices(withActions, null);
        } catch (Exception e) {
            throw new RuntimeException("while stopping services", e);
        }
    }

    public <T> T get(Class<T> serviceClass, ServiceLifecycleActions<?> withActions) {
        final T instance = _injector.getInstance(serviceClass);
        return startService(serviceClass, instance, withActions);
    }

    public boolean serviceIsStarted(Class<?> serviceClass) {
        for (Object service : services) {
            if (serviceClass.isInstance(service))
                return true;
        }
        return false;
    }

    public boolean isRequired(Class<?> interfaceClass) {
        return directlyRequiredClasses.contains(interfaceClass);
    }

    /**
     * <p>Builds and returns a list of dependencies for an instance of the specified class. The list will include an
     * instance of the class itself, as well as any instances that root instance requires, directly or indirectly,
     * according to Guice constructor injection.</p>
     *
     * <p>The order of the resulting list is fully deterministic and guarantees that for any element N in the list,
     * if N depends on another element M, then M will also be in the list and will have an index greater than N's.
     * In other words, traversing the list in reverse order will guarantee that you see any class's dependency
     * before you see it.</p>
     *
     * <p>Each instance will appear exactly once; in other words, if there is an element N in the list,
     * there will be no element M such that {@code N == M} or such that
     * {@code N.getClass() == M.getClass()}</p>
     *
     * <p>More specifically, the order of the list is generated by doing a depth-first traversal of the dependency
     * graph, with each element's dependents visited in alphabetical order of their class names, and then removing
     * duplicates by doing a reverse traversal of the list. By that last point we mean that if N and M are duplicates,
     * and N.index > M.index, then we would keep N and discard M.</p>
     *
     * <p>This method returns a new list with each invocation; the caller is free to modify the returned list.</p>
     * @param rootClass the root class for which to get dependencies
     * @return a mutable list of dependency instances, including the instance specified by the root class, in an order
     * such that any element in the list always precedes all of its dependencies
     * @throws CircularDependencyException if a circular dependency is found
     */
    public List<?> dependenciesFor(Class<?> rootClass) {
        LinkedHashMap<Class<?>,Object> result = new LinkedHashMap<>(16, .75f, true);
        Deque<Object> dependents = new ArrayDeque<>();
        buildDependencies(rootClass, result, dependents);
        assert dependents.isEmpty() : dependents;
        return new ArrayList<>(result.values());
    }

    public void graph(String filename, Collection<Class<?>> roots) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter out = new PrintWriter(stringWriter);
            Injector injector = Guice.createInjector(new GraphvizModule(), new GrapherModule());
            GraphvizRenderer renderer = injector.getInstance(GraphvizRenderer.class);
            renderer.setOut(out);
            InjectorGrapher grapher = injector.getInstance(InjectorGrapher.class);
            grapher.of(_injector);
            grapher.rootedAt(roots.toArray(new Class<?>[roots.size()]));
            grapher.graph();
            out.flush();
            out.close();

            String dotText = stringWriter.toString();
            dotText = dotText.replace("invis", "solid");
            PrintWriter fileOut = new PrintWriter(uniqueFile(filename), "UTF-8");
            fileOut.print(dotText);
            fileOut.flush();
            fileOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File uniqueFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            int lastDot = filename.lastIndexOf('.');
            final String prefix, suffix;
            if (lastDot > 0) {
                prefix = filename.substring(0, lastDot);
                suffix = filename.substring(lastDot);
            } else {
                prefix = filename;
                suffix = "";
            }
            for (int i=1; file.exists(); ++i) {
                file = new File(prefix + i + suffix);
            }

        }
        return file;
    }

    // public class methods

    public static Guicer forServices(Collection<ServiceBinding> serviceBindings)
            throws ClassNotFoundException 
    {
        return forServices(null, null, serviceBindings, Collections.<String>emptyList(),
                Collections.<Module>emptyList());
    }

    public static <M extends ServiceManagerBase> Guicer forServices(Class<M> serviceManagerInterfaceClass,
                                                                    M serviceManager,
                                                                    Collection<ServiceBinding> serviceBindings,
                                                                    List<String> priorities,
                                                                    Collection<? extends Module> modules)
            throws ClassNotFoundException 
    {
        ArgumentValidation.notNull("bindings", serviceBindings);
        if (serviceManagerInterfaceClass != null) {
            if (!serviceManagerInterfaceClass.isInstance(serviceManager)) {
                throw new IllegalArgumentException(serviceManager + " is not a "
                                                   + serviceManagerInterfaceClass);
            }
        }
        return new Guicer(serviceManagerInterfaceClass, serviceManager, 
                          serviceBindings, priorities, modules);
    }

    // private methods

    private Guicer(Class<? extends ServiceManagerBase> serviceManagerInterfaceClass, ServiceManagerBase serviceManager,
                   Collection<ServiceBinding> serviceBindings, List<String> priorities,
                   Collection<? extends Module> modules)
    throws ClassNotFoundException
    {
        this.serviceManagerInterfaceClass = serviceManagerInterfaceClass;
        
        List<Class<?>> localDirectlyRequiredClasses = new ArrayList<>();
        List<ResolvedServiceBinding> resolvedServiceBindings = new ArrayList<>();

        for (ServiceBinding serviceBinding : serviceBindings) {
            ResolvedServiceBinding resolvedServiceBinding = new ResolvedServiceBinding(serviceBinding);
            resolvedServiceBindings.add(resolvedServiceBinding);
            if (serviceBinding.isDirectlyRequired()) {
                localDirectlyRequiredClasses.add(resolvedServiceBinding.serviceInterfaceClass());
            }
        }
        Collections.sort(localDirectlyRequiredClasses, BY_CLASS_NAME);
        // Pull to front in reverse order.
        for (int i = priorities.size() - 1; i >= 0; i--) {
            Class<?> clazz = Class.forName(priorities.get(i));
            if (localDirectlyRequiredClasses.remove(clazz)) {
                localDirectlyRequiredClasses.add(0, clazz);
            }
            else {
                throw new IllegalArgumentException("priority service " + priorities.get(i) + " is not a dependency");
            }
        }
        directlyRequiredClasses = Collections.unmodifiableCollection(localDirectlyRequiredClasses);

        this.services = Collections.synchronizedSet(new LinkedHashSet<>());

        AbstractModule module = new ServiceBindingsModule(serviceManagerInterfaceClass, serviceManager,
                                                          resolvedServiceBindings);
        List<Module> modulesList = new ArrayList<>(modules.size() + 1);
        modulesList.add(module);
        modulesList.addAll(modules);
        _injector = Guice.createInjector(modulesList.toArray(new Module[modulesList.size()]));
    }

    private void buildDependencies(Class<?> forClass, LinkedHashMap<Class<?>,Object> results, Deque<Object> dependents) {
        Object instance = _injector.getInstance(forClass);
        if (dependents.contains(instance)) {
            throw circularDependencyInjection(forClass, instance, dependents);
        }

        // Start building this object
        dependents.addLast(instance);

        Class<?> actualClass = instance.getClass();
        Object oldInstance = results.put(actualClass, instance);
        if (oldInstance != null) {
            assert oldInstance == instance : oldInstance + " != " + instance;
        }

        // Build the dependency list
        List<Class<?>> dependencyClasses = new ArrayList<>();
        for (Dependency<?> dependency : InjectionPoint.forConstructorOf(actualClass).getDependencies()) {
            dependencyClasses.add(dependency.getKey().getTypeLiteral().getRawType());
        }
        for (InjectionPoint injectionPoint : InjectionPoint.forInstanceMethodsAndFields(actualClass)) {
            for (Dependency<?> dependency : injectionPoint.getDependencies()) {
                dependencyClasses.add(dependency.getKey().getTypeLiteral().getRawType());
            }
        }
        for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(actualClass)) {
            for (Dependency<?> dependency : injectionPoint.getDependencies()) {
                dependencyClasses.add(dependency.getKey().getTypeLiteral().getRawType());
            }
        }

        // This dependency is already handled.
        dependencyClasses.remove(serviceManagerInterfaceClass);

        // Sort it and recursively invoke
        Collections.sort(dependencyClasses, BY_CLASS_NAME);
        for (Class<?> dependencyClass : dependencyClasses) {
            buildDependencies(dependencyClass, results, dependents);
        }

        // Done building the object; pop the deque and confirm the instance
        Object removed = dependents.removeLast();
        assert removed == instance : removed + " != " + instance;
    }

    private CircularDependencyException circularDependencyInjection(Class<?> forClass, Object instance, Deque<Object> dependents) {
        String forClassName = forClass.getSimpleName();
        List<String> classNames = new ArrayList<>();
        for (Object o : dependents) {
            classNames.add(o.getClass().getSimpleName());
        }
        classNames.add(instance.getClass().getSimpleName());
        return new CircularDependencyException (forClassName, classNames);
    }

    private <T,S> T startService(Class<T> serviceClass, T instance, ServiceLifecycleActions<S> withActions) {
        // quick check; startServiceIfApplicable will do this too, but this way we can avoid finding dependencies
        if (services.contains(instance)) {
            return instance;
        }
        synchronized (services) {
            for (Object dependency : reverse(dependenciesFor(serviceClass))) {
                startServiceIfApplicable(dependency, withActions);
            }
        }
        return instance;
    }

    private static <T> List<T> reverse(List<T> list) {
        Collections.reverse(list);
        return list;
    }

    private <T, S> void startServiceIfApplicable(T instance, ServiceLifecycleActions<S> withActions) {
        if (services.contains(instance)) {
            return;
        }
        if (withActions == null) {
            services.add(instance);
            return;
        }
        S service = withActions.castIfActionable(instance);
        if (service != null) {
            try {
                withActions.onStart(service);
                services.add(service);
            } catch (Exception e) {
                try {
                    stopServices(withActions, e);
                } catch (Exception e1) {
                    e = e1;
                }
                throw new ProvisionException("While starting service " + instance.getClass(), e);
            }
        }
    }

    private void stopServices(ServiceLifecycleActions<?> withActions, Exception initialCause) throws Exception {
        List<Throwable> exceptions = tryStopServices(withActions, initialCause);
        if (!exceptions.isEmpty()) {
            if (exceptions.size() == 1) {
                throw Exceptions.throwAlways(exceptions, 0);
            }
            for (Throwable t : exceptions) {
                t.printStackTrace();
            }
            Throwable cause = exceptions.get(0);
            throw new Exception("Failure(s) while shutting down services: " + exceptions, cause);
        }
    }

    private <S> List<Throwable> tryStopServices(ServiceLifecycleActions<S> withActions, Exception initialCause) {
        ListIterator<?> reverseIter;
        synchronized (services) {
            reverseIter = new ArrayList<>(services).listIterator(services.size());
        }
        List<Throwable> exceptions = new ArrayList<>();
        if (initialCause != null) {
            exceptions.add(initialCause);
        }
        while (reverseIter.hasPrevious()) {
            try {
                Object serviceObject = reverseIter.previous();
                services.remove(serviceObject);
                if (withActions != null) {
                    S service = withActions.castIfActionable(serviceObject);
                    if (service != null) {
                        withActions.onShutdown(service);
                    }
                }
            } catch (Throwable t) {
                exceptions.add(t);
            }
        }
        // TODO because our dependency graph is created via Service.start() invocations, if service A uses service B
        // in stop() but not start(), and service B has already been shut down, service B will be resurrected. Yuck.
        // I don't know of a good way around this, other than by formalizing our dependency graph via constructor
        // params (and thus removing ServiceManagerImpl.get() ). Until this is resolved, simplest is to just shrug
        // our shoulders and not check
//        synchronized (lock) {
//            assert services.isEmpty() : services;
//        }
        return exceptions;
    }

    // object state

    private final Class<? extends ServiceManagerBase> serviceManagerInterfaceClass;
    private final Collection<Class<?>> directlyRequiredClasses;
    private final Set<Object> services;
    private final Injector _injector;

    // consts

    private static final Comparator<? super Class<?>> BY_CLASS_NAME = new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    public List<Class<?>> servicesClassesInStartupOrder() {
        List<Class<?>> result = new ArrayList<>(services.size());
        for (Object service : services) {
            result.add(service.getClass());
        }
        return result;
    }

    // nested classes

    private static final class ResolvedServiceBinding {

        // ResolvedServiceBinding interface

        public Class<?> serviceInterfaceClass() {
            return serviceInterfaceClass;
        }

        public Class<?> serviceImplementationClass() {
            return serviceImplementationClass;
        }

        public ResolvedServiceBinding(ServiceBinding serviceBinding) throws ClassNotFoundException {
            ClassLoader loader = serviceBinding.getClassLoader();
            this.serviceInterfaceClass = Class.forName(serviceBinding.getInterfaceName(), true, loader);
            this.serviceImplementationClass = Class.forName(serviceBinding.getImplementingClassName(), true, loader);
            if (!this.serviceInterfaceClass.isAssignableFrom(this.serviceImplementationClass)) {
                throw new IllegalArgumentException(this.serviceInterfaceClass + " is not assignable from "
                        + this.serviceImplementationClass);
            }
        }

        // object state
        private final Class<?> serviceInterfaceClass;
        private final Class<?> serviceImplementationClass;
    }

    private static final class ServiceBindingsModule extends AbstractModule {
        @Override
        // we use unchecked, raw Class, relying on the invariant established by ResolvedServiceBinding's ctor
        @SuppressWarnings("unchecked")
        protected void configure() {
            if (serviceManagerInterfaceClass != null)
                bind((Class)serviceManagerInterfaceClass).toInstance(serviceManager);
            for (ResolvedServiceBinding binding : bindings) {
                Class unchecked = binding.serviceInterfaceClass();
                bind(unchecked).to(binding.serviceImplementationClass()).in(Scopes.SINGLETON);
            }
        }

        // ServiceBindingsModule interface

        private ServiceBindingsModule(Class<? extends ServiceManagerBase> serviceManagerInterfaceClass, ServiceManagerBase serviceManager,
                                      Collection<ResolvedServiceBinding> bindings)
        {
            this.serviceManagerInterfaceClass = serviceManagerInterfaceClass;
            this.serviceManager = serviceManager;
            this.bindings = bindings;
        }

        // object state

        private final Class<? extends ServiceManagerBase> serviceManagerInterfaceClass;
        private final ServiceManagerBase serviceManager;
        private final Collection<ResolvedServiceBinding> bindings;
    }

    static interface ServiceLifecycleActions<T> {
        void onStart(T service);
        void onShutdown(T service);

        /**
         * Cast the given object to the actionable type if possible, or return {@code null} otherwise.
         * @param object the object which may or may not be actionable
         * @return the object reference, correctly casted; or null
         */
        T castIfActionable(Object object);
    }
}
