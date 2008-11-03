/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Map;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.natures.PDE;

public class UpdateSiteBuilder extends IncrementalProjectBuilder {
	class DeltaVisitor implements IResourceDeltaVisitor {
		private IProgressMonitor monitor;

		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with feature nature
				IProject project = (IProject) resource;
				try {
					return (project.hasNature(PDE.SITE_NATURE));
				} catch (CoreException e) {
					PDECore.logException(e);
					return false;
				}
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (candidate.getName().equals("site.xml")) { //$NON-NLS-1$
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

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

		IResourceDelta delta = null;
		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			IProject project = getProject();
			IFile file = project.getFile("site.xml"); //$NON-NLS-1$
			if (file.exists()) {
				checkFile(file, monitor);
			}
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return null;
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		String message = NLS.bind(PDECoreMessages.Builders_verifying, file.getFullPath().toString());
		monitor.subTask(message);
		UpdateSiteErrorReporter reporter = new UpdateSiteErrorReporter(file);
		DefaultSAXParser.parse(file, reporter);
		if (reporter.getErrorCount() == 0) {
			reporter.validateContent(monitor);
		}
		monitor.subTask(PDECoreMessages.Builders_updating);
		monitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void clean(IProgressMonitor monitor) throws CoreException {
		IFile site = getProject().getFile("site.xml"); //$NON-NLS-1$
		if (site.exists()) {
			SubMonitor localmonitor = SubMonitor.convert(monitor, NLS.bind(PDECoreMessages.UpdateSiteBuilder_0, site.getName()), 1);
			try {
				// clean problem markers on site XML file
				site.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
				localmonitor.worked(1);
			} finally {
				localmonitor.done();
			}
		}
	}
}
