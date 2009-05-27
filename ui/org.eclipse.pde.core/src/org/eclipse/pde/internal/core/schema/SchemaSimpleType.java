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

import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;

public class SchemaSimpleType extends SchemaType implements ISchemaSimpleType, IWritable {

	private static final long serialVersionUID = 1L;
	private ISchemaRestriction restriction;
	public static final String P_RESTRICTION = "restriction"; //$NON-NLS-1$

	public SchemaSimpleType(ISchema schema, String typeName) {
		super(schema, typeName);
	}

	public SchemaSimpleType(ISchemaSimpleType type) {
		super(type.getSchema(), type.getName());
		ISchemaRestriction rest = type.getRestriction();
		if (rest != null) {
			if (rest instanceof ChoiceRestriction) {
				restriction = new ChoiceRestriction((ChoiceRestriction) rest);
				restriction.setBaseType(this);
			}
		}
	}

	public ISchemaRestriction getRestriction() {
		return restriction;
	}

	public void setSchema(ISchema schema) {
		super.setSchema(schema);
		if (restriction != null)
			restriction.setParent(schema);
	}

	public void setRestriction(ISchemaRestriction restriction) {
		Object oldValue = this.restriction;
		this.restriction = restriction;
		if (restriction != null)
			restriction.setBaseType(this);
		getSchema().fireModelObjectChanged(this, P_RESTRICTION, oldValue, restriction);
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<simpleType>"); //$NON-NLS-1$
		if (restriction != null) {
			restriction.write(indent + Schema.INDENT, writer);
		}
		writer.println(indent + "</simpleType>"); //$NON-NLS-1$
	}
}
