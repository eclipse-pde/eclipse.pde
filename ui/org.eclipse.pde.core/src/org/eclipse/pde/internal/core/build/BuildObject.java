/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;

public class BuildObject implements IBuildObject {
	private IBuildModel fModel;

	private boolean fInTheModel;

	@Override
	public boolean isInTheModel() {
		return fInTheModel;
	}

	@Override
	public void setInTheModel(boolean inTheModel) {
		fInTheModel = inTheModel;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!fModel.isEditable()) {
			throwCoreException(PDECoreMessages.BuildObject_readOnlyException);
		}
	}

	public IBuildModel getModel() {
		return fModel;
	}

	void setModel(IBuildModel model) {
		fModel = model;
	}

	protected void throwCoreException(String message) throws CoreException {
		Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * If the property defined by name is valid, changes its value to newValue
	 * 
	 * @param name name of the property to modify
	 * @param oldValue the previous value
	 * @param newValue the new value
	 * @throws CoreException if the build model is read only
	 */
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
	}
}
