/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.xml.sax.SAXException;

public class PluginDocumentHandler extends AbstractPluginDocumentHandler {
	
	private PluginDocumentNodeFactory fFactory;
	
	public PluginDocumentHandler(PluginModelBase model) {
		super(model);
		fFactory = (PluginDocumentNodeFactory)getModel().getPluginFactory();
	}
		
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {		
		IDocumentNode parent = (IDocumentNode)fDocumentNodeStack.peek();
		if (parent == null)
			return;
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(ch, start, length);
		IDocumentTextNode textNode = parent.getTextNode();
		if (textNode == null) {
			if (buffer.toString().trim().length() > 0) {
				textNode = new DocumentTextNode();
				textNode.setEnclosingElement(parent);
				parent.addTextNode(textNode);
				textNode.setText(buffer.toString().trim());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentNode(java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentNode getDocumentNode(String name, IDocumentNode parent) {
		return fFactory.createDocumentNode(name, parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentAttribute getDocumentAttribute(String name,
			String value, IDocumentNode parent) {
		return fFactory.createAttribute(name, value, parent);
	}
}
