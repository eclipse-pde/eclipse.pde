/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.osgi.framework.Bundle;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE || monitor.isCanceled())
			return new IProject[0];

		IProject project = getProject();
		
		// Ignore binary plug-in projects
		if (!WorkspaceModelManager.isBinaryPluginProject(project))
			checkThisProject(monitor);
		return new IProject[0];
	}
		
	private void checkThisProject(IProgressMonitor monitor) {
		IProject project = getProject();
		IFile file = project.getFile("plugin.xml"); //$NON-NLS-1$
		if (!file.exists())
			file = project.getFile("fragment.xml"); //$NON-NLS-1$
		
		if (file.exists()) {
			checkFile(file, monitor);
		} else {	
			IFile manifestFile = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
			if (manifestFile.exists())
				checkManifestFile(manifestFile, monitor);
		}
		checkProjectDescription(monitor);
		checkBuildProperties(monitor);
	}

	private void checkManifestFile(IFile file, IProgressMonitor monitor) {
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

	private void checkFile(IFile file, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);

		IFile bundleManifest = file.getProject().getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		XMLErrorReporter reporter = null;
		BundleErrorReporter bundleReporter = null;
		if (bundleManifest.exists()) {
			reporter = new ExtensionsErrorReporter(file);
			bundleReporter = new BundleErrorReporter(bundleManifest);
		} else if (file.getName().equals("plugin.xml")) { //$NON-NLS-1$
			reporter = new PluginErrorReporter(file);
		} else if (file.getName().equals("fragment.xml")){ //$NON-NLS-1$
			reporter = new FragmentErrorReporter(file);
		}
		if (reporter != null) {
			ValidatingSAXParser.parse(file, reporter);
			reporter.validateContent(monitor);
			monitor.subTask(PDECoreMessages.Builders_updating);
		}
		if (bundleReporter != null) {
			bundleReporter.validateContent(monitor);
			monitor.subTask(PDECoreMessages.Builders_updating);
		}
		monitor.done();
	}
	
	private void checkProjectDescription(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		monitor.subTask(NLS.bind(PDECoreMessages.Builders_verifying, ".project")); //$NON-NLS-1$
		IProject project = getProject();
		IFile file = project.getFile(".project"); //$NON-NLS-1$
		if (!file.exists())
			return;
		try {
			file.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
			IProject[] refProjects = project.getDescription().getReferencedProjects();
			if (refProjects.length > 0) {
				try {
					IMarker marker = new PDEMarkerFactory().createMarker(file, PDEMarkerFactory.PROJECT_BUILD_ORDER_ENTRIES);
					marker.setAttribute(IMarker.MESSAGE, PDECoreMessages.ManifestConsistencyChecker_projectCheck);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
					marker.setAttribute(IMarker.LINE_NUMBER, 5);
				} catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		} catch (CoreException e) {
		}
		monitor.done();
	}
	
	private void checkBuildProperties(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		IProject project = getProject();
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		if (!file.exists())
			return;

		monitor.subTask(PDECoreMessages.ManifestConsistencyChecker_buildPropertiesSubtask);
		int severity = CompilerFlags.getFlag(project, CompilerFlags.P_BUILD);
		BuildErrorReporter ber = new BuildErrorReporter(file, severity);
		ber.validateContent(monitor);
	}
}
