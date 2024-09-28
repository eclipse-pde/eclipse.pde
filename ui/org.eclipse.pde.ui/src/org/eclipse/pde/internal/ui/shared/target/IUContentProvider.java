/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.P2TargetUtils;

/**
 * Content Provider for the  {@link IUBundleContainer} target location
 */
public class IUContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		IUBundleContainer location = (IUBundleContainer) parentElement;
		if (location.isResolved()) {
			// if this is a bundle container we must be sure that all bundle
			// containers are happy since they all share the same profile.
			ITargetDefinition target = location.getTarget();
			if (target == null || !P2TargetUtils.isResolved(target)) {
				return new Object[0];
			}
			Collection<IInstallableUnit> units = location.getInstallableUnits();
			// Wrap the units so that they remember their parent container
			return units.stream().map(unit -> new IUWrapper(unit, location)).toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IUWrapper wrapper) {
			return wrapper.parent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}

	/**
	 * Wraps an installable unit so that it knows what bundle container parent it belongs to
	 * in the tree.
	 */
	public static record IUWrapper(IInstallableUnit iu, IUBundleContainer parent) {
	}
}
