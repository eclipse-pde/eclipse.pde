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
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ExtensionDeltaEvent implements IExtensionDeltaEvent {

	private IPluginModelBase[] added;
	private IPluginModelBase[] changed;
	private IPluginModelBase[] removed;
	private int types;

	public ExtensionDeltaEvent(int types, IPluginModelBase[] added, IPluginModelBase[] removed, IPluginModelBase[] changed) {
		this.types = types;
		this.added = added;
		this.changed = changed;
		this.removed = removed;
	}

	public IPluginModelBase[] getAddedModels() {
		return added;
	}

	public IPluginModelBase[] getChangedModels() {
		return changed;
	}

	public IPluginModelBase[] getRemovedModels() {
		return removed;
	}

	public int getEventTypes() {
		return types;
	}

}
