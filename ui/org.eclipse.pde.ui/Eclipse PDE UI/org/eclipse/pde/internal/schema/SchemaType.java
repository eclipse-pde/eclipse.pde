package org.eclipse.pde.internal.schema;

import org.eclipse.pde.internal.base.schema.*;

public abstract class SchemaType implements ISchemaType {
	private String name;
	private ISchema schema;

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
public String toString() {
	return name;
}
}
