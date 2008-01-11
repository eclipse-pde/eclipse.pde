/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;

public abstract class XMLUtil {

	/**
	 * Scans up the node's parents till it reaches
	 * a IPluginExtension or IPluginExtensionPoint (or null)
	 * and returns the result.
	 * 
	 * @param node
	 * @return the IPluginExtension or IPluginExtensionPoint that contains <code>node</code>
	 */
	public static IPluginObject getTopLevelParent(IDocumentRange range) {
		IDocumentElementNode node = null;
		if (range instanceof IDocumentAttributeNode)
			node = ((IDocumentAttributeNode) range).getEnclosingElement();
		else if (range instanceof IDocumentTextNode)
			node = ((IDocumentTextNode) range).getEnclosingElement();
		else if (range instanceof IPluginElement)
			node = (IDocumentElementNode) range;
		else if (range instanceof IPluginObject)
			// not an attribute/text node/element -> return direct node
			return (IPluginObject) range;

		while (node != null && !(node instanceof IPluginExtension) && !(node instanceof IPluginExtensionPoint))
			node = node.getParentNode();

		return node != null ? (IPluginObject) node : null;
	}

	private static boolean withinRange(int start, int len, int offset) {
		return start <= offset && offset <= start + len;
	}

	public static boolean withinRange(IDocumentRange range, int offset) {
		if (range instanceof IDocumentAttributeNode)
			return withinRange(((IDocumentAttributeNode) range).getValueOffset(), ((IDocumentAttributeNode) range).getValueLength(), offset);
		if (range instanceof IDocumentElementNode)
			return withinRange(((IDocumentElementNode) range).getOffset(), ((IDocumentElementNode) range).getLength(), offset);
		if (range instanceof IDocumentTextNode)
			return withinRange(((IDocumentTextNode) range).getOffset(), ((IDocumentTextNode) range).getLength(), offset);
		return false;
	}

	/**
	 * Get the ISchemaElement corresponding to this IDocumentElementNode
	 * @param node
	 * @param extensionPoint the extension point of the schema, if <code>null</code> it will be deduced
	 * @return the ISchemaElement for <code>node</code>
	 */
	public static ISchemaElement getSchemaElement(IDocumentElementNode node, String extensionPoint) {
		if (extensionPoint == null) {
			IPluginObject obj = getTopLevelParent(node);
			if (!(obj instanceof IPluginExtension))
				return null;
			extensionPoint = ((IPluginExtension) obj).getPoint();
		}
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(extensionPoint);
		if (schema == null)
			return null;

		// Bug 213457 - look up elements based on the schema in which the parent is found
		if (schema.getIncludes().length == 0 || "extension".equals(node.getXMLTagName())) //$NON-NLS-1$
			return schema.findElement(node.getXMLTagName());

		// if element is not "extension" & has multiple sub-schemas,
		// Then search for the element in the same schema in which the parent element if found.
		Stack stack = new Stack();
		while (node != null) {
			String tagName = node.getXMLTagName();
			if ("extension".equals(tagName)) //$NON-NLS-1$
				break;
			stack.push(node.getXMLTagName());
			node = node.getParentNode();
		}
		ISchemaElement element = null;
		while (!stack.isEmpty()) {
			element = schema.findElement((String) stack.pop());
			if (element == null)
				return null;
			schema = element.getSchema();
		}
		return element;
	}

	/**
	 * Get the ISchemaAttribute corresponding to this IDocumentAttributeNode
	 * @param attr
	 * @param extensionPoint the extension point of the schema, if <code>null</code> it will be deduced
	 * @return the ISchemaAttribute for <code>attr</code>
	 */
	public static ISchemaAttribute getSchemaAttribute(IDocumentAttributeNode attr, String extensionPoint) {
		ISchemaElement ele = getSchemaElement(attr.getEnclosingElement(), extensionPoint);
		if (ele == null)
			return null;

		return ele.getAttribute(attr.getAttributeName());
	}

	/**
	 * @param project
	 * @param attInfo
	 * @param counter
	 */
	public static String createDefaultClassName(IProject project, ISchemaAttribute attInfo, int counter) {
		String tag = attInfo.getParent().getName();
		String expectedType = attInfo.getBasedOn();
		String className = ""; //$NON-NLS-1$
		if (expectedType == null) {
			StringBuffer buf = new StringBuffer(tag);
			buf.setCharAt(0, Character.toUpperCase(tag.charAt(0)));
			className = buf.toString();
		} else {
			// package will be the same as the plugin ID
			// class name will be generated based on the required interface
			className = expectedType;
			int dotLoc = className.lastIndexOf('.');
			if (dotLoc != -1)
				className = className.substring(dotLoc + 1);
			if (className.length() > 2 && className.charAt(0) == 'I' && Character.isUpperCase(className.charAt(1)))
				className = className.substring(1);
		}
		String packageName = createDefaultPackageName(project, className);
		className += counter;
		return packageName + "." + className; //$NON-NLS-1$
	}

	/**
	 * @param id
	 * @param className
	 */
	public static String createDefaultPackageName(IProject project, String className) {
		String id = project.getName();
		StringBuffer buffer = new StringBuffer();
		IStatus status;
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(Character.toLowerCase(ch));
			} else {
				if (Character.isJavaIdentifierPart(ch))
					buffer.append(ch);
				else if (ch == '.') {
					status = JavaConventions.validatePackageName(buffer.toString(), PDEJavaHelper.getJavaSourceLevel(project), PDEJavaHelper.getJavaComplianceLevel(project));
					if (status.getSeverity() == IStatus.ERROR)
						buffer.append(className.toLowerCase(Locale.ENGLISH));
					buffer.append(ch);
				}
			}
		}

		status = JavaConventions.validatePackageName(buffer.toString(), PDEJavaHelper.getJavaSourceLevel(project), PDEJavaHelper.getJavaComplianceLevel(project));
		if (status.getSeverity() == IStatus.ERROR)
			buffer.append(className.toLowerCase(Locale.ENGLISH));

		return buffer.toString();
	}

	/**
	 * @param project
	 * @param attInfo
	 * @param counter
	 */
	public static String createDefaultName(IProject project, ISchemaAttribute attInfo, int counter) {
		if (attInfo.getType().getName().equals("boolean")) //$NON-NLS-1$
			return "true"; //$NON-NLS-1$

		String tag = attInfo.getParent().getName();
		return project.getName() + "." + tag + counter; //$NON-NLS-1$
	}

	/**
	 * @param elementInfo
	 */
	public static int getCounterValue(ISchemaElement elementInfo) {
		Hashtable counters = PDEPlugin.getDefault().getDefaultNameCounters();
		String counterKey = getCounterKey(elementInfo);
		Integer counter = (Integer) counters.get(counterKey);
		if (counter == null) {
			counter = new Integer(1);
		} else
			counter = new Integer(counter.intValue() + 1);
		counters.put(counterKey, counter);
		return counter.intValue();
	}

	public static String getCounterKey(ISchemaElement elementInfo) {
		return elementInfo.getSchema().getQualifiedPointId() + "." + elementInfo.getName(); //$NON-NLS-1$
	}

}
