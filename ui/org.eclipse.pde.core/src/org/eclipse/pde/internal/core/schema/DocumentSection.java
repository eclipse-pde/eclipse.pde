/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;

public class DocumentSection extends SchemaObject implements IDocumentSection {

	private static final long serialVersionUID = 1L;
	private String sectionId;

	public DocumentSection(ISchemaObject parent, String sectionId, String name) {
		super(parent, name);
		this.sectionId = sectionId;
	}

	public String getSectionId() {
		return sectionId;
	}

	public void write(String indent, PrintWriter writer) {
		String indent2 = indent + Schema.INDENT;
		String indent3 = indent2 + Schema.INDENT;
		writer.println(indent + "<annotation>"); //$NON-NLS-1$
		writer.println(indent2 + "<appInfo>"); //$NON-NLS-1$
		writer.println(indent3 + "<meta.section type=\"" + sectionId + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent2 + "</appInfo>"); //$NON-NLS-1$
		writer.println(indent2 + "<documentation>"); //$NON-NLS-1$
		writer.println(indent3 + getWritableDescription());
		writer.println(indent2 + "</documentation>"); //$NON-NLS-1$
		writer.println(indent + "</annotation>"); //$NON-NLS-1$
	}
}
