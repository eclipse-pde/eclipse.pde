package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.*;
import java.util.*;
import java.io.PrintWriter;

public abstract class PluginObject
	extends PlatformObject
	implements IPluginObject, ISourceObject {
	protected String name;
	private String translatedName;
	private IPluginObject parent;
	private IPluginModelBase model;
	private Vector comments;
	private int lineNumber;

	public PluginObject() {
	}
	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException("Illegal attempt to change read-only plug-in manifest model");
		}
	}
	protected void firePropertyChanged(String property) {
		firePropertyChanged(this, property);
	}
	protected void firePropertyChanged(IPluginObject object, String property) {
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelObjectChanged(object, property);
		}
	}
	protected void fireStructureChanged(IPluginObject child, int changeType) {
		IPluginModelBase model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(
				new ModelChangedEvent(changeType, new Object[] { child }, null));
		}
	}
	private String getAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null)
			return attribute.getNodeValue();
		return null;
	}
	public IPluginModelBase getModel() {
		return model;
	}
	public String getName() {
		return name;
	}

	public String getTranslatedName() {
		if (translatedName != null && !model.isEditable())
			return translatedName;
		if (translatedName == null && name != null && model != null) {
			translatedName = model.getResourceString(name);
		}
		return translatedName;
	}

	String getNodeAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null)
			return attribute.getNodeValue();
		return null;
	}
	public IPluginObject getParent() {
		return parent;
	}
	public IPluginBase getPluginBase() {
		return model != null ? model.getPluginBase() : null;
	}
	public String getResourceString(String key) {
		return model.getResourceString(key);
	}
	static boolean isNotEmpty(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i)) == false)
				return true;
		}
		return false;
	}
	abstract void load(Node node, Hashtable lineTable);

	void setModel(IPluginModelBase model) {
		this.model = model;
	}
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		this.name = name;
		firePropertyChanged(P_NAME);
	}
	void setParent(IPluginObject parent) {
		this.parent = parent;
	}
	protected void throwCoreException(String message) throws CoreException {
		Status status =
			new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
		throw new CoreException(status);
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

	public String getWritableString(String source) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;");
					break;
				case '<' :
					buf.append("&lt;");
					break;
				case '>' :
					buf.append("&gt;");
					break;
				case '\'' :
					buf.append("&apos;");
					break;
				case '\"' :
					buf.append("&quot;");
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	void bindSourceLocation(Node node, Hashtable lineTable) {
		Integer lineValue = (Integer) lineTable.get(node);
		if (lineValue != null) {
			lineNumber = lineValue.intValue();
		}
	}

	public int getStartLine() {
		return lineNumber;
	}
}