package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.ui.ischema.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.base.model.*;

public class SchemaSimpleType extends SchemaType implements ISchemaSimpleType, IWritable {
	private ISchemaRestriction restriction;
	public static final String P_RESTRICTION = "restriction";

public SchemaSimpleType(ISchema schema, String typeName) {
	super(schema, typeName);
}
public SchemaSimpleType(ISchemaSimpleType type) {
	super(type.getSchema(), type.getName());
	ISchemaRestriction rest = type.getRestriction();
	if (rest!=null) {
		if (rest instanceof ChoiceRestriction) {
			restriction = new ChoiceRestriction((ChoiceRestriction)rest);
			restriction.setBaseType(this);
		}
	}
}
public ISchemaRestriction getRestriction() {
	return restriction;
}
public void setRestriction(ISchemaRestriction restriction) {
	Object oldValue = this.restriction;
	this.restriction = restriction;
	if (restriction != null)
		restriction.setBaseType(this);
	getSchema().fireModelObjectChanged(this, P_RESTRICTION, oldValue, restriction);
}
public void write(String indent, PrintWriter writer) {
	writer.println(indent+"<simpleType>");
	if (restriction!=null) {
		restriction.write(indent+Schema.INDENT, writer);
	}
	writer.println(indent+"</simpleType>");
}
}
