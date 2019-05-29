//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.servlet.listener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Utility Methods for manual execution of {@link javax.servlet.ServletContainerInitializer} when
 * using Embedded Jetty.
 */
public final class ContainerInitializer
{
    /**
     * Utility Method to allow for manual execution of {@link javax.servlet.ServletContainerInitializer} when
     * using Embedded Jetty.
     *
     * <code>
     * ServletContextHandler context = new ServletContextHandler();
     * ServletContainerInitializer corpSci = new MyCorporateSCI();
     * context.addEventListener(ContainerInitializer.asContextListener(corpSci));
     * </code>
     *
     * <p>
     * The {@link ServletContainerInitializer} will have its {@link ServletContainerInitializer#onStartup(Set, ServletContext)}
     * method called with the manually configured list of {@code Set<Class<?>> c} set.
     * In other words, this usage does not perform bytecode or annotation scanning against the classes in
     * your {@code ServletContextHandler} or {@code WebAppContext}.
     * </p>
     *
     * @param sci the {@link ServletContainerInitializer} to call
     * @return the {@link ServletContextListener} wrapping the SCI
     * @see {@link SCIAsContextListener#addClasses(Class[])}
     * @see {@link SCIAsContextListener#addClasses(String...)}
     * @see {@link SCIAsContextListener#setClassLoader(ClassLoader)}
     */
    public static SCIAsContextListener asContextListener(ServletContainerInitializer sci)
    {
        return new SCIAsContextListener(sci);
    }

    public static class SCIAsContextListener implements ServletContextListener
    {
        private final ServletContainerInitializer sci;
        private ClassLoader classLoader;
        private Set<String> classNames;
        private Set<Class<?>> classes = new HashSet<>();
        private Consumer<ServletContext> initConsumer;

        public SCIAsContextListener(ServletContainerInitializer sci)
        {
            this.sci = sci;
        }

        public SCIAsContextListener setClassLoader(ClassLoader classLoader)
        {
            this.classLoader = classLoader;
            return this;
        }

        public SCIAsContextListener addClasses(String... classNames)
        {
            if (this.classNames == null)
            {
                this.classNames = new HashSet<>();
            }
            this.classNames.addAll(Arrays.asList(classNames));
            return this;
        }

        public SCIAsContextListener addClasses(Class<?>... classes)
        {
            this.classes.addAll(Arrays.asList(classes));
            return this;
        }

        public SCIAsContextListener setInitConsumer(Consumer<ServletContext> consumer)
        {
            this.initConsumer = consumer;
            return this;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce)
        {
            ServletContext servletContext = sce.getServletContext();
            try
            {
                sci.onStartup(getClasses(), servletContext);
                if (initConsumer != null)
                {
                    initConsumer.accept(servletContext);
                }
            }
            catch (RuntimeException rte)
            {
                throw rte;
            }
            catch (Throwable cause)
            {
                throw new RuntimeException(cause);
            }
        }

        public Set<Class<?>> getClasses()
        {
            if (classNames != null && !classNames.isEmpty())
            {
                ClassLoader cl = this.classLoader;
                if (cl == null)
                {
                    cl = Thread.currentThread().getContextClassLoader();
                }

                for (String className : classNames)
                {
                    try
                    {
                        Class<?> clazz = cl.loadClass(className);
                        classes.add(clazz);
                    }
                    catch (ClassNotFoundException e)
                    {
                        throw new RuntimeException("Unable to find class: " + className, e);
                    }
                }
            }

            return classes;
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce)
        {
            // ignore
        }
    }
}
