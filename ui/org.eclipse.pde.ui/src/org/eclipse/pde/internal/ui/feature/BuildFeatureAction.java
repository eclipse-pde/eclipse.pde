package org.eclipse.pde.internal.ui.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.ant.internal.ui.AntAction;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.TargetPlatform;
import org.eclipse.pde.internal.ui.model.ifeature.IFeature;
import org.eclipse.pde.internal.ui.model.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.model.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class BuildFeatureAction implements IObjectActionDelegate {
	public static final String KEY_ERRORS_TITLE = "GenerateFeatureJars.errorsTitle";
	public static final String KEY_ERRORS_MESSAGE =
		"GenerateFeatureJars.errorsMessage";
	public static final String KEY_VERIFYING = "GenerateFeatureJars.verifying";
	public static final String KEY_GENERATING = "GenerateFeatureJars.generating";
	public static final String KEY_UPDATING = "GenerateFeatureJars.updating";
	private IWorkbenchPart targetPart;
	private IFile featureFile;

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
			pmd.run(true, false, op);
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
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
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
		if (ensureValid(monitor) == false)
			return;
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
		IMarker[] markers =
			featureFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		if (markers.length > 0) {
			// There are errors against this file - abort
			String message;
			message = PDEPlugin.getResourceString(KEY_ERRORS_MESSAGE);
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(KEY_ERRORS_TITLE),
				message);
			return false;
		}
		return true;
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

	private void runAnt() {
		IFile file = featureFile.getProject().getFile("build.xml");
		if (!file.exists()) {
			// should probably warn the user
			return;
		}
		AntAction action = new AntAction(file);
		action.run();
	}
}