/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;

public class ModelProviderEvent implements IModelProviderEvent {
	private final int types;
	private final Object source;
	private final IModel[] added;
	private final IModel[] removed;
	private final IModel[] changed;

	public ModelProviderEvent(Object source, int types, IModel[] added, IModel[] removed, IModel[] changed) {
		this.source = source;
		this.types = types;
		this.added = added;
		this.removed = removed;
		this.changed = changed;
	}

	@Override
	public IModel[] getAddedModels() {
		return (added == null) ? new IModel[0] : added;
	}

	@Override
	public IModel[] getRemovedModels() {
		return (removed == null) ? new IModel[0] : removed;
	}

	@Override
	public IModel[] getChangedModels() {
		return (changed == null) ? new IModel[0] : changed;
	}

	@Override
	public int getEventTypes() {
		return types;
	}

	@Override
	public Object getEventSource() {
		return source;
	}
}
