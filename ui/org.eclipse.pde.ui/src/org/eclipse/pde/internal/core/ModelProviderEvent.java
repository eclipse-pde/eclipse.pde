package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.*;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public class ModelProviderEvent implements IModelProviderEvent {
	private int types;
	private Object source;
	private IModel [] added;
	private IModel [] removed;
	private IModel [] changed;

public ModelProviderEvent(Object source, int types, IModel [] added, IModel [] removed, IModel [] changed) {
	this.source = source;
	this.types = types;
	this.added = added;
	this.removed = removed;
	this.changed = changed;
}

public IModel [] getAddedModels() {
	return added;
}

public IModel [] getRemovedModels() {
	return removed;
}

public IModel [] getChangedModels() {
	return changed;
}

public int getEventTypes() {
	return types;
}

public Object getEventSource() {
	return source;
}
}
