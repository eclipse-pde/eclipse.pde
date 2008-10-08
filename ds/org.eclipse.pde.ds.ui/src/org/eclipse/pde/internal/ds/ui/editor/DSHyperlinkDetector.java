/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ui.editor.PDEHyperlinkDetector;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.JavaHyperlink;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;

public class DSHyperlinkDetector extends PDEHyperlinkDetector {

	/**
	 * @param editor
	 *            the editor in which to detect the hyperlink
	 */
	public DSHyperlinkDetector(PDESourcePage page) {
		super(page);
	}

	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttributeNode attr) {
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;

		if (!(attr.getEnclosingElement() instanceof IDSObject)) {
			return null;
		}
		IDSObject node = (IDSObject) attr.getEnclosingElement();

		if (node == null || !node.getModel().isEditable())
			return null;

		IRegion linkRegion = new Region(attr.getValueOffset(), attr
				.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		IDSModel base = node.getModel();
		IResource res = base.getUnderlyingResource();

		// Java HyperLink
		if (node instanceof IDSImplementation) {
			link[0] = new JavaHyperlink(linkRegion, attrValue, res);
		} else if (node instanceof IDSComponent) {
			if (attr.getAttributeName().equals(IDSConstants.ATTRIBUTE_COMPONENT_FACTORY)) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			}
		} else if (node instanceof IDSReference) {
			if (attr.getAttributeName().equals(
					IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE)) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			}
		} else if (node instanceof IDSProperty) {
			if (attr.getAttributeName().equals(
					IDSConstants.ATTRIBUTE_PROPERTY_TYPE)) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			}
		} else if (node instanceof IDSProvide) {
			if (attr.getAttributeName().equals(
					IDSConstants.ATTRIBUTE_PROVIDE_INTERFACE)) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			}

			// Resource Hyperlink
		} else if (node instanceof IDSProperties) {
			if (attr.getAttributeName().equals(IDSConstants.ATTRIBUTE_PROPERTIES_ENTRY)) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}

			// TODO reference bind and reference unbind should link to methods
			// declarations?
		}

		if (link[0] != null)
			return link;

		return null;
	}

	protected IHyperlink[] detectNodeHyperlink(IDocumentElementNode node) {
		return null;
	}

	protected IHyperlink[] detectTextNodeHyperlink(IDocumentTextNode node) {
		return null;
	}
}
