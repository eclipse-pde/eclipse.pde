package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.Serializable;

import org.eclipse.pde.internal.core.ischema.*;

public abstract class SchemaType implements ISchemaType, Serializable {
	private String name;
	transient private ISchema schema;

public SchemaType(ISchema schema, String typeName) {
	this.schema = schema;
	name = typeName;
}
public String getName() {
	return name;
}
public ISchema getSchema() {
	return schema;
}

public void setSchema(ISchema schema) {
	this.schema = schema;
}

public String toString() {
	return name;
}
}
