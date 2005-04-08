/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.io.Serializable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;

public class BundleObject implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient IBundleModel model;

	public BundleObject() {
	}
	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException(PDECoreMessages.BundleObject_readOnlyException); //$NON-NLS-1$
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
    
    protected void fireStructureChanged(BundleObject child, int changeType) {
        if (model.isEditable() && model instanceof IModelChangeProvider) {
            IModelChangedEvent e = new ModelChangedEvent(
                    (IModelChangeProvider)model, 
                    changeType,
                    new Object[]{child}, 
                    null);
            fireModelChanged(e);
        }
    }

    protected void fireModelChanged(IModelChangedEvent e) {
        if (model.isEditable() && model instanceof IModelChangeProvider) {
            IModelChangeProvider provider = (IModelChangeProvider) model;
            provider.fireModelChanged(e);
        }
    }
    protected void firePropertyChanged(BundleObject object, String property,
            Object oldValue, Object newValue) {
        if (model.isEditable() && model instanceof IModelChangeProvider) {
            IModelChangeProvider provider = (IModelChangeProvider) model;
            provider.fireModelObjectChanged(object, property, oldValue, newValue);
        }
    }


}
