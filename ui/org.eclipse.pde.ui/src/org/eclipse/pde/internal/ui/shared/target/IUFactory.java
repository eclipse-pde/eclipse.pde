/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;
import org.eclipse.pde.ui.target.ITargetLocationEditor;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;

/**
 * Adapter factory for providing all necessary UI components for the {@link IUBundleContainer}
 *
 */
public class IUFactory implements IAdapterFactory, ITargetLocationEditor, ITargetLocationUpdater {

	private ILabelProvider fLabelProvider;
	private ITreeContentProvider fContentProvider;

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	public Class[] getAdapterList() {
		return new Class[] {ILabelProvider.class, ITreeContentProvider.class, ITargetLocationEditor.class, ITargetLocationUpdater.class};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof IUBundleContainer) {
			if (adapterType == ILabelProvider.class) {
				return getLabelProvider();
			} else if (adapterType == ITreeContentProvider.class) {
				return getContentProvider();
			} else if (adapterType == ITargetLocationEditor.class) {
				return this;
			} else if (adapterType == ITargetLocationUpdater.class) {
				return this;
			}
		} else if (adaptableObject instanceof IUWrapper) {
			if (adapterType == ILabelProvider.class) {
				return getLabelProvider();
			} else if (adapterType == IContentProvider.class) {
				return getContentProvider();
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.target.ITargetLocationEditor#canEdit(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.pde.core.target.ITargetLocation)
	 */
	public boolean canEdit(ITargetDefinition target, ITargetLocation targetLocation) {
		return targetLocation instanceof IUBundleContainer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.target.ITargetLocationEditor#getEditWizard(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.pde.core.target.ITargetLocation)
	 */
	public IWizard getEditWizard(ITargetDefinition target, ITargetLocation targetLocation) {
		return new EditBundleContainerWizard(target, targetLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.target.ITargetLocationUpdater#canUpdate(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.pde.core.target.ITargetLocation)
	 */
	public boolean canUpdate(ITargetDefinition target, ITargetLocation targetLocation) {
		return targetLocation instanceof IUBundleContainer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.target.ITargetLocationUpdater#update(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.pde.core.target.ITargetLocation, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus update(ITargetDefinition target, ITargetLocation targetLocation, IProgressMonitor monitor) {
		// This method has to run synchronously, so we do the update ourselves instead of using UpdateTargetJob
		if (targetLocation instanceof IUBundleContainer) {
			try {
				boolean result = ((IUBundleContainer) targetLocation).update(new HashSet(0), monitor);
				if (result) {
					return Status.OK_STATUS;
				}
				return new Status(IStatus.OK, PDECore.PLUGIN_ID, ITargetLocationUpdater.STATUS_CODE_NO_CHANGE, "", null); //$NON-NLS-1$
			} catch (CoreException e) {
				return e.getStatus();
			}
		}
		return Status.CANCEL_STATUS;
	}

	private ILabelProvider getLabelProvider() {
		if (fLabelProvider == null) {
			fLabelProvider = new StyledBundleLabelProvider(true, false);
		}
		return fLabelProvider;
	}

	private ITreeContentProvider getContentProvider() {
		if (fContentProvider == null) {
			fContentProvider = new IUContentProvider();
		}
		return fContentProvider;
	}

}
