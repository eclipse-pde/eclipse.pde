/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Brian de Alwis (MTI) - bug 429420
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

	private Vector<String> comments;

	private int[] range;

	public SchemaElementReference(ISchemaCompositor compositor, String ref) {
		referenceName = ref;
		this.compositor = compositor;
	}

	@Override
	public ISchemaAttribute getAttribute(String name) {
		if (element == null) {
			return null;
		}
		return element.getAttribute(name);
	}

	@Override
	public int getAttributeCount() {
		if (element == null) {
			return 0;
		}
		return element.getAttributeCount();
	}

	@Override
	public ISchemaAttribute[] getAttributes() {
		if (element == null) {
			return new ISchemaAttribute[0];
		}
		return element.getAttributes();
	}

	@Override
	public String[] getAttributeNames() {
		if (element == null) {
			return new String[0];
		}
		return element.getAttributeNames();
	}

	public ISchemaCompositor getCompositor() {
		return compositor;
	}

	@Override
	public String getDescription() {
		if (element == null) {
			return ""; //$NON-NLS-1$
		}
		return element.getDescription();
	}

	@Override
	public String getDTDRepresentation(boolean addLinks) {
		if (element == null) {
			return PDECoreMessages.SchemaElementReference_refElementMissing;
		}
		return element.getDTDRepresentation(addLinks);
	}

	@Override
	public String getIconProperty() {
		if (element == null) {
			return ""; //$NON-NLS-1$
		}
		return element.getIconProperty();
	}

	@Override
	public String getLabelProperty() {
		if (element == null) {
			return ""; //$NON-NLS-1$
		}
		return element.getLabelProperty();
	}

	@Override
	public int getMaxOccurs() {
		return maxOccurs;
	}

	@Override
	public int getMinOccurs() {
		return minOccurs;
	}

	@Override
	public String getName() {
		return referenceName;
	}

	@Override
	public ISchemaObject getParent() {
		return compositor;
	}

	@Override
	public void setParent(ISchemaObject parent) {
	}

	public ISchemaElement getReferencedElement() {
		return element;
	}

	@Override
	public ISchemaObject getReferencedObject() {
		return element;
	}

	@Override
	public Class<ISchemaElement> getReferencedObjectClass() {
		return ISchemaElement.class;
	}

	public String getReferenceName() {
		return referenceName;
	}

	@Override
	public ISchema getSchema() {
		if (element != null) {
			ISchema schema = element.getSchema();
			if (schema != null) {
				ISchemaDescriptor desc = schema.getSchemaDescriptor();
				if (!(desc instanceof IncludedSchemaDescriptor)) {
					return schema;
				}
			}
		}
		return getCompositorsSchema();
	}

	public ISchema getCompositorsSchema() {
		if (compositor != null) {
			return compositor.getSchema();
		}
		return null;
	}

	@Override
	public ISchemaType getType() {
		if (element == null) {
			return null;
		}
		return element.getType();
	}

	public boolean isLinked() {
		return getReferencedObject() != null;
	}

	public void setCompositor(ISchemaCompositor newCompositor) {
		compositor = newCompositor;
	}

	public void setMaxOccurs(int newMaxOccurs) {
		Integer oldValue = Integer.valueOf(maxOccurs);
		maxOccurs = newMaxOccurs;
		ISchema schema = getCompositorsSchema();
		if (schema != null) {
			schema.fireModelObjectChanged(this, P_MAX_OCCURS, oldValue, Integer.valueOf(maxOccurs));
		}
	}

	public void setMinOccurs(int newMinOccurs) {
		Integer oldValue = Integer.valueOf(minOccurs);
		minOccurs = newMinOccurs;
		ISchema schema = getCompositorsSchema();
		if (schema != null) {
			schema.fireModelObjectChanged(this, P_MIN_OCCURS, oldValue, Integer.valueOf(minOccurs));
		}
	}

	@Override
	public void setReferencedObject(ISchemaObject referencedObject) {
		if (referencedObject instanceof ISchemaElement) {
			element = (ISchemaElement) referencedObject;
		} else {
			element = null;
		}
	}

	public void setReferenceName(String name) {
		String oldValue = this.referenceName;
		this.referenceName = name;
		ISchema schema = getCompositorsSchema();
		if (schema != null) {
			schema.fireModelObjectChanged(this, P_REFERENCE_NAME, oldValue, name);
		}
	}

	@Override
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

	public Vector<String> addComments(Node node, Vector<String> result) {
		for (Node prev = node.getPreviousSibling(); prev != null; prev = prev.getPreviousSibling()) {
			if (prev.getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			if (prev instanceof Comment) {
				String comment = prev.getNodeValue();
				if (result == null) {
					result = new Vector<>();
				}
				result.add(comment);
			} else {
				break;
			}
		}
		return result;
	}

	void writeComments(PrintWriter writer) {
		writeComments(writer, comments);
	}

	void writeComments(PrintWriter writer, Vector<String> source) {
		if (source == null) {
			return;
		}
		for (int i = 0; i < source.size(); i++) {
			String comment = source.elementAt(i);
			writer.println("<!--" + comment + "-->"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public int getStartLine() {
		return range == null ? -1 : range[0];
	}

	@Override
	public int getStopLine() {
		return range == null ? -1 : range[1];
	}

	void bindSourceLocation(Node node, Hashtable<?, ?> lineTable) {
		if (lineTable == null) {
			return;
		}
		Integer[] data = (Integer[]) lineTable.get(node);
		if (data != null) {
			range = new int[] {data[0].intValue(), data[1].intValue()};
		}
	}

	@Override
	public boolean hasTranslatableContent() {
		if (element == null) {
			return false;
		}
		return element.hasTranslatableContent();
	}

	@Override
	public boolean isDeprecated() {
		if (element == null) {
			return false;
		}
		return element.isDeprecated();
	}

	@Override
	public boolean hasDeprecatedAttributes() {
		if (element == null) {
			return false;
		}
		return element.hasDeprecatedAttributes();
	}

	@Override
	public int compareTo(Object arg0) {
		if (element == null) {
			return -1;
		} else if (arg0 == null) {
			return -1;
		}
		return element.compareTo(arg0);
	}
}
