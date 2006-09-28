/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * CheatSheetUtil
 *
 */
public class CheatSheetUtil {

	/**
	 * Recursively finds and accumulates all element's text and element 
	 * children into a String in raw XML form 
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
					buffer.append(((Text)node).getData());
				} else if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element subElement = (Element)node;
					// Recursively accumulate element children
					String value = parseElementText(subElement);
					if (value.length() > 0) {
						// The element had children
						// Enclose the accumulated children with start and end tags
						buffer.append('<' + subElement.getNodeName() + '>');
						buffer.append(value);
						buffer.append("</" + subElement.getNodeName() + '>'); //$NON-NLS-1$
					} else {
						// The element had no children
						// Generate an abbreviated element tag
						buffer.append('<' + subElement.getNodeName() + "/>"); //$NON-NLS-1$
					}
				}
			}
			// Return all accumulated children under the input element as a raw
			// XML string
			return buffer.toString();
		}
		return ""; //$NON-NLS-1$
	}
		
	
}
