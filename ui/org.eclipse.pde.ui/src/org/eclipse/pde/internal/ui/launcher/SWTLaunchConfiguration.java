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
import com.ibm.icu.text.MessageFormat;
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
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.Version;

public class SWTLaunchConfiguration extends
		AbstractJavaLaunchConfigurationDelegate {

	private boolean fShouldDelete;

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		fShouldDelete = true;
		
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
		
		// Find SWT Fragments from the target platform
		BundleDescription[] fragments = findFragments();
		
		// VM-specific attributes
		Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
		
		// Classpath
		String[] classpath = getClasspath(fragments, configuration);
		
		// Create VM config
		VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
		runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
		runConfig.setEnvironment(envp);
		runConfig.setVMArguments(getVMArguments(fragments, execArgs));
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
	
	private String[] getVMArguments(BundleDescription[] fragments, ExecutionArguments execArgs) {
		if (fragments.length == 0)
			return execArgs.getVMArgumentsArray();
		
		String location = getNativeLibrariesLocations(fragments);
		if (location == null)
			return execArgs.getVMArgumentsArray();
		
		String[] vmArgs = execArgs.getVMArgumentsArray();
		for (int i = vmArgs.length - 1; i >= 0; i--) {
			if (vmArgs[i].startsWith("-Djava.library.path")) { //$NON-NLS-1$
				vmArgs[i] +=  File.pathSeparatorChar + location; 
				return vmArgs;
			}
		}
		String[] all = new String[vmArgs.length + 1];
		all[0] = "-Djava.library.path=" + location;  //$NON-NLS-1$
		System.arraycopy(vmArgs, 0, all, 1, vmArgs.length);
		return all;
	}
	
	private String getNativeLibrariesLocations(BundleDescription[] bundles) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < bundles.length; i++) {
			String location = getNativeLibrariesLocation(bundles[i]);
			if (location != null) {
				if (buffer.length() > 0)
					buffer.append(File.pathSeparatorChar);
				buffer.append(location);
			}
		}
		return buffer.length() == 0 ? null : buffer.toString();
	}

	protected static BundleDescription[] findFragments() {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel("org.eclipse.swt"); //$NON-NLS-1$
		if (model != null && model.isEnabled()) {
			BundleDescription desc = model.getBundleDescription();
			if (desc.getContainingState() != null)
				return desc.getFragments();
		}
		return new BundleDescription[0];
	}

	private String getNativeLibrariesLocation(BundleDescription fragment) {
		if (!fragment.isResolved())
			return null;
		Version version = fragment.getVersion();
		if (version.getMajor() < 3 || version.getMinor() < 1)
			return getLegacyNativeLibrariesLocation(fragment);
		
		File file = new File(fragment.getLocation());
		return file.isDirectory() ? fragment.getLocation() : getExtractionLocation(file);
	}
	
	private String getExtractionLocation(File file) {
		long timestamp = file.lastModified() ^ file.getAbsolutePath().hashCode();
		File metadata = PDEPlugin.getDefault().getStateLocation().toFile();
		File cache = new File(metadata, Long.toString(timestamp) + ".swt"); //$NON-NLS-1$
		if (!cache.exists()){
			if (fShouldDelete) {
				deleteStaleCache(metadata);
				fShouldDelete = false;
			}
			cache.mkdirs();
			extractZipFile(file, cache);
		}		
		return cache.getAbsolutePath();
	}
	
	private void deleteStaleCache(File metadata) {
		if (!metadata.exists())
			return;
		
		File[] children = metadata.listFiles();
		if (children == null)
			return;
		for (int i = 0; i < children.length; i++) {
			if (children[i].isDirectory() && children[i].getName().endsWith(".swt")) { //$NON-NLS-1$
				CoreUtility.deleteContent(children[i]);
			}
		}
	}
	
	private void extractZipFile(File fragment, File destination) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(fragment);
			for (Enumeration zipEntries = zipFile.entries(); zipEntries.hasMoreElements();) {
				ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
				if (zipEntry.isDirectory())
					continue;
				if (isInterestingFile(zipEntry.getName())) {
					InputStream in = null;
					try {
						in = zipFile.getInputStream(zipEntry);
						if (in != null) {
							File file = new File(destination, zipEntry.getName());
							CoreUtility.readFile(in, file);
							if (!Platform.getOS().equals(Constants.OS_WIN32))
								Runtime.getRuntime().exec(new String[] {"chmod", "755", file.getAbsolutePath()}).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	private boolean isInterestingFile(String name) {
		Path path = new Path(name);
		if (path.segmentCount() > 1)
			return false;
		return name.endsWith(".dll") //$NON-NLS-1$
				|| name.endsWith(".jnilib") //$NON-NLS-1$
				|| name.endsWith(".sl") //$NON-NLS-1$
				|| name.endsWith(".a") //$NON-NLS-1$
				|| name.indexOf(".so") != -1; //$NON-NLS-1$
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
		
	private String[] getClasspath(BundleDescription[] fragments, ILaunchConfiguration configuration) throws CoreException {
		String[] entries = getClasspath(configuration);
		
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ArrayList extra = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			IFragmentModel fragment = manager.findFragmentModel(fragments[i].getSymbolicName());	
			if (fragment == null)
				continue;
			
			IResource resource = fragment.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jProject = JavaCore.create(project);
					extra.add(JavaRuntime.newProjectRuntimeClasspathEntry(jProject).getPath());
					IClasspathEntry[] classEntries = jProject.getRawClasspath();
					for (int j = 0; j < classEntries.length; j++) {
						int kind = classEntries[j].getEntryKind();
						if (kind == IClasspathEntry.CPE_LIBRARY) {
							extra.add(JavaRuntime.newArchiveRuntimeClasspathEntry(classEntries[j].getPath()).getLocation());
						} 
					}
				}
			} else {
				IPluginLibrary[] libraries = fragment.getFragment().getLibraries();
				String location = fragment.getInstallLocation();
				for (int j = 0; j < libraries.length; j++) {
					String name = ClasspathUtilCore.expandLibraryName(libraries[j].getName());
					extra.add(new Path(location).append(name).toOSString());
				}
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
