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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;

/**
 * Content Provider for the  {@link IUBundleContainer} target location
 *
 */
public class IUContentProvider extends DefaultTableProvider implements ITreeContentProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		IUBundleContainer location = (IUBundleContainer) parentElement;
		if (location.isResolved()) {
			try {
				// if this is a bundle container then we must be sure that all bundle containers are
				// happy since they all share the same profile.
				ITargetDefinition target = location.getTarget();
				if (target == null || !P2TargetUtils.isResolved(target)) {
					return new Object[0];
				}
				IInstallableUnit[] units = location.getInstallableUnits();
				// Wrap the units so that they remember their parent container
				List wrappedUnits = new ArrayList(units.length);
				for (int i = 0; i < units.length; i++) {
					wrappedUnits.add(new IUWrapper(units[i], location));
				}
				return wrappedUnits.toArray();
			} catch (CoreException e) {
				return new Object[] {e.getStatus()};
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
		return false;
	}

	/**
	 * Wraps an installable unit so that it knows what bundle container parent it belongs to
	 * in the tree.
	 */
	public class IUWrapper {
		private IInstallableUnit fIU;
		private IUBundleContainer fParent;

		public IUWrapper(IInstallableUnit unit, IUBundleContainer parent) {
			fIU = unit;
			fParent = parent;
		}

		public IInstallableUnit getIU() {
			return fIU;
		}

		public IUBundleContainer getParent() {
			return fParent;
		}
	}
}
