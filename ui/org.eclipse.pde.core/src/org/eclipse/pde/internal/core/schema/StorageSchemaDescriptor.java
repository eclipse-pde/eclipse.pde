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

import java.io.*;
import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;

public class StorageSchemaDescriptor extends AbstractSchemaDescriptor {
	private IStorage storage;

	public StorageSchemaDescriptor(IStorage storage) {
		this.storage = storage;
	}

	public URL getSchemaURL() {
		return null;
	}
	
	public String getPointId() {
		if (schema==null) return null;
		return schema.getQualifiedPointId();
	}
	
	protected void loadSchema() {
		schema = new Schema(this, null);
		try {
			InputStream stream = storage.getContents();
			schema.load(storage.getContents());
			stream.close();
		}
		catch (CoreException e) {
			PDECore.logException(e);
		}
		catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public void reload() {
		if (schema != null) {
			schema.reload();
		}
	}	

	public boolean isEnabled() {
		return true;
	}
}
