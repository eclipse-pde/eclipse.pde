/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;

/**
 * A host API description combines descriptions of a host and all its fragments.
 *
 * @since 1.0
 */
public class CompositeApiDescription implements IApiDescription {

	private IApiDescription[] fDescriptions;

	/**
	 * Constructs a composite API description out of the given descriptions.
	 *
	 * @param descriptions
	 */
	public CompositeApiDescription(IApiDescription[] descriptions) {
		fDescriptions = descriptions;
	}

	@Override
	public void accept(ApiDescriptionVisitor visitor, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, fDescriptions.length);
		for (IApiDescription fDescription : fDescriptions) {
			fDescription.accept(visitor, subMonitor.split(1));
		}
	}

	@Override
	public boolean accept(ApiDescriptionVisitor visitor, IElementDescriptor element, IProgressMonitor monitor) {
		for (IApiDescription fDescription : fDescriptions) {
			if (fDescription.accept(visitor, element, monitor)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IApiAnnotations resolveAnnotations(IElementDescriptor element) {
		for (IApiDescription fDescription : fDescriptions) {
			IApiAnnotations ann = fDescription.resolveAnnotations(element);
			if (ann != null) {
				return ann;
			}
		}
		return null;
	}

	@Override
	public IStatus setRestrictions(IElementDescriptor element, int restrictions) {
		for (int i = 0; i < fDescriptions.length; i++) {
			IStatus status = fDescriptions[i].setRestrictions(element, restrictions);
			if (status.isOK() || i == (fDescriptions.length - 1)) {
				return status;
			}
		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus setAddedProfile(IElementDescriptor element, int addedProfile) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setRemovedProfile(IElementDescriptor element, int removedProfile) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setSuperclass(IElementDescriptor element, String superclass) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setSuperinterfaces(IElementDescriptor element, String superinterfaces) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setInterface(IElementDescriptor element, boolean interfaceFlag) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setVisibility(IElementDescriptor element, int visibility) {
		for (int i = 0; i < fDescriptions.length; i++) {
			IStatus status = fDescriptions[i].setVisibility(element, visibility);
			if (status.isOK() || i == (fDescriptions.length - 1)) {
				return status;
			}
		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public void setAccessLevel(IElementDescriptor element, IPackageDescriptor pelement, int access) {
		//
	}

	@Override
	public IApiAccess resolveAccessLevel(IElementDescriptor element, IPackageDescriptor pelement) {
		IApiAccess access = null;
		for (IApiDescription fDescription : fDescriptions) {
			access = fDescription.resolveAccessLevel(element, pelement);
			if (access != null) {
				return access;
			}
		}
		return null;
	}
}
