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
