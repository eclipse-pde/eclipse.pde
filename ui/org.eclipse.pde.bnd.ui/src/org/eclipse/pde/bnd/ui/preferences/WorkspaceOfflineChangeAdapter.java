/*******************************************************************************
 * Copyright (c) 2016, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sean Bright <sean@malleable.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.preferences;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public abstract class WorkspaceOfflineChangeAdapter implements IPropertyChangeListener, WorkspaceOfflineChangeListener {
	@Override
	public final void propertyChange(PropertyChangeEvent event) {
		if (!event.getProperty()
			.equals(BndPreferences.PREF_WORKSPACE_OFFLINE)) {
			return;
		}
		workspaceOfflineChanged((boolean) event.getNewValue());
	}
}
