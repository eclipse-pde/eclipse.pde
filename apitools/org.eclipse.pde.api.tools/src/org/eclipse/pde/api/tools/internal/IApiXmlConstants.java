/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

/**
 * Interface containing all of the constants used in XML documents
 * in API Tools 
 * 
 * @since 1.0.0
 */
public interface IApiXmlConstants {

	/**
	 * Constant representing the current version for API description files
	 */
	public static final String API_DESCRIPTION_CURRENT_VERSION = "1.2"; //$NON-NLS-1$
	/**
	 * Constant representing the current version for API filter store files
	 */
	public static final String API_FILTER_STORE_CURRENT_VERSION = Integer.toString(ApiFilterStore.CURRENT_STORE_VERSION);
	/**
	 * Constant representing the current version for API profile files
	 */
	public static final String API_PROFILE_CURRENT_VERSION = "2"; //$NON-NLS-1$
	/**
	 * Constant representing the current version for API report XML file
	 */
	public static final String API_REPORT_CURRENT_VERSION = "1"; //$NON-NLS-1$
	/**
	 * Constant representing the category attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>category</code>
	 */
	public static final String ATTR_CATEGORY = "category"; //$NON-NLS-1$
	/**
	 * Constant representing the element kind attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>elementkind</code>
	 */
	public static final String ATTR_ELEMENT_KIND = "elementkind"; //$NON-NLS-1$
	/**
	 * Constant representing the element attribute for a comment.
	 * value is: <code>comment</code>
	 * @since 1.1
	 */
	public static final String ATTR_COMMENT = "comment"; //$NON-NLS-1$
	/**
	 * Constant representing the element severity attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>severity</code>
	 */
	public static final String ATTR_SEVERITY = "severity"; //$NON-NLS-1$
	/**
	 * Constant representing the extend attribute for a type XML node.
	 * Value is: <code>extend</code>
	 */
	public static final String ATTR_EXTEND = "extend"; //$NON-NLS-1$
	/**
	 * Constant representing the override attribute for a method XML node.
	 * Value is: <code>override</code>
	 */
	public static final String ATTR_OVERRIDE = "override"; //$NON-NLS-1$
	/**
	 * Constant representing the subclass attribute for a class XML node.
	 * Value is: <code>subclass</code>
	 */
	public static final String ATTR_SUBCLASS = "subclass"; //$NON-NLS-1$
	/**
	 * Constant representing the flags attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>flags</code>
	 */
	public static final String ATTR_FLAGS = "flags"; //$NON-NLS-1$
	/**
	 * Constant representing the java element handle attribute name in XML.
	 * Value is <code>handle</code>
	 */
	public static final String ATTR_HANDLE = "handle"; //$NON-NLS-1$
	/**
	 * Constant representing the id attribute
	 * Value is: <code>id</code>
	 */
	public static final String ATTR_ID = "id"; //$NON-NLS-1$
	/**
	 * Constant representing the implement attribute for a type XML node.
	 * Value is: <code>implement</code>
	 */
	public static final String ATTR_IMPLEMENT = "implement"; //$NON-NLS-1$
	/**
	 * Constant representing the instantiate attribute for a type XML node.
	 * Value is: <code>instantiate</code>
	 */
	public static final String ATTR_INSTANTIATE = "instantiate";	 //$NON-NLS-1$
	/**
	 * Constant representing the kind attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>kind</code>
	 */
	public static final String ATTR_KIND = "kind"; //$NON-NLS-1$
	/**
	 * Constant representing the location attribute name for an API profile XML file.
	 * Value is <code>location</code>
	 */
	static final String ATTR_LOCATION = "location"; //$NON-NLS-1$
	/**
	 * Constant representing the message attribute of an {@link IApiProblem problem}  or a {@link Delta delta} in XML.
	 * Value is: <code>message</code>
	 */
	public static final String ATTR_MESSAGE = "message"; //$NON-NLS-1$
	/**
	 * Constant representing the resource modification stamp attribute name in XML.
	 * Value is <code>modificationStamp</code>
	 */
	public static final String ATTR_MODIFICATION_STAMP = "modificationStamp"; //$NON-NLS-1$
	/**
	 * Constant representing the name attribute for component, package, type, method, field and bundle XML nodes.
	 * Value is: <code>name</code>
	 */
	public static final String ATTR_NAME = "name"; //$NON-NLS-1$
	/**
	 * Constant representing the compatibility attribute of a delta in XML report.
	 * Value is: <code>compatible</code>
	 */
	public static final String ATTR_NAME_COMPATIBLE = "compatible"; //$NON-NLS-1$
	/**
	 * Constant representing the element type attribute of a delta in XML report.
	 * Value is: <code>element_type</code>
	 */
	public static final String ATTR_NAME_ELEMENT_TYPE = "element_type"; //$NON-NLS-1$
	/**
	 * Constant representing the char start attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>charstart</code>
	 */
	public static final String ATTR_CHAR_START = "charstart"; //$NON-NLS-1$
	/**
	 * Constant representing the charend attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>charend</code>
	 */
	public static final String ATTR_CHAR_END = "charend"; //$NON-NLS-1$
	/**
	 * Constant representing the new modifiers attribute of a delta in XML report.
	 * Value is: <code>newModifiers</code>
	 */
	public static final String ATTR_NAME_NEW_MODIFIERS = "newModifiers"; //$NON-NLS-1$
	/**
	 * Constant representing the old modifiers attribute of a delta in XML report.
	 * Value is: <code>oldModifiers</code>
	 */
	public static final String ATTR_NAME_OLD_MODIFIERS = "oldModifiers"; //$NON-NLS-1$
	/**
	 * Constant representing the type name attribute of a delta in XML report.
	 * Value is: <code>type_name</code>
	 */
	public static final String ATTR_NAME_TYPE_NAME = "type_name"; //$NON-NLS-1$
	/**
	 * Constant representing the linenumber attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>linenumber</code>
	 */
	public static final String ATTR_LINE_NUMBER = "linenumber"; //$NON-NLS-1$
	/**
	 * Constant representing the message argument attribute of an {@link IApiProblem} in XML.
	 * Value is: <code>messageargs</code>
	 */
	public static final String ATTR_MESSAGE_ARGUMENTS = "messageargs"; //$NON-NLS-1$
	/**
	 * Constant representing the path attribute of a resource in XML.
	 * Value is: <code>path</code>
	 */
	public static final String ATTR_PATH = "path"; //$NON-NLS-1$
	/**
	 * Constant representing the reference attribute for a type XML node.
	 * Value is: <code>reference</code>
	 */
	public static final String ATTR_REFERENCE = "reference";	 //$NON-NLS-1$
	/**
	 * Constant representing the API restrictions mask attribute name in XML.
	 * Value is <code>restrictions</code>
	 */
	public static final String ATTR_RESTRICTIONS = "restrictions"; //$NON-NLS-1$
	/**
	 * Constant representing the API profile attribute name in XML for which the element
	 * was added.
	 * Value is <code>addedprofile</code>
	 */
	public static final String ATTR_ADDED_PROFILE = "addedprofile"; //$NON-NLS-1$
	/**
	 * Constant representing the API profile attribute name in XML for which the element
	 * was defined.
	 * Value is <code>profile</code>
	 */
	public static final String ATTR_PROFILE = "profile"; //$NON-NLS-1$
	/**
	 * Constant representing the API profile attribute name in XML for which the element
	 * was removed.
	 * Value is <code>removedprofile</code>
	 */
	public static final String ATTR_REMOVED_PROFILE = "removedprofile"; //$NON-NLS-1$
	/**
	 * Constant representing the superclass attribute name in XML.
	 * Value is <code>sc</code>
	 */
	public static final String ATTR_SUPER_CLASS = "sc"; //$NON-NLS-1$
	/**
	 * Constant representing the superinterfaces attribute name in XML.
	 * Value is <code>sis</code>
	 */
	public static final String ATTR_SUPER_INTERFACES = "sis"; //$NON-NLS-1$
	/**
	 * Constant representing the interface flag attribute name in XML.
	 * Value is <code>int</code>
	 */
	public static final String ATTR_INTERFACE = "int"; //$NON-NLS-1$
	/**
	 * Constant representing the status of a member attribute name in XML.
	 * Value is <code>status</code>
	 */
	public static final String ATTR_STATUS = "status"; //$NON-NLS-1$
	/**
	 * Constant representing the delta component id attribute name in XML.
	 * Value is <code>componentId</code>
	 */
	public static final String ATTR_NAME_COMPONENT_ID = "componentId"; //$NON-NLS-1$
	/**
	 * Constant representing the signature attribute for a method XML node.
	 * Value is: <code>signature</code> 
	 */
	public static final String ATTR_SIGNATURE = "signature"; //$NON-NLS-1$
	/**
	 * Constant representing the version attribute name for an API profile XML file.
	 * Value is <code>version</code>
	 */
	static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	/**
	 * Constant representing the visibility attribute for component, package, type, method and field XML nodes.
	 * Will be one of: "API", "private", "private_permissable", or "SPI"
	 */
	public static final String ATTR_VISIBILITY = "visibility"; //$NON-NLS-1$
	/**
	 * Constant representing the delta element name.
	 * Value is: <code>delta</code>
	 */
	public static final String DELTA_ELEMENT_NAME = "delta"; //$NON-NLS-1$
	/**
	 * Constant representing the deltas element name.
	 * Value is: <code>deltas</code>
	 */
	public static final String DELTAS_ELEMENT_NAME = "deltas"; //$NON-NLS-1$
	/**
	 * Constant representing the API component node name for an API profile XML file.
	 * Value is <code>apicomponent</code>
	 */
	public static final String ELEMENT_APICOMPONENT = "apicomponent";  //$NON-NLS-1$
	
