package org.eclipse.pde.internal.ui.editor.site;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FeatureBuildOperation implements IRunnableWithProgress {
	public static final String KEY_ERRORS_MESSAGE =
		"GenerateFeatureJars.errorsMessage";
	public static final String KEY_VERIFYING = "GenerateFeatureJars.verifying";
	public static final String KEY_GENERATING =
		"GenerateFeatureJars.generating";
	public static final String KEY_RUNNING = "FeatureBuildOperation.running";
	private ArrayList features;

	public FeatureBuildOperation(ArrayList features) {
		this.features = features;
	}

	public FeatureBuildOperation(ISiteBuildFeature sbfeature) {
		this.features = new ArrayList();
		features.add(sbfeature);
	}

	public void run(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException {
		try {
			monitor.beginTask("Building:", features.size());
			for (int i = 0; i < features.size(); i++) {
				if (monitor.isCanceled())
					break;
				ISiteBuildFeature sbfeature =
					(ISiteBuildFeature) features.get(i);
				SubProgressMonitor subMonitor =
					new SubProgressMonitor(
						monitor,
						1,
						SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
				doBuildFeature(sbfeature, subMonitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	private void doBuildFeature(
		ISiteBuildFeature sbfeature,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		IFeatureModel featureModel =
			sbfeature.getReferencedFeature().getModel();
		monitor.subTask("'" + featureModel.getFeature().getLabel() + "'");
		monitor.beginTask(PDEPlugin.getResourceString(KEY_VERIFYING), 4);
		File workingFolder = createWorkingFolder(featureModel);
		monitor.worked(1);
		ensureValid(featureModel, monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_GENERATING));
		File scriptFile = makeScript(featureModel, workingFolder, monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_RUNNING));
		runScript(scriptFile, sbfeature.getModel(), monitor);
		monitor.worked(1);
		removeWorkingFolder(workingFolder);
	}

	private File createWorkingFolder(IFeatureModel featureModel) {
		IPath stateLocation = PDEPlugin.getDefault().getStateLocation();
		IFeature feature = featureModel.getFeature();
		String folder = feature.getId() + "_" + feature.getVersion();
		IPath workingPath = stateLocation.append("builds");
		ensureDirectoryExists(workingPath.toFile(), false);
		workingPath = workingPath.append(folder);
		File dir = workingPath.toFile();
		ensureDirectoryExists(dir, true);
		return dir;
	}

	private void removeWorkingFolder(File folder) {
		folder.delete();
	}

	private void ensureDirectoryExists(File dir, boolean clean) {
		if (dir.exists() == false) {
			dir.mkdir();
		} else if (clean) {
			dir.delete();
			dir.mkdir();
		}
	}

	private void ensureValid(
		IFeatureModel featureModel,
		IProgressMonitor monitor)
		throws CoreException {
		// Force the build if autobuild is off
		IFile file = (IFile) featureModel.getUnderlyingResource();
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}
		// Check if there are errors against feature file
		if (hasErrors(file)) {
			// There are errors against this file - abort
			String message = PDEPlugin.getResourceString(KEY_ERRORS_MESSAGE);
			Status status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.PLUGIN_ID,
					IStatus.OK,
					message,
					null);
			throw new CoreException(status);
		}
	}

	private boolean hasErrors(IFile file) throws CoreException {
		// Check if there are errors against feature file
		IMarker[] markers =
			file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		for (int i = 0; i < markers.length; i++) {
			IMarker marker = markers[i];
			Object att = marker.getAttribute(IMarker.SEVERITY);
			if (att != null && att instanceof Integer) {
				Integer severity = (Integer) att;
				if (severity.intValue() == IMarker.SEVERITY_ERROR)
					return true;
			}
		}
		return false;
	}

	private File makeScript(
		IFeatureModel featureModel,
		File workingFolder,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		FeatureBuildScriptGenerator generator =
			new FeatureBuildScriptGenerator();
		IFile featureFile = (IFile) featureModel.getUnderlyingResource();
		String scriptName = "build.xml";
		generator.setBuildScriptName(scriptName);
		generator.setFeatureRootLocation(
			featureFile.getParent().getLocation().toOSString());
		//generator.setTargetLocation(workingFolder.getPath());
		IPath platform =
			Platform.getLocation().append(featureFile.getProject().getName());
		generator.setInstallLocation(platform.toOSString());
		generator.setDevEntries(new String[] { "bin" });
		URL[] pluginPath = TargetPlatform.createPluginPath();
		generator.setPluginPath(pluginPath);
		generator.setFeature(featureModel.getFeature().getId());
		generator.generate();
		File scriptFile = new File(workingFolder, scriptName);
		return scriptFile;
	}

	private void runScript(
		File scriptFile,
		ISiteBuildModel buildModel,
		IProgressMonitor monitor) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}
}