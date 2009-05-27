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
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ischema.IMetaElement;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;

public class SchemaElementReference extends PlatformObject implements ISchemaElement, IMetaElement, ISchemaObjectReference, ISourceObject, Serializable {

	private static final long serialVersionUID = 1L;

	private ISchemaElement element;

	private ISchemaCompositor compositor;

	private String referenceName;

	public static final String P_MAX_OCCURS = "max_occurs"; //$NON-NLS-1$

	public static final String P_MIN_OCCURS = "min_occurs"; //$NON-NLS-1$

	public static final String P_REFERENCE_NAME = "reference_name"; //$NON-NLS-1$

	private int minOccurs = 1;

	private int maxOccurs = 1;

	private Vector comments;

	private int[] range;

	public SchemaElementReference(ISchemaCompositor compositor, String ref) {
		referenceName = ref;
		this.compositor = compositor;
	}

	public ISchemaAttribute getAttribute(String name) {
		if (element == null)
			return null;
		return element.getAttribute(name);
	}

	public int getAttributeCount() {
		if (element == null)
			return 0;
		return element.getAttributeCount();
	}

	public ISchemaAttribute[] getAttributes() {
		if (element == null)
			return new ISchemaAttribute[0];
		return element.getAttributes();
	}

	public String[] getAttributeNames() {
		if (element == null)
			return new String[0];
		return element.getAttributeNames();
	}

	public ISchemaCompositor getCompositor() {
		return compositor;
	}

	public String getDescription() {
		if (element == null)
			return ""; //$NON-NLS-1$
		return element.getDescription();
	}

	public String getDTDRepresentation(boolean addLinks) {
		if (element == null)
			return PDECoreMessages.SchemaElementReference_refElementMissing;
		return element.getDTDRepresentation(addLinks);
	}

	public String getIconProperty() {
		if (element == null)
			return ""; //$NON-NLS-1$
		return element.getIconProperty();
	}

	public String getLabelProperty() {
		if (element == null)
			return ""; //$NON-NLS-1$
		return element.getLabelProperty();
	}

	public int getMaxOccurs() {
		return maxOccurs;
	}

	public int getMinOccurs() {
		return minOccurs;
	}

	public String getName() {
		return referenceName;
	}

	public ISchemaObject getParent() {
		return compositor;
	}

	public void setParent(ISchemaObject parent) {
	}

	public ISchemaElement getReferencedElement() {
		return element;
	}

	public ISchemaObject getReferencedObject() {
		return element;
	}

	public Class getReferencedObjectClass() {
		return ISchemaElement.class;
	}

	public String getReferenceName() {
		return referenceName;
	}

	public ISchema getSchema() {
		if (element != null) {
			ISchema schema = element.getSchema();
			if (schema != null) {
				ISchemaDescriptor desc = schema.getSchemaDescriptor();
				if (!(desc instanceof IncludedSchemaDescriptor))
					return schema;
			}
		}
		return getCompositorsSchema();
	}

	public ISchema getCompositorsSchema() {
		if (compositor != null)
			return compositor.getSchema();
		return null;
	}

	public ISchemaType getType() {
		if (element == null)
			return null;
		return element.getType();
	}

	public boolean isLinked() {
		return getReferencedObject() != null;
	}

	public void setCompositor(ISchemaCompositor newCompositor) {
		compositor = newCompositor;
	}

	public void setMaxOccurs(int newMaxOccurs) {
		Integer oldValue = new Integer(maxOccurs);
		maxOccurs = newMaxOccurs;
		ISchema schema = getCompositorsSchema();
		if (schema != null)
			schema.fireModelObjectChanged(this, P_MAX_OCCURS, oldValue, new Integer(maxOccurs));
	}

	public void setMinOccurs(int newMinOccurs) {
		Integer oldValue = new Integer(minOccurs);
		minOccurs = newMinOccurs;
		ISchema schema = getCompositorsSchema();
		if (schema != null)
			schema.fireModelObjectChanged(this, P_MIN_OCCURS, oldValue, new Integer(minOccurs));
	}

	public void setReferencedObject(ISchemaObject referencedObject) {
		if (referencedObject instanceof ISchemaElement)
			element = (ISchemaElement) referencedObject;
		else
			element = null;
	}

	public void setReferenceName(String name) {
		String oldValue = this.referenceName;
		this.referenceName = name;
		ISchema schema = getCompositorsSchema();
		if (schema != null)
			schema.fireModelObjectChanged(this, P_REFERENCE_NAME, oldValue, name);
	}

	public void write(String indent, PrintWriter writer) {
		writeComments(writer);
		writer.print(indent + "<element"); //$NON-NLS-1$
		writer.print(" ref=\"" + getReferenceName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (getMinOccurs() != 1 || getMaxOccurs() != 1) {
			String min = "" + getMinOccurs(); //$NON-NLS-1$
			String max = getMaxOccurs() == Integer.MAX_VALUE ? "unbounded" //$NON-NLS-1$
					: ("" + getMaxOccurs()); //$NON-NLS-1$
			writer.print(" minOccurs=\"" + min + "\" maxOccurs=\"" + max + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	public void addComments(Node node) {
		comments = addComments(node, comments);
	}

	public Vector addComments(Node node, Vector result) {
		for (Node prev = node.getPreviousSibling(); prev != null; prev = prev.getPreviousSibling()) {
			if (prev.getNodeType() == Node.TEXT_NODE)
				continue;
			if (prev instanceof Comment) {
				String comment = prev.getNodeValue();
				if (result == null)
					result = new Vector();
				result.add(comment);
			} else
				break;
		}
		return result;
	}

	void writeComments(PrintWriter writer) {
		writeComments(writer, comments);
	}

	void writeComments(PrintWriter writer, Vector source) {
		if (source == null)
			return;
		for (int i = 0; i < source.size(); i++) {
			String comment = (String) source.elementAt(i);
			writer.println("<!--" + comment + "-->"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public int getStartLine() {
		return range == null ? -1 : range[0];
	}

	public int getStopLine() {
		return range == null ? -1 : range[1];
	}

	void bindSourceLocation(Node node, Hashtable lineTable) {
		if (lineTable == null)
			return;
		Integer[] data = (Integer[]) lineTable.get(node);
		if (data != null) {
			range = new int[] {data[0].intValue(), data[1].intValue()};
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ischema.IMetaElement#isTranslatable()
	 */
	public boolean hasTranslatableContent() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.IMetaElement#isDeprecated()
	 */
	public boolean isDeprecated() {
		return false;
	}

	public int compareTo(Object arg0) {
		if (element == null) {
			return -1;
		} else if (arg0 == null) {
			return -1;
		}
		return element.compareTo(arg0);
	}
}
