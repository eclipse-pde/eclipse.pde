package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.core.ischema.*;

public class DocumentSection extends SchemaObject implements IDocumentSection {
	private String sectionId;

public DocumentSection(ISchemaObject parent, String sectionId, String name) {
	super(parent, name);
	this.sectionId = sectionId;
}
public String getSectionId() {
	return sectionId;
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	String indent2 = indent+Schema.INDENT;
	String indent3 = indent2+Schema.INDENT;
	writer.println(indent+"<annotation>");
	writer.println(indent2+"<appInfo>");
	writer.println(indent3+"<meta.section type=\""+sectionId+"\"/>");
	writer.println(indent2+"</appInfo>");
	writer.println(indent2+"<documentation>");
	writer.println(indent3+getWritableDescription());
	writer.println(indent2+"</documentation>");
	writer.println(indent+"</annotation>");
}
}
