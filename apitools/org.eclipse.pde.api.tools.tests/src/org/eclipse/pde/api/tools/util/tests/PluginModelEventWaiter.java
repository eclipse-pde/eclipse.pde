/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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

	public PluginModelEventWaiter(int kind) {
		this.fKind = kind;
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
	}

	@Override
	public synchronized void modelsChanged(PluginModelDelta delta) {
		if (delta.getKind() == fKind) {
			setEvent(delta);
			notifyAll();
			unregister();
		}
	}

	@Override
	protected void unregister() {
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
	}
}
