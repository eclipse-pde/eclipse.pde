package org.eclipse.pde.internal.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.base.schema.*;

public class SchemaEnumeration extends SchemaObject implements ISchemaEnumeration {

public SchemaEnumeration(ISchemaObject parent, String name) {
	super(parent, name);
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.println(indent+"<enumeration value=\""+getName()+"\">");
	String description = getDescription();
	if (description!=null) description.trim();
	if (description!=null && description.length()>0) {
		String indent2 = indent + Schema.INDENT;
		String indent3 = indent2 + Schema.INDENT;
		writer.println(indent2+"<annotation>");
		writer.println(indent3+"<documentation>");
		writer.println(indent3+description);
		writer.println(indent3+"</documentation>");
		writer.println(indent2+"</annotation>");
	}
	writer.println(indent+"</enumeration>");
}
}
