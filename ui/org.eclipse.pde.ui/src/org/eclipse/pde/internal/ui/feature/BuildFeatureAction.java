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
import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;

public class BuildFeatureAction implements IObjectActionDelegate {
	public static final String KEY_ERRORS_TITLE = "GenerateFeatureJars.errorsTitle";
	public static final String KEY_ERRORS_MESSAGE =
		"GenerateFeatureJars.errorsMessage";
	public static final String KEY_VERIFYING = "GenerateFeatureJars.verifying";
	public static final String KEY_GENERATING = "GenerateFeatureJars.generating";
	public static final String KEY_UPDATING = "GenerateFeatureJars.updating";
	private IWorkbenchPart targetPart;
	private IFile featureFile;
	private boolean errors;

	public IFile getFeatureFile() {
		return featureFile;
	}
	public void run(IAction action) {
		if (featureFile == null)
			return;
		if (featureFile.exists() == false)
			return;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IWorkspaceRunnable wop = new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						try {
							doBuildFeature(monitor);
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
		ProgressMonitorDialog pmd =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
		try {
			errors = false;
			pmd.run(false, false, op);
//			if (errors) return;
//			final Display display = PDEPlugin.getActiveWorkbenchShell().getDisplay();
//			display.asyncExec(new Runnable() {
//				public void run() {
//					BusyIndicator.showWhile(display, new Runnable() {
//						public void run() {
//							runAnt();
//						}
//					});
//				}
//			});
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void syncLogException(final Throwable e) {
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
				if (!name.equals("feature.xml")) {
					file = null;
				}
			}
		}
		this.featureFile = file;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
	public void setFeatureFile(IFile featureFile) {
		this.featureFile = featureFile;
	}

	private void doBuildFeature(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(KEY_VERIFYING), 3);
		if (ensureValid(monitor) == false) {
			errors = true;
			return;
		}
		monitor.worked(1);
		WorkspaceFeatureModel model = new WorkspaceFeatureModel(featureFile);
		model.load();
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_GENERATING));
		makeScripts(model, monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		refreshLocal(model, monitor);
		monitor.worked(1);
		monitor.done();
	}

	private boolean ensureValid(IProgressMonitor monitor) throws CoreException {
		// Force the build if autobuild is off
		IProject project = featureFile.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}
		// Check if there are errors against feature file
		if (hasErrors(featureFile)) {
			// There are errors against this file - abort
			String message;
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

	private void makeScripts(IFeatureModel model, IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();

		String scriptName = MainPreferencePage.getBuildScriptName();
		generator.setBuildScriptName(scriptName);
		generator.setFeatureRootLocation(
			getFeatureFile().getParent().getLocation().toOSString());

		IPath platform =
			Platform.getLocation().append(featureFile.getProject().getName());
		generator.setInstallLocation(platform.toOSString());
		generator.setDevEntries(new String[] { "bin" }); // FIXME: look at bug #5747

		// RTP: set this to false when you do not want to generate scripts for this
		// feature's children. The default is true.
		//	generator.setGenerateChildrenScript(children);

		URL[] pluginPath = TargetPlatform.createPluginPath();
		generator.setPluginPath(pluginPath);

		try {
			monitor.subTask(PDEPlugin.getResourceString(KEY_GENERATING));
			generator.setFeature(model.getFeature().getId());
			generator.generate();
			monitor.subTask(PDEPlugin.getResourceString(KEY_UPDATING));
		} catch (Exception e) {
			PDEPlugin.logException(e);
		}
	}

	private void refreshLocal(IFeature feature, IProgressMonitor monitor)
		throws CoreException {
		IFeaturePlugin[] references = feature.getPlugins();
		for (int i = 0; i < references.length; i++) {
			IFeaturePlugin ref = references[i];
			IPluginModelBase refmodel = feature.getReferencedModel(ref);
			if (refmodel != null) {
				refmodel.getUnderlyingResource().getProject().refreshLocal(
					IResource.DEPTH_INFINITE,
					monitor);
			}
		}
	}
	private void refreshLocal(IFeatureModel model, IProgressMonitor monitor)
		throws CoreException {
		// refresh feature
		featureFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		// refresh references
		IFeature feature = model.getFeature();
		refreshLocal(feature, monitor);
	}

//	private void runAnt() {
//		String scriptName = MainPreferencePage.getBuildScriptName();
//		IFile file = featureFile.getProject().getFile(scriptName);
//		if (!file.exists()) {
//			// should probably warn the user
//			return;
//		}
//		AntLaunchShortcut launch = new AntLaunchShortcut();
//		launch.launch(new StructuredSelection(file), ILaunchManager.RUN_MODE);
//	}
}