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
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.build.builder.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

/**
 * @author melhem
 */
public class BuildSiteAction implements IObjectActionDelegate {
	
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
						for (int i = 0; i < sbFeatures.length; i++) {
							try {
								monitor.setTaskName(PDEPlugin.getResourceString("SiteBuild.feature") + " " + sbFeatures[i].getId());
								doBuildFeature(sbFeatures[i], new SubProgressMonitor(monitor, 1));
							} catch (Exception e) {
							} finally {
								if (siteXML != null)
									siteXML.touch(null);								
							}
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

		IProject project = fBuildModel.getUnderlyingResource().getProject();
		project.refreshLocal(
			IResource.DEPTH_INFINITE,
			new SubProgressMonitor(monitor, 1));
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
	
	private Map createProperties() {
		HashMap map = new HashMap(3);
		IPath location = fBuildModel.getUnderlyingResource().getProject().getLocation();

		IPath resultFolder = location.append(PDECore.SITEBUILD_DIR);
		resultFolder = resultFolder.append(PDECore.SITEBUILD_TEMP_FOLDER);
		map.put("build.result.folder", resultFolder.toOSString());
		
		ISiteBuild siteBuild = fBuildModel.getSiteBuild();
		map.put("plugin.destination", location.append(siteBuild.getPluginLocation()).toOSString());
		map.put("feature.destination", location.append(siteBuild.getFeatureLocation()).toOSString());

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
		if (tempFolder.exists()) {
			try {
				tempFolder.delete(true, false, null);
			} catch (CoreException e) {
			}
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

}
