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
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.w3c.dom.*;

public abstract class SchemaObject
	extends PlatformObject
	implements ISchemaObject, ISourceObject, Serializable {
	protected String name;
	private String description;
	transient private ISchemaObject parent;
	private Vector comments;
	private int [] range;

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
	
	public void setParent(ISchemaObject parent) {
		this.parent = parent;
	}
	public ISchema getSchema() {
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
				case '\'' :
					buf.append("&apos;");
					break;
				case '\"' :
					buf.append("&quot;");
					break;
				default :
					buf.append(c);
			}
		}
		return buf.toString();
	}
	public void setDescription(String newDescription) {
		String oldValue = description;
		description = newDescription;
		getSchema().fireModelObjectChanged(this, P_DESCRIPTION, oldValue, description);
	}
	public void setName(String newName) {
		String oldValue = name;
		name = newName;
		getSchema().fireModelObjectChanged(this, P_NAME, oldValue, name);
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
		if (range==null)
			return -1;
		return range[0];
	}
	
	public int getStopLine() {
		if (range==null)
			return -1;
		return range[1];
	}

	void bindSourceLocation(Node node, Hashtable lineTable) {
		if (lineTable==null) return;
		Integer [] lines = (Integer[]) lineTable.get(node);
		if (lines != null) {
			range = new int[] { lines[0].intValue(), lines[1].intValue() };
		}
	}
}