	/**
	 * Constant representing the API profile node name for an API profile XML file.
	 * Value is <code>apiprofile</code>
	 */
	public static final String ELEMENT_APIPROFILE = "apiprofile";  //$NON-NLS-1$

	/**
	 * Constant representing a component element node in XML.
	 * Value is: <code>component</code> 
	 */
	public static final String ELEMENT_COMPONENT = "component"; //$NON-NLS-1$

	/**
	 * Constant representing a components element node in XML.
	 * Value is: <code>components</code> 
	 */
	public static final String ELEMENT_COMPONENTS = "components"; //$NON-NLS-1$
	
	/**
	 * Constant representing a field element node in XML.
	 * Value is: <code>field</code>
	 */
	public static final String ELEMENT_FIELD = "field"; //$NON-NLS-1$
	/**
	 * Constant representing a API filter element node in XML.
	 * Value is: <code>filter</code>
	 */
	public static final String ELEMENT_FILTER = "filter"; //$NON-NLS-1$
	/**
	 * Constant representing a method element node in XML.
	 * Value is: <code>method</code>
	 */
	public static final String ELEMENT_METHOD = "method"; //$NON-NLS-1$
	/**
	 * Constant representing a package element node in XML.
	 * Value is: <code>package</code>
	 */
	public static final String ELEMENT_PACKAGE = "package"; //$NON-NLS-1$
	/**
	 * Constant representing a package fragment element node in XML.
	 * Value is: <code>package</code>
	 */
	public static final String ELEMENT_PACKAGE_FRAGMENT = "fragment"; //$NON-NLS-1$	
	/**
	 * Constant representing a plugin element node in XML.
	 * Value is: <code>plugin</code>
	 */
	public static final String ELEMENT_PLUGIN = "plugin"; //$NON-NLS-1$
	/**
	 * Constant representing the API component pool node name for an API profile XML file.
	 * Value is <code>pool</code>
	 */
	public static final String ELEMENT_POOL = "pool";  //$NON-NLS-1$	
	/**
	 * Constant representing a resource element node in XML.
	 * Value is: <code>resource</code>
	 */
	public static final String ELEMENT_RESOURCE = "resource"; //$NON-NLS-1$
	/**
	 * Constant representing a type element node in XML.
	 * Value is: <code>type</code>
	 */
	public static final String ELEMENT_TYPE = "type"; //$NON-NLS-1$
	
