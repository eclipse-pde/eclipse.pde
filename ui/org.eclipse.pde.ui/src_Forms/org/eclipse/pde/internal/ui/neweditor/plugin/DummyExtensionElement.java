/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.util.*;

import org.eclipse.pde.internal.core.ischema.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DummyExtensionElement {
	private String name;
	private DummyExtensionElement parent;
	private ArrayList children;
	private Hashtable properties;
	/**
	 * 
	 */
	public DummyExtensionElement(String name) {
		this(name, null);
	}
	
	public DummyExtensionElement(String name, DummyExtensionElement parent) {
		this.name = name;
		this.parent = parent;
		if (parent!=null) parent.add(this);
		properties= new Hashtable();
	}
	public String getProperty(String key) {
		return (String)properties.get(key);
	}
	public void setProperty(String key, String name) {
		if (name==null)
			properties.remove(key);
		else
			properties.put(key, name);
	}	
	public DummyExtensionElement getParent() {
		return parent;
	}
	public String getName() {
		return name;
	}
	public ISchemaElement getSchemaElement() {
		DummyExtensionElement root = parent;
		while (!(root instanceof DummyExtension)) {
			root = root.getParent();
		}
		ISchema schema = ((DummyExtension)root).getSchema();
		return schema.findElement(name);
	}
	public void add(DummyExtensionElement child) {
		if (children==null)
			children = new ArrayList();
		children.add(child);
	}
	public void remove(DummyExtensionElement child) {
		children.remove(child);
	}
	public DummyExtensionElement [] getChildren() {
		if (children==null) return new DummyExtensionElement[0];
		return (DummyExtensionElement[])children.toArray(new DummyExtensionElement[children.size()]);
	}
	public boolean hasChildren() {
		return children!=null && children.size()>0;
	}
	public String toString() {
		ISchemaElement selement = getSchemaElement();
		String labelProperty=null;
		if (selement!=null) {
			labelProperty = selement.getLabelProperty();
		}
		String result=null;
		if (labelProperty!=null) {
			result = getProperty(labelProperty);
		}
		if (result==null) {
			result = getProperty("id");
		}
		if (result==null) {
			result = getProperty("label");
		}
		if (result==null) {
			result = getProperty("name");
		}
		if (result!=null) {
			return result + " ("+getName()+")";
		}
		return getName();
	}
}