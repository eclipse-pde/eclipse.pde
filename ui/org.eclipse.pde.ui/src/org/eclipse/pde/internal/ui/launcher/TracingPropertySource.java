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
package org.eclipse.pde.internal.ui.launcher;

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
	private IPluginModelBase fModel;
	private Vector fDescriptors;
	private Hashtable fTemplate;
	private Hashtable fValues;
	private Hashtable fDvalues;
	private static final String[] fBooleanChoices = { "false", "true" };
	private Properties fMasterOptions;
	private boolean fIsModified;
	private TracingLauncherTab fTab;

	class BooleanLabelProvider extends LabelProvider {
		String id;
		public BooleanLabelProvider(String id) {
			this.id = id;
		}
		public String getText(Object obj) {
			Object value = getPropertyValue(id);
			if (value instanceof Integer) {
				Integer ivalue = (Integer) value;
				return fBooleanChoices[ivalue.intValue()];
			}
			return value.toString();
		}
	}

	public TracingPropertySource(
		IPluginModelBase model,
		Properties masterOptions,
		Hashtable template,
		TracingLauncherTab tab) {
		this.fModel = model;
		this.fMasterOptions = masterOptions;
		this.fTemplate = template;
		this.fTab = tab;
		fValues = new Hashtable();
		fDvalues = new Hashtable();
	}

	/**
	 * @return org.eclipse.ui.views.properties.PropertyDescriptor
	 * @param key
	 *            java.lang.String
	 * @param value
	 *            java.lang.String
	 */
	private PropertyDescriptor createBooleanPropertyDescriptor(
		String key,
		String value) {
		ChoicePropertyDescriptor desc =
			new ChoicePropertyDescriptor(key, key, fBooleanChoices);
		desc.setLabelProvider(new BooleanLabelProvider(key));
		return desc;
	}
	/**
	 * @param options
	 *            java.util.Properties
	 */
	private void createDescriptors() {
		fDescriptors = new Vector();
		for (Enumeration enum = fTemplate.keys(); enum.hasMoreElements();) {
			String key = (String) enum.nextElement();
			IPath path = new Path(key);
			path = path.removeFirstSegments(1);
			String shortKey = path.toString().toLowerCase();
			String value = (String) fTemplate.get(key);
			String lvalue = null;
			PropertyDescriptor desc;
			String masterValue = fMasterOptions.getProperty(key);
			if (value != null)
				lvalue = value.toLowerCase();
			if (lvalue != null && (lvalue.equals("true") || lvalue.equals("false"))) {
				desc = createBooleanPropertyDescriptor(shortKey, value);
				Integer dvalue = new Integer(lvalue.equals("true") ? 1 : 0);
				fDvalues.put(shortKey, dvalue);
				if (masterValue != null) {
					Integer mvalue = new Integer(masterValue.equals("true") ? 1 : 0);
					fValues.put(shortKey, mvalue);
				}
			} else {
				desc = createTextPropertyDescriptor(shortKey, value);
				fDvalues.put(shortKey, value != null ? value : "");
				if (masterValue != null) {
					fValues.put(shortKey, masterValue);
				}
			}
			fDescriptors.add(desc);
		}
	}
	/**
	 * @return org.eclipse.ui.views.properties.PropertyDescriptor
	 * @param key
	 *            java.lang.String
	 * @param value
	 *            java.lang.String
	 */
	private PropertyDescriptor createTextPropertyDescriptor(String key, String value) {
		return new TextPropertyDescriptor(key, key);
	}
	/**
	 * Returns an object which is an instance of the given class associated
	 * with this object. Returns <code>null</code> if no such object can be
	 * found.
	 * 
	 * @param adapter
	 *            the adapter class to look up
	 * @return a object castable to the given class, or <code>null</code> if
	 *         this object does not have an adapter for the given class
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
		if (fDescriptors == null)
			createDescriptors();
		return (IPropertyDescriptor[]) fDescriptors.toArray(
			new IPropertyDescriptor[fDescriptors.size()]);
	}
	public Object getPropertyValue(Object id) {
		Object value = fValues.get(id);
		return value;
	}
	/**
	 * Returns whether the value of the property with the given id has changed
	 * from its default value. Returns <code>false</code> if the notion of
	 * default value is not meaningful for the specified property, or if this
	 * source does not have the specified property.
	 * 
	 * @param id
	 *            the id of the property
	 * @return <code>true</code> if the value of the specified property has
	 *         changed from its original default value, and <code>false</code>
	 *         otherwise
	 */
	public boolean isPropertySet(Object id) {
		return false;
	}
	/**
	 */
	public void reset() {
		fValues = (Hashtable) fDvalues.clone();
		fIsModified = true;
		fTab.updateLaunchConfigurationDialog();
	}
	/**
	 * Resets the property with the given id to its default value if possible.
	 * Does nothing if the notion of default value is not meaningful for the
	 * specified property, or if the property's value cannot be changed, or if
	 * this source does not have the specified property.
	 * 
	 * @param id
	 *            the id of the property being reset
	 */
	public void resetPropertyValue(Object id) {
	}
	/**
	 */
	public void save() {
		String pid = fModel.getPluginBase().getId();
		for (Enumeration enum = fValues.keys(); enum.hasMoreElements();) {
			String shortKey = (String) enum.nextElement();
			Object value = fValues.get(shortKey);
			String svalue = value.toString();
			if (value instanceof Integer)
				svalue = fBooleanChoices[((Integer) value).intValue()];
			IPath path = new Path(pid).append(shortKey);
			fMasterOptions.setProperty(path.toString(), svalue);
		}
		fIsModified = false;
	}
	/**
	 *  
	 */
	public void setPropertyValue(Object id, Object value) {
		fValues.put(id, value);
		fIsModified = true;
		fTab.updateLaunchConfigurationDialog();
	}

	public boolean isModified() {
		return fIsModified;
	}
}
