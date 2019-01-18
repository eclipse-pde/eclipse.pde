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
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import java.net.URL;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

public class EditableSchema extends Schema implements IEditable {

	public EditableSchema(ISchemaDescriptor schemaDescriptor, URL url, boolean abbreviated) {
		super(schemaDescriptor, url, abbreviated);
	}

	public EditableSchema(String pluginId, String pointId, String name, boolean abbreviated) {
		super(pluginId, pointId, name, abbreviated);
	}

	private boolean dirty;

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		if (isNotificationEnabled()) {
			dirty = true;
		}
		super.fireModelChanged(event);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public void save(PrintWriter writer) {
		this.write("", writer); //$NON-NLS-1$
		dirty = false;
	}

	@Override
	public void setDirty(boolean newDirty) {
		dirty = newDirty;
	}
}
