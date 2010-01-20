/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.project.PDEProject;

/**
 * Helper class for the various export operation classes, making it easier to export using workspace
 * compiled files rather than having PDE Build compile everything on its own.  Provides access to 
 * methods in debug that determine what projects need to be built before the operation as well as
 * checking for errors. 
 * 
 * @see FeatureExportOperation
 * @see PluginExportOperation
 */
public class WorkspaceExportHelper extends LaunchConfigurationDelegate {

	private IProject[] fWorkspaceProjects;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// This class is not intended to be launched. 
	}

	/**
	 * Builds the workspace projects that are being exported or are required plug-ins
	 * of the exported items.  Uses the incremental builder.
	 * 
	 * @param exportedItems The plugins or features being exported
	 * @param monitor a progress monitor or <code>null</code> if progress reporting is not desired 
	 * @throws CoreException
	 */
	public void buildBeforeExport(Object[] exportedItems, IProgressMonitor monitor) throws CoreException {
		IProject[] projects = getExportedWorkspaceProjects(exportedItems);
		for (int i = 0; i < projects.length; i++) {
			projects[i].build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}
	}

	/**
	 * Checks the workspace projects that are being exported or are required plug-ins
	 * of the exported items for build errors.  A project will be reported as having an
	 * error if it has a marker with a severity of error and is of Java model or PDE type.
	 * 
	 * @param exportedItems the plugins or features being exported
	 * @return set of IProjects containing errors
	 * @throws CoreException
	 */
	public Set checkForErrors(Object[] exportedItems) throws CoreException {
		IProject[] projects = getExportedWorkspaceProjects(exportedItems);
		Set projectsWithErrors = new HashSet(projects.length);
		for (int i = 0; i < projects.length; i++) {
			IMarker[] markers = projects[i].findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			if (markers.length > 0) {
				for (int j = 0; j < markers.length; j++) {
					Integer severity = (Integer) (markers[j].getAttribute(IMarker.SEVERITY));
					if (severity != null && severity.intValue() >= IMarker.SEVERITY_ERROR) {
						if (markers[j].getType().equals(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER) || markers[j].getType().equals(PDEMarkerFactory.MARKER_ID)) {
							projectsWithErrors.add(projects[i]);
							break;
						}
					}
				}
			}
		}
		return projectsWithErrors;
	}

	/**
	 * Returns a map containing information associating libraries to the output locations the
	 * workspace compiles them to.  Uses information in the build.properties and the classpath.
	 * The map will be of the following form: 
	 * String symbolic name > lib output map
	 * The lib output map will be of the following form:
	 * String lib name > Set of IPath output folders
	 *
	 * @param exportedItems the plugins or features being exported
	 * @return a map of library output folders for each plugin in the workspace
	 */
	public Map getWorkspaceOutputFolders(Object[] exportedItems) throws CoreException {
		IProject[] projects = getExportedWorkspaceProjects(exportedItems);
		Map result = new HashMap(projects.length);
		for (int i = 0; i < projects.length; i++) {
			IFile buildFile = PDEProject.getBuildProperties(projects[i]);
			if (buildFile.exists()) {
				IBuildModel buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
				IJavaProject javaProject = JavaCore.create(projects[i]);
				if (javaProject.exists()) {
					Map modelOutput = getPluginOutputFolders(buildModel, javaProject);
					if (!modelOutput.isEmpty()) {
						IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(projects[i]);
						if (model != null) {
							result.put(model.getBundleDescription().getSymbolicName(), modelOutput);
						}
					}
				}
			}
		}
		return result;
	}

	private Map getPluginOutputFolders(IBuildModel buildModel, IJavaProject javaProject) throws JavaModelException {
		Map outputEntries = new HashMap();

		IBuildEntry[] buildEntries = buildModel.getBuild().getBuildEntries();
		for (int i = 0; i < buildEntries.length; i++) {
			String name = buildEntries[i].getName();
			if (name.startsWith(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX)) {
				Set outputPaths = new HashSet();

				String[] sourceFolders = buildEntries[i].getTokens();
				for (int j = 0; j < sourceFolders.length; j++) {

					IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
					for (int k = 0; k < classpathEntries.length; k++) {
						if (classpathEntries[k].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							IPath sourcePath = classpathEntries[k].getPath().removeFirstSegments(1); // Entries include project as first segment
							if (sourcePath.equals(new Path(sourceFolders[j]))) {
								IPath outputPath = classpathEntries[k].getOutputLocation();
								if (outputPath == null) {
									outputPath = javaProject.getOutputLocation();
								}
								outputPaths.add(outputPath.removeFirstSegments(1)); // Entries include project as first segment
							}
						}
					}
				}
				if (!outputPaths.isEmpty()) {
					outputEntries.put(name.substring(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX.length()), outputPaths);
				}
			}
		}
		return outputEntries;
	}

	private IProject[] getExportedWorkspaceProjects(Object[] exportedItems) throws CoreException {
		if (fWorkspaceProjects == null) {
			// TODO This won't work for nested features either
			Set projects = new HashSet();
			for (int i = 0; i < exportedItems.length; i++) {
				if (exportedItems[i] instanceof IPluginModelBase) {
					IPath installLocation = new Path(((IPluginModelBase) exportedItems[i]).getInstallLocation());
					IProject project = PDECore.getWorkspace().getRoot().getProject(installLocation.lastSegment());
					if (project.exists()) {
						projects.add(project);
					}
				} else if (exportedItems[i] instanceof IFeatureModel) {
					IFeatureModel feature = (IFeatureModel) exportedItems[i];
					IFeaturePlugin[] plugins = feature.getFeature().getPlugins();
					for (int j = 0; j < plugins.length; j++) {
						IPluginModelBase plugin = PDECore.getDefault().getModelManager().findModel(plugins[j].getId());
						if (plugin != null) {
							IPath installLocation = new Path(plugin.getInstallLocation());
							IProject project = PDECore.getWorkspace().getRoot().getProject(installLocation.lastSegment());
							if (project.exists()) {
								projects.add(project);
							}
						}
					}

				}
			}
			fWorkspaceProjects = computeReferencedBuildOrder((IProject[]) projects.toArray(new IProject[projects.size()]));
		}
		return fWorkspaceProjects;
	}

}
