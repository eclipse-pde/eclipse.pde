package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;

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
