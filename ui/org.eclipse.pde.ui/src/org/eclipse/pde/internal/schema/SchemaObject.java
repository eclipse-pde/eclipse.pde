package org.eclipse.pde.internal.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.runtime.PlatformObject;
import java.util.*;
import org.w3c.dom.*;
import java.io.PrintWriter;
import org.eclipse.pde.internal.base.model.ISourceObject;

public abstract class SchemaObject
	extends PlatformObject
	implements ISchemaObject, ISourceObject {
	protected String name;
	private String description;
	private ISchemaObject parent;
	private Vector comments;
	private int line;

	public SchemaObject(ISchemaObject parent, String name) {
		this.parent = parent;
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public java.lang.String getName() {
		return name;
	}
	public ISchemaObject getParent() {
		return parent;
	}
	public ISchema getSchema() {
		ISchemaObject parent = this.parent;

		ISchemaObject object = this;

		while (object.getParent() != null) {
			object = object.getParent();
		}
		return (ISchema) object;
	}
	public String getWritableDescription() {
		return getWritableDescription(getDescription());
	}
	public static String getWritableDescription(String input) {
		if (input == null)
			return "";
		String result = input.trim();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < result.length(); i++) {
			char c = result.charAt(i);
			switch (c) {
				case '<' :
					buf.append("&lt;");
					break;
				case '>' :
					buf.append("&gt;");
					break;
				case '&' :
					buf.append("&amp;");
					break;
				default :
					buf.append(c);
			}
		}
		return buf.toString();
	}
	public void setDescription(String newDescription) {
		description = newDescription;
		getSchema().fireModelObjectChanged(this, P_DESCRIPTION);
	}
	public void setName(String newName) {
		name = newName;
		getSchema().fireModelObjectChanged(this, P_NAME);
	}
	public String toString() {
		if (name != null)
			return name;
		return super.toString();
	}

	public void addComments(Node node) {
		comments = addComments(node, comments);
	}

	public Vector addComments(Node node, Vector result) {
		for (Node prev = node.getPreviousSibling();
			prev != null;
			prev = prev.getPreviousSibling()) {
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
			writer.println("<!--" + comment + "-->");
		}
	}

	public int getStartLine() {
		return line;
	}

	void bindSourceLocation(Node node, Hashtable lineTable) {
		if (lineTable==null) return;
		Integer lineObject = (Integer) lineTable.get(node);
		if (lineObject != null) {
			line = lineObject.intValue();
		}
	}
}