/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.*;

public class BundleObject {
	private IBundleModel model;

	public BundleObject() {
	}
	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException(PDECore.getResourceString("BundleObject.readOnlyException")); //$NON-NLS-1$
		}
	}
	public IBundleModel getModel() {
		return model;
	}
	public void setModel(IBundleModel newModel) {
		model = newModel;
	}
	protected void throwCoreException(String message) throws CoreException {
		Status status =
			new Status(
				IStatus.ERROR,
				PDECore.getPluginId(),
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}
}
