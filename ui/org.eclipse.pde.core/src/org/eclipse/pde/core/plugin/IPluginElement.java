package org.eclipse.pde.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
/**
 * Classes that implement this interface model the
 * XML elements found in the plug-in model.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IPluginElement extends IPluginParent {
	/**
	 * A property name that will be used to notify
	 * about element body text change.
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final String P_TEXT = "text";
	/**
	 * A property name that will be used to notify
	 * about global replacement of the element's attributes.
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final String P_ATTRIBUTES = "attributes";

	/**
	 * A property name that will be used to notify individual
	 * change in an element's attribute.
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final String P_ATTRIBUTE = "attribute";
	/**
	 * Creates an identical copy of this XML element.
	 * The new element will share the same model and
	 * the parent.
	 *
	 * @return a copy of this element
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IPluginElement createCopy();
	/**
	 * Returns an attribute object whose name
	 * matches the provided name.
	 * @param name the name of the attribute
	 * @return the attribute object, or <samp>null</samp> if not found
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IPluginAttribute getAttribute(String name);
	/**
	 * Returns all attributes currently defined in this element
	 * @return an array of attribute objects that belong to this element
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IPluginAttribute[] getAttributes();
	/**
	 * Returns the number of attributes in this element.
	 * @return number of attributes defined in this element
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	int getAttributeCount();
	/**
	 * Returns the body text of this element.
	 *
	 * @return body text of this element or <samp>null</samp> if not set.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getText();
	/**
	 * Sets the attribute with the provided name
	 * to the provided value. If attribute object
	 * is not found, a new one will be created and
	 * its value set to the provided value.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param name the name of the attribute
	 * @param value the value to be set 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void setAttribute(String name, String value) throws CoreException;
	/**
	 * Sets the body text of this element
	 * to the provided value. This method
	 * will throw a CoreException if the
	 * model is not editable.
	 *
	 * @param text the new body text of this element
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void setText(String text) throws CoreException;
}