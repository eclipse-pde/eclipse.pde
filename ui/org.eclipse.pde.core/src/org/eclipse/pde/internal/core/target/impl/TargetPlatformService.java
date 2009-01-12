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
package org.eclipse.pde.internal.core.target.impl;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;

/**
 * Target platform service implementation.
 * 
 * @since 3.5
 */
public class TargetPlatformService implements ITargetPlatformService {

	private static ITargetPlatformService fgDefault;

	private TargetPlatformService() {
	}

	public synchronized static ITargetPlatformService getDefault() {
		if (fgDefault == null) {
			fgDefault = new TargetPlatformService();
		}
		return fgDefault;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#deleteTarget(org.eclipse.pde.internal.core.target.provisional.ITargetHandle)
	 */
	public void deleteTarget(ITargetHandle handle) throws CoreException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#getTarget(org.eclipse.core.resources.IFile)
	 */
	public ITargetHandle getTarget(IFile file) {
		return new WorkspaceFileTargetHandle(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#getTarget(java.lang.String)
	 */
	public ITargetHandle getTarget(String memento) throws CoreException {
		try {
			URI uri = new URI(memento);
			String scheme = uri.getScheme();
			if (WorkspaceFileTargetHandle.SCHEME.equals(scheme)) {
				return WorkspaceFileTargetHandle.restoreHandle(uri);
			} else if (LocalTargetHandle.SCHEME.equals(scheme)) {
				return LocalTargetHandle.restoreHandle(uri);
			}
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetPlatformService_0, e));
		}
		throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetPlatformService_1, null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#getTargets(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ITargetHandle[] getTargets(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#newDirectoryContainer(java.lang.String)
	 */
	public IBundleContainer newDirectoryContainer(String path) {
		return new DirectoryBundleContainer(path);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#newProfileContainer(java.lang.String)
	 */
	public IBundleContainer newProfileContainer(String home) {
		return newProfileContainer(home, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#newProfileContainer(java.lang.String, java.lang.String)
	 */
	public IBundleContainer newProfileContainer(String home, String configurationLocation) {
		return new ProfileBundleContainer(home, configurationLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#newTarget()
	 */
	public ITargetDefinition newTarget() {
		return new TargetDefinition(new LocalTargetHandle());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#saveTargetDefinition(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition)
	 */
	public void saveTargetDefinition(ITargetDefinition definition) throws CoreException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService#newFeatureContainer(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IBundleContainer newFeatureContainer(String home, String id, String version) {
		return new FeatureBundleContainer(home, id, version);
	}

}
