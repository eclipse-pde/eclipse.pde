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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.text.SimpleCSObject;
import org.eclipse.pde.internal.ui.editor.PDEHyperlinkDetector;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;

/**
 * SimpleCSHyperlinkDetector
 *
 */
public class SimpleCSHyperlinkDetector extends PDEHyperlinkDetector {

	/**
	 * @param page
	 */
	public SimpleCSHyperlinkDetector(PDESourcePage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEHyperlinkDetector#detectAttributeHyperlink(org.eclipse.pde.internal.core.text.IDocumentAttributeNode)
	 */
	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttributeNode attribute) {
		// Get the attribute value
		String attributeValue = attribute.getAttributeValue();
		// Ensure the value is defined
		if (PDETextHelper.isDefinedAfterTrim(attributeValue) == false) {
			return null;
		}
		// Get attribute parent
		IDocumentElementNode node = attribute.getEnclosingElement();
		// Ensure the node is defined
		if (node == null) {
			return null;
		} else if ((node instanceof SimpleCSObject) == false) {
			return null;
		}
		SimpleCSObject csObject = (SimpleCSObject) node;
		ISimpleCSModel model = csObject.getModel();
		// Ensure the model is editable
		if (model.isEditable() == false) {
			return null;
		}
		// Get the underlying model resource
		IResource resource = model.getUnderlyingResource();
		// Create the link region
		IRegion linkRegion = new Region(attribute.getValueOffset(), attribute.getValueLength());
		// Create the link container
		IHyperlink[] link = new IHyperlink[1];
		// Create the appropriate resource hyperlink
		if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			// Item
			if (attribute.getAttributeName().equals(ISimpleCSConstants.ATTRIBUTE_HREF)) {
				// Href
				link[0] = new ResourceHyperlink(linkRegion, attributeValue, resource);
			}
		} else if (csObject.getType() == ISimpleCSConstants.TYPE_INTRO) {
			// Intro
			if (attribute.getAttributeName().equals(ISimpleCSConstants.ATTRIBUTE_HREF)) {
				// Href
				link[0] = new ResourceHyperlink(linkRegion, attributeValue, resource);
			}
		}
		// If the link is defined return it
		if (link[0] != null) {
			return link;
		}
		return null;
	}

}
