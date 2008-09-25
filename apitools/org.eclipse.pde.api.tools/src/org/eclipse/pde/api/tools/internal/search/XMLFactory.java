/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Serializes references to XML.
 * 
 * @since 1.0.0
 */
public class XMLFactory {
	
	/**
	 * Constant representing a method signature attribute in XML.
	 * Value is: <code>methodSignature</code>
	 */
	public static final String ATTR_METHOD_SIGNATURE = "methodSignature"; //$NON-NLS-1$

	/**
	 * Constant representing a method attribute in XML.
	 * Value is: <code>method</code>
	 */
	public static final String ATTR_METHOD = "method"; //$NON-NLS-1$

	/**
	 * Constant representing a field attribute in XML.
	 * Value is: <code>field</code>
	 */
	public static final String ATTR_FIELD = "field"; //$NON-NLS-1$

	/**
	 * Constant representing a type attribute in XML.
	 * Value is: <code>type</code>
	 */
	public static final String ATTR_TYPE = "type"; //$NON-NLS-1$

	/**
	 * Constant representing a package attribute in XML.
	 * Value is: <code>package</code>
	 */
	public static final String ATTR_PACKAGE = "package"; //$NON-NLS-1$

	/**
	 * Constant representing a line number attribute in XML.
	 * Value is: <code>lineNumber</code>
	 */
	public static final String ATTR_LINE_NUMBER = "lineNumber"; //$NON-NLS-1$

	/**
	 * Constant representing a restrictions attribute in XML.
	 * Value is: <code>restrictions</code>
	 */
	public static final String ATTR_RESTRICTIONS = "restrictions"; //$NON-NLS-1$

	/**
	 * Constant representing a visibility attribute in XML.
	 * Value is: <code>visibility</code>
	 */
	public static final String ATTR_VISIBILITY = "visibility"; //$NON-NLS-1$

	/**
	 * Constant representing a component attribute in XML.
	 * Value is: <code>component</code>
	 */
	public static final String ATTR_COMPONENT = "component"; //$NON-NLS-1$

	/**
	 * Constant representing the referenced element in XML.
	 * Value is: <code>target</code>
	 */
	public static final String ELEMENT_TARGET = "target"; //$NON-NLS-1$

	/**
	 * Constant representing the origin of a reference element in XML.
	 * Value is: <code>origin</code>
	 */
	public static final String ELEMENT_ORIGIN = "origin"; //$NON-NLS-1$
	
	/**
	 * Constant representing reference elements node in XML.
	 * Value is: <code>references</code>
	 */
	public static final String ELEMENT_REFERNCES = "references"; //$NON-NLS-1$
	
	/**
	 * Constant representing a reference element node in XML.
	 * Value is: <code>reference</code>
	 */
	public static final String ELEMENT_REFERNCE = "reference"; //$NON-NLS-1$
	
	/**
	 * Constant representing a kind attribute in XML.
	 * Value is: <code>kind</code>
	 */
	public static final String ATTR_KIND = "kind"; //$NON-NLS-1$	
	
	/**
	 * Returns an XML element for the given reference element and document.
	 * Clients are responsible for appending the element to an appropriate
	 * parent node in the document.
	 * 
	 * @param reference reference to create XML for
	 * @param document document in which to create the element
	 * @return XML element
	 */
	public static Element serializeElement(IReference reference, Document document) {
		Element element = document.createElement(ELEMENT_REFERNCE);
		element.setAttribute(ATTR_KIND, toString(reference.getReferenceKind(), ReferenceModifiers.class, null));
		ILocation origin = reference.getSourceLocation();
		Element child = serializeLocation(ELEMENT_ORIGIN, origin.getApiComponent().getId(), origin.getMember(), origin.getLineNumber(), null, document);
		element.appendChild(child);
		ILocation target = reference.getResolvedLocation();
		child = serializeLocation(ELEMENT_TARGET, target.getApiComponent().getId(), target.getMember(), -1, reference.getResolvedAnnotations(), document);
		element.appendChild(child);
		return element;
	}
	
