/*
 * Copyright 2016 Florian Pirchner <florian.pirchner@gmail.com>, Sampsa Sohlman.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.osgi.portlet.liferay.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.vaadin.osgi.servlet.api.OSGiConstants;
import com.vaadin.osgi.servlet.provider.PushMode;
import com.vaadin.osgi.servlet.provider.PushTransport;

@ObjectClassDefinition(name = "Vaadin Portlet Config", description = "Configures the Vaadin Portlet")
public @interface Configuration {

	@AttributeDefinition(name = "config name", defaultValue = OSGiConstants.DEFAULT_CONFIG_NAME, description = "The unique name of the config. Add the property com.vaadin.config to the UI component to address the proper portlet instance.", required = true)
	String configName() default OSGiConstants.DEFAULT_CONFIG_NAME;

	@AttributeDefinition(name = "displayName", defaultValue = "My Portlet", description = "The displayName", required = false)
	String displayName() default "My Portlet";
	
	@AttributeDefinition(name = "description", defaultValue = "", description = "Description about the application", required = false)
	String description() default "";
	
	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "context path", defaultValue = "/", description = "The context path which should be created in the web server", required = false)
	String contextPath() default "/";

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "alias", defaultValue = "/", description = "Alias to access application", required = true)
	String alias() default "/";

	@AttributeDefinition(name = "widgetset", defaultValue = "", description = "The widgetset that should be used. Or empty for default widgetset.", required = false)
	String widgetset() default "";

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "pageTitle", defaultValue = "My Vaadin App", description = "The title of the application", required = false)
	String pageTitle() default "My Vaadin App";

	@AttributeDefinition(name = "theme", defaultValue = "valo", description = "The theme of the application", required = false)
	String theme() default "valo";

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "productionMode", description = "False, if debug mode is used. True for production mode.", required = false)
	boolean productionMode() default false;

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "heartbeatInterval", description = "The heartbeat interval in seconds. Enter 0 for Vaadin default. Or a negative value to turn of the heartbeat.", required = false)
	int heartbeatInterval() default 0;

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "resourceCacheTime", description = "The time a browser may cache a resource in seconds. Enter -1 for Vaadin default.", required = false)
	int resourceCacheTime() default -1;

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "pushMode", description = "Whether and how push should be used in Vaadin", required = false)
	PushMode pushMode() default PushMode.DISABLED;

	// TODO Sampsa - do you need this?
	@AttributeDefinition(name = "pushTransport", description = "The type of transport used with push", required = false)
	PushTransport pushTransport() default PushTransport.WEBSOCKET;

}
