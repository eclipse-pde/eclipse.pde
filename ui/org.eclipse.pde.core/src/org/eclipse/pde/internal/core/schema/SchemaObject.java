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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ischema.*;

public abstract class SchemaObject extends PlatformObject implements
		ISchemaObject, Serializable {
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
		return getWritableDescription(getDescription());
	}

	public static String getWritableDescription(String input) {
		if (input == null)
			return ""; //$NON-NLS-1$
		String result = input.trim();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < result.length(); i++) {
			char c = result.charAt(i);
			switch (c) {
			case '<':
				buf.append("&lt;"); //$NON-NLS-1$
				break;
			case '>':
				buf.append("&gt;"); //$NON-NLS-1$
				break;
			case '&':
				buf.append("&amp;"); //$NON-NLS-1$
				break;
			case '\'':
				buf.append("&apos;"); //$NON-NLS-1$
				break;
			case '\"':
				buf.append("&quot;"); //$NON-NLS-1$
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

	public void setDescription(String newDescription) {
		String oldValue = fDescription;
		fDescription = newDescription;
		getSchema().fireModelObjectChanged(this, P_DESCRIPTION, oldValue,
				fDescription);
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
