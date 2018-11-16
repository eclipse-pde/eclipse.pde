/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;

/**
 * Content provider for the tree, primary input is a ITargetDefinition, children are ITargetLocation or IStatus
 */
public class TargetLocationContentProvider implements ITreeContentProvider {

	private boolean showContent = false;


	@Override
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
					TargetBundle[] bundles = location.getBundles();
					return bundles != null ? bundles : new Object[0];
				} else if (!status.isOK()) {
					// Show multi-status children so user can easily see problems
					if (status.isMultiStatus()) {
						return status.getChildren();
					}
				} else {
					// Always check for provider last to avoid hurting performance
					ITreeContentProvider provider = Platform.getAdapterManager().getAdapter(parentElement, ITreeContentProvider.class);
					if (provider != null) {
						Object[] provided = provider.getChildren(parentElement);
						return provided != null ? provided : new Object[0];
					}
				}
			} else {
				HashMap<ITargetHandle, List<TargetDefinition>> targetFlagMap = TargetPlatformHelper
						.getTargetDefinitionMap();
				for (List<TargetDefinition> targetDefinitionValues : targetFlagMap.values()) {
					if (!targetDefinitionValues.isEmpty()) {
						ITargetLocation[] locs = targetDefinitionValues.get(0).getTargetLocations();
						if (locs != null) {
							for (ITargetLocation loc : locs) {
								if (location.equals(loc)) {
									IStatus status = loc.getStatus();
									if (status == null)
										continue;
									if (!status.isOK() && !status.isMultiStatus()) {
										return new Object[] { status };
									}
								}
							}
						}
					}
				}
			}
		} else if (parentElement instanceof IStatus) {
			return ((IStatus) parentElement).getChildren();
		} else {
			ITreeContentProvider provider = Platform.getAdapterManager().getAdapter(parentElement, ITreeContentProvider.class);
			if (provider != null) {
				Object[] provided = provider.getChildren(parentElement);
				return provided != null ? provided : new Object[0];
			}
		}
		return new Object[0];
	}


	@Override
	public Object getParent(Object element) {
		if (element instanceof IUWrapper) {
			return ((IUWrapper) element).getParent();
		}
		return null;
	}


	@Override
	public boolean hasChildren(Object element) {
		// Since we are already resolved we can't be more efficient
		return getChildren(element).length > 0;
	}


	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ITargetDefinition) {
			boolean hasContainerStatus = false;
			Collection<Object> result = new ArrayList<>();
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