/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.tests;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;

public class BuildConfiguration {
	public static final String TRUE = "true";
	public static final String FALSE = "false";

	private static Properties defaultBuildConfig = new Properties();
	private static Properties defaultScriptConfig = new Properties();
	static {
		// use the template build.properties from pde.build
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/templates/headless-build/build.properties"), null);
		try {
			String buildPropertiesPath = FileLocator.toFileURL(resource).getPath();
			defaultBuildConfig.load(new BufferedInputStream(new FileInputStream(buildPropertiesPath)));
		} catch (IOException e) {
		}

		// just a few changes from the template
		defaultBuildConfig.put("archiveNamePrefix", "eclipse");
		String baseLocation = Platform.getInstallLocation().getURL().getPath();
		defaultBuildConfig.put("base", new File(baseLocation).getParent());
		defaultBuildConfig.put("baseLocation", baseLocation);
		defaultBuildConfig.put("baseos", Platform.getOS());
		defaultBuildConfig.put("basews", Platform.getWS());
		defaultBuildConfig.put("basearch", Platform.getOSArch());
		defaultBuildConfig.put("filterP2Base", TRUE);
		defaultBuildConfig.put("bootclasspath", System.getProperty("sun.boot.class.path"));
		defaultScriptConfig = (Properties) defaultBuildConfig.clone();
		if (!defaultBuildConfig.containsKey("configs"))
			defaultScriptConfig.put("configs", "*,*,*");
		if (!defaultBuildConfig.containsKey("buildingOSGi"))
			defaultScriptConfig.put("buildingOSGi", TRUE);
		if (!defaultBuildConfig.containsKey("signJars"))
			defaultScriptConfig.put("signJars", FALSE);
		if (!defaultBuildConfig.containsKey("generateFeatureVersionSuffix"))
			defaultScriptConfig.put("generateFeatureVersionSuffix", FALSE);
		if (!defaultBuildConfig.containsKey("generateVersionsLists"))
			defaultScriptConfig.put("generateVersionsLists", TRUE);
		if (!defaultBuildConfig.containsKey("groupConfigurations"))
			defaultScriptConfig.put("groupConfigurations", FALSE);
		if (!defaultBuildConfig.containsKey("pluginPath"))
			defaultScriptConfig.put("pluginPath", "");
		if (!defaultBuildConfig.containsKey("filteredDependencyCheck"))
			defaultScriptConfig.put("filteredDependencyCheck", FALSE);
	}

	private static Properties getBuildConfig() {
		Properties properties = new Properties();
		Enumeration e = defaultBuildConfig.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			properties.put(key, defaultBuildConfig.get(key));
		}

		return properties;
	}

	/**
	 * Get a default build configuration build.properties based on the template from pde.build
	 * @param buildFolder
	 * @return
	 */
	public static Properties getBuilderProperties(IFolder buildFolder) {
		String builder = buildFolder.getLocation().toOSString();
		Properties builderProperties = getBuildConfig();
		builderProperties.put("buildDirectory", builder);
		builderProperties.put("builder", builder);
		builderProperties.put(IBuildPropertiesConstants.PROPERTY_SUPPRESS_RESOLUTION_ERRORS, TRUE);
		return builderProperties;
	}

	/**
	 * Get a default set of properties used for invoking the genericTargets/generateScript task 
	 * (which invokes the eclipse.buildScript target)
	 * @param buildFolder
	 * @param type
	 * @param id
	 * @return
	 */
	public static Properties getScriptGenerationProperties(IFolder buildFolder, String type, String id) {
		Properties properties = (Properties) defaultScriptConfig.clone();
		properties.put("type", type);
		properties.put("id", id);
		properties.put("buildDirectory", buildFolder.getLocation().toOSString());
		properties.put(IBuildPropertiesConstants.PROPERTY_SUPPRESS_RESOLUTION_ERRORS, TRUE);
		return properties;
	}
}
