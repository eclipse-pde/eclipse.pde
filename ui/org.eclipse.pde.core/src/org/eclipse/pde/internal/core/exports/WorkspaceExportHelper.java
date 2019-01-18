/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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

	@Override
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
		for (IProject project : projects) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
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
	public Set<IProject> checkForErrors(Object[] exportedItems) throws CoreException {
		IProject[] projects = getExportedWorkspaceProjects(exportedItems);
		Set<IProject> projectsWithErrors = new HashSet<>(projects.length);
		for (IProject project : projects) {
			IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			if (markers.length > 0) {
				for (IMarker marker : markers) {
					Integer severity = (Integer) (marker.getAttribute(IMarker.SEVERITY));
					if (severity != null && severity.intValue() >= IMarker.SEVERITY_ERROR) {
						if (marker.getType().equals(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER) || marker.getType().equals(PDEMarkerFactory.MARKER_ID)) {
							projectsWithErrors.add(project);
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
	public Map<String, Map<String, Set<IPath>>> getWorkspaceOutputFolders(Object[] exportedItems) throws CoreException {
		IProject[] projects = getExportedWorkspaceProjects(exportedItems);
		Map<String, Map<String, Set<IPath>>> result = new LinkedHashMap<>(projects.length);
		for (IProject project : projects) {
			IFile buildFile = PDEProject.getBuildProperties(project);
			if (buildFile.exists()) {
				IBuildModel buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
				IJavaProject javaProject = JavaCore.create(project);
				if (javaProject.exists()) {
					Map<String, Set<IPath>> modelOutput = getPluginOutputFolders(buildModel, javaProject);
					if (!modelOutput.isEmpty()) {
						IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
						if (model != null) {
							result.put(model.getBundleDescription().getSymbolicName(), modelOutput);
						}
					}
				}
			}
		}
		return result;
	}

	private Map<String, Set<IPath>> getPluginOutputFolders(IBuildModel buildModel, IJavaProject javaProject) throws JavaModelException {
		Map<String, Set<IPath>> outputEntries = new LinkedHashMap<>();

		IBuildEntry[] buildEntries = buildModel.getBuild().getBuildEntries();
		for (IBuildEntry buildEntry : buildEntries) {
			String name = buildEntry.getName();
			if (name.startsWith(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX)) {
				Set<IPath> outputPaths = new LinkedHashSet<>();

				String[] sourceFolders = buildEntry.getTokens();
				for (String sourceFolder : sourceFolders) {

					IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
					for (IClasspathEntry classpathEntry : classpathEntries) {
						if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							IPath sourcePath = classpathEntry.getPath().removeFirstSegments(1); // Entries include project as first segment
							if (sourcePath.equals(new Path(sourceFolder))) {
								IPath outputPath = classpathEntry.getOutputLocation();
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
			Set<IProject> projects = new HashSet<>();
			for (Object exportedItem : exportedItems) {
				if (exportedItem instanceof IPluginModelBase) {
					IPath installLocation = new Path(((IPluginModelBase) exportedItem).getInstallLocation());
					IProject project = PDECore.getWorkspace().getRoot().getProject(installLocation.lastSegment());
					if (project.exists()) {
						projects.add(project);
					}
				} else if (exportedItem instanceof IFeatureModel) {
					IFeatureModel feature = (IFeatureModel) exportedItem;
					IFeaturePlugin[] plugins = feature.getFeature().getPlugins();
					for (IFeaturePlugin plugin : plugins) {
						IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(plugin.getId());
						if (model != null) {
							IPath installLocation = new Path(model.getInstallLocation());
							IProject project = PDECore.getWorkspace().getRoot().getProject(installLocation.lastSegment());
							if (project.exists()) {
								projects.add(project);
							}
						}
					}

				}
			}
			fWorkspaceProjects = computeReferencedBuildOrder(projects.toArray(new IProject[projects.size()]));
		}
		return fWorkspaceProjects;
	}

}
