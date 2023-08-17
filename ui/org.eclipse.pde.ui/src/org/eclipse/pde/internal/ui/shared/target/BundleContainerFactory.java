/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.eclipse.pde.internal.core.target.FeatureBundleContainer;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class BundleContainerFactory implements IAdapterFactory, ITargetLocationHandler {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof AbstractBundleContainer bundleContainer) {
			if (isValidTargetLocation(bundleContainer)) {
				if (adapterType == ITargetLocationHandler.class) {
					return adapterType.cast(this);
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ITargetLocationHandler.class };
	}

	@Override
	public boolean canEdit(ITargetDefinition target, TreePath path) {
		Object root = path.getLastSegment();
		if (root instanceof ITargetLocation location) {
			return isValidTargetLocation(location);
		}
		return false;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath path) {
		Object root = path.getFirstSegment();
		if (root instanceof ITargetLocation location) {
			if (isValidTargetLocation(location)) {
				return new EditBundleContainerWizard(target, location);
			}
		}
		return null;
	}

	private static boolean isValidTargetLocation(ITargetLocation targetLocation) {
		return targetLocation instanceof DirectoryBundleContainer || targetLocation instanceof ProfileBundleContainer
				|| targetLocation instanceof FeatureBundleContainer;
	}

	@Override
	public IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
		for (ITargetLocation location : targetLocations) {
			if (location instanceof DirectoryBundleContainer) {
				((DirectoryBundleContainer) location).reload();
			} else if (location instanceof ProfileBundleContainer) {
				((ProfileBundleContainer) location).reload();
			} else if (location instanceof FeatureBundleContainer) {
				((FeatureBundleContainer) location).reload();
			}
		}
		return Status.OK_STATUS;
	}

}
