/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.BundleValidationOperation;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.osgi.framework.Constants;

public abstract class LaunchValidationOperation implements IWorkspaceRunnable {

	private BundleValidationOperation fOperation;
	protected ILaunchConfiguration fLaunchConfiguration;

	public LaunchValidationOperation(ILaunchConfiguration configuration) {
		fLaunchConfiguration = configuration;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		fOperation = new BundleValidationOperation(getModels(), getPlatformProperties());
		fOperation.run(monitor);
	}

	protected abstract IPluginModelBase[] getModels() throws CoreException;

	protected Dictionary[] getPlatformProperties() throws CoreException {
		IExecutionEnvironment[] envs = getMatchingEnvironments();
		if (envs.length == 0)
			return new Dictionary[] {TargetPlatformHelper.getTargetEnvironment()};

		// add java profiles for those EE's that have a .profile file in the current system bundle
		ArrayList result = new ArrayList(envs.length);
		for (int i = 0; i < envs.length; i++) {
			Properties profileProps = getJavaProfileProperties(envs[i].getId());
			if (profileProps != null) {
				Dictionary props = TargetPlatformHelper.getTargetEnvironment();
				String systemPackages = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
				if (systemPackages != null)
					props.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackages);
				String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
				if (ee != null)
					props.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
				result.add(props);
			}
		}
		if (result.size() > 0)
			return (Dictionary[]) result.toArray(new Dictionary[result.size()]);
		return new Dictionary[] {TargetPlatformHelper.getTargetEnvironment()};

	}

	protected IExecutionEnvironment[] getMatchingEnvironments() throws CoreException {
		IVMInstall install = VMHelper.getVMInstall(fLaunchConfiguration);
		if (install == null)
			return new IExecutionEnvironment[0];

		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		List result = new ArrayList(envs.length);
		for (int i = 0; i < envs.length; i++) {
			IExecutionEnvironment env = envs[i];
			IVMInstall[] compatible = env.getCompatibleVMs();
			for (int j = 0; j < compatible.length; j++) {
				if (compatible[j].equals(install)) {
					result.add(env);
					break;
				}
			}
		}
		return (IExecutionEnvironment[]) result.toArray(new IExecutionEnvironment[result.size()]);
	}

	private Properties getJavaProfileProperties(String ee) {
		IPluginModelBase model = PluginRegistry.findModel("system.bundle"); //$NON-NLS-1$
		if (model == null)
			return null;

		File location = new File(model.getInstallLocation());
		String filename = ee.replace('/', '_') + ".profile"; //$NON-NLS-1$
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			// find the input stream to the profile properties file
			if (location.isDirectory()) {
				File file = new File(location, filename);
				if (file.exists())
					is = new FileInputStream(file);
			} else {
				zipFile = null;
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(filename);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			if (is != null) {
				Properties profile = new Properties();
				profile.load(is);
				return profile;
			}
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return null;
	}

	public boolean hasErrors() {
		return fOperation.hasErrors();
	}

	public Map getInput() {
		return fOperation.getResolverErrors();
	}

	public boolean isEmpty() {
		return fOperation.getState().getHighestBundleId() == -1;
	}

	protected State getState() {
		return fOperation.getState();
	}

}
