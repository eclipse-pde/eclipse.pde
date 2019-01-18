/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ExtensionDeltaEvent implements IExtensionDeltaEvent {

	private final IPluginModelBase[] added;
	private final IPluginModelBase[] changed;
	private final IPluginModelBase[] removed;
	private final int types;

	public ExtensionDeltaEvent(int types, IPluginModelBase[] added, IPluginModelBase[] removed, IPluginModelBase[] changed) {
		this.types = types;
		this.added = added;
		this.changed = changed;
		this.removed = removed;
	}

	@Override
	public IPluginModelBase[] getAddedModels() {
		return added;
	}

	@Override
	public IPluginModelBase[] getChangedModels() {
		return changed;
	}

	@Override
	public IPluginModelBase[] getRemovedModels() {
		return removed;
	}

	@Override
	public int getEventTypes() {
		return types;
	}

}
