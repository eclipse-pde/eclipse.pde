package org.eclipse.pde.internal.ui.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;

public class BuildPluginAction implements IObjectActionDelegate {
	public static final String KEY_ERRORS_TITLE = "GeneratePluginJars.errorsTitle";
	public static final String KEY_ERRORS_MESSAGE =
		"GeneratePluginJars.errorsMessage";
	public static final String KEY_ERRORS_FMESSAGE =
		"GeneratePluginJars.errorsFMessage";
	public static final String KEY_VERIFYING = "GeneratePluginJars.verifying";
	public static final String KEY_GENERATING = "GeneratePluginJars.generating";
	public static final String KEY_UPDATING = "GeneratePluginJars.updating";
	private IWorkbenchPart targetPart;
	private IFile pluginBaseFile;
	private boolean fragment;
	private boolean errors;

	public IFile getPluginBaseFile() {
		return pluginBaseFile;
	}
	public void run(IAction action) {
		if (pluginBaseFile == null)
			return;
		if (pluginBaseFile.exists() == false)
			return;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IWorkspaceRunnable wop = new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						try {
							doBuildPlugin(monitor);
						} catch (InvocationTargetException e) {
							syncLogException(e);
						}
					}
				};
				try {
					PDEPlugin.getWorkspace().run(wop, monitor);
				} catch (CoreException e) {
					syncLogException(e);
				}
			}
		};
		errors = false;
		ProgressMonitorDialog pmd =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
		try {
			pmd.run(false, false, op);
			/*
			if (errors) return;
			final Display display = PDEPlugin.getActiveWorkbenchShell().getDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					BusyIndicator.showWhile(display, new Runnable() {
						public void run() {
							runAnt();
						}
					});
				}
			});
			*/
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void syncLogException(final Throwable e) {
		//final Display display = PDEPlugin.getActiveWorkbenchShell().getDisplay();
		final Display display = SWTUtil.getStandardDisplay();
		if (display!=null) {
			display.syncExec(new Runnable() {
				public void run() {
					PDEPlugin.logException(e);
				}
			});
		}
		else 
			PDEPlugin.log(e);
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		IFile file = null;

		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				file = (IFile) obj;
				String name = file.getName().toLowerCase();
				if (name.equals("plugin.xml")) {
					fragment = false;
				} else if (name.equals("fragment.xml")) {
					fragment = true;
				} else
					file = null;
			}
		}
		this.pluginBaseFile = file;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
	public void setPluginBaseFile(IFile pluginBaseFile) {
		this.pluginBaseFile = pluginBaseFile;
	}

	private void doBuildPlugin(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(KEY_VERIFYING), 3);
		if (ensureValid(monitor) == false) {
			errors = true;
			return;
		}
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_GENERATING));
		makeScripts(monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		refreshLocal(monitor);
		monitor.worked(1);
		monitor.done();
	}

	private boolean ensureValid(IProgressMonitor monitor) throws CoreException {
		// Force the build if autobuild is off
		IProject project = pluginBaseFile.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}

		if (hasErrors(pluginBaseFile)) {
			// There are errors against this file - abort
			String message;
			if (fragment)
				message = PDEPlugin.getResourceString(KEY_ERRORS_FMESSAGE);
			else
				message = PDEPlugin.getResourceString(KEY_ERRORS_MESSAGE);
			MessageDialog.openError(
				null,
				PDEPlugin.getResourceString(KEY_ERRORS_TITLE),
				message);
			return false;
		}
		return true;
	}
	
	private boolean hasErrors(IFile file) throws CoreException {
		// Check if there are errors against feature file
		IMarker[] markers =file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		for (int i=0; i<markers.length; i++) {
			IMarker marker = markers[i];
			Object att = marker.getAttribute(IMarker.SEVERITY);
			if (att!=null && att instanceof Integer) {
				Integer severity = (Integer)att;
				if (severity.intValue()==IMarker.SEVERITY_ERROR) return true;
			}
		}
		return false;
	}

	private void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		ModelBuildScriptGenerator generator;
		if (fragment)
			generator = new FragmentBuildScriptGenerator();
		else
			generator = new PluginBuildScriptGenerator();
		
		String scriptName = MainPreferencePage.getBuildScriptName();
		generator.setBuildScriptName(scriptName);

		IProject project = pluginBaseFile.getProject();
		IPluginModelBase model = (IPluginModelBase)
			PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel(project);
		IPath platform =
			Platform.getLocation().append(
				model.getUnderlyingResource().getProject().getName());
		generator.setInstallLocation(platform.toOSString());
		generator.setDevEntries(new String[] {"bin"}); // FIXME: look at bug #5747

		URL [] pluginPath = TargetPlatform.createPluginPath();
		generator.setPluginPath(pluginPath);

		try {
			generator.setModelId(model.getPluginBase().getId());
			generator.generate();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	private void refreshLocal(IProgressMonitor monitor) throws CoreException {
		// refresh feature
		pluginBaseFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

//	private void runAnt() {
//		String scriptName = MainPreferencePage.getBuildScriptName();
//		IFile file = pluginBaseFile.getProject().getFile(scriptName);
//		if (!file.exists()) {
//			// should probably warn the user
//			return;
//		}
//		AntLaunchShortcut launch = new AntLaunchShortcut();
//		launch.launch(new StructuredSelection(file), ILaunchManager.RUN_MODE);
//	}
}