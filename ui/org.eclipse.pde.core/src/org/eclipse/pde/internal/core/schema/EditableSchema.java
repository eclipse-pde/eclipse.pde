package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

public class EditableSchema extends Schema implements IEditable {
	private boolean dirty;

public EditableSchema(String pluginId, String pointId, String name) {
	super(pluginId, pointId, name);
}
public EditableSchema(ISchemaDescriptor schemaDescriptor, java.net.URL url) {
	super(schemaDescriptor, url);
}
public void fireModelChanged(IModelChangedEvent event) {
	if (isNotificationEnabled())
		dirty = true;
	super.fireModelChanged(event);
}
public boolean isDirty() {
	return dirty;
}
public boolean isEditable() {
	return true;
}
public void save(PrintWriter writer) {
	this.write("", writer);
	dirty = false;
}
public void setDirty(boolean newDirty) {
	dirty = newDirty;
}
}
