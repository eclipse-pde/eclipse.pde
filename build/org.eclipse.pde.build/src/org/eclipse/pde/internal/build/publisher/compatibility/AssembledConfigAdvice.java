/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher.compatibility;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.eclipse.IConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IExecutableAdvice;
import org.eclipse.osgi.service.environment.Constants;

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

	@Override
	public BundleInfo[] getBundles() {
		return configData.getBundles();
	}

	@Override
	public Map<String, String> getProperties() {
		Properties configProps = configData.getProperties();
		Map<String, String> props = new HashMap<>(configProps.size() + 1);
		CollectionUtils.putAll(configProps, props);
		int startLevel = configData.getInitialBundleStartLevel();
		if (startLevel != BundleInfo.NO_LEVEL) {
			props.put("osgi.bundles.defaultStartLevel", String.valueOf(startLevel)); //$NON-NLS-1$
		}
		return props;
	}

	@Override
	public boolean isApplicable(String spec, boolean includeDefault, String id, Version version) {
		return configSpec.equals(spec);
	}

	private File getLauncher(File root) {
		if (launcherName == null) {
			launcherName = "eclipse"; //$NON-NLS-1$
		}
		if (configSpec.indexOf(Constants.OS_MACOSX) > 0) {
			File launcher = new File(root, launcherName + ".app/Contents/MacOS/" + launcherName); //$NON-NLS-1$
			if (!launcher.exists()) {
				//try upcase first letter
				return new File(root, Character.toUpperCase(launcherName.charAt(0)) + launcherName.substring(1) + ".app/Contents/MacOS/" + launcherName); //$NON-NLS-1$
			}
			return launcher;
		}
		if (configSpec.indexOf("win32") > 0) { //$NON-NLS-1$
			return new File(root, launcherName + ".exe"); //$NON-NLS-1$
		}
		return new File(root, launcherName);
	}

	private void initializeData(File configRoot) {
		DataLoader loader = new DataLoader(new File(configRoot, "configuration/config.ini"), getLauncher(configRoot)); //$NON-NLS-1$
		configData = loader.getConfigData();
		launcherData = loader.getLauncherData();
	}

	@Override
	public String getExecutableName() {
		return (launcherName != null) ? launcherName : "eclipse"; //$NON-NLS-1$
	}

	@Override
	public String[] getProgramArguments() {
		return (launcherData != null) ? launcherData.getProgramArgs() : new String[0];
	}

	@Override
	public String[] getVMArguments() {
		return (launcherData != null) ? launcherData.getJvmArgs() : new String[0];
	}
}
