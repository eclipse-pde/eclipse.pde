/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;

public abstract class AbstractModelManager {

	private final ListenerList<IModelProviderListener> fListeners = new ListenerList<>();

	public synchronized void removeModelProviderListener(IModelProviderListener listener) {
		// TODO see bug 214613... investigate why FeatureModelManager is being bad
		if (listener != null) {
			fListeners.remove(listener);
		}
	}

	public synchronized void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}

	public void fireModelProviderEvent(IModelProviderEvent event) {
		for (IModelProviderListener listener : fListeners) {
			listener.modelsChanged(event);
		}
	}

	public void shutdown() {
		removeListeners();
	}

	protected void removeListeners() {
		fListeners.clear();
	}

}
