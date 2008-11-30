/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.toc.ITocConstants;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ui.editor.PDEHyperlinkDetector;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;

public class TocHyperlinkDetector extends PDEHyperlinkDetector {

	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public TocHyperlinkDetector(PDESourcePage page) {
		super(page);
	}

	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttributeNode attr) {
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;

		IDocumentElementNode node = attr.getEnclosingElement();
		if (node == null || !(node instanceof TocObject) || !((TocObject) node).getModel().isEditable()) {
			return null;
		}

		TocObject tocObject = (TocObject) node;
		TocModel model = tocObject.getModel();
		IResource res = model.getUnderlyingResource();
		IRegion linkRegion = new Region(attr.getValueOffset(), attr.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		if (tocObject.getType() == ITocConstants.TYPE_TOC) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_TOPIC)) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		} else if (tocObject.getType() == ITocConstants.TYPE_TOPIC) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_HREF)) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		} else if (tocObject.getType() == ITocConstants.TYPE_LINK) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_TOC)) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		}

		if (link[0] != null) {
			return link;
		}

		return null;
	}

}
