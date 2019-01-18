/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Carver - Standards for Technology in Automotive Retail - bug 213255
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;

public class DocumentSection extends SchemaObject implements IDocumentSection, Comparable<Object> {

	private static final long serialVersionUID = 1L;

	public static final String[] DOC_SECTIONS = {IDocumentSection.SINCE, IDocumentSection.EXAMPLES, IDocumentSection.API_INFO, IDocumentSection.IMPLEMENTATION, IDocumentSection.COPYRIGHT};

	private final String sectionId;

	public DocumentSection(ISchemaObject parent, String sectionId, String name) {
		super(parent, name);
		this.sectionId = sectionId;
	}

	@Override
	public String getSectionId() {
		return sectionId;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		String description = getWritableDescription();
		if (description == null || description.equals("")) { //$NON-NLS-1$
			return;
		}
		String indent2 = indent + Schema.INDENT;
		String indent3 = indent2 + Schema.INDENT;
		writer.println(indent + "<annotation>"); //$NON-NLS-1$
		writer.println(indent2 + (getSchema().getSchemaVersion() >= 3.4 ? "<appinfo>" : "<appInfo>")); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent3 + "<meta.section type=\"" + sectionId + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent2 + (getSchema().getSchemaVersion() >= 3.4 ? "</appinfo>" : "</appInfo>")); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent2 + "<documentation>"); //$NON-NLS-1$
		writer.println(indent3 + description);
		writer.println(indent2 + "</documentation>"); //$NON-NLS-1$
		writer.println(indent + "</annotation>"); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DocumentSection && ((DocumentSection) obj).getSectionId().equalsIgnoreCase(sectionId)) {
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(Object arg0) {
		if (arg0 instanceof DocumentSection) {
			int otherIndex = getIndex(((DocumentSection) arg0).getSectionId());
			int thisIndex = getIndex(sectionId);
			if (otherIndex == thisIndex) {
				return 0;
			} else if (otherIndex == -1) {
				return -1;
			} else if (thisIndex == -1) {
				return 1;
			} else {
				return thisIndex - otherIndex;
			}
		}
		return 0;
	}

	private int getIndex(String sectionId) {
		if (sectionId == null) {
			return -1;
		}
		for (int i = 0; i < DOC_SECTIONS.length; i++) {
			if (DOC_SECTIONS[i].equalsIgnoreCase(sectionId)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public String toString() {
		return sectionId;
	}
}
