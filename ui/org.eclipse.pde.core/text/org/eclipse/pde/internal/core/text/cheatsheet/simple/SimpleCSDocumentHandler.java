/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.DocumentHandler;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;

/**
 * SimpleCSDocumentHandler
 *
 */
public class SimpleCSDocumentHandler extends DocumentHandler {

	private SimpleCSModel fModel;
	
	private SimpleCSDocumentFactory fFactory;	
	
	/**
	 * @param reconciling
	 */
	public SimpleCSDocumentHandler(SimpleCSModel model, boolean reconciling) {
		super(reconciling);
		
		fModel = model;
		// TODO: MP: TEO: Update cast
		fFactory = (SimpleCSDocumentFactory)model.getFactory();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		// TODO: MP: TEO: Could generalize this		
		return fModel.getDocument();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocumentAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	protected IDocumentAttribute getDocumentAttribute(String name,
			String value, IDocumentNode parent) {
// TODO: MP: TEO: Could generalize this
		IDocumentAttribute attribute = parent.getDocumentAttribute(name);
		try {
			if (attribute == null) {
				attribute = fFactory.createAttribute(name, value, parent);				
			} else {
				if (name.equals(attribute.getAttributeName()) == false) {
					attribute.setAttributeName(name);
				}
				if (value.equals(attribute.getAttributeValue()) == false) {
					attribute.setAttributeValue(value);
				}
			}
		} catch (CoreException e) {
			// Ignore
		}
		return attribute;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocumentNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	protected IDocumentNode getDocumentNode(String name, IDocumentNode parent) {
		// TODO: MP: TEO: Could generalize this
		// TODO: MP: TEO: Clean up
		IDocumentNode node = null;
		if (parent == null) {
			node = (IDocumentNode)fModel.getSimpleCS();
			if (node != null) {
				node.setOffset(-1);
				node.setLength(-1);
			}
		} else {
			IDocumentNode[] children = parent.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i].getOffset() < 0) {
					if (name.equals(children[i].getXMLTagName())) {
						node = children[i];
					}
					break;
				}
			}
		}
		
		if (node == null)
			return fFactory.createDocumentNode(name, parent);
		
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
		
		// clear text nodes if the user is typing on the source page
		// they will be recreated in the characters() method
		if (isReconciling()) {
			node.removeTextNode();
			node.setIsErrorNode(false);
		}
		
		return node;
		
	}

}
