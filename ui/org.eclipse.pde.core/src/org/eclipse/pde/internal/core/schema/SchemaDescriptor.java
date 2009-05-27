/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

public class SchemaDescriptor implements ISchemaDescriptor {

	private String fPoint;
	private URL fSchemaURL;
	private Schema fSchema;
	private long fLastModified;
	private boolean fEditable;

	public SchemaDescriptor(String extPointID, URL schemaURL) {
		fPoint = extPointID;
		fSchemaURL = schemaURL;
		if (fSchemaURL != null) {
			File file = new File(fSchemaURL.getFile());
			if (file.exists())
				fLastModified = file.lastModified();
		}
	}

	public SchemaDescriptor(IFile file, boolean editable) {
		this(new File(file.getLocation().toOSString()));
		fEditable = editable;
	}

	public SchemaDescriptor(File file) {
		try {
			if (file.exists()) {
				fSchemaURL = file.toURL();
				fLastModified = file.lastModified();
			}
		} catch (MalformedURLException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getPointId()
	 */
	public String getPointId() {
		if (fPoint != null)
			return fPoint;
		return (fSchema == null) ? null : fSchema.getQualifiedPointId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchema(boolean)
	 */
	public ISchema getSchema(boolean abbreviated) {
		if (fSchema == null && fSchemaURL != null) {
			if (fEditable)
				fSchema = new EditableSchema(this, fSchemaURL, abbreviated);
			else
				fSchema = new Schema(this, fSchemaURL, abbreviated);
			fSchema.load();
		}
		return fSchema;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchemaURL()
	 */
	public URL getSchemaURL() {
		return fSchemaURL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#isStandalone()
	 */
	public boolean isStandalone() {
		return true;
	}

	public long getLastModified() {
		return fLastModified;
	}

}
