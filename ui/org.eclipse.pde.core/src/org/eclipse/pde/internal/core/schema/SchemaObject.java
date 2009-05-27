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

import java.io.Serializable;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;

public abstract class SchemaObject extends PlatformObject implements ISchemaObject, Serializable {

	private static final long serialVersionUID = 1L;

	protected String fName;

	private String fDescription;

	transient private ISchemaObject fParent;

	public SchemaObject(ISchemaObject parent, String name) {
		fParent = parent;
		fName = name;
	}

	public String getDescription() {
		return fDescription;
	}

	public java.lang.String getName() {
		return fName;
	}

	public ISchemaObject getParent() {
		return fParent;
	}

	public void setParent(ISchemaObject parent) {
		fParent = parent;
	}

	public ISchema getSchema() {
		ISchemaObject object = this;

		while (object.getParent() != null) {
			object = object.getParent();
		}
		return (ISchema) object;
	}

	public String getWritableDescription() {
		String lineDelimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		String description = PDEXMLHelper.getWritableString(getDescription());
		String platformDescription = description.replaceAll("\\r\\n|\\r|\\n", lineDelimiter); //$NON-NLS-1$

		return platformDescription;
	}

	public void setDescription(String newDescription) {
		String oldValue = fDescription;
		fDescription = newDescription;
		getSchema().fireModelObjectChanged(this, P_DESCRIPTION, oldValue, fDescription);
	}

	public void setName(String newName) {
		String oldValue = fName;
		fName = newName;
		getSchema().fireModelObjectChanged(this, P_NAME, oldValue, fName);
	}

	public String toString() {
		if (fName != null)
			return fName;
		return super.toString();
	}

}
