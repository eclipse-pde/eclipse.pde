/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.osgi.framework.Bundle;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	
	private int MANIFEST = 0x1;
	private int EXTENSIONS = 0x2;
	private int BUILD = 0x4;
	
	private static boolean DEBUG = false;
	private static IProject[] EMPTY_LIST = new IProject[0];

	static {
		DEBUG  = PDECore.getDefault().isDebugging() 
					&& "true".equals(Platform.getDebugOption("org.eclipse.pde.core/validation")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private SelfVisitor fSelfVisitor = new SelfVisitor();
	
	private ClassChangeVisitor fClassFileVisitor = new ClassChangeVisitor();
	
	class ClassChangeVisitor implements IResourceDeltaVisitor {
		boolean hasChanged = false;
		boolean veto = false;

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
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta != null && type != (MANIFEST|EXTENSIONS|BUILD)) {
				int kind = delta.getKind();
				if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
					type = MANIFEST | EXTENSIONS | BUILD;
					if (DEBUG) {
						System.out.print("Needs to rebuild project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
						System.out.print(delta.getResource().getProjectRelativePath().toString());
						System.out.print(" - "); //$NON-NLS-1$
						System.out.println(kind == IResourceDelta.ADDED ? "added" : "removed"); //$NON-NLS-1$ //$NON-NLS-2$
					}						
					return false;
				}
				IResource resource = delta.getResource();
				if (resource instanceof IFile) {
					String name = resource.getName();
					IPath path = resource.getProjectRelativePath();
					if (isLocalizationFile(resource)) { 
						type |= MANIFEST | EXTENSIONS;
						if (DEBUG) {
							System.out.print("Needs to rebuild manifest and extensions in project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					} else if (path.equals(ICoreConstants.MANIFEST_PATH)) { 
						type |= MANIFEST | EXTENSIONS | BUILD;
						if (DEBUG) {
							System.out.print("Needs to rebuild project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					} else if (name.endsWith(".exsd") || path.equals(ICoreConstants.PLUGIN_PATH) || path.equals(ICoreConstants.FRAGMENT_PATH)) { //$NON-NLS-1$
						type |= EXTENSIONS;
						if (DEBUG) {
							System.out.print("Needs to rebuild extensions in project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					} else if (path.equals(ICoreConstants.BUILD_PROPERTIES_PATH)) {
						type |= BUILD;
						if (DEBUG) {
							System.out.print("Needs to rebuild build.properties in project [" + getProject().getName() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
							System.out.print(delta.getResource().getProjectRelativePath().toString());
							System.out.println(" - changed"); //$NON-NLS-1$
						}
					}
				}
			}
			return type != (MANIFEST|EXTENSIONS|BUILD);
		}		
		public int getType() {
			return type;
		}		
		public void reset() {
			type = 0;
		}
	}
	
	private boolean isLocalizationFile(IResource file) {
		IPluginModelBase model = PluginRegistry.findModel(getProject());
		String localization = null;
		if (model instanceof IBundlePluginModelBase) {
			localization = ((IBundlePluginModelBase)model).getBundleLocalization();
		} else {
			localization = "plugin"; //$NON-NLS-1$
		}
		if (localization != null)
			return file.getProjectRelativePath().equals(new Path(localization + ".properties")); //$NON-NLS-1$
		return false;
	}
	
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {		
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE 
			|| monitor.isCanceled())
			return EMPTY_LIST;

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
			if (DEBUG) {
				System.out.println("Project [" + getProject().getName() + "] - full build"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return MANIFEST|EXTENSIONS|BUILD;
		}	
		
		// the project has been "touched" by PluginRebuilder to indicate
		// that one of the dependencies (either in the target or workspace)
		// has changed and a StateDelta was fired
		if (Boolean.TRUE.equals(project.getSessionProperty(PDECore.TOUCH_PROJECT))) {
			project.setSessionProperty(PDECore.TOUCH_PROJECT, null);
			if (DEBUG) {
				System.out.println("Dependencies Changed: Project [" + getProject().getName() + "] - full build"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return MANIFEST|EXTENSIONS|BUILD;
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
		if ((type & MANIFEST|EXTENSIONS) != (MANIFEST|EXTENSIONS)) {
			fClassFileVisitor.reset();
			delta.accept(fClassFileVisitor);
			if (fClassFileVisitor.hasChanged()) {
				type |= MANIFEST|EXTENSIONS;
				if (DEBUG) {
					System.out.println("Class files changed due to dependency changes: Project [" + getProject().getName() + "] - rebuild manifest files"); //$NON-NLS-1$ //$NON-NLS-2$
				}				
			}
		}
		return type;
	}
	
	private void validateProject(int type, IProgressMonitor monitor) {
		if ((type & MANIFEST|EXTENSIONS) != 0) {
			IProject project = getProject();
			IFile file = project.getFile("plugin.xml"); //$NON-NLS-1$
			if (!file.exists())
				file = project.getFile("fragment.xml"); //$NON-NLS-1$
			
			if (file.exists()) {
				validateFiles(file, type, monitor);
			} else if ((type & MANIFEST) != 0){	
				IFile manifestFile = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
				if (manifestFile.exists())
					validateManifestFile(manifestFile, monitor);
			}
		}
		if ((type & BUILD) != 0)
			validateBuildProperties(monitor);
	}

	private void validateManifestFile(IFile file, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);

		BundleErrorReporter reporter = new BundleErrorReporter(file);
		if (reporter != null) {
			reporter.validateContent(monitor);
			monitor.subTask(PDECoreMessages.Builders_updating);
		}
		monitor.done();
	}

	private void validateFiles(IFile file, int type, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);

		IFile bundleManifest = file.getProject().getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		XMLErrorReporter reporter = null;
		BundleErrorReporter bundleReporter = null;
		if (bundleManifest.exists()) {
			if ((type & EXTENSIONS) != 0)
				reporter = new ExtensionsErrorReporter(file);
			if ((type & MANIFEST) != 0)
				bundleReporter = new BundleErrorReporter(bundleManifest);
		} else if ((type & MANIFEST) != 0 || (type & EXTENSIONS) != 0){
			if (file.getName().equals("plugin.xml")) { //$NON-NLS-1$
				reporter = new PluginErrorReporter(file);
			} else if (file.getName().equals("fragment.xml")){ //$NON-NLS-1$
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
		if (monitor.isCanceled())
			return;
		IProject project = getProject();
		try {
			project.deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
		}
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		if (!file.exists()) {
			int severity = CompilerFlags.getFlag(project, CompilerFlags.P_BUILD);
			if (severity == CompilerFlags.IGNORE)
				return;
			// if build.properties doesn't exist and build problems != IGNORE, create a marker on the project bug 172451
			try {
				IMarker marker = project.createMarker(PDEMarkerFactory.MARKER_ID);
				marker.setAttribute(IMarker.SEVERITY, CompilerFlags.ERROR == severity ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
				marker.setAttribute(IMarker.MESSAGE, PDECoreMessages.ManifestConsistencyChecker_buildDoesNotExist);
			} catch (CoreException e) {
			}
		} else {
			monitor.subTask(PDECoreMessages.ManifestConsistencyChecker_buildPropertiesSubtask);
			BuildErrorReporter ber = new BuildErrorReporter(file);
			ber.validateContent(monitor);
		}
	}
	
}
