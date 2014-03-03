/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;

/**
 * Content provider for the tree, primary input is a ITargetDefinition, children are ITargetLocation or IStatus
 */
public class TargetLocationContentProvider implements ITreeContentProvider {

	private boolean showContent = false;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ITargetDefinition) {
			ITargetLocation[] containers = ((ITargetDefinition) parentElement).getTargetLocations();
			return containers != null ? containers : new Object[0];
		} else if (parentElement instanceof ITargetLocation) {
			ITargetLocation location = (ITargetLocation) parentElement;
			if (location.isResolved()) {
				IStatus status = location.getStatus();
				if (!status.isOK() && !status.isMultiStatus()) {
					return new Object[] {status};
				}
				if (isShowLocationContent()) {
					return location.getBundles();
				} else if (!status.isOK()) {
					// Show multi-status children so user can easily see problems
					if (status.isMultiStatus()) {
						return status.getChildren();
					}
				} else {
					// Always check for provider last to avoid hurting performance
					ITreeContentProvider provider = (ITreeContentProvider) Platform.getAdapterManager().getAdapter(parentElement, ITreeContentProvider.class);
					if (provider != null) {
						return provider.getChildren(parentElement);
					}
				}
			}
		} else if (parentElement instanceof IStatus) {
			return ((IStatus) parentElement).getChildren();
		} else {
			ITreeContentProvider provider = (ITreeContentProvider) Platform.getAdapterManager().getAdapter(parentElement, ITreeContentProvider.class);
			if (provider != null) {
				return provider.getChildren(parentElement);
			}
		}
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof IUWrapper) {
			return ((IUWrapper) element).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		// Since we are already resolved we can't be more efficient
		return getChildren(element).length > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ITargetDefinition) {
			boolean hasContainerStatus = false;
			Collection<Object> result = new ArrayList<Object>();
			ITargetLocation[] containers = ((ITargetDefinition) inputElement).getTargetLocations();
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					result.add(containers[i]);
					if (containers[i].getStatus() != null && !containers[i].getStatus().isOK()) {
						hasContainerStatus = true;
					}
				}
			}
			// If a container has a problem, it is displayed as a child, if there is a status outside of the container status (missing bundle, etc.) put it as a separate item
			if (!hasContainerStatus) {
				IStatus status = ((ITargetDefinition) inputElement).getStatus();
				if (status != null && !status.isOK()) {
					result.add(status);
				}
			}
			return result.toArray();
		} else if (inputElement instanceof String) {
			return new Object[] {inputElement};
		}
		return new Object[0];
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * @return whether this content provider will include plug-in content as children of a location
	 */
	public boolean isShowLocationContent() {
		return showContent;
	}

	/**
	 * Set whether this content provider will include plug-in content as children of a location
	 * @param showContent whether to include plug-in content
	 */
	public void setShowLocationContent(boolean showContent) {
		this.showContent = showContent;
	}

}