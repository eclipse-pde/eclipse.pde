/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetObject;
import org.eclipse.pde.internal.core.util.CoreUtility;

public abstract class TargetObject extends PlatformObject implements ITargetObject {

	private transient ITargetModel fModel;
	
	public TargetObject(ITargetModel model) {
		fModel = model;
	}

	public ITargetModel getModel() {
		return fModel;
	}

	public void setModel(ITargetModel model) {
		fModel = model;
	}

	public ITarget getTarget() {
		return getModel().getTarget();
	}

	protected void firePropertyChanged(String property, Object oldValue, Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	protected void firePropertyChanged(ITargetObject object, String property,
			Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(ITargetObject child, int changeType) {
		fireStructureChanged(new ITargetObject[] { child }, changeType);
	}

	protected void fireStructureChanged(ITargetObject[] children, int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider, changeType,
					children, null));
		}
	}

	protected boolean isEditable() {
		return getModel().isEditable();
	}

	public String getWritableString(String source) {
		return CoreUtility.getWritableString(source);
	}

}
