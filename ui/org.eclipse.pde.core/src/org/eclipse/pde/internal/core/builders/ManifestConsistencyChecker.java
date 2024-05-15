/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 500495
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.Bundle;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {

	private static final int MANIFEST = 0x1;
	private static final int EXTENSIONS = 0x2;
	private static final int BUILD = 0x4;
	private static final int STRUCTURE = 0x8;
	static final IPath SETTINGS_PATH = IPath.fromOSString(".settings"); //$NON-NLS-1$

	private static IProject[] EMPTY_LIST = new IProject[0];

	private final SelfVisitor fSelfVisitor = new SelfVisitor();

	/*
	 * Bug 549839:In case auto-building on PDE compiler setting change is not
	 * desired, specify VM property: {@code
	 * -Dorg.eclipse.disableAutoBuildOnSettingsChange=true}
	 */
	private static final boolean DISABLE_AUTO_BUILDING_ON_SETTINGS_CHANGE = Boolean.getBoolean("org.eclipse.disableAutoBuildOnSettingsChange"); //$NON-NLS-1$

	private final ClassChangeVisitor fClassFileVisitor = new ClassChangeVisitor();

	static class ClassChangeVisitor implements IResourceDeltaVisitor {
		boolean hasChanged = false;
		boolean veto = false;

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta != null && !veto) {
				int kind = delta.getKind();
				if (kind == IResourceDelta.CHANGED) {
					IResource resource = delta.getResource();
					if (resource instanceof IFile) {
						String extension = resource.getFileExtension();
						// do nothing if a java file has changed.
						if ("java".equals(extension)) { //$NON-NLS-1$
							veto = true;
						} else if ("class".equals(extension) && !hasChanged) { //$NON-NLS-1$
							// only interested in .class file changes
							hasChanged = true;
						}
					}
					return !veto;
				}
			}
			return false;
		}

		public void reset() {
			veto = false;
			hasChanged = false;
		}

		public boolean hasChanged() {
			return hasChanged && !veto;
		}

	}

	class SelfVisitor implements IResourceDeltaVisitor {
		int type = 0;

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta != null && delta.findMember(SETTINGS_PATH) != null) {
				if (!DISABLE_AUTO_BUILDING_ON_SETTINGS_CHANGE) {
					type |= MANIFEST | EXTENSIONS | BUILD | STRUCTURE;
				}
			}
			if (delta != null && type != (MANIFEST | EXTENSIONS | BUILD | STRUCTURE)) {
				int kind = delta.getKind();
				if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
					type = MANIFEST | EXTENSIONS | BUILD | STRUCTURE;
					if (PDECore.DEBUG_VALIDATION) {
						System.out.print("Needs to rebuild project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
						System.out.print(delta.getResource().getProjectRelativePath().toString());
						System.out.print(" - "); //$NON-NLS-1$
						System.out.println(kind == IResourceDelta.ADDED ? "added" : "removed"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return false;
				}
				IResource resource = delta.getResource();
				// by ignoring derived resources we should scale a bit better.
				if (resource.isDerived()) {
					return false;
				}
				if (resource.getType() == IResource.FILE) {
					IFile file = (IFile) resource;
					IProject project = file.getProject();
					String name = resource.getName();
					if (isLocalizationFile(resource)) {
						type |= MANIFEST | EXTENSIONS;
						if (PDECore.DEBUG_VALIDATION) {
							System.out.print("Needs to rebuild manifest and extensions in project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					} else if (file.equals(PDEProject.getManifest(project))) {
						type |= MANIFEST | EXTENSIONS | BUILD;
						if (PDECore.DEBUG_VALIDATION) {
							System.out.print("Needs to rebuild project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					} else if (name.endsWith(".exsd") || file.equals(PDEProject.getPluginXml(project)) || file.equals(PDEProject.getFragmentXml(project))) { //$NON-NLS-1$
						type |= EXTENSIONS | BUILD;
						if (PDECore.DEBUG_VALIDATION) {
							System.out.print("Needs to rebuild project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					} else if (file.equals(PDEProject.getBuildProperties(project))) {
						type |= BUILD;
						if (PDECore.DEBUG_VALIDATION) {
							System.out.print("Needs to rebuild build.properties in project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					}
				}
			}
			return type != (MANIFEST | EXTENSIONS | BUILD | STRUCTURE);
		}

		public int getType() {
			return type;
		}

		public void reset() {
			type = 0;
		}
	}

	private boolean isLocalizationFile(IResource file) {
		return file.equals(PDEProject.getLocalizationFile(getProject()));
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE || monitor.isCanceled()) {
			return EMPTY_LIST;
		}

		IProject project = getProject();
		if (!WorkspaceModelManager.isBinaryProject(project)) {
			int type = getDeltaType(project);
			if (type != 0) {
				validateProject(type, monitor);
			}
		}
		return EMPTY_LIST;
	}

	private int getDeltaType(IProject project) throws CoreException {
		IResourceDelta delta = getDelta(project);

		// always do a build of the project if a full build or an unspecified change has occurred
		if (delta == null) {
			if (PDECore.DEBUG_VALIDATION) {
				System.out.println("Project [" + getProject().getName() + "] - full build"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return MANIFEST | EXTENSIONS | BUILD | STRUCTURE;
		}

		// the project has been "touched" by PluginRebuilder to indicate
		// that one of the dependencies (either in the target or workspace)
		// has changed and a StateDelta was fired
		if (Boolean.TRUE.equals(project.getSessionProperty(PDECore.TOUCH_PROJECT))) {
			project.setSessionProperty(PDECore.TOUCH_PROJECT, null);
			if (PDECore.DEBUG_VALIDATION) {
				System.out.println("Dependencies Changed: Project [" + getProject().getName() + "] - full build"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return MANIFEST | EXTENSIONS | BUILD;
		}

		// check if any "significant" files have been changed/added/removed
		// and build a subset or all manifest files accordingly
		fSelfVisitor.reset();
		delta.accept(fSelfVisitor);
		int type = fSelfVisitor.getType();

		// catch anything we have missed
		// For example, upon startup, when target has changed since shutdown
		// we depend on class file changes in the project that have resulted
		// from the Java compiler detecting the change in classpath.
		// Note that we do NOT validate anything if there was a change
		// in a .java file.  A change in a .java file means that the user has modified
		// its content and this does not warrant a rebuild of manifest files.
		if ((type & MANIFEST | EXTENSIONS) != (MANIFEST | EXTENSIONS)) {
			fClassFileVisitor.reset();
			delta.accept(fClassFileVisitor);
			if (fClassFileVisitor.hasChanged()) {
				type |= MANIFEST | EXTENSIONS | BUILD;
				if (PDECore.DEBUG_VALIDATION) {
					System.out.println("Class files changed due to dependency changes: Project [" + getProject().getName() + "] - rebuild manifest and properties files"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		return type;
	}

	private void validateProject(int type, IProgressMonitor monitor) {
		if (!PDEBuilderHelper.hasManifestBuilder(getProject())) {
			ILog.get().error(String.format(
					"eclipse.pde/issues/1185: validateBuildPropertiesExists called for project without ManifestBuilder: %s", //$NON-NLS-1$
					getProject().getName()), new IllegalStateException());
			return;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, PDECoreMessages.ManifestConsistencyChecker_builderTaskName, getWorkAmount(type));
		if ((type & STRUCTURE) != 0) {
			validateProjectStructure(type, subMonitor.split(1));
		}

		if ((type & (MANIFEST | EXTENSIONS)) != 0) {
			IProject project = getProject();
			IFile file = PDEProject.getPluginXml(project);
			if (!file.exists()) {
				file = PDEProject.getFragmentXml(project);
			}

			if (file.exists()) {
				validateFiles(file, type, monitor);
			} else if ((type & MANIFEST) != 0) {
				IFile manifestFile = PDEProject.getManifest(project);
				if (manifestFile.exists()) {
					validateManifestFile(manifestFile, subMonitor.split(1));
				}
			}
		}
		if ((type & BUILD) != 0) {
			validateBuildProperties(subMonitor.split(1));
		}
	}

	private int getWorkAmount(int type) {
		int work = 1;
		if ((type & (MANIFEST | EXTENSIONS)) != 0) {
			++work;
		}
		if ((type & BUILD) != 0) {
			++work;
		}
		return work;
	}

	private void validateProjectStructure(int type, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
		// clear markers from project
		IProject project = getProject();
		try {
			project.deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
		}

		// make sure build.properties exists
		validateBuildPropertiesExists(project);

		// if META-INF exists, make sure MANIFEST.MF exists in correct casing
		validateManifestCasing(project);
	}

	private void validateManifestFile(IFile file, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);

		BundleErrorReporter reporter = new BundleErrorReporter(file);
		reporter.validateContent(monitor);
		monitor.subTask(PDECoreMessages.Builders_updating);
		monitor.done();
	}

	private void validateFiles(IFile file, int type, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);

		IFile bundleManifest = PDEProject.getManifest(getProject());
		XMLErrorReporter reporter = null;
		BundleErrorReporter bundleReporter = null;
		if (bundleManifest.exists()) {
			if ((type & EXTENSIONS) != 0) {
				reporter = new ExtensionsErrorReporter(file);
			}
			if ((type & MANIFEST) != 0) {
				bundleReporter = new BundleErrorReporter(bundleManifest);
			}
		} else if ((type & MANIFEST) != 0 || (type & EXTENSIONS) != 0) {
			if (file.equals(PDEProject.getPluginXml(getProject()))) {
				reporter = new PluginErrorReporter(file);
			} else if (file.equals(PDEProject.getFragmentXml(getProject()))) {
				reporter = new FragmentErrorReporter(file);
			}
		}
		if (reporter != null) {
			DefaultSAXParser.parse(file, reporter);
			reporter.validateContent(monitor);
			monitor.subTask(PDECoreMessages.Builders_updating);
		}
		if (bundleReporter != null) {
			bundleReporter.validateContent(monitor);
			monitor.subTask(PDECoreMessages.Builders_updating);
		}
		monitor.done();
	}

	private void validateBuildProperties(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
		IProject project = getProject();
		IFile file = PDEProject.getBuildProperties(project);
		if (file.exists()) {
			monitor.subTask(PDECoreMessages.ManifestConsistencyChecker_buildPropertiesSubtask);
			BuildErrorReporter ber = new BuildErrorReporter(file);
			ber.validateContent(monitor);
		}
	}

	// Will place a marker on the project if the build.properties does not exist
	private void validateBuildPropertiesExists(IProject project) {
		IFile file = PDEProject.getBuildProperties(project);
		if (!file.exists()) {
			int severity = CompilerFlags.getFlag(project, CompilerFlags.P_BUILD);
			if (severity == CompilerFlags.IGNORE) {
				return;
			}
			// if build.properties doesn't exist and build problems != IGNORE, create a marker on the project bug 172451
			try {
				Map<String, Object> attributes = new HashMap<>();
				attributes.put(IMarker.SEVERITY, CompilerFlags.ERROR == severity ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
				attributes.put(IMarker.MESSAGE, PDECoreMessages.ManifestConsistencyChecker_buildDoesNotExist);
				attributes.put(PDEMarkerFactory.compilerKey, CompilerFlags.P_BUILD);

				project.createMarker(PDEMarkerFactory.MARKER_ID, attributes);
			} catch (CoreException e) {
				ILog.get().error("Unable to create marker", e); //$NON-NLS-1$
			}
		}
	}

	// Will place a marker on either the project (if META-INF exist but not a MANIFEST.MF) or on the MANIFEST.MF file with incorrect casing.
	private void validateManifestCasing(IProject project) {
		IFolder manifestFolder = PDEProject.getMetaInf(project);
		if (manifestFolder.exists()) {
			try {
				manifestFolder.deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ONE);
			} catch (CoreException e1) {
			}
			// exit if the proper casing exists (should be majority of the time)
			if (PDEProject.getManifest(project).exists()) {
				return;
			}

			IPath location = manifestFolder.getLocation();
			if (location != null) {
				File metaFolder = location.toFile();
				String[] fileList = metaFolder.list(new ManifestFilter());

				if (fileList == null || fileList.length == 0) {
					// no MANIFEST.MF at all -> flag the project
					try {
						project.createMarker(PDEMarkerFactory.MARKER_ID,
								Map.of(//
										IMarker.SEVERITY, IMarker.SEVERITY_ERROR, //
										IMarker.MESSAGE,
										PDECoreMessages.ManifestConsistencyChecker_manifestDoesNotExist));
					} catch (CoreException e) {
					}
				} else {
					// check for misspelled MANIFEST.MF files
					for (String fileName : fileList) {
						IFile currentFile = manifestFolder.getFile(fileName);
						try {
							IMarker marker = currentFile.createMarker(PDEMarkerFactory.MARKER_ID);
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
							marker.setAttribute(IMarker.MESSAGE, PDECoreMessages.ManifestConsistencyChecker_manifestMisspelled);
						} catch (CoreException e) {
						}
					}
				}
			}
		}
	}

	static class ManifestFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return (name.equalsIgnoreCase(ICoreConstants.MANIFEST_FILENAME));
		}
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, NLS.bind(PDECoreMessages.ManifestConsistencyChecker_0, getProject().getName()), 1);
		// clean problem markers on the project
		cleanProblems(getProject(), IResource.DEPTH_ZERO);
		// clean the manifest directory (since errors can be created on manifest
		// files with incorrect casing)
		IFile manifestFile = PDEProject.getManifest(getProject());
		cleanProblems(manifestFile.getParent(), IResource.DEPTH_ONE);
		// clean plug-in XML file
		cleanProblems(PDEProject.getPluginXml(getProject()), IResource.DEPTH_ZERO);
		// clean fragment XML file
		cleanProblems(PDEProject.getFragmentXml(getProject()), IResource.DEPTH_ZERO);
		// clean build properties
		cleanProblems(PDEProject.getBuildProperties(getProject()), IResource.DEPTH_ZERO);
		localmonitor.split(1);
	}

	/**
	 * Cleans PDE problems from the given resource to the specified depth.
	 *
	 * @param resource resource to delete markers from
	 * @param depth one of the depth constants in {@link IResource}
	 */
	private void cleanProblems(IResource resource, int depth) throws CoreException {
		if (resource.exists()) {
			resource.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, depth);
		}
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return new MultiRule(Arrays.stream(getProject().getWorkspace().getRoot().getProjects())
				.filter(PDEBuilderHelper::isPDEProject).toArray(ISchedulingRule[]::new));
	}
}
