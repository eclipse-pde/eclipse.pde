/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SchemaInclude extends SchemaObject implements ISchemaInclude {
	private String location;
	private ISchema includedSchema;
	private boolean internal;

	public SchemaInclude(ISchemaObject parent, String location) {
		super(parent, location);
		this.location = location;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaInclude#getLocation()
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaInclude#setLocation(java.lang.String)
	 */
	public void setLocation(String location) throws CoreException {
		String oldValue = (String) this.location;
		this.location = location;
		includedSchema = null;
		getSchema().fireModelObjectChanged(
			this,
			P_LOCATION,
			oldValue,
			location);
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.println("<include schemaLocation=\"" + location + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public ISchema getIncludedSchema() {
		if (includedSchema != null && includedSchema.isDisposed()) {
			includedSchema = null;
		}
		if (includedSchema == null) {
			// load it relative to the parent schema
			ISchemaDescriptor descriptor = getSchema().getSchemaDescriptor();
			if (descriptor != null && !descriptor.isStandalone()) {
				includedSchema =
					PDECore.getDefault().getSchemaRegistry().getIncludedSchema(
						descriptor,
						location);
				internal = false;
			} else {
				URL url = getSchema().getURL();
				if (url != null) {
					includedSchema = createInternalSchema(descriptor, url, location);
					if (includedSchema != null)
						internal = true;

				}
			}
		}
		return includedSchema;
	}
	private ISchema createInternalSchema(IPluginLocationProvider locationProvider, URL parentURL, String location) {
		try {
			URL schemaURL =
				IncludedSchemaDescriptor.computeURL(locationProvider, parentURL, location);
			Schema ischema = new Schema(null, schemaURL);
			ischema.load();
			return ischema;
		} catch (MalformedURLException e) {
			return null;
		}
	}
	public void dispose() {
		if (internal && includedSchema!=null && !includedSchema.isDisposed()) {
			includedSchema.dispose();
			includedSchema = null;
			internal = false;
		}
	}
}