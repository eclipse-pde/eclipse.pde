package org.eclipse.pde.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.schema.*;
/**
 * An attribute of XML elements found in the plug-in.
 */
public interface IPluginAttribute extends IPluginObject {
	/**
	 * This property will be used to notify that the value
	 * of the attribute has changed.
	 */
	public static final String P_VALUE = "value";
	/**
	 * Returns the value of this attribute.
	 *
	 * @return the string value of the attribute
	 */
	String getValue();
	/**
	 * Sets the value of this attribute.
	 * This method will throw a CoreExeption
	 * if the model is not editable.
	 *
	 * @param value the new attribute value
	 */
	void setValue(String value) throws CoreException;
}