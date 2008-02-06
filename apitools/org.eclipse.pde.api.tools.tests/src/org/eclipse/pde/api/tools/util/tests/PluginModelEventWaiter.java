/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;

/**
 * Waiter for {@link PluginModelDelta}s of a specified kind
 * 
 * since 1.0.0
 */
public class PluginModelEventWaiter extends AbstractApiEventWaiter implements IPluginModelListener {

	private int fKind = -1;
	
	/**
	 * Constructor
	 */
	public PluginModelEventWaiter(int kind) {
		this.fKind = kind;
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
	 */
	public void modelsChanged(PluginModelDelta delta) {
		if(delta.getKind() == fKind) {
			setEvent(delta);
			notifyAll();
			unregister();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.util.tests.AbstractApiEventWaiter#unregister()
	 */
	protected void unregister() {
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
	}
}
