/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232
 *     Maarten Meijer <mjmeijer@eclipsohpy.com> - bug 426874
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.builders;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.DefaultSAXParser;
import org.eclipse.pde.internal.core.builders.PDEBuilderHelper;
import org.eclipse.pde.internal.ds.core.Activator;
import org.eclipse.pde.internal.ds.core.Messages;

public class DSBuilder extends IncrementalProjectBuilder {

	private static String PDE_NATURE = "org.eclipse.pde.PluginNature"; //$NON-NLS-1$
	private static IProject[] EMPTY_LIST = new IProject[0];

	class ResourceVisitor implements IResourceVisitor {
		private final IProgressMonitor monitor;

		public ResourceVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResource resource) {
			if (resource instanceof IProject) {
				// TODO only check PDE projects...
				IProject project = (IProject) resource;
				try {
					return (project.hasNature(PDE_NATURE));
				} catch (CoreException e) {
					// TODO log exception
					return false;
				}
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isDSFile(candidate)) {
					checkFile(candidate, monitor);
					return true;
				}
			}
			return false;
		}
	}

	class DeltaVisitor implements IResourceDeltaVisitor {
		private final IProgressMonitor monitor;

		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// TODO only check PDE projects...
				IProject project = (IProject) resource;
				try {
					return (project.hasNature(PDE_NATURE));
				} catch (CoreException e) {
					// TODO log exception
					return false;
				}
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isDSFile(candidate)) {
					// That's it, but only check it if it has been added or changed
					if (delta.getKind() != IResourceDelta.REMOVED) {
						checkFile(candidate, monitor);
						return true;
					}
				}
			}
			return true;
		}

	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
			throws CoreException {
		IResourceDelta delta = null;
		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			IProject project = getProject();
			project.accept(new ResourceVisitor(monitor));
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return EMPTY_LIST;
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// bug 426874 - delete markers set and files created
		getProject().deleteMarkers(DSErrorReporter.MARKER_ID, true, IResource.DEPTH_INFINITE);
	}


	private boolean isDSFile(IFile candidate) {
		try {
			IContentDescription description = candidate.getContentDescription();
			if (description == null)
				return false;
			IContentType type = description.getContentType();
			return Activator.CONTENT_TYPE_ID.equals(type.getId());
		} catch (CoreException e) {
			return false;
		}
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		String message = NLS.bind(Messages.DSBuilder_verifying, file
				.getFullPath().toString());
		monitor.subTask(message);

		DSErrorReporter reporter = new DSErrorReporter(file);
		DefaultSAXParser.parse(file, reporter);
		reporter.validateContent(monitor);
		monitor.subTask(Messages.DSBuilder_updating);
		monitor.done();
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return new MultiRule(Arrays.stream(getProject().getWorkspace().getRoot().getProjects())
				.filter(PDEBuilderHelper::isPDEProject).toArray(ISchedulingRule[]::new));
	}

}
