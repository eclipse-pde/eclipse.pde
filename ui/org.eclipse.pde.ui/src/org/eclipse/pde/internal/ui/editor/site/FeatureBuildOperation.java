/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.ant.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FeatureBuildOperation implements IRunnableWithProgress {
	public static final String KEY_RUNNING = "FeatureBuildOperation.running";
	private static final String BUILD_LISTENER_CLASS =
		"org.eclipse.pde.internal.ui.ant.SiteBuildListener";
	private ArrayList features;
	private StateListener stateListener;
	private boolean scrubOutput;
	private boolean fullBuild;

	private File logFile = null;

	private static FeatureBuildOperation instance;

	public FeatureBuildOperation(
		ArrayList features,
		StateListener stateListener,
		boolean scrubOutput,
		boolean fullBuild) {
		this.features = features;
		this.stateListener = stateListener;
		instance = this;
		this.fullBuild = fullBuild;
		this.scrubOutput = scrubOutput;
	}

	public FeatureBuildOperation(
		ISiteBuildFeature sbfeature,
		boolean scrubOutput) {
		this.features = new ArrayList();
		features.add(sbfeature);
		this.scrubOutput = scrubOutput;
		this.fullBuild = false;
		instance = this;
	}

	public static FeatureBuildOperation getDefault() {
		return instance;
	}

	public void run(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException {
		try {
			int count =
				scrubOutput ? 2 * features.size() + 1 : 2 * features.size();
			monitor.beginTask("Building:", count);
			if (scrubOutput) {
				monitor.setTaskName("Scrubbing output folders ...");
				scrubOutput(
					(ISiteBuildFeature) features.get(0),
					new SubProgressMonitor(monitor, 1));
			}
			monitor.setTaskName("Building:");
			for (int i = 0; i < features.size(); i++) {
				if (monitor.isCanceled())
					break;
				ISiteBuildFeature sbfeature =
					(ISiteBuildFeature) features.get(i);
				SubProgressMonitor subMonitor =
					new SubProgressMonitor(
						monitor,
						2,
						SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
				doBuildFeature(sbfeature, subMonitor);
			}
			if (fullBuild && stateListener!=null) {
				if (stateListener.isActive()==false)
					stateListener.setActive(true);
				stateListener.removeAllProjects();
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			postBuildCleanup((ISiteBuildFeature) features.get(0));
			monitor.done();
		}
	}

	private void scrubOutput(
		ISiteBuildFeature sbfeature,
		IProgressMonitor monitor)
		throws CoreException {
		monitor.beginTask("", 2);
		ISiteBuildModel model = sbfeature.getModel();
		IProject project = model.getUnderlyingResource().getProject();
		ISiteBuild siteBuild = model.getSiteBuild();
		IFolder pluginFolder = project.getFolder(siteBuild.getPluginLocation());
		IFolder featureFolder =
			project.getFolder(siteBuild.getFeatureLocation());
		scrubFolder(pluginFolder, new SubProgressMonitor(monitor, 1));
		scrubFolder(featureFolder, new SubProgressMonitor(monitor, 1));
	}

	private void scrubFolder(IFolder folder, IProgressMonitor monitor)
		throws CoreException {
		IResource[] members = folder.members();
		monitor.beginTask("Scrubbing " + folder.getName(), members.length);
		for (int i = 0; i < members.length; i++) {
			IResource member = members[i];
			member.delete(true, new SubProgressMonitor(monitor, 1));
		}
	}

	private void doBuildFeature(
		ISiteBuildFeature sbfeature,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		IFeatureModel featureModel =
			sbfeature.getReferencedFeature().getModel();
		monitor.subTask("'" + featureModel.getFeature().getLabel() + "'");
		monitor.beginTask("Verifying...", 5);
		monitor.worked(1);
		ensureValid(featureModel, monitor);
		monitor.worked(1);
		monitor.setTaskName("Generating...");
		IFile scriptFile = makeScript(featureModel, monitor);
		monitor.worked(1);
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_RUNNING));
		runScript(
			scriptFile,
			sbfeature.getModel(),
			new SubProgressMonitor(monitor, 1));
		IProject buildProject =
			sbfeature.getModel().getUnderlyingResource().getProject();
		buildProject.refreshLocal(
			IResource.DEPTH_INFINITE,
			new SubProgressMonitor(monitor, 1));
		if (stateListener != null)
			stateListener.setBuilt(sbfeature);

	}

	private void postBuildCleanup(ISiteBuildFeature sbfeature) {
		ISiteBuildModel model = sbfeature.getModel();
		IProject siteProject = model.getUnderlyingResource().getProject();
		IFolder tempFolder =
			siteProject.getFolder(
				PDECore.SITEBUILD_DIR + "/" + PDECore.SITEBUILD_TEMP_FOLDER);
		if (tempFolder.exists()) {
			try {
				tempFolder.delete(true, false, null);
			} catch (CoreException e) {
			}
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
			String message = "Manifest file contains errors.  Aborted build";
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

	private IFile makeScript(
		IFeatureModel featureModel,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		FeatureBuildScriptGenerator generator =
			new FeatureBuildScriptGenerator();
		IFile featureFile = (IFile) featureModel.getUnderlyingResource();
		generator.setFeatureRootLocation(
			featureFile.getParent().getLocation().toOSString());
		generator.setGenerateIncludedFeatures(true);
		generator.setAnalyseChildren(true);
		IPath platform =
			Platform.getLocation().append(featureFile.getProject().getName());
		generator.setWorkingDirectory(platform.toOSString());
		generator.setDevEntries(new String[] { "bin" });

		ArrayList paths = new ArrayList();
		IFeatureModel[] models = PDECore.getDefault().getWorkspaceModelManager().getFeatureModels();
		for (int i = 0; i < models.length; i++) {
			try {
				paths.add(new URL("file:" + models[i].getInstallLocation() + Path.SEPARATOR + "feature.xml"));
			} catch (MalformedURLException e1) {
			}
		}		
		URL[] plugins = TargetPlatform.createPluginPath();
		URL[] features = (URL[]) paths.toArray(new URL[paths.size()]);
		URL[] all = new URL[plugins.length + paths.size()];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(features, 0, all, plugins.length, features.length);		
		generator.setPluginPath(all);
		
		setConfigInfo(featureModel.getFeature());
		generator.setFeature(featureModel.getFeature().getId());
		generator.generate();
		return featureFile.getProject().getFile("build.xml");
	}
	
	private void setConfigInfo(IFeature feature) throws CoreException {
		String os = feature.getOS() == null ? "*" : feature.getOS();
		String ws = feature.getWS() == null ? "*" : feature.getWS();
		String arch = feature.getArch() == null ? "*" : feature.getArch();
		
		FeatureBuildScriptGenerator.setConfigInfo(os + "," + ws + "," + arch);
	}

	private void runScript(
		IFile scriptFile,
		ISiteBuildModel buildModel,
		IProgressMonitor monitor)
		throws CoreException {
		AntRunner runner = new AntRunner();
		createLogFile(buildModel);
		runner.setBuildFileLocation(scriptFile.getLocation().toOSString());
		runner.setArguments(computeBuildArguments(buildModel));
		//URL[] customURLs = computeCustomClasspath();
		//if (customURLs!=null)
		//runner.setCustomClasspath(customURLs);
		runner.setExecutionTargets(computeTargets());
		runner.addBuildListener(BUILD_LISTENER_CLASS);
		runner.run(monitor);
	}

	/*private URL[] computeCustomClasspath() {
		// Add this plug-in's space
		AntCorePreferences preferences =
			AntCorePlugin.getPlugin().getPreferences();
		URL[] defaultAntURLs = preferences.getDefaultAntURLs();
		URL installURL = PDEPlugin.getDefault().getDescriptor().getInstallURL();
		try {
			int length = defaultAntURLs.length;
			URL[] customURLs = new URL[length + 1];
			System.arraycopy(defaultAntURLs, 0, customURLs, 0, length);
			customURLs[length] = new URL(installURL, "bin/");
			return customURLs;
		} catch (IOException e) {
			PDEPlugin.logException(e);
			return null;
		}
	}*/

	private void createLogFile(ISiteBuildModel buildModel) {
		IProject project = buildModel.getUnderlyingResource().getProject();
		IPath location =
			project.getLocation().append(PDECore.SITEBUILD_DIR).append(
				PDECore.SITEBUILD_LOG);
		logFile = new File(location.toOSString());
	}

	public File getLogFile() {
		return logFile;
	}

	private String computeBuildArguments(ISiteBuildModel buildModel) {
		StringBuffer buff = new StringBuffer();
		IProject project = buildModel.getUnderlyingResource().getProject();
		IPath projectLocation = project.getLocation();
		IPath resultFolder = projectLocation.append(PDECore.SITEBUILD_DIR);
		resultFolder = resultFolder.append(PDECore.SITEBUILD_TEMP_FOLDER);
		ISiteBuild siteBuild = buildModel.getSiteBuild();

		buff.append(
			createArgument("build.result.folder", resultFolder.toOSString()));
		String location =
			projectLocation.append(siteBuild.getPluginLocation()).toOSString();
		buff.append(createArgument("plugin.destination", location));
		location =
			projectLocation.append(siteBuild.getFeatureLocation()).toOSString();
		buff.append(createArgument("feature.destination", location));

		return buff.toString().trim();
	}

	private String createArgument(String name, String value) {
		return "-D" + name + "=" + "\"" + value + "\" ";
	}

	private String[] computeTargets() {
		return new String[] { "build.update.jar", "refresh" };
	}
}