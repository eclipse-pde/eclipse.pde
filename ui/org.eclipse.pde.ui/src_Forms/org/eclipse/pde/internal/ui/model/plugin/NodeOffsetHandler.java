/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.pde.internal.ui.model.*;
import org.xml.sax.*;

public class NodeOffsetHandler extends AbstractPluginDocumentHandler {

	/**
	 * @param model
	 */
	public NodeOffsetHandler(PluginModelBase model) {
		super(model);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.DocumentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		IDocumentNode node = (IDocumentNode)fDocumentNodeStack.peek();
		IDocumentAttribute[] attrs = node.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			//if (attrs[i].getNameOffset() == -1)
				//node.removeDocumentAttribute(attrs[i]);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentNode(java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentNode getDocumentNode(String name, IDocumentNode parent) {
		IDocumentNode node = null;
		if (parent == null) {
			node = (IDocumentNode)getModel().getPluginBase();
			node.setOffset(-1);
			node.setLength(-1);
		} else {
			IDocumentNode[] children = parent.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i].getOffset() < 0) {
					node = children[i];
					break;
				}
			}
		}
		
		IDocumentAttribute[] attrs = node.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			attrs[i].setNameOffset(-1);
			attrs[i].setNameLength(-1);
			attrs[i].setValueOffset(-1);
			attrs[i].setValueLength(-1);
		}
		
		for (int i = 0; i < node.getChildNodes().length; i++) {
			IDocumentNode child = node.getChildAt(i);
			child.setOffset(-1);
			child.setLength(-1);
		}
		
		return node;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.DocumentHandler#appendChildToParent(org.eclipse.pde.internal.ui.model.IDocumentNode, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected void appendChildToParent(IDocumentNode parent, IDocumentNode child) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentAttribute getDocumentAttribute(String name,
			String value, IDocumentNode parent) {
		return parent.getDocumentAttribute(name);
	}

}
