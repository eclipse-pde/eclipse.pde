package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.ant.internal.ui.launchConfigurations.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

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
				PDEPlugin.getResourceString("BuildAction.Validate"), 4); //$NON-NLS-1$
		if (!ensureValid(fManifestFile, monitor)) {
			monitor.done();
			return;
		}
		monitor.worked(1);
		monitor
				.setTaskName(PDEPlugin
						.getResourceString("BuildAction.Generate")); //$NON-NLS-1$
		makeScripts(monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString("BuildAction.Update")); //$NON-NLS-1$
		refreshLocal(monitor);
		monitor.worked(1);
		setDefaultValues();
		monitor.worked(1);

	}

	protected abstract void makeScripts(IProgressMonitor monitor)
			throws InvocationTargetException, CoreException;

	public static boolean ensureValid(IFile file, IProgressMonitor monitor)
			throws CoreException {
		// Force the build if autobuild is off
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}

		if (hasErrors(file)) {
			// There are errors against this file - abort
			MessageDialog
					.openError(
							null,
							PDEPlugin
									.getResourceString("BuildAction.ErrorDialog.Title"), //$NON-NLS-1$
							PDEPlugin
									.getResourceString("BuildAction.ErrorDialog.Message")); //$NON-NLS-1$
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

	private void setDefaultValues() {
		IProject project = fManifestFile.getProject();
		IFile generatedFile = (IFile) project.findMember("build.xml"); //$NON-NLS-1$
		if (generatedFile == null)
			return;

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

			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			properties.put(IXMLConstants.PROPERTY_JAVAC_FAIL_ON_ERROR, store.getString(PROP_JAVAC_FAIL_ON_ERROR));
			properties.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off"); //$NON-NLS-1$ //$NON-NLS-2$ 
			properties.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, store.getString(PROP_JAVAC_VERBOSE));
			
			Preferences pref = JavaCore.getPlugin().getPluginPreferences();
			properties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, pref.getString(JavaCore.COMPILER_SOURCE)); 
			properties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, pref.getString(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM)); 

			properties.put(IXMLConstants.PROPERTY_BOOTCLASSPATH, getBootClasspath()); 
			
			launchCopy.setAttribute(
					IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES,
					properties);
			launchCopy.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME,
					(String) null);
			launchCopy.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE,
					(String) null);
			launchCopy.doSave();
		} catch (CoreException e) {
		}
	}
	
	public static String getBootClasspath() {
		StringBuffer buffer = new StringBuffer();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(JavaRuntime.getDefaultVMInstall());
		for (int i = 0; i < locations.length; i++) {
			buffer.append(locations[i].getSystemLibraryPath().toOSString());
			if (i < locations.length - 1)
				buffer.append(";"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

}