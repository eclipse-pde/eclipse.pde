/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;

public class SchemaCompositor
	extends RepeatableSchemaObject
	implements ISchemaCompositor {
	public static final String P_KIND = "p_kind"; //$NON-NLS-1$

	private int kind;
	private Vector children = new Vector();

	public SchemaCompositor(ISchemaObject parent, int kind) {
		super(parent, ""); //$NON-NLS-1$
		this.kind = kind;
		switch (kind) {
			case ALL :
				fName = PDECore.getResourceString("SchemaCompositor.all"); //$NON-NLS-1$
				break;
			case CHOICE :
				fName = PDECore.getResourceString("SchemaCompositor.choice"); //$NON-NLS-1$
				break;
			case GROUP :
				fName = PDECore.getResourceString("SchemaCompositor.group"); //$NON-NLS-1$
				break;
			case SEQUENCE :
				fName = PDECore.getResourceString("SchemaCompositor.sequence"); //$NON-NLS-1$
				break;
		}
	}
	public SchemaCompositor(ISchemaObject parent, String id, int kind) {
		super(parent, id);
		this.kind = kind;
	}
	public void addChild(ISchemaObject child) {
		children.addElement(child);
		child.setParent(this);
		getSchema().fireModelChanged(
			new ModelChangedEvent(getSchema(),
				ModelChangedEvent.INSERT,
				new Object[] { child },
				null));
	}
	public int getChildCount() {
		return children.size();
	}
	public ISchemaObject[] getChildren() {
		ISchemaObject[] result = new ISchemaObject[children.size()];
		children.copyInto(result);
		return result;
	}

	public void setParent(ISchemaObject parent) {
		super.setParent(parent);
		for (int i = 0; i < children.size(); i++) {
			ISchemaObject child = (ISchemaObject) children.get(i);
			child.setParent(this);
		}
	}
	public int getKind() {
		return kind;
	}
	public void removeChild(ISchemaObject child) {
		children.removeElement(child);
		getSchema().fireModelChanged(
			new ModelChangedEvent(getSchema(),
				ModelChangedEvent.REMOVE,
				new Object[] { child },
				null));
	}
	public void setKind(int kind) {
		if (this.kind != kind) {
			Integer oldValue = new Integer(this.kind);
			this.kind = kind;
			switch (kind) {
				case ALL :
					fName = PDECore.getResourceString("SchemaCompositor.all"); //$NON-NLS-1$
					break;
				case CHOICE :
					fName = PDECore.getResourceString("SchemaCompositor.choice"); //$NON-NLS-1$
					break;
				case GROUP :
					fName = PDECore.getResourceString("SchemaCompositor.group"); //$NON-NLS-1$
					break;
				case SEQUENCE :
					fName = PDECore.getResourceString("SchemaCompositor.sequence"); //$NON-NLS-1$
					break;
			}
			getSchema().fireModelObjectChanged(
				this,
				P_KIND,
				oldValue,
				new Integer(kind));
		}
	}
	public void updateReferencesFor(ISchemaElement element, int kind) {
		for (int i = 0; i < children.size(); i++) {
			Object child = children.elementAt(i);
			if (child instanceof SchemaElementReference) {
				SchemaElementReference ref = (SchemaElementReference) child;
				String refName = ref.getReferenceName();
				switch (kind) {
					case ISchema.REFRESH_ADD :
						if (element.getName().equals(refName)) {
							ref.setReferencedObject(element);
							getSchema().fireModelObjectChanged(
								ref,
								null,
								null,
								null);
						}
						break;
					case ISchema.REFRESH_DELETE :
						if (element.getName().equals(refName)) {
							ref.setReferencedObject(null);
							getSchema().fireModelObjectChanged(
								ref,
								null,
								null,
								null);
						}

						break;
					case ISchema.REFRESH_RENAME :
						// Using the object comparison, try to
						// resolve and set the name if there is
						// a match. This is done to repair the
						// reference when the referenced object's
						// name changes.
						if (ref.getReferencedElement() == element)
							ref.setReferenceName(element.getName());
						// Also handle the case where rename
						// will satisfy a previously broken
						// reference.
						else if (element.getName().equals(refName)) {
							ref.setReferencedObject(element);
							getSchema().fireModelObjectChanged(
								ref,
								null,
								null,
								null);
						}
						break;
				}
			} else {
				SchemaCompositor compositor = (SchemaCompositor) child;
				compositor.updateReferencesFor(element, kind);
			}
		}
	}
	public void write(String indent, PrintWriter writer) {
		String tag = null;

		switch (kind) {
			case ALL :
				tag = "all"; //$NON-NLS-1$
				break;
			case CHOICE :
				tag = "choice"; //$NON-NLS-1$
				break;
			case GROUP :
				tag = "group"; //$NON-NLS-1$
				break;
			case SEQUENCE :
				tag = "sequence"; //$NON-NLS-1$
				break;
		}
		if (tag == null)
			return;
		writer.print(indent + "<" + tag); //$NON-NLS-1$
		if (getMinOccurs() != 1 && getMaxOccurs() != 1) {
			String min = "" + getMinOccurs(); //$NON-NLS-1$
			String max =
				getMaxOccurs() == Integer.MAX_VALUE
					? "unbounded" //$NON-NLS-1$
					: ("" + getMaxOccurs()); //$NON-NLS-1$
			writer.print(
				" minOccurs=\"" + min + "\" maxOccurs=\"" + max + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		writer.println(">"); //$NON-NLS-1$
		String indent2 = indent + Schema.INDENT;
		for (int i = 0; i < children.size(); i++) {
			Object obj = children.elementAt(i);
			if (obj instanceof IWritable) {
				((IWritable) obj).write(indent2, writer);
			}
		}
		writer.println(indent + "</" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
