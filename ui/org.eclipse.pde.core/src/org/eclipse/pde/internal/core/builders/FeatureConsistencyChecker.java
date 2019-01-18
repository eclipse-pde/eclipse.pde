/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Arrays;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.Bundle;

public class FeatureConsistencyChecker extends IncrementalProjectBuilder {
	class DeltaVisitor implements IResourceDeltaVisitor {
		private final IProgressMonitor monitor;

		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with feature nature
				IProject project = (IProject) resource;
				try {
					return (project.hasNature(PDE.FEATURE_NATURE));
				} catch (CoreException e) {
					PDECore.logException(e);
					return false;
				}
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isManifestFile(candidate)) {
					// That's it, but only check it if it has been added or changed
					if (delta.getKind() != IResourceDelta.REMOVED) {
						checkFile(candidate, monitor);
						return true;
					}
				}
			}
			return false;
		}
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE || monitor.isCanceled()) {
			return new IProject[0];
		}

		if (WorkspaceModelManager.isBinaryProject(getProject())) {
			return new IProject[0];
		}

		IResourceDelta delta = null;

		if (kind != FULL_BUILD) {
			delta = getDelta(getProject());
		}

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			checkProject(monitor);
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return new IProject[0];
	}

	private void checkProject(IProgressMonitor monitor) {
		IFile file = PDEProject.getFeatureXml(getProject());
		if (file.exists()) {
			checkFile(file, monitor);
		}
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);
		FeatureErrorReporter reporter = new FeatureErrorReporter(file);
		DefaultSAXParser.parse(file, reporter);
		if (reporter.getErrorCount() == 0) {
			reporter.validateContent(monitor);
		}
		monitor.subTask(PDECoreMessages.Builders_updating);
		monitor.done();
	}

	private boolean isManifestFile(IFile file) {
		return file.equals(PDEProject.getFeatureXml(file.getProject()));
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		IFile file = PDEProject.getFeatureXml(getProject());
		if (file.exists()) {
			SubMonitor localmonitor = SubMonitor.convert(monitor, NLS.bind(PDECoreMessages.FeatureConsistencyChecker_0, file.getName()), 1);
			// clean problem markers on feature XML file
			file.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
			localmonitor.split(1);
		}
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return new MultiRule(Arrays.stream(getProject().getWorkspace().getRoot().getProjects())
				.filter(PDEBuilderHelper::isPDEProject)
				.toArray(ISchedulingRule[]::new));
	}

}
