/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;

public class SchemaInclude extends SchemaObject implements ISchemaInclude {

	private static final long serialVersionUID = 1L;

	private String fLocation;

	private ISchema fIncludedSchema;

	private boolean fAbbreviated;

	public SchemaInclude(ISchemaObject parent, String location, boolean abbreviated) {
		super(parent, location);
		fLocation = location;
		fAbbreviated = abbreviated;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaInclude#getLocation()
	 */
	public String getLocation() {
		return fLocation;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaInclude#setLocation(java.lang.String)
	 */
	public void setLocation(String location) throws CoreException {
		String oldValue = this.fLocation;
		this.fLocation = location;
		fIncludedSchema = null;
		getSchema().fireModelObjectChanged(this, P_LOCATION, oldValue, location);
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.println("<include schemaLocation=\"" + fLocation + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public ISchema getIncludedSchema() {
		ISchemaDescriptor descriptor = getSchema().getSchemaDescriptor();
		if (fAbbreviated) {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			fIncludedSchema = registry.getIncludedSchema(descriptor, fLocation);
		} else if (fIncludedSchema == null) {
			fIncludedSchema = createInternalSchema(descriptor, fLocation);
		}
		return fIncludedSchema;
	}

	private ISchema createInternalSchema(ISchemaDescriptor desc, String location) {
		try {
			URL schemaURL = IncludedSchemaDescriptor.computeURL(desc, location);
			if (schemaURL == null)
				return null;
			Schema ischema = new Schema(null, schemaURL, fAbbreviated);
			ischema.load();
			return ischema;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public void dispose() {
		if (fIncludedSchema != null && !fIncludedSchema.isDisposed()) {
			fIncludedSchema.dispose();
			fIncludedSchema = null;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof ISchemaInclude) {
			ISchemaInclude other = (ISchemaInclude) obj;
			if (fLocation != null)
				return fLocation.equals(other.getLocation());
			return other.getLocation() == null;
		}
		return false;
	}
}
