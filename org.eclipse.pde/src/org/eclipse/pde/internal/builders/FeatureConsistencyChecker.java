/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.osgi.framework.*;

public class FeatureConsistencyChecker extends IncrementalProjectBuilder {
	private static final String BUILDERS_VERIFYING = "Builders.verifying"; //$NON-NLS-1$
	private static final String BUILDERS_UPDATING = "Builders.updating"; //$NON-NLS-1$
	
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
					return (project.hasNature(PDE.FEATURE_NATURE));
				} catch (CoreException e) {
					PDE.logException(e);
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
	
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException {
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE || monitor.isCanceled())
			return new IProject[0];

		IResourceDelta delta = null;

		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			checkProject(monitor);
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return new IProject[0];
	}

	private void checkProject(IProgressMonitor monitor) {
		IFile file = getProject().getFile("feature.xml"); //$NON-NLS-1$
		if (file.exists()) {
			checkFile(file, monitor);
		}
	}
	
	private void checkFile(IFile file, IProgressMonitor monitor) {
		String message =
			PDE.getFormattedMessage(BUILDERS_VERIFYING, file.getFullPath().toString());
		monitor.subTask(message);
		FeatureErrorReporter reporter = new FeatureErrorReporter(file);
		ValidatingSAXParser.parse(file, reporter);
		if (reporter.getErrorCount() == 0) {
			reporter.validateContent(monitor);
		}
		monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		monitor.done();
	}
	
	private boolean isManifestFile(IFile file) {
		return file.getParent().equals(file.getProject())
				&& file.getName().toLowerCase().equals("feature.xml"); //$NON-NLS-1$
	}
	

	
}
