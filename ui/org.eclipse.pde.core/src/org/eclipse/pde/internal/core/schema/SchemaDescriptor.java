/*******************************************************************************
 *  Copyright (c) 2005, 2013 IBM Corporation and others.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

public class SchemaDescriptor implements ISchemaDescriptor {

	private String fPoint;
	private URL fSchemaURL;
	private Schema fSchema;
	private long fLastModified;
	private boolean fEditable;
	private List<IPath> fSearchPath;

	public SchemaDescriptor(String extPointID, URL schemaURL) {
		this(extPointID, schemaURL, null);
	}

	/**
	 * Creates a new schema descriptor for a schema at the given url.  The searchPath will
	 * be used to lookup included schemas.
	 *
	 * @param extPointID the extension point the schema describes
	 * @param schemaURL the url location of the schema
	 * @param searchPath list of absolute or schema relative paths to search for included schemas, may be <code>null</code>
	 */
	public SchemaDescriptor(String extPointID, URL schemaURL, List<IPath> searchPath) {
		fPoint = extPointID;
		fSchemaURL = schemaURL;
		if (fSchemaURL != null) {
			File file = new File(fSchemaURL.getFile());
			if (file.exists()) {
				fLastModified = file.lastModified();
			}
		}
		fSearchPath = searchPath;
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

	@Override
	public String getPointId() {
		if (fPoint != null) {
			return fPoint;
		}
		return (fSchema == null) ? null : fSchema.getQualifiedPointId();
	}

	@Override
	public ISchema getSchema(boolean abbreviated) {
		if (fSchema == null && fSchemaURL != null) {
			if (fEditable) {
				fSchema = new EditableSchema(this, fSchemaURL, abbreviated);
			} else {
				fSchema = new Schema(this, fSchemaURL, abbreviated);
			}
			fSchema.setSearchPath(fSearchPath);
			fSchema.load();
		}
		return fSchema;
	}

	@Override
	public URL getSchemaURL() {
		return fSchemaURL;
	}

	@Override
	public boolean isStandalone() {
		return true;
	}

	@Override
	public long getLastModified() {
		return fLastModified;
	}

}
