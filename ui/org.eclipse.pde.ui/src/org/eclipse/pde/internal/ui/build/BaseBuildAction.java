/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.ant.internal.ui.launchConfigurations.AntLaunchShortcut;
import org.eclipse.ant.ui.launching.IAntLaunchConfigurationConstants;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.exports.BuildUtilities;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class BaseBuildAction extends AbstractHandler {

	protected IFile fManifestFile;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				this.fManifestFile = (IFile) obj;
			}
		}

		if (fManifestFile == null || !fManifestFile.exists())
			return null;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IWorkspaceRunnable wop = new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
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
			PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}

	private void doBuild(IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		monitor.beginTask(PDEUIMessages.BuildAction_Validate, 4);
		if (!ensureValid(fManifestFile, monitor)) {
			monitor.done();
			return;
		}
		monitor.worked(1);
		monitor.setTaskName(PDEUIMessages.BuildAction_Generate);
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

	protected abstract void makeScripts(IProgressMonitor monitor) throws InvocationTargetException, CoreException;

	public static boolean ensureValid(IFile file, IProgressMonitor monitor) throws CoreException {
		// Force the build if autobuild is off
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			String builderID = ICoreConstants.FEATURE_FILENAME_DESCRIPTOR.equals(file.getName()) ? PDE.FEATURE_BUILDER_ID : PDE.MANIFEST_BUILDER_ID;
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, builderID, null, monitor);
		}

		if (hasErrors(file)) {
			// There are errors against this file - abort
			MessageDialog.openError(null, PDEUIMessages.BuildAction_ErrorDialog_Title, PDEUIMessages.BuildAction_ErrorDialog_Message);
			return false;
		}
		return true;
	}

	public static boolean hasErrors(IFile file) throws CoreException {
		IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
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
			List configs = AntLaunchShortcut.findExistingLaunchConfigurations(generatedFile);
			ILaunchConfigurationWorkingCopy launchCopy;
			if (configs.size() == 0) {
				ILaunchConfiguration config = AntLaunchShortcut.createDefaultLaunchConfiguration(generatedFile);
				launchCopy = config.getWorkingCopy();
			} else {
				launchCopy = ((ILaunchConfiguration) configs.get(0)).getWorkingCopy();
			}
			if (launchCopy == null)
				return;

			Map properties = new HashMap();
			properties = launchCopy.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
			properties.put(IXMLConstants.PROPERTY_BASE_WS, TargetPlatform.getWS());
			properties.put(IXMLConstants.PROPERTY_BASE_OS, TargetPlatform.getOS());
			properties.put(IXMLConstants.PROPERTY_BASE_ARCH, TargetPlatform.getOSArch());
			properties.put(IXMLConstants.PROPERTY_BASE_NL, TargetPlatform.getNL());
			properties.put("eclipse.running", "true"); //$NON-NLS-1$ //$NON-NLS-2$

			properties.put(IXMLConstants.PROPERTY_JAVAC_FAIL_ON_ERROR, "false"); //$NON-NLS-1$
			properties.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$  
			properties.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, "false"); //$NON-NLS-1$

			IProject project = generatedFile.getProject();
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				PDEPreferencesManager pref = new PDEPreferencesManager(JavaCore.PLUGIN_ID);
				properties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, pref.getString(JavaCore.COMPILER_SOURCE));
				properties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, pref.getString(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
			} else {
				IJavaProject jProject = JavaCore.create(project);
				properties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, jProject.getOption(JavaCore.COMPILER_SOURCE, true));
				properties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, jProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
			}
			properties.put(IXMLConstants.PROPERTY_BOOTCLASSPATH, BuildUtilities.getBootClasspath());
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
			for (int i = 0; i < envs.length; i++) {
				String id = envs[i].getId();
				if (id != null)
					properties.put(id, BuildUtilities.getBootClasspath(id));
			}

			launchCopy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, (String) null);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String) null);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
			launchCopy.setAttribute(IAntLaunchConfigurationConstants.ATTR_DEFAULT_VM_INSTALL, (String) null);
			launchCopy.doSave();
		} catch (CoreException e) {
		}
	}

}
