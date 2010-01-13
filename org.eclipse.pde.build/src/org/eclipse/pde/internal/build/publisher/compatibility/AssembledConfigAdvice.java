/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher.compatibility;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.eclipse.IConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IExecutableAdvice;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

public class AssembledConfigAdvice implements IConfigAdvice, IExecutableAdvice {
	private String configSpec = null;
	private String launcherName = null;
	private LauncherData launcherData = null;
	private ConfigData configData = null;

	public AssembledConfigAdvice(String configSpec, File configRoot, String launcherName) {
		this.configSpec = configSpec;
		this.launcherName = launcherName;
		initializeData(configRoot);
	}

	public BundleInfo[] getBundles() {
		return configData.getBundles();
	}

	public Map getProperties() {
		Properties configProps = configData.getProperties();
		Map props = new HashMap(configProps.size() + 1);
		CollectionUtils.putAll(configProps, props);
		int startLevel = configData.getInitialBundleStartLevel();
		if (startLevel != BundleInfo.NO_LEVEL)
			props.put("osgi.bundles.defaultStartLevel", String.valueOf(startLevel)); //$NON-NLS-1$
		return props;
	}

	public boolean isApplicable(String spec, boolean includeDefault, String id, Version version) {
		return configSpec.equals(spec);
	}

	private File getLauncher(File root) {
		if (launcherName == null)
			launcherName = "eclipse"; //$NON-NLS-1$
		if (configSpec.indexOf("win32") > 0) //$NON-NLS-1$
			return new File(root, launcherName + ".exe"); //$NON-NLS-1$
		return new File(root, launcherName);
	}

	private void initializeData(File configRoot) {
		DataLoader loader = new DataLoader(new File(configRoot, "configuration/config.ini"), getLauncher(configRoot)); //$NON-NLS-1$
		configData = loader.getConfigData();
		normalizeBundleVersions(configData);
		launcherData = loader.getLauncherData();
	}

	private void normalizeBundleVersions(ConfigData data) {
		BundleInfo[] bundles = data.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			// If the bundle doesn't have a version set it to 0.0.0
			if (bundles[i].getVersion() == null)
				bundles[i].setVersion(IPDEBuildConstants.GENERIC_VERSION_NUMBER);
		}
	}

	public String getExecutableName() {
		return (launcherName != null) ? launcherName : "eclipse"; //$NON-NLS-1$
	}

	public String[] getProgramArguments() {
		return (launcherData != null) ? launcherData.getProgramArgs() : new String[0];
	}

	public String[] getVMArguments() {
		return (launcherData != null) ? launcherData.getJvmArgs() : new String[0];
	}
}
