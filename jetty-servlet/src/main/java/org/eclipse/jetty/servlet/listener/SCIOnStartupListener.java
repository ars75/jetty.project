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

import java.util.Collections;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.util.MultiException;

/**
 * Utility Method to allow for manual execution of {@link javax.servlet.ServletContainerInitializer} when
 * using Embedded Jetty.
 *
 * <code>
 * ServletContextHandler context = new ServletContextHandler();
 * ServletContainerInitializer corpSci = new MyCorporateSCI();
 * ServletContainerInitializer websocketSci = new JavaxWebSocketServletContainerInitializer();
 * context.addEventListener(new SCIOnStartupListener(corpSci, websocketSci));
 * </code>
 *
 * <p>
 * Note: each {@link ServletContainerInitializer} will have its {@link ServletContainerInitializer#onStartup(Set, ServletContext)}
 * method called with an empty {@code Set<Class<?>> c} set.   In other words, this usage does not perform bytecode or annotation
 * scanning against the classes in your {@code ServletContextHandler} or {@code WebAppContext}
 * </p>
 */
@SuppressWarnings("unused")
public class SCIOnStartupListener implements ServletContextListener
{
    private ServletContainerInitializer[] scis;

    public SCIOnStartupListener(ServletContainerInitializer... scis)
    {
        this.scis = scis;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        MultiException me = new MultiException();
        ServletContext servletContext = sce.getServletContext();
        Set<Class<?>> classSet = Collections.emptySet();
        for (ServletContainerInitializer sci : scis)
        {
            try
            {
                sci.onStartup(classSet, servletContext);
            }
            catch (Throwable cause)
            {
                me.add(cause);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
    }
}
