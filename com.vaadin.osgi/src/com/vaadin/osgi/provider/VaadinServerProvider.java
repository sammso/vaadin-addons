package com.vaadin.osgi.provider;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.osgi.api.Constants;
import com.vaadin.ui.UI;

import osgi.enroute.configurer.api.RequireConfigurerExtender;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = {
		"com.vaadin.osgi.provider" })
@Designate(ocd = Configuration.class, factory = true)
@RequireConfigurerExtender
public class VaadinServerProvider {

	static final Logger LOGGER = LoggerFactory.getLogger(StaticResources.class);

	Configuration config;
	ComponentContext context;

	ServiceRegistration<ServletContextHelper> contextReg;

	ServiceTracker<UI, ServiceObjects<UI>> uiTracker;

	ServiceRegistration<Servlet> servletReg;

	ServiceRegistration<Servlet> resourcesReg;

	@Activate
	void activate(Configuration config, ComponentContext context) {
		this.config = config;
		this.context = context;

		activateVaadinServer(config);
	}

	protected void activateVaadinServer(Configuration config) {

		uiTracker = new ServiceTracker<>(context.getBundleContext(), createUIFilter(config),
				new ServiceTrackerCustomizer<UI, ServiceObjects<UI>>() {
					@Override
					public ServiceObjects<UI> addingService(ServiceReference<UI> reference) {
						ServiceObjects<UI> serviceObjects = context.getBundleContext().getServiceObjects(reference);
						doActivateVaadinServer(serviceObjects);
						return serviceObjects;
					}

					@Override
					public void modifiedService(ServiceReference<UI> reference, ServiceObjects<UI> service) {

					}

					@Override
					public void removedService(ServiceReference<UI> reference, ServiceObjects<UI> service) {
						doDeactivateVaadinServer();
					}
				});
		uiTracker.open();
	}

	protected Filter createUIFilter(Configuration config) {
		String filter = String.format("(&(objectClass=com.vaadin.ui.UI)(%s=%s))", Constants.PROP__VAADIN_CONFIG,
				config.configName());
		try {
			return context.getBundleContext().createFilter(filter);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Activates the context path based on the config.
	 */
	protected void doActivateContextPath() {
		// Translate the config to proper http whiteboard properties
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, config.configName());
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, config.contextPath());

		// create a new helper service and pass in the http whiteboard
		// properties
		contextReg = context.getBundleContext().registerService(ServletContextHelper.class,
				new ServletContextHelperFactory() {
				}, properties);

		LOGGER.debug("Registered contextpath '" + config.contextPath() + "'");

	}

	/**
	 * Activates the static resources.
	 */
	protected void doActivateStaticResources() {
		// Translate the config to proper http whiteboard properties
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, config.configName()));
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, config.configName() + ".static.resources");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/VAADIN");

		resourcesReg = context.getBundleContext().registerService(Servlet.class,
				new StaticResources(context.getBundleContext()), properties);

		LOGGER.debug("Registered static resources '/VAADIN' under contextpath '" + config.contextPath() + "'");
	}

	/**
	 * Activates the Vaadin server based on the config.
	 * 
	 * @param serviceObjects
	 */
	protected void doActivateVaadinServer(ServiceObjects<UI> serviceObjects) {

		// first, do activate the context path
		doActivateContextPath();

		// activates the static resources servlet
		doActivateStaticResources();

		// activates the vaadin servlet
		doActivateVaadinServlet(serviceObjects);
	}

	protected void doActivateVaadinServlet(ServiceObjects<UI> serviceObjects) {
		OSGiServlet servlet = new OSGiServlet(new OSGiUIProvider(serviceObjects), config);
		// Translate the config to proper http whiteboard properties
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, config.configName()));
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, config.configName());
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, config.alias());

		servletReg = context.getBundleContext().registerService(Servlet.class, servlet, properties);

		LOGGER.debug("Registered Vaadin servlet with alias '" + config.alias() + "' under contextpath '"
				+ config.contextPath() + "'");
	}

	protected void doDeactivateVaadinServer() {
		if (resourcesReg != null) {
			resourcesReg.unregister();
			resourcesReg = null;
		}

		if (servletReg != null) {
			servletReg.unregister();
			servletReg = null;
		}
	}

	@Deactivate
	void deactivate() {
		uiTracker.close();
		uiTracker = null;

		if (resourcesReg != null) {
			resourcesReg.unregister();
			resourcesReg = null;
		}

		if (servletReg != null) {
			servletReg.unregister();
			servletReg = null;
		}

		contextReg.unregister();
		contextReg = null;
	}

	/**
	 * ServletContext needs to use <code>scope=bundle</code>. Every bundle which
	 * uses the ServletContext, requires its own instance. Otherwise static
	 * resources can not be found in different bundles.
	 */
	static class ServletContextHelperFactory implements ServiceFactory<ServletContextHelper> {

		@Override
		public ServletContextHelper getService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration) {
			return new InternalServletContextHelper();
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration,
				ServletContextHelper service) {

		}
	}

	static class InternalServletContextHelper extends ServletContextHelper {

	}
}