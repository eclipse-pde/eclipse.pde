package org.eclipse.pde.internal.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.runtime.PlatformObject;

public abstract class SchemaObject extends PlatformObject implements ISchemaObject {
	protected String name;
	private String description;
	private ISchemaObject parent;

public SchemaObject(ISchemaObject parent, String name) {
	this.parent = parent;
	this.name = name;
}
public String getDescription() {
	return description;
}
public java.lang.String getName() {
	return name;
}
public ISchemaObject getParent() {
	return parent;
}
public ISchema getSchema() {
	ISchemaObject parent = this.parent;

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
		return "";
	String result = input.trim();
	StringBuffer buf = new StringBuffer();
	for (int i = 0; i < result.length(); i++) {
		char c = result.charAt(i);
		switch (c) {
			case '<' :
				buf.append("&lt;");
				break;
			case '>' :
				buf.append("&gt;");
				break;
			case '&' :
				buf.append("&amp;");
				break;
			default :
				buf.append(c);
		}
	}
	return buf.toString();
}
public void setDescription(String newDescription) {
	description = newDescription;
	getSchema().fireModelObjectChanged(this, P_DESCRIPTION);
}
public void setName(String newName) {
	name = newName;
	getSchema().fireModelObjectChanged(this, P_NAME);
}
public String toString() {
	if (name!=null) return name;
	return super.toString();
}
}
