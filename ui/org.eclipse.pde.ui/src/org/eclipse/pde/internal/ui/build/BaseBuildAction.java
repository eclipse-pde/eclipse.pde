/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.launchConfigurations.AntLaunchShortcut;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public abstract class BaseBuildAction
		implements
			IObjectActionDelegate,
			IPreferenceConstants {

	protected IFile fManifestFile;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		if (!fManifestFile.exists())
			return;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IWorkspaceRunnable wop = new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor)
							throws CoreException {
						try {
							doBuild(monitor);
						} catch (InvocationTargetException e) {
							PDEPlugin.logException(e);
						}
					}
				};
				try {
					PDEPlugin.getWorkspace().run(wop, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(
					PDEPlugin.getActiveWorkbenchWindow(), op,
					PDEPlugin.getWorkspace().getRoot());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				this.fManifestFile = (IFile) obj;
			}
		}

	}

	private void doBuild(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException {
		monitor.beginTask(
				PDEUIMessages.BuildAction_Validate, 4); 
		if (!ensureValid(fManifestFile, monitor)) {
			monitor.done();
			return;
		}
		monitor.worked(1);
		monitor
				.setTaskName(PDEUIMessages.BuildAction_Generate); 
		makeScripts(monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEUIMessages.BuildAction_Update); 
		refreshLocal(monitor);
		monitor.worked(1);
		IProject project = fManifestFile.getProject();
		IFile generatedFile = (IFile) project.findMember("build.xml"); //$NON-NLS-1$
		if (generatedFile != null)
			setDefaultValues(generatedFile);
		monitor.worked(1);

	}

	protected abstract void makeScripts(IProgressMonitor monitor)
			throws InvocationTargetException, CoreException;

	public static boolean ensureValid(IFile file, IProgressMonitor monitor)
			throws CoreException {
		// Force the build if autobuild is off
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			String builderID = "feature.xml".equals(file.getName()) ? PDE.FEATURE_BUILDER_ID : PDE.MANIFEST_BUILDER_ID; //$NON-NLS-1$
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, builderID, null, monitor);
		}

		if (hasErrors(file)) {
			// There are errors against this file - abort
			MessageDialog
					.openError(
							null,
							PDEUIMessages.BuildAction_ErrorDialog_Title, 
							PDEUIMessages.BuildAction_ErrorDialog_Message); 
			return false;
		}
		return true;
	}

	public static boolean hasErrors(IFile file) throws CoreException {
		IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_ZERO);
		for (int i = 0; i < markers.length; i++) {
			Object att = markers[i].getAttribute(IMarker.SEVERITY);
			if (att != null && att instanceof Integer) {
				if (((Integer) att).intValue() == IMarker.SEVERITY_ERROR)
					return true;
			}
		}
		return false;
	}

	protected void refreshLocal(IProgressMonitor monitor) throws CoreException {
		IProject project = fManifestFile.getProject();
		project.refreshLocal(IResource.DEPTH_ONE, monitor);
		IFile file = project.getFile("dev.properties"); //$NON-NLS-1$
		if (file.exists())
			file.delete(true, false, monitor);
		project.refreshLocal(IResource.DEPTH_ONE, monitor);
	}

	public static void setDefaultValues(IFile generatedFile) {
		try {
			List configs = AntLaunchShortcut
					.findExistingLaunchConfigurations(generatedFile);
			ILaunchConfigurationWorkingCopy launchCopy;
			if (configs.size() == 0) {
				ILaunchConfiguration config = AntLaunchShortcut
						.createDefaultLaunchConfiguration(generatedFile);
				launchCopy = config.getWorkingCopy();
			} else {
				launchCopy = ((ILaunchConfiguration) configs.get(0))
						.getWorkingCopy();
			}
			if (launchCopy == null)
				return;

			Map properties = new HashMap();
			properties = launchCopy.getAttribute(
					IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES,
					properties);
			properties.put(IXMLConstants.PROPERTY_BASE_WS, TargetPlatform.getWS()); 
			properties.put(IXMLConstants.PROPERTY_BASE_OS, TargetPlatform.getOS()); 
			properties.put(IXMLConstants.PROPERTY_BASE_ARCH, TargetPlatform.getOSArch());
			properties.put(IXMLConstants.PROPERTY_BASE_NL, TargetPlatform.getNL()); 
			properties.put("eclipse.running", "true"); //$NON-NLS-1$ //$NON-NLS-2$

			properties.put(IXMLConstants.PROPERTY_JAVAC_FAIL_ON_ERROR, "false"); //$NON-NLS-1$
			properties.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$  
			properties.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, "true"); //$NON-NLS-1$
			
			IProject project = generatedFile.getProject();
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				Preferences pref = JavaCore.getPlugin().getPluginPreferences();
				properties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, pref.getString(JavaCore.COMPILER_SOURCE)); 
				properties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, pref.getString(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM)); 
			} else {
				IJavaProject jProject = JavaCore.create(project);
				properties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, jProject.getOption(JavaCore.COMPILER_SOURCE, true)); 
				properties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, jProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)); 				
			}
			properties.put(IXMLConstants.PROPERTY_BOOTCLASSPATH, getBootClasspath()); 
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
			for (int i = 0; i < envs.length; i++) {
				String id = envs[i].getId();
				if (id != null)
					properties.put(id, BaseBuildAction.getBootClasspath(id));
			}
			
			launchCopy.setAttribute(
					IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES,
					properties);
			launchCopy.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME,
					(String) null);
			launchCopy.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE,
					(String) null);
			launchCopy.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
					(String) null);
			launchCopy.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
					(String) null);
			launchCopy.setAttribute(
					IAntUIConstants.ATTR_DEFAULT_VM_INSTALL,
					(String) null);
			launchCopy.doSave();
		} catch (CoreException e) {
		}
	}
	
	public static String getBootClasspath() {
		return getBootClasspath(JavaRuntime.getDefaultVMInstall());
	}

	public static String getBootClasspath(String environmentID) {
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment environment = manager.getEnvironment(environmentID);
		IVMInstall vm = null;
		if (environment != null) {
			vm = environment.getDefaultVM();
			if (vm == null) {
				IVMInstall[] installs = environment.getCompatibleVMs();
				// take the first strictly compatible vm if there is no default
				for (int i = 0; i < installs.length; i++) {
					IVMInstall install = installs[i];
					if (environment.isStrictlyCompatible(install)) {
						vm = install;
						break;
					}
				}
				// use the first vm failing that
				if (vm == null && installs.length > 0) {
					vm = installs[0];
				}
			}
		}
		if (vm == null)
			vm = JavaRuntime.getDefaultVMInstall();
		return getBootClasspath(vm);
	}

	public static String getBootClasspath(IVMInstall install) {
		StringBuffer buffer = new StringBuffer();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(install);
		for (int i = 0; i < locations.length; i++) {
			buffer.append(locations[i].getSystemLibraryPath().toOSString());
			if (i < locations.length - 1)
				buffer.append(";"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

}
