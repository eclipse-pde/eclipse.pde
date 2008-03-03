/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IApiProblem;

/**
 * Interface containing all of the constants used in XML documents
 * in API tooling 
 * 
 * @since 1.0.0
 */
public interface IApiXmlConstants {

	/**
	 * Constant representing the API profile node name for an API profile xml file.
	 * Value is <code>apiprofile</code>
	 */
	public static final String ELEMENT_APIPROFILE = "apiprofile";  //$NON-NLS-1$
	/**
	 * Constant representing the API component node name for an API profile xml file.
	 * Value is <code>apicomponent</code>
	 */
	public static final String ELEMENT_APICOMPONENT = "apicomponent";  //$NON-NLS-1$
	/**
	 * Constant representing the API component pool node name for an API profile xml file.
	 * Value is <code>pool</code>
	 */
	public static final String ELEMENT_POOL = "pool";  //$NON-NLS-1$	
	/**
	 * Constant representing the ee attribute name for an API profile xml file.
	 * Value is <code>ee</code>
	 */
	public static final String ELEMENT_EE = "ee"; //$NON-NLS-1$
	/**
	 * Constant representing a component element node in xml.
	 * Value is: <code>component</code> 
	 */
	public static final String ELEMENT_COMPONENT = "component"; //$NON-NLS-1$
	/**
	 * Constant representing a plugin element node in xml.
	 * Value is: <code>plugin</code>
	 */
	public static final String ELEMENT_PLUGIN = "plugin"; //$NON-NLS-1$
	/**
	 * Constant representing a package element node in xml.
	 * Value is: <code>package</code>
	 */
	public static final String ELEMENT_PACKAGE = "package"; //$NON-NLS-1$
	/**
	 * Constant representing a type element node in xml.
	 * Value is: <code>type</code>
	 */
	public static final String ELEMENT_TYPE = "type"; //$NON-NLS-1$
	/**
	 * Constant representing a method element node in xml.
	 * Value is: <code>method</code>
	 */
	public static final String ELEMENT_METHOD = "method"; //$NON-NLS-1$
	/**
	 * Constant representing a field element node in xml.
	 * Value is: <code>field</code>
	 */
	public static final String ELEMENT_FIELD = "field"; //$NON-NLS-1$
	/**
	 * Constant representing a resource element node in xml.
	 * Value is: <code>resource</code>
	 */
	public static final String ELEMENT_RESOURCE = "resource"; //$NON-NLS-1$
	/**
	 * Constant representing a api filter element node in xml.
	 * Value is: <code>filter</code>
	 */
	public static final String ELEMENT_FILTER = "filter"; //$NON-NLS-1$
	/**
	 * Constant representing the id attribute for plug-in xml node.
	 * Value is: <code>id</code>
	 */
	public static final String ATTR_ID = "id"; //$NON-NLS-1$
	/**
	 * Constant representing the name attribute for component, package, type, method and field xml nodes.
	 * Value is: <code>name</code>
	 */
	public static final String ATTR_NAME = "name"; //$NON-NLS-1$
	/**
	 * Constant representing the context attribute for rules xml nodes specific to a component.
	 * Value is: <code>context</code>
	 */
	public static final String ATTR_CONTEXT = "context"; //$NON-NLS-1$
	/**
	 * Constant representing the path attribute of a resource in xml.
	 * Value is: <code>path</code>
	 */
	public static final String ATTR_PATH = "path"; //$NON-NLS-1$
	/**
	 * Constant representing the category attribute of an {@link IApiProblem} in xml.
	 * Value is: <code>category</code>
	 */
	public static final String ATTR_CATEGORY = "category"; //$NON-NLS-1$
	/**
	 * Constant representing the severity attribute of an {@link IApiProblem} in xml.
	 * Value is: <code>severity</code>
	 */
	public static final String ATTR_SEVERITY = "severity"; //$NON-NLS-1$
	/**
	 * Constant representing the kind attribute of an {@link IApiProblem} in xml.
	 * Value is: <code>kind</code>
	 */
	public static final String ATTR_KIND = "kind"; //$NON-NLS-1$
	/**
	 * Constant representing the flags attribute of an {@link IApiProblem} in xml.
	 * Value is: <code>flags</code>
	 */
	public static final String ATTR_FLAGS = "flags"; //$NON-NLS-1$
	/**
	 * Constant representing the message attribute of an {@link IApiProblem} in xml.
	 * Value is: <code>message</code>
	 */
	public static final String ATTR_MESSAGE = "message"; //$NON-NLS-1$
	/**
	 * Constant representing the extend attribute for a type xml node.
	 * Value is: <code>extend</code>
	 */
	public static final String ATTR_EXTEND = "extend"; //$NON-NLS-1$
	/**
	 * Constant representing the instantiate attribute for a type xml node.
	 * Value is: <code>instantiate</code>
	 */
	public static final String ATTR_INSTANTIATE = "instantiate";	 //$NON-NLS-1$
	/**
	 * Constant representing the implement attribute for a type xml node.
	 * Value is: <code>implement</code>
	 */
	public static final String ATTR_IMPLEMENT = "implement"; //$NON-NLS-1$
	/**
	 * Constant representing the reference attribute for a type xml node.
	 * Value is: <code>reference</code>
	 */
	public static final String ATTR_REFERENCE = "reference";	 //$NON-NLS-1$
	/**
	 * Constant representing the signature attribute for a method xml node.
	 * Value is: <code>signature</code> 
	 */
	public static final String ATTR_SIGNATURE = "signature"; //$NON-NLS-1$
	/**
	 * Constant representing the visibility attribute for component, package, type, method and field xml nodes.
	 * Will be one of: "API", "private", "private_permissable", or "SPI"
	 */
	public static final String ATTR_VISIBILITY = "visibility"; //$NON-NLS-1$
	/**
	 * Constant representing the location attribute name for an API profile xml file.
	 * Value is <code>location</code>
	 */
	static final String ATTR_LOCATION = "location"; //$NON-NLS-1$
	/**
	 * Constant representing the version attribute name for an API profile xml file.
	 * Value is <code>version</code>
	 */
	static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	/**
	 * Constant representing the java element handle attribute name in xml.
	 * Value is <code>handle</code>
	 */
	public static final String ATTR_HANDLE = "handle"; //$NON-NLS-1$
	/**
	 * Constant representing the resource modification stamp attribute name in xml.
	 * Value is <code>modificationStamp</code>
	 */
	public static final String ATTR_MODIFICATION_STAMP = "modificationStamp"; //$NON-NLS-1$
	/**
	 * Constant representing the API restrictions mask attribute name in xml.
	 * Value is <code>restrictions</code>
	 */
	public static final String ATTR_RESTRICTIONS = "restrictions"; //$NON-NLS-1$

}
