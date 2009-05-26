/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleVersionHeader;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.osgi.framework.Constants;

public class UpdateBundleVersionOperation {
	IMarker fMarker;
	String fVersion;

	public UpdateBundleVersionOperation(IMarker marker, String version) {
		this.fMarker = marker;
		this.fVersion = version;
	}
	public IStatus run(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) return Status.CANCEL_STATUS;
		if (monitor != null) {
			monitor.beginTask(MarkerMessages.UpdateVersionNumberingOperation_title, 2);
		}
		try {
			if (monitor != null) {
				monitor.worked(1);
			}
			IResource resource = this.fMarker.getResource();
			IProject project = resource.getProject();
			if (!project.isAccessible()) {
				System.err.println("Project " + project.getName() + " doesn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
				return Status.OK_STATUS;
			}
			if (resource.getType() == IResource.FILE) {
				IFile file = (IFile) resource;
				ModelModification mod = new ModelModification(file) {
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (!(model instanceof IBundlePluginModelBase))
							return;
						IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
						IBundle bundle = modelBase.getBundleModel().getBundle();
						IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_VERSION);
						if (header instanceof BundleVersionHeader) {
							BundleVersionHeader versionHeader = (BundleVersionHeader) header;
							versionHeader.setValue(fVersion);
						}
					}
				};
				PDEModelUtility.modifyModel(mod, null);
			}
			Util.getBuildJob(new IProject[] {project}, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
			if (monitor != null) {
				monitor.worked(1);
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		return Status.OK_STATUS;
	}
}
