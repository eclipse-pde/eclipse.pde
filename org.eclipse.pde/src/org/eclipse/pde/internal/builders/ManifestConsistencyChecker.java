/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying"; //$NON-NLS-1$
	public static final String BUILDERS_UPDATING = "Builders.updating"; //$NON-NLS-1$

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
		
		if (file.exists())
			checkFile(file, monitor);
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		String message = PDE.getFormattedMessage(BUILDERS_VERIFYING, file.getFullPath().toString());
		monitor.subTask(message);

		IFile bundleManifest = file.getProject().getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		XMLErrorReporter reporter = null;
		if (bundleManifest.exists()) {
			reporter = new ExtensionsErrorReporter(file);
		} else if (file.getName().equals("plugin.xml")) { //$NON-NLS-1$
			reporter = new PluginErrorReporter(file);
		} else if (file.getName().equals("fragment.xml")){ //$NON-NLS-1$
			reporter = new FragmentErrorReporter(file);
		}
		if (reporter != null) {
			ValidatingSAXParser.parse(file, reporter);
			reporter.validateContent(monitor);
			monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		}
		monitor.done();
	}
	
}
