/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.builders;

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
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ds.core.Activator;
import org.eclipse.pde.internal.ds.core.Messages;

public class DSBuilder extends IncrementalProjectBuilder {

	private static String PDE_NATURE = "org.eclipse.pde.PluginNature"; //$NON-NLS-1$
	private static IProject[] EMPTY_LIST = new IProject[0];

	class ResourceVisitor implements IResourceVisitor {
		private IProgressMonitor monitor;

		public ResourceVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		
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
		private IProgressMonitor monitor;
	
		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
	
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

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
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
	
}
