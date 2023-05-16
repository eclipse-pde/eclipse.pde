/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.tests;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;

public class BuildConfiguration {
	public static final String TRUE = "true";
	public static final String FALSE = "false";

	private static Properties defaultBuildConfig = new Properties();
	private static Properties defaultScriptConfig = new Properties();
	static {
		// use the template build.properties from pde.build
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"),
				IPath.fromOSString("/templates/headless-build/build.properties"), null);
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

		String version = System.getProperty("java.specification.version"); //$NON-NLS-1$
		boolean isJava9OrGreater = false;
		if (version != null) {
			String[] nums = version.split("\\."); //$NON-NLS-1$
			if (nums.length == 1) {
				// java 9 and above
				try {
					int major = Integer.parseInt(nums[0]);
					if (major >= 9) {
						isJava9OrGreater = true;
					}

				} catch (NumberFormatException e) {
				}
			}
		}
		if (isJava9OrGreater == false)
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
		Enumeration<Object> e = defaultBuildConfig.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			properties.put(key, defaultBuildConfig.get(key));
		}

		return properties;
	}

	/**
	 * Get a default build configuration build.properties based on the template from
	 * pde.build
	 * 
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
	 * Get a default set of properties used for invoking the
	 * genericTargets/generateScript task (which invokes the eclipse.buildScript
	 * target)
	 * 
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
