package org.eclipse.pde.internal.ui.launcher;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.ui.views.properties.*;
import java.util.Vector;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.util.ChoicePropertyDescriptor;
/**
 */
public class TracingPropertySource implements IPropertySource, IAdaptable {
	private IPluginModel model;
	private Vector descriptors;
	private Hashtable template;
	private Hashtable values;
	private Hashtable dvalues;
	private static final String[] booleanChoices = { "false", "true" };
	private Properties masterOptions;
	private boolean modified;
	private TracingLauncherTab tab;

	class BooleanLabelProvider extends LabelProvider {
		String id;
		public BooleanLabelProvider(String id) {
			this.id = id;
		}
		public String getText(Object obj) {
			Object value = getPropertyValue(id);
			if (value instanceof Integer) {
				Integer ivalue = (Integer) value;
				return booleanChoices[ivalue.intValue()];
			}
			return value.toString();
		}
	}
	
/**
 * @param model org.eclipse.pde.ui.model.plugin.IPluginModel
 */
public TracingPropertySource(IPluginModel model, Properties masterOptions, Hashtable template, TracingLauncherTab tab) {
	this.model = model;
	this.masterOptions = masterOptions;
	this.template = template;
	this.tab = tab;
	values = new Hashtable();
	dvalues = new Hashtable();
}

/**
 * @return org.eclipse.ui.views.properties.PropertyDescriptor
 * @param key java.lang.String
 * @param value java.lang.String
 */
private PropertyDescriptor createBooleanPropertyDescriptor(
	String key,
	String value) {
	ChoicePropertyDescriptor desc =
		new ChoicePropertyDescriptor(key, key, booleanChoices);
	desc.setLabelProvider(new BooleanLabelProvider(key));
	return desc;
}
/**
 * @param options java.util.Properties
 */
private void createDescriptors() {
	descriptors = new Vector();
	for (Enumeration enum = template.keys(); enum.hasMoreElements();) {
		String key = (String) enum.nextElement();
		IPath path = new Path(key);
		path = path.removeFirstSegments(1);
		String shortKey = path.toString().toLowerCase();
		String value = (String) template.get(key);
		String lvalue = null;
		PropertyDescriptor desc;
		String masterValue = masterOptions.getProperty(key);
		if (value != null)
			lvalue = value.toLowerCase();
		if (lvalue != null && (lvalue.equals("true") || lvalue.equals("false"))) {
			desc = createBooleanPropertyDescriptor(shortKey, value);
			Integer dvalue = new Integer(lvalue.equals("true") ? 1 : 0);
			dvalues.put(shortKey, dvalue);
			if (masterValue != null) {
				Integer mvalue = new Integer(masterValue.equals("true") ? 1 : 0);
				values.put(shortKey, mvalue);
			}
		} else {
			desc = createTextPropertyDescriptor(shortKey, value);
			String dvalue = value != null ? value : "";
			dvalues.put(shortKey, dvalue);
			if (masterValue != null) {
				values.put(shortKey, masterValue);
			}
		}
		descriptors.add(desc);
	}
}
/**
 * @return org.eclipse.ui.views.properties.PropertyDescriptor
 * @param key java.lang.String
 * @param value java.lang.String
 */
private PropertyDescriptor createTextPropertyDescriptor(String key, String value) {
	return new TextPropertyDescriptor(key, key);
}
/**
 * Returns an object which is an instance of the given class
 * associated with this object. Returns <code>null</code> if
 * no such object can be found.
 *
 * @param adapter the adapter class to look up
 * @return a object castable to the given class, 
 *    or <code>null</code> if this object does not
 *    have an adapter for the given class
 */
public Object getAdapter(java.lang.Class adapter) {
	if (adapter.equals(IPropertySource.class))
		return this;
	return null;
}
public Object getEditableValue() {
	return null;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors==null) createDescriptors();
	return (IPropertyDescriptor[])descriptors.toArray(new IPropertyDescriptor[descriptors.size()]);
}
public Object getPropertyValue(Object id) {
	Object value = values.get(id);
	return value;
}
/**
 * Returns whether the value of the property with the given id has changed from
 * its default value. Returns <code>false</code> if the notion of default value
 * is not meaningful for the specified property, or if this source does not have
 * the specified property.
 * 
 * @param id the id of the property 
 * @return <code>true</code> if the value of the specified property has changed
 *   from its original default value, and <code>false</code> otherwise
 */
public boolean isPropertySet(Object id) {
	return false;
}
/**
 */
public void reset() {
	values = (Hashtable)dvalues.clone();
	modified = true;
	tab.updateLaunchConfigurationDialog();
}
/**
 * Resets the property with the given id to its default value if possible.
 * Does nothing if the notion of default value is not meaningful for 
 * the specified property, or if the property's value cannot be changed,
 * or if this source does not have the specified property.
 * 
 * @param id the id of the property being reset
 */
public void resetPropertyValue(Object id) {}
/**
 */
public void save() {
	String pid = model.getPlugin().getId();
	for (Enumeration enum = values.keys(); enum.hasMoreElements();) {
		String shortKey = (String) enum.nextElement();
		Object value = values.get(shortKey);
		String svalue = value.toString();
		if (value instanceof Integer)
			svalue = booleanChoices[((Integer) value).intValue()];
		IPath path = new Path(pid).append(shortKey);
		masterOptions.setProperty(path.toString(), svalue);
	}
	modified = false;
}
/**
 *
 */
public void setPropertyValue(Object id, Object value) {
	values.put(id, value);
	modified = true;
	tab.updateLaunchConfigurationDialog();
}

public boolean isModified() {
	return modified;
}
}
