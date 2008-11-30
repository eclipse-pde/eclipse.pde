/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class CheatSheetUtil {

	/**
	 * Recursively finds and accumulates all element's text and element children
	 * into a String in raw XML form
	 * 
	 * @param element
	 * @return
	 */
	public static String parseElementText(Element element) {
		// Puts all Text nodes in the full depth of the sub-tree
		// underneath this Node
		// i.e., there are neither adjacent Text nodes nor empty Text nodes.
		element.normalize();
		// Process only if there are children
		if (element.getChildNodes().getLength() > 0) {
			NodeList children = element.getChildNodes();
			StringBuffer buffer = new StringBuffer();
			// Traverse over each childe
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeType() == Node.TEXT_NODE) {
					// Accumulate the text children
					buffer.append(((Text) node).getData());
				} else if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element subElement = (Element) node;
					// Append the open bracket
					buffer.append('<');
					// Append the element name
					buffer.append(subElement.getNodeName());
					// Parse element attributes
					String elementAttributeText = parseElementAttributes(subElement);
					// Append the attributes (if any)
					if (elementAttributeText != null) {
						buffer.append(elementAttributeText);
					}
					// Recursively accumulate element children
					String value = parseElementText(subElement);
					// Determine whether the element has any content or not
					if (value.length() > 0) {
						// The element had children
						// Enclose the accumulated children with start and end
						// tags
						// Close off start element
						buffer.append('>');
						// Append element content
						buffer.append(value);
						// Append open bracket for end element tag
						buffer.append('<');
						buffer.append('/');
						buffer.append(subElement.getNodeName());
					} else {
						// The element had no children
						// Generate an abbreviated element tag
						buffer.append('/');
					}
					// Append the close bracket
					buffer.append('>');
				}
			}
			// Return all accumulated children under the input element as a raw
			// XML string
			return buffer.toString();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Aggregates all attributes from the given element, formats then into the
	 * proper key="value" XML format and returns them as one String
	 * 
	 * @param element
	 * @return The formatted String or null if the element has no attributes
	 */
	private static String parseElementAttributes(Element element) {
		// Verify we have attributes
		if (element.hasAttributes() == false) {
			return null;
		}
		// Create the buffer
		StringBuffer buffer = new StringBuffer();
		// Get the attributes
		NamedNodeMap attributeMap = element.getAttributes();
		// Accumulate all attributes
		for (int i = 0; i < attributeMap.getLength(); i++) {
			Node node = attributeMap.item(i);
			if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
				Attr attribute = (Attr) node;
				// Append space before attribute
				buffer.append(' ');
				// Append attribute name
				buffer.append(attribute.getName());
				// Append =
				buffer.append('=');
				// Append quote
				buffer.append('"');
				// Append attribute value
				buffer.append(attribute.getValue());
				// Append quote
				buffer.append('"');
			}
		}

		return buffer.toString();
	}

}
