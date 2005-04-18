/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.framework.adaptor.core.AbstractFrameworkAdaptor;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.Version;

public class SWTLaunchConfiguration extends
		AbstractJavaLaunchConfigurationDelegate {

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		monitor.beginTask(MessageFormat.format("{0}...", new String[]{configuration.getName()}), 3); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		
		String mainTypeName = verifyMainTypeName(configuration);

		IVMInstall vm = verifyVMInstall(configuration);

		IVMRunner runner = vm.getVMRunner(mode);
		if (runner == null) {
			monitor.setCanceled(true);
		}

		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) {
			workingDirName = workingDir.getAbsolutePath();
		}
		
		// Environment variables
		String[] envp= DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		
		// Program & VM args
		String pgmArgs = getProgramArguments(configuration);
		String vmArgs = getVMArguments(configuration);
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);
		
		// Find SWT Fragment for the target platform
		BundleDescription fragment = findFragment();
		
		// VM-specific attributes
		Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
		
		// Classpath
		String[] classpath = getClasspath(fragment, configuration);
		
		// Create VM config
		VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
		runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
		runConfig.setEnvironment(envp);
		runConfig.setVMArguments(getVMArguments(fragment, execArgs));
		runConfig.setWorkingDirectory(workingDirName);
		runConfig.setVMSpecificAttributesMap(vmAttributesMap);

		// Bootpath
		runConfig.setBootClassPath(getBootpath(configuration));
		
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}		
		
		// stop in main
		prepareStopInMain(configuration);
		
		// done the verification phase
		monitor.worked(1);
		
		// set the default source locator if required
		setDefaultSourceLocator(launch, configuration);
		monitor.worked(1);		
		
		// Launch the configuration - 1 unit of work
		runner.run(runConfig, launch, monitor);
		
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}	
		
		monitor.done();
	}
	
	private String[] getVMArguments(BundleDescription fragment, ExecutionArguments execArgs) {
		if (fragment == null)
			return execArgs.getVMArgumentsArray();
		
		String location = getNativeLibrariesLocation(fragment);
		String[] vmArgs = execArgs.getVMArgumentsArray();
		for (int i = vmArgs.length - 1; i >= 0; i--) {
			if (vmArgs[i].startsWith("-Djava.library.path")) { //$NON-NLS-1$
				vmArgs[i] +=  ";" + location; //$NON-NLS-1$
				return vmArgs;
			}
		}
		String[] all = new String[vmArgs.length + 1];
		all[0] = "-Djava.library.path=" + location;  //$NON-NLS-1$
		System.arraycopy(vmArgs, 0, all, 1, vmArgs.length);
		return all;
	}
	
	public static BundleDescription findFragment() {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel("org.eclipse.swt");
		if (model != null && model.isEnabled()) {
			BundleDescription desc = model.getBundleDescription();
			if (desc.getContainingState() != null) {
				BundleDescription[] fragments = desc.getFragments();
				if (fragments.length > 0) {
					return fragments[0];
				}
			}
		}
		return null;
	}

	private String getNativeLibrariesLocation(BundleDescription fragment) {
		Version version = fragment.getVersion();
		if (version.getMajor() < 3 || version.getMinor() < 1)
			return getLegacyNativeLibrariesLocation(fragment);
		
		File file = new File(fragment.getLocation());
		return file.isDirectory() ? fragment.getLocation() : getExtractedLocation(file);
	}
	
	private String getExtractedLocation(File file) {
		long timestamp = file.lastModified() ^ file.getAbsolutePath().hashCode();
		File metadata = PDEPlugin.getDefault().getStateLocation().toFile();
		File cache = new File(metadata, Long.toString(timestamp) + ".swt");
		if (!cache.exists()){
			cache.mkdirs();
			extractZipFile(file, cache);
		}		
		return cache.getAbsolutePath();
	}
	
	private void extractZipFile(File fragment, File destination) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(fragment);
			for (Enumeration zipEntries = zipFile.entries(); zipEntries.hasMoreElements();) {
				ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
				String name = zipEntry.getName();
				IPath entryPath = new Path(name);
				if (entryPath.segmentCount() == 1 && name.indexOf("swt") != -1) {
					InputStream in = null;
					try {
						in = zipFile.getInputStream(zipEntry);
						if (in != null) {
							File file = new File(destination, zipEntry.getName());
							AbstractFrameworkAdaptor.readFile(in, file);
							if (Platform.getOS().equals(Constants.OS_HPUX))
								Runtime.getRuntime().exec(new String[] {"chmod", "755", file.getAbsolutePath()}).waitFor();
						}
					} catch (IOException e) {
					} catch (InterruptedException e) {
					} finally {
						try {
							if (in != null)
								in.close();
						} catch (IOException e1) {
						}						
					}
				}
			}
		} catch (ZipException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (zipFile != null)
					zipFile.close();
			} catch (IOException e) {
			}
		}
	}
	
	private String getLegacyNativeLibrariesLocation(BundleDescription fragment) {
		StringBuffer buffer = new StringBuffer();
		IPath path = new Path(fragment.getLocation());
		buffer.append(path.removeTrailingSeparator().toString());
		buffer.append(IPath.SEPARATOR);
		buffer.append("os"); //$NON-NLS-1$
		buffer.append(IPath.SEPARATOR);
		buffer.append(TargetPlatform.getOS());
		buffer.append(IPath.SEPARATOR);
		buffer.append(TargetPlatform.getOSArch());
		return buffer.toString();
	}
		
	private String[] getClasspath(BundleDescription desc, ILaunchConfiguration configuration) throws CoreException {
		String[] entries = getClasspath(configuration);
		
		IFragmentModel fragment = PDECore.getDefault().getModelManager().findFragmentModel(desc.getSymbolicName());
		
		if (fragment == null)
			return entries;
		
		ArrayList extra = new ArrayList();
		IResource resource = fragment.getUnderlyingResource();
		if (resource != null) {
			IProject project = resource.getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				extra.add(JavaRuntime.newProjectRuntimeClasspathEntry(jProject).getPath());
				IClasspathEntry[] classEntries = jProject.getRawClasspath();
				for (int i = 0; i < classEntries.length; i++) {
					int kind = classEntries[i].getEntryKind();
					if (kind == IClasspathEntry.CPE_LIBRARY) {
						extra.add(JavaRuntime.newArchiveRuntimeClasspathEntry(classEntries[i].getPath()).getLocation());
					} 
				}
			}
		} else {
			IPluginLibrary[] libraries = fragment.getFragment().getLibraries();
			String location = fragment.getInstallLocation();
			for (int i = 0; i < libraries.length; i++) {
				String name = ClasspathUtilCore.expandLibraryName(libraries[i].getName());
				extra.add(new Path(location).append(name).toOSString());
			}
		}
		if (extra.size() > 0) {
			String[] all = new String[entries.length + extra.size()];
			System.arraycopy(entries, 0, all, 0, entries.length);
			for (int i = 0; i < extra.size(); i++) {
				all[i+entries.length] = extra.get(i).toString();
			}
			return all;
		}
		return entries;
	}
}
