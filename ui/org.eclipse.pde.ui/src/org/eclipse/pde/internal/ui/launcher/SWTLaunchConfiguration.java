package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.text.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

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
		IFragment fragment = findFragment();
		
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
	
	private String[] getVMArguments(IFragment fragment, ExecutionArguments execArgs) {
		if (fragment == null)
			return execArgs.getVMArgumentsArray();
		
		String[] vmArgs = execArgs.getVMArgumentsArray();		
		String[] all = new String[vmArgs.length + 1];
		System.arraycopy(vmArgs, 0, all, 0, vmArgs.length);
		all[vmArgs.length] = "-Djava.library.path=\"" + getNativeLibrariesLocation(fragment) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		return all;
	}
	
	public static IFragment findFragment() {
		IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.swt"); //$NON-NLS-1$
		if (plugin != null) {
			IFragment[] fragments = PDECore.getDefault().findFragmentsFor("org.eclipse.swt", plugin.getVersion()); //$NON-NLS-1$
			for (int i = 0; i < fragments.length; i++) {
				if (fragments[i].getId().equals("org.eclipse.swt." + TargetPlatform.getWS())) { //$NON-NLS-1$
					return fragments[i];
				}
			}
		}
		return null;
	}
	
	private String getNativeLibrariesLocation(IFragment fragment) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(fragment.getModel().getInstallLocation());
		buffer.append(IPath.SEPARATOR);
		buffer.append("os"); //$NON-NLS-1$
		buffer.append(IPath.SEPARATOR);
		buffer.append(TargetPlatform.getOS());
		buffer.append(IPath.SEPARATOR);
		buffer.append(TargetPlatform.getOSArch());
		return buffer.toString();
	}
	
	private String[] getClasspath(IFragment fragment, ILaunchConfiguration configuration) throws CoreException {
		String[] entries = getClasspath(configuration);
		if (fragment == null)
			return entries;
		
		ArrayList extra = new ArrayList();
		IResource resource = fragment.getModel().getUnderlyingResource();
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
			IPluginLibrary[] libraries = fragment.getLibraries();
			String location = fragment.getModel().getInstallLocation();
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
