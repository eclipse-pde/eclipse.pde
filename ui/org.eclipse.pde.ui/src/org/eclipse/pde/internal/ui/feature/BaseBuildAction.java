package org.eclipse.pde.internal.ui.feature;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.ant.internal.ui.launchConfigurations.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

public abstract class BaseBuildAction implements IObjectActionDelegate, IPreferenceConstants {
	
	protected IFile file;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		if (!file.exists())
			return;

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
		ProgressMonitorDialog pmd =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
		try {
			pmd.run(false, false, op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				this.file = (IFile) obj;
			}
		}

	}
	
	private void doBuild(IProgressMonitor monitor)
		throws CoreException, InvocationTargetException {
			monitor.beginTask(PDEPlugin.getResourceString("BuildAction.Validate"), 4);
			if (!ensureValid(file, monitor)) {
				monitor.done();
				return;
			}
			monitor.worked(1);
			monitor.setTaskName(PDEPlugin.getResourceString("BuildAction.Generate"));
			makeScripts(monitor);
			monitor.worked(1);
			monitor.setTaskName(PDEPlugin.getResourceString("BuildAction.Update"));
			refreshLocal(monitor);
			monitor.worked(1);
			setDefaultValues();
			monitor.worked(1);
			
		}
		
	protected abstract void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException;

	public static boolean ensureValid(IFile file, IProgressMonitor monitor) throws CoreException {
		// Force the build if autobuild is off
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}

		if (hasErrors(file)) {
			// There are errors against this file - abort
			MessageDialog.openError(
				null,
				PDEPlugin.getResourceString("BuildAction.ErrorDialog.Title"),
				PDEPlugin.getResourceString("BuildAction.ErrorDialog.Message"));
			return false;
		}
		return true;
	}
	
	public static  boolean hasErrors(IFile file) throws CoreException {
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
		file.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
	}
	
	private void setDefaultValues() {
		IProject project = file.getProject();
		IFile generatedFile = (IFile) project.findMember("build.xml");
		if (generatedFile == null)
			return;

		try {
			List configs =
				AntLaunchShortcut.findExistingLaunchConfigurations(generatedFile);
			ILaunchConfigurationWorkingCopy launchCopy;
			if (configs.size() == 0) {
				ILaunchConfiguration config =
					AntLaunchShortcut.createDefaultLaunchConfiguration(generatedFile);
				launchCopy = config.getWorkingCopy();
			} else {
				launchCopy = ((ILaunchConfiguration) configs.get(0)).getWorkingCopy();
			}
			if (launchCopy == null)
				return;

			Map properties = new HashMap();
			properties =
				launchCopy.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
			properties.put("basews", TargetPlatform.getWS());
			properties.put("baseos", TargetPlatform.getOS());
			properties.put("basearch", TargetPlatform.getOSArch());
			properties.put("basenl", TargetPlatform.getNL());
			properties.put("eclipse.running", "true");
			
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			properties.put("javacFailOnError", store.getString(PROP_JAVAC_FAIL_ON_ERROR));
			properties.put("javacDebugInfo", store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off");
			properties.put("javacVerbose", store.getString(PROP_JAVAC_VERBOSE));
			properties.put("javacSource", store.getString(PROP_JAVAC_SOURCE));
			properties.put("javacTarget", store.getString(PROP_JAVAC_TARGET));
			launchCopy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, 
					(String)null);
			launchCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, 
					(String)null);
			launchCopy.doSave();
		} catch (CoreException e) {
		}
	}

}
