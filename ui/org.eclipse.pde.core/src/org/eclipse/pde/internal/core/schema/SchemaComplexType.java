/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import java.util.Vector;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;

public class SchemaComplexType extends SchemaType implements ISchemaComplexType {

	private static final long serialVersionUID = 1L;
	public static final String P_COMPOSITOR = "compositorProperty"; //$NON-NLS-1$
	private boolean mixed;
	private ISchemaCompositor compositor;
	private final Vector<ISchemaAttribute> attributes = new Vector<>();

	public SchemaComplexType(ISchema schema) {
		this(schema, null);
	}

	public SchemaComplexType(ISchema schema, String typeName) {
		super(schema, typeName != null ? typeName : "__anonymous__"); //$NON-NLS-1$
	}

	public void addAttribute(ISchemaAttribute attribute) {
		addAttribute(attribute, null);
	}

	public void addAttribute(ISchemaAttribute attribute, ISchemaAttribute afterSibling) {
		int index = -1;
		if (afterSibling != null) {
			index = attributes.indexOf(afterSibling);
		}
		if (index != -1) {
			attributes.add(index + 1, attribute);
		} else {
			attributes.addElement(attribute);
		}
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.INSERT, new Object[] {attribute}, null));
	}

	public void moveAttributeTo(ISchemaAttribute attribute, ISchemaAttribute sibling) {
		int index = attributes.indexOf(attribute);
		int newIndex;
		if (sibling != null && attributes.contains(sibling)) {
			newIndex = attributes.indexOf(sibling);
		} else {
			newIndex = attributes.size() - 1;
		}

		if (index > newIndex) {
			for (int i = index; i > newIndex; i--) {
				attributes.set(i, attributes.elementAt(i - 1));
			}
		} else if (index < newIndex) {
			for (int i = index; i < newIndex; i++) {
				attributes.set(i, attributes.elementAt(i + 1));
			}
		} else {
			// don't need to move
			return;
		}
		attributes.set(newIndex, attribute);
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.CHANGE, new Object[] {attribute.getParent()}, null));
	}

	@Override
	public ISchemaAttribute getAttribute(String name) {
		for (int i = 0; i < attributes.size(); i++) {
			ISchemaAttribute attribute = attributes.elementAt(i);
			if (attribute.getName().equals(name)) {
				return attribute;
			}
		}
		return null;
	}

	@Override
	public int getAttributeCount() {
		return attributes.size();
	}

	@Override
	public ISchemaAttribute[] getAttributes() {
		ISchemaAttribute[] result = new ISchemaAttribute[attributes.size()];
		attributes.copyInto(result);
		return result;
	}

	@Override
	public ISchemaCompositor getCompositor() {
		return compositor;
	}

	@Override
	public boolean isMixed() {
		return mixed;
	}

	public void removeAttribute(ISchemaAttribute attribute) {
		attributes.removeElement(attribute);
		getSchema().fireModelChanged(new ModelChangedEvent(getSchema(), IModelChangedEvent.REMOVE, new Object[] {attribute}, null));
	}

	public void setCompositor(ISchemaCompositor newCompositor) {
		Object oldValue = compositor;
		compositor = newCompositor;
		getSchema().fireModelObjectChanged(this, P_COMPOSITOR, oldValue, compositor);
	}

	public void setMixed(boolean newMixed) {
		mixed = newMixed;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<complexType>"); //$NON-NLS-1$
		String indent2 = indent + Schema.INDENT;
		SchemaCompositor compositor = (SchemaCompositor) getCompositor();
		if (compositor != null) {
			compositor.write(indent2, writer);
		}
		for (int i = 0; i < attributes.size(); i++) {
			ISchemaAttribute attribute = attributes.elementAt(i);
			attribute.write(indent2, writer);
		}
		writer.println(indent + "</complexType>"); //$NON-NLS-1$
	}
}
