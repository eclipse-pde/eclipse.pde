/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.net.*;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.ischema.*;

public abstract class AbstractSchemaDescriptor implements ISchemaDescriptor {
	protected Schema schema;

	public AbstractSchemaDescriptor() {
		super();
	}

	public IPath getPluginRelativePath(String pluginId, IPath path) {
		return null;
	}

	protected Schema createSchema() {
		URL url = getSchemaURL();
		if (url == null)
			return null;
		return new Schema(this, url);
	}
	public void dispose() {
		if (schema != null && schema.isDisposed() == false) {
			schema.dispose();
		}
		schema = null;
	}

	public ISchema getSchema() {
		if (schema == null) {
			loadSchema();
		}
		return schema;
	}
	protected void loadSchema() {
		schema = createSchema();
		if (schema != null)
			schema.load();
	}
	public void reload() {
		if (schema != null) {
			schema.reload();
		}
	}
	public boolean isStandalone() {
		return false;
	}

	public abstract boolean isEnabled();
}
