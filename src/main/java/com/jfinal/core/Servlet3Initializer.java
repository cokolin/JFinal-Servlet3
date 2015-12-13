package com.jfinal.core;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

/**
 * extend this class, register JFinalFilter for Servler3.x web server
 * 
 * @author cokolin
 * @see org.springframework.web.SpringServletContainerInitializer since 3.2.x
 */
@HandlesTypes(JFinalInitializer.class)
public class Servlet3Initializer implements ServletContainerInitializer {

	@Override
	public void onStartup(Set<Class<?>> cs, ServletContext ctx) throws ServletException {

		List<JFinalInitializer> initializers = new LinkedList<JFinalInitializer>();

		if (cs != null) {
			for (Class<?> clas : cs) {
				if (!clas.isInterface() && !Modifier.isAbstract(clas.getModifiers())
						&& JFinalInitializer.class.isAssignableFrom(clas)) {
					try {
						initializers.add((JFinalInitializer) clas.newInstance());
					} catch (Throwable ex) {
						throw new ServletException("Failed to instantiate JFinalInitializer class", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			ctx.log("No JFinalInitializer types detected on classpath");
			return;
		}

		ctx.log("JFinalInitializers detected on classpath: " + initializers);

		for (JFinalInitializer initializer : initializers) {
			initializer.onStartup(ctx);
		}
	}

}