	/**
	 * Returns an XML element representing the given location.
	 * 
	 * @param nodeName name of XML element
	 * @param componentId component containing the element or <code>null</code> if unspecified
	 * @param element the element being serialized
	 * @param lineNumber the line number associated with the location or -1 if unspecified
	 * @param annotation the API annotations associated with the element, or <code>null</code>
	 *  if unspecified
	 * @param document document in which to create the element
	 * @return XML element
	 */
	private static Element serializeLocation(String nodeName, String componentId, IMemberDescriptor element, int lineNumber, IApiAnnotations annotation, Document document) {
		Element node = document.createElement(nodeName);
		if (componentId != null) {
			node.setAttribute(ATTR_COMPONENT, componentId);
		}
		if (annotation != null) {
			node.setAttribute(ATTR_VISIBILITY, toString(annotation.getVisibility(), VisibilityModifiers.class, null));
			node.setAttribute(ATTR_RESTRICTIONS, toString(annotation.getRestrictions(), RestrictionModifiers.class, "NO_RESTRICTIONS")); //$NON-NLS-1$
		}
		if (lineNumber >= 0) {
			node.setAttribute(ATTR_LINE_NUMBER, Integer.toString(lineNumber));
		}
		node.setAttribute(ATTR_PACKAGE, element.getPackage().getName());
		if (element instanceof IReferenceTypeDescriptor) {
			node.setAttribute(ATTR_TYPE, ((IReferenceTypeDescriptor)element).getName());
		} else {
			node.setAttribute(ATTR_TYPE, element.getEnclosingType().getName());
			if (element instanceof IFieldDescriptor) {
				node.setAttribute(ATTR_FIELD, element.getName());
			} else if (element instanceof IMethodDescriptor) {
				node.setAttribute(ATTR_METHOD, element.getName());
				node.setAttribute(ATTR_METHOD_SIGNATURE, ((IMethodDescriptor)element).getSignature());
			}
		}
		return node;
	}
	
	/**
	 * Returns the given modifiers in a string format
	 * suitable for writing to an XML document. The string is
	 * a comma separated list of constant names defined in
	 * the given class.
	 * 
	 * @param modifiers reference kind bit mask
	 * @param clazz class the modifiers are defined in
	 * @param emptyValue string to return if the mask is empty or <code>null</code> if
	 *  an empty string should be returned
	 * @return comma separated list of reference kind constant names
	 */
	private static String toString(int modifiers, Class clazz, String emptyValue) {
		Object obj = null;
		try {
			obj = clazz.newInstance();
		} catch (InstantiationException e) {
			ApiPlugin.log(e);
		} catch (IllegalAccessException e) {
			ApiPlugin.log(e);
		}
		StringBuffer buffer = new StringBuffer();
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			if (Modifier.isPublic(field.getModifiers()) && field.getType() == int.class) {
				try {
					int flag = field.getInt(obj);
					if (flag != 0 && (flag & modifiers) == flag) {
						append(buffer, field.getName());
					}
				} catch (IllegalArgumentException e) {
					ApiPlugin.log(e);
				} catch (IllegalAccessException e) {
					ApiPlugin.log(e);
				}
			}
		} 
		if (buffer.length() == 0 && emptyValue != null) {
			return emptyValue;
		}
		return buffer.toString();
	}
	
	/**
	 * Appends the given modifier string to the buffer. A comma
	 * is inserted if required.
	 * 
	 * @param buffer string buffer
	 * @param modifier modifier to append
	 */
	private static void append(StringBuffer buffer, String modifier) {
		if (buffer.length() > 0) {
			buffer.append(',');
		}
		buffer.append(modifier);
	}	
	
	/**
	 * Serializes references into an XML document,
	 * 
	 * @param references to include
	 * @return XML document
	 * @throws CoreException
	 */
	public static Document serializeReferences(IReference[] references) throws CoreException {
		Document document = Util.newDocument();
		Element root = document.createElement(ELEMENT_REFERNCES);
		document.appendChild(root);
		for (int i = 0; i < references.length; i++) {
			Element child = XMLFactory.serializeElement(references[i], document);
			root.appendChild(child);
		}
		return document;
	}
}
