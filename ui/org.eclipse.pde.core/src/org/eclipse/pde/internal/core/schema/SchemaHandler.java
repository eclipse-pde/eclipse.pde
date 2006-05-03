/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class SchemaHandler extends XMLDefaultHandler {

	public SchemaHandler(boolean abbreviated) {
		super(abbreviated);
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
		if (!fAbbreviated) {
			super.characters(characters, start, length);
			return;
		}
	
		if (onAttributeDescription()) {
			StringBuffer buff = new StringBuffer();
			buff.append(characters, start, length);
			Node node = ((Node)fElementStack.peek());
			Node child = node.getFirstChild();
			if (child == null)
				node.appendChild(getDocument().createTextNode(buff.toString()));
			else
				((Text)child).appendData(buff.toString());
		}
	}
	
	private boolean onAttributeDescription() {
		Node node = (Node)fElementStack.peek();
		if (node == null)
			return false;
		if (!node.getNodeName().equals("documentation")) //$NON-NLS-1$
			return false;
		node = node.getParentNode();
		if (node == null)
			return false;
		if (!node.getNodeName().equals("annotation")) //$NON-NLS-1$
			return false;
		node = node.getParentNode();
		if (node == null)
			return false;
		return node.getNodeName().equals("attribute"); //$NON-NLS-1$
	}
}
