/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.pde.internal.core.ischema.*;

public class StorageSchemaDescriptor implements ISchemaDescriptor {
	private IStorage fStorage;
	private Schema fSchema;

	public StorageSchemaDescriptor(IStorage storage) {
		fStorage = storage;
	}

	public URL getSchemaURL() {
		return fSchema != null ? fSchema.getURL() : null;
	}
	
	public String getPointId() {
		return fSchema == null ? null : fSchema.getQualifiedPointId();
	}
	
	protected void loadSchema(boolean abbreviated) {
		fSchema = new Schema(this, null, false);
		try {
			InputStream stream = fStorage.getContents();
			fSchema.load(fStorage.getContents());
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
		if (fSchema != null) {
			fSchema.reload();
		}
	}	

	public boolean isEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchema(boolean)
	 */
	public ISchema getSchema(boolean abbreviated) {
		if (fSchema == null)
			loadSchema(abbreviated);
		return fSchema;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#isStandalone()
	 */
	public boolean isStandalone() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getLastModified()
	 */
	public long getLastModified() {
		return 0;
	}

}
