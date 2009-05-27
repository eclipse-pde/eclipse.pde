/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import java.util.Vector;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;

public class SchemaCompositor extends RepeatableSchemaObject implements ISchemaCompositor {

	private static final long serialVersionUID = 1L;

	public static final String P_KIND = "p_kind"; //$NON-NLS-1$

	private int kind;
	private Vector children = new Vector();

	public SchemaCompositor(ISchemaObject parent, int kind) {
		super(parent, ""); //$NON-NLS-1$
		this.kind = kind;
		switch (kind) {
			case ALL :
				fName = PDECoreMessages.SchemaCompositor_all;
				break;
			case CHOICE :
				fName = PDECoreMessages.SchemaCompositor_choice;
				break;
			case GROUP :
				fName = PDECoreMessages.SchemaCompositor_group;
				break;
			case SEQUENCE :
				fName = PDECoreMessages.SchemaCompositor_sequence;
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
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.INSERT, new Object[] {child}, null));
	}

	public void moveChildToSibling(ISchemaObject element, ISchemaObject sibling) {
		int index = children.indexOf(element);
		int newIndex;
		if (sibling != null && children.contains(sibling))
			newIndex = children.indexOf(sibling);
		else
			newIndex = children.size() - 1;

		if (index > newIndex) {
			for (int i = index; i > newIndex; i--) {
				children.set(i, children.elementAt(i - 1));
			}
		} else if (index < newIndex) {
			for (int i = index; i < newIndex; i++) {
				children.set(i, children.elementAt(i + 1));
			}
		} else
			// don't need to move
			return;
		children.set(newIndex, element);
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.CHANGE, new Object[] {this}, null));
	}

	public void addChild(ISchemaObject newChild, ISchemaObject afterSibling) {
		int index = -1;
		if (afterSibling != null) {
			index = children.indexOf(afterSibling);
		}
		if (index != -1)
			children.add(index + 1, newChild);
		else
			children.addElement(newChild);
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.INSERT, new Object[] {newChild}, null));
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
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.REMOVE, new Object[] {child}, null));
	}

	public void setKind(int kind) {
		if (this.kind != kind) {
			Integer oldValue = new Integer(this.kind);
			this.kind = kind;
			switch (kind) {
				case ALL :
					fName = PDECoreMessages.SchemaCompositor_all;
					break;
				case CHOICE :
					fName = PDECoreMessages.SchemaCompositor_choice;
					break;
				case GROUP :
					fName = PDECoreMessages.SchemaCompositor_group;
					break;
				case SEQUENCE :
					fName = PDECoreMessages.SchemaCompositor_sequence;
					break;
			}
			getSchema().fireModelObjectChanged(this, P_KIND, oldValue, new Integer(kind));
		}
	}

	public void updateReferencesFor(ISchemaElement element, int kind) {
		for (int i = children.size() - 1; i >= 0; i--) {
			Object child = children.elementAt(i);
			if (child instanceof SchemaElementReference) {
				SchemaElementReference ref = (SchemaElementReference) child;
				String refName = ref.getReferenceName();
				switch (kind) {
					case ISchema.REFRESH_ADD :
						if (element.getName().equals(refName)) {
							ref.setReferencedObject(element);
							getSchema().fireModelObjectChanged(ref, null, null, null);
						}
						break;
					case ISchema.REFRESH_DELETE :
						if (element.getName().equals(refName)) {
							removeChild(ref);
							getSchema().fireModelObjectChanged(this, null, ref, null);
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
							getSchema().fireModelObjectChanged(ref, null, null, null);
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
			case CHOICE :
				tag = "choice"; //$NON-NLS-1$
				break;
			case ALL :
			case GROUP :
			case SEQUENCE :
				tag = "sequence"; //$NON-NLS-1$
				break;
		}
		if (tag == null)
			return;
		writer.print(indent + "<" + tag); //$NON-NLS-1$
		if (getMinOccurs() != 1 || getMaxOccurs() != 1) {
			String min = "" + getMinOccurs(); //$NON-NLS-1$
			String max = getMaxOccurs() == Integer.MAX_VALUE ? "unbounded" //$NON-NLS-1$
					: ("" + getMaxOccurs()); //$NON-NLS-1$
			writer.print(" minOccurs=\"" + min + "\" maxOccurs=\"" + max + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
