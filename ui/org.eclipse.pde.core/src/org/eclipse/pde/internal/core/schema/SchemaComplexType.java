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
import java.util.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.*;

public class SchemaComplexType
	extends SchemaType
	implements ISchemaComplexType {
	public static final String P_COMPOSITOR = "compositorProperty";
	private boolean mixed;
	private ISchemaCompositor compositor;
	private Vector attributes = new Vector();

	public SchemaComplexType(ISchema schema) {
		this(schema, null);
	}
	public SchemaComplexType(ISchema schema, String typeName) {
		super(schema, typeName != null ? typeName : "__anonymous__");
	}
	public void addAttribute(ISchemaAttribute attribute) {
		addAttribute(attribute, null);
	}
	public void addAttribute(
		ISchemaAttribute attribute,
		ISchemaAttribute afterSibling) {
		int index = -1;
		if (afterSibling != null) {
			index = attributes.indexOf(afterSibling);
		}
		if (index != -1)
			attributes.add(index + 1, attribute);
		else
			attributes.addElement(attribute);
		getSchema().fireModelChanged(
			new ModelChangedEvent(getSchema(),
				ModelChangedEvent.INSERT,
				new Object[] { attribute },
				null));
	}
	public ISchemaAttribute getAttribute(String name) {
		for (int i = 0; i < attributes.size(); i++) {
			ISchemaAttribute attribute =
				(ISchemaAttribute) attributes.elementAt(i);
			if (attribute.getName().equals(name))
				return attribute;
		}
		return null;
	}

	public int getAttributeCount() {
		return attributes.size();
	}
	public ISchemaAttribute[] getAttributes() {
		ISchemaAttribute[] result = new ISchemaAttribute[attributes.size()];
		attributes.copyInto(result);
		return result;
	}
	public ISchemaCompositor getCompositor() {
		return compositor;
	}
	public boolean isMixed() {
		return mixed;
	}
	public void removeAttribute(ISchemaAttribute attribute) {
		attributes.removeElement(attribute);
		getSchema().fireModelChanged(
			new ModelChangedEvent(getSchema(),
				ModelChangedEvent.REMOVE,
				new Object[] { attribute },
				null));
	}
	public void setCompositor(ISchemaCompositor newCompositor) {
		Object oldValue = compositor;
		compositor = newCompositor;
		getSchema().fireModelObjectChanged(
			this,
			P_COMPOSITOR,
			oldValue,
			compositor);
	}
	public void setMixed(boolean newMixed) {
		mixed = newMixed;
	}
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<complexType>");
		String indent2 = indent + Schema.INDENT;
		SchemaCompositor compositor = (SchemaCompositor) getCompositor();
		if (compositor != null) {
			compositor.write(indent2, writer);
		}
		for (int i = 0; i < attributes.size(); i++) {
			ISchemaAttribute attribute =
				(ISchemaAttribute) attributes.elementAt(i);
			attribute.write(indent2, writer);
		}
		writer.println(indent + "</complexType>");
	}
}