	/**
	 * Constant representing a target element node in XML.
	 * Value is: <code>target</code> 
	 */
	public static final String ELEMENT_TARGET = "target";  //$NON-NLS-1$
	
	/**
	 * Constant representing an API problems element node in XML.
	 * Value is: <code>api_problems</code>
	 */
	public static final String ELEMENT_API_PROBLEMS = "api_problems"; //$NON-NLS-1$
	/**
	 * Constant representing an API problem element node in XML.
	 * Value is: <code>api_problem</code>
	 */
	public static final String ELEMENT_API_PROBLEM = "api_problem"; //$NON-NLS-1$
	/**
	 * Constant representing an extra arguments element node for an API problem element in XML.
	 * Value is: <code>extra_arguments</code>
	 */
	public static final String ELEMENT_PROBLEM_EXTRA_ARGUMENTS = "extra_arguments"; //$NON-NLS-1$
	/**
	 * Constant representing an extra argument element node for an API problem element in XML.
	 * Value is: <code>extra_argument</code>
	 */
	public static final String ELEMENT_PROBLEM_EXTRA_ARGUMENT = "extra_argument"; //$NON-NLS-1$
	/**
	 * Constant representing the value attribute for extra argument element node or a category node.
	 * Value is: <code>value</code>
	 */
	public static final String ATTR_VALUE = "value"; //$NON-NLS-1$
	/**
	 * Constant representing a message arguments element node for an API problem element in XML.
	 * Value is: <code>message_arguments</code>
	 */
	public static final String ELEMENT_PROBLEM_MESSAGE_ARGUMENTS = "message_arguments"; //$NON-NLS-1$
	/**
	 * Constant representing a message argument element node for an API problem element in XML.
	 * Value is: <code>message_argument</code>
	 */
	public static final String ELEMENT_PROBLEM_MESSAGE_ARGUMENT = "message_argument"; //$NON-NLS-1$
	/**
	 * Constant representing the component id attribute for report element XML node.
	 * Value is: <code>componentID</code>
	 */
	public static final String ATTR_COMPONENT_ID = "componentID"; //$NON-NLS-1$
	/**
	 * Constant representing a report element node in XML.
	 * Value is: <code>report</code>
	 */
	public static final String ELEMENT_API_TOOL_REPORT = "report"; //$NON-NLS-1$<
	/**
	 * Constant representing the key attribute for category node.
	 * Value is: <code>key</code>
	 */
	public static final String ATTR_KEY = "key"; //$NON-NLS-1$
	/**
	 * Constant representing the type attribute for resource node inside API filters.
	 * Value is: <code>type</code>
	 */
	public static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	/**
	 * Constant representing the type name attribute for API problem node.
	 * Value is: <code>typeName</code>
	 */
	public static final String ATTR_TYPE_NAME = "typeName"; //$NON-NLS-1$
	/**
	 * Constant representing the bundle element for for a report element in XML.
	 * Value is: <code>bundle</code>
	 */
	public static final String ELEMENT_BUNDLE = "bundle"; //$NON-NLS-1$
	/**
	 * Constant representing a message arguments element node for a delta element in XML.
	 * Value is: <code>message_arguments</code>
	 */
	public static final String ELEMENT_DELTA_MESSAGE_ARGUMENTS = "message_arguments"; //$NON-NLS-1$
	/**
	 * Constant representing a message argument element node for a delta element in XML.
	 * Value is: <code>message_argument</code>
	 */
	public static final String ELEMENT_DELTA_MESSAGE_ARGUMENT = "message_argument"; //$NON-NLS-1$
	public static final String REFERENCES = "references"; //$NON-NLS-1$
	public static final String REFERENCE_KIND = "reference_kind"; //$NON-NLS-1$
	public static final String ATTR_REFERENCE_KIND_NAME = "reference_kind_name"; //$NON-NLS-1$
	public static final String ATTR_ORIGIN = "origin"; //$NON-NLS-1$
	public static final String ATTR_REFEREE = "referee"; //$NON-NLS-1$
	public static final String ATTR_REFERENCE_COUNT = "reference_count"; //$NON-NLS-1$
	public static final String ATTR_REFERENCE_VISIBILITY = "reference_visibility"; //$NON-NLS-1$
	public static final String SKIPPED_DETAILS = "details"; //$NON-NLS-1$
	public static final String EXCLUDED = "excluded"; //$NON-NLS-1$
	public static final String ATTR_MEMBER_NAME = "member"; //$NON-NLS-1$
	
