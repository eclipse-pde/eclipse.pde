/*******************************************************************************
 *  Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.BundleValidationOperation;
import org.eclipse.pde.internal.core.TargetPlatformHelper;

public class LaunchValidationOperation implements IWorkspaceRunnable {

	private BundleValidationOperation fOperation;
	protected final ILaunchConfiguration fLaunchConfiguration;
	protected final Set<IPluginModelBase> fModels;

	public LaunchValidationOperation(ILaunchConfiguration configuration, Set<IPluginModelBase> models) {
		fLaunchConfiguration = configuration;
		fModels = models;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		fOperation = new BundleValidationOperation(fModels, getPlatformProperties());
		fOperation.run(monitor);
	}

	@SuppressWarnings("unchecked")
	protected Dictionary<String, String>[] getPlatformProperties() throws CoreException {
		IExecutionEnvironment[] envs = getMatchingEnvironments();
		if (envs.length == 0) {
			return new Dictionary[] {TargetPlatformHelper.getTargetEnvironment()};
		}
		// add java profiles for those EE's that have a .profile file in the current system bundle
		List<Dictionary<String, String>> result = new ArrayList<>(envs.length);
		for (IExecutionEnvironment env : envs) {
			Properties profileProps = getJavaProfileProperties(env.getId());
			if (profileProps == null) {
				// Java10 onwards, we take profile via this method
				profileProps = env.getProfileProperties();
			}
			if (profileProps != null) {
				Dictionary<String, String> props = TargetPlatformHelper.getTargetEnvironment();
				TargetPlatformHelper.addEnvironmentProperties(props, env, profileProps);
				result.add(props);
			}
		}
		if (!result.isEmpty()) {
			return result.toArray(Dictionary[]::new);
		}
		return new Dictionary[] {TargetPlatformHelper.getTargetEnvironment()};
	}

	protected IExecutionEnvironment[] getMatchingEnvironments() throws CoreException {
		IVMInstall install = VMHelper.getVMInstall(fLaunchConfiguration);
		return install == null ? new IExecutionEnvironment[0] : getMatchingEEs(install);
	}

	static IExecutionEnvironment[] getMatchingEEs(IVMInstall install) {
		return Arrays.stream(JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments()) //
				.filter(env -> Arrays.stream(env.getCompatibleVMs()).anyMatch(install::equals)) //
				.toArray(IExecutionEnvironment[]::new);
	}

	private Properties getJavaProfileProperties(String ee) {
		IPluginModelBase model = PluginRegistry.findModel("system.bundle"); //$NON-NLS-1$
		if (model == null) {
			return null;
		}
		File location = new File(model.getInstallLocation());
		String filename = ee.replace('/', '_') + ".profile"; //$NON-NLS-1$
		try {
			// find the input stream to the profile properties file
			if (location.isDirectory()) {
				File file = new File(location, filename);
				if (file.exists())
					try (InputStream is = new FileInputStream(file)) {
						return loadProperties(is);
					}
			} else {
				try (ZipFile zipFile = new ZipFile(location, ZipFile.OPEN_READ)) {
					ZipEntry entry = zipFile.getEntry(filename);
					if (entry != null) {
						return loadProperties(zipFile.getInputStream(entry));
					}
				}
			}
		} catch (IOException e) {
			// nothing to do
		}
		return null;
	}

	private static Properties loadProperties(InputStream is) throws IOException {
		Properties profile = new Properties();
		profile.load(is);
		return profile;
	}

	public boolean hasErrors() {
		return fOperation.hasErrors();
	}

	public Map<Object, Object[]> getInput() {
		return fOperation.getResolverErrors();
	}

	public boolean isEmpty() {
		return fOperation.getState().getHighestBundleId() == -1;
	}

	protected State getState() {
		return fOperation.getState();
	}

}
