/*
 * Created on Oct 6, 2003
 */
package org.eclipse.pde.internal.ui.feature;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.eclipse.ant.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.builder.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

/**
 * @author melhem
 */
public class BuildSiteAction implements IObjectActionDelegate, IPreferenceConstants {
	
	private ISiteBuildModel fBuildModel;
	private IFile siteXML;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fBuildModel == null)
			return;
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IWorkspaceRunnable wop = new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						ISiteBuildFeature[] sbFeatures =
							fBuildModel.getSiteBuild().getFeatures();
						monitor.beginTask("", sbFeatures.length);
						deleteLogDestination();
						for (int i = 0; i < sbFeatures.length; i++) {
							try {
								monitor.setTaskName(PDEPlugin.getResourceString("SiteBuild.feature") + " " + sbFeatures[i].getId());
								doBuildFeature(sbFeatures[i], new SubProgressMonitor(monitor, 1));
							} catch (InvocationTargetException e) {
							} finally {
								deleteBuildFiles(sbFeatures[i].getReferencedFeature().getModel());
							}
						}
					}
				};
				try {
					PDEPlugin.getWorkspace().run(wop, monitor);
				} catch (CoreException e) {
					MessageDialog.openError(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString("SiteBuild.errorDialog"),
						PDEPlugin.getResourceString("SiteBuild.errorMessage"));
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
		} finally {
			postBuildCleanup();
		}
	}
	
	private void doBuildFeature(ISiteBuildFeature sbFeature, IProgressMonitor monitor)
		throws CoreException, InvocationTargetException {
		monitor.beginTask("", 10);
		IFeatureModel model = sbFeature.getReferencedFeature().getModel();
		IFile file = (IFile) model.getUnderlyingResource();
		if (file == null) {
			monitor.done();
			return;
		}
		if (!BaseBuildAction.ensureValid(file, new SubProgressMonitor(monitor, 1))) {
			monitor.done();
			return;
		}
		IFile scriptFile = makeScript(model, new SubProgressMonitor(monitor, 1));
		runScript(scriptFile, new SubProgressMonitor(monitor, 7));
	}

	private IFile makeScript(IFeatureModel featureModel, IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();
		IProject project = featureModel.getUnderlyingResource().getProject();
		generator.setFeatureRootLocation(project.getLocation().toOSString());
		generator.setGenerateIncludedFeatures(true);
		generator.setAnalyseChildren(true);
		generator.setWorkingDirectory(project.getLocation().toOSString());
		generator.setDevEntries(new String[] { "bin" });

		ArrayList paths = new ArrayList();
		IFeatureModel[] models =
			PDECore.getDefault().getWorkspaceModelManager().getFeatureModels();
		for (int i = 0; i < models.length; i++) {
			try {
				paths.add(
					new URL(
						"file:"
							+ models[i].getInstallLocation()
							+ Path.SEPARATOR
							+ "feature.xml"));
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
		monitor.done();
		return project.getFile("build.xml");
	}

	private void runScript(
		IFile scriptFile,
		IProgressMonitor monitor)
		throws CoreException {
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(scriptFile.getLocation().toOSString());
		runner.addUserProperties(createProperties());
		runner.setExecutionTargets(new String[] {"build.update.jar", "refresh"});
		runner.run(monitor);
		monitor.done();
	}
	
	private Map createProperties() throws CoreException {
		HashMap map = new HashMap(13);
		IProject project = fBuildModel.getUnderlyingResource().getProject();
		IPath path =
			new Path(PDECore.SITEBUILD_DIR).append(PDECore.SITEBUILD_TEMP_FOLDER);
		IFolder folder = project.getFolder(path);
		if (!folder.exists()) {
			CoreUtility.createFolder(folder, true, true, null);
		}
		map.put("build.result.folder", folder.getLocation().toOSString());

		ISiteBuild siteBuild = fBuildModel.getSiteBuild();
		map.put(
			"plugin.destination",
			project.getLocation().append(siteBuild.getPluginLocation()).toOSString());
		map.put(
			"feature.destination",
			project.getLocation().append(siteBuild.getFeatureLocation()).toOSString());

		map.put("baseos", TargetPlatform.getOS());
		map.put("basews", TargetPlatform.getWS());
		map.put("basearch", TargetPlatform.getOSArch());
		map.put("basenl", TargetPlatform.getNL());
		map.put("eclipse.running", "true");

		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		map.put("javacFailOnError", "true");
		map.put("javacDebugInfo", store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off");
		map.put("javacVerbose", store.getString(PROP_JAVAC_VERBOSE));
		map.put("javacSource", store.getString(PROP_JAVAC_SOURCE));
		map.put("javacTarget", store.getString(PROP_JAVAC_TARGET));

		return map;
	}
	
	private void setConfigInfo(IFeature feature) throws CoreException {
		String os = feature.getOS() == null ? "*" : feature.getOS();
		String ws = feature.getWS() == null ? "*" : feature.getWS();
		String arch = feature.getArch() == null ? "*" : feature.getArch();		
		FeatureBuildScriptGenerator.setConfigInfo(os + "," + ws + "," + arch);
	}
	
	private void postBuildCleanup() {
		IProject siteProject = fBuildModel.getUnderlyingResource().getProject();
		IFolder tempFolder =
			siteProject.getFolder(
				PDECore.SITEBUILD_DIR + "/" + PDECore.SITEBUILD_TEMP_FOLDER);
		try {
			if (tempFolder.exists()) {
				tempFolder.delete(true, false, null);
			}
			if (siteXML != null)
				siteXML.touch(null);			
			siteProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fBuildModel = null;
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				siteXML = (IFile)obj;
				IProject project = siteXML.getProject();
				IWorkspaceModelManager manager =
					PDECore.getDefault().getWorkspaceModelManager();
				IResource buildFile =
					project.findMember(
						new Path(PDECore.SITEBUILD_DIR).append(
							PDECore.SITEBUILD_PROPERTIES));
				if (buildFile != null && buildFile instanceof IFile) {
					manager.connect(buildFile, this);
					fBuildModel = (ISiteBuildModel) manager.getModel(buildFile, this);
					try {
						fBuildModel.load();
					} catch (CoreException e) {
					}
					manager.disconnect(buildFile, this);
				}
			}
		}
	}
	
	private void deleteBuildFiles(IFeatureModel model) throws CoreException {
		deleteBuildFile(model);
		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (int i = 0; i < plugins.length; i++) {
			ModelEntry entry =
				manager.findEntry(plugins[i].getId(), plugins[i].getVersion());
			if (entry != null) {
				deleteBuildFile(entry.getActiveModel());
			}
		}
	}

	public void deleteBuildFile(IModel model) throws CoreException {
		IResource resource = model.getUnderlyingResource();
		if (resource == null && isCustomBuild(model))
			return;
		
		IProject project = resource.getProject();
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
		}
		resource = project.getFile("build.xml");
		if (resource.exists())
			resource.delete(true, null);
		
		IFolder folder = project.getFolder("temp.folder");
		if (folder.exists()) {
			IResource[] files = folder.members();
			for (int i = 0; i < files.length; i++) {
				if (files[i] instanceof IFile)
					files[i].move(getLogDestination().getFullPath().append(files[i].getName()), true, null);
			}
			folder.delete(true, null);
		}
	}

	protected boolean isCustomBuild(IModel model) throws CoreException {
		IBuildModel buildModel = null;
		IFile buildFile =
			model.getUnderlyingResource().getProject().getFile("build.properties");
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		if (buildModel != null) {
			IBuild build = buildModel.getBuild();
			IBuildEntry entry = build.getEntry("custom");
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].equals("true"))
						return true;
				}
			}
		}
		return false;
	}
	
	private void deleteLogDestination() {
		IFolder folder = siteXML.getProject().getFolder("logs");
		if (folder.exists()) {
			try {
				folder.delete(true, null);
			} catch (CoreException e) {
			}
		}
	}
	
	private IFolder getLogDestination() {
		IFolder folder = siteXML.getProject().getFolder("logs");
		if (!folder.exists()) {
			try {
				CoreUtility.createFolder(folder, true, true, null);
			} catch (CoreException e) {
			}
		}
		return folder;
	}

}