	/**
	 * Constant representing an alternate API component in which references were
	 * resolved. Value is: <code>alternate</code>
	 */
	public static final String ATTR_ALTERNATE = "alternate"; //$NON-NLS-1$
	/**
	 * Constant representing the root element of a reference count XML file
	 * Value is: <code>referenceCount</code>
	 */
	public static final String ELEMENT_REPORTED_COUNT = "reportedcount"; //$NON-NLS-1$
	/**
	 * XML attribute name for the total number of references or problems found
	 * Value is: <code>total</code>
	 */
	public static final String ATTR_TOTAL = "total"; //$NON-NLS-1$
	/**
	 * XML attribute name for the total number of problems with severity 'warning' found
	 * Value is: <code>warnings</code>
	 */
	public static final String ATTR_COUNT_WARNINGS = "warnings"; //$NON-NLS-1$
	/**
	 * XML attribute name for the total number of problems with severity 'error' found
	 * Value is: <code>errors</code>
	 */
	public static final String ATTR_COUNT_ERRORS = "errors"; //$NON-NLS-1$
	/**
	 * XML attribute name for the total number of illegal references found
	 * Value is: <code>illegal</code>
	 */
	public static final String ATTR_COUNT_ILLEGAL = "illegal"; //$NON-NLS-1$
	/**
	 * XML attribute name for the total number of internal references found
	 * Value is: <code>internal</code>
	 */
	public static final String ATTR_COUNT_INTERNAL = "internal"; //$NON-NLS-1$
}
