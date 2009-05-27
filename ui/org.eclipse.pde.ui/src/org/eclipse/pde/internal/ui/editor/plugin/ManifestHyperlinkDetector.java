/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaRootElement;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.ui.editor.PDEHyperlinkDetector;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.*;

public class ManifestHyperlinkDetector extends PDEHyperlinkDetector {

	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public ManifestHyperlinkDetector(PDESourcePage page) {
		super(page);
	}

	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttributeNode attr) {
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;

		IPluginObject node = XMLUtil.getTopLevelParent(attr);
		if (node == null || !node.getModel().isEditable())
			return null;

		IPluginModelBase base = node.getPluginModel();
		IResource res = base.getUnderlyingResource();
		IRegion linkRegion = new Region(attr.getValueOffset(), attr.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		if (node instanceof IPluginExtensionPoint) {
			if (attr.getAttributeName().equals(IPluginExtensionPoint.P_SCHEMA))
				link[0] = new SchemaHyperlink(linkRegion, attrValue, res);
			else if (attr.getAttributeName().equals(IPluginObject.P_NAME))
				if (attrValue.charAt(0) == '%')
					link[0] = new TranslationHyperlink(linkRegion, attrValue, base);

		} else if (node instanceof IPluginExtension) {
			ISchemaAttribute sAttr = XMLUtil.getSchemaAttribute(attr, ((IPluginExtension) node).getPoint());
			if (sAttr == null)
				return null;

			if (sAttr.getKind() == IMetaAttribute.JAVA) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			} else if (sAttr.getKind() == IMetaAttribute.RESOURCE) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			} else if (sAttr.getParent() instanceof SchemaRootElement) {
				if (attr.getAttributeName().equals(IPluginExtension.P_POINT))
					link[0] = new ExtensionHyperLink(linkRegion, attrValue);
			} else if (sAttr.isTranslatable()) {
				if (attrValue.charAt(0) == '%')
					link[0] = new TranslationHyperlink(linkRegion, attrValue, base);
			}
		}

		if (link[0] != null)
			return link;
		return null;
	}

	protected IHyperlink[] detectNodeHyperlink(IDocumentElementNode node) {
		// TODO what can we do here?
		// suggestions:
		//   - use SchemaEditor.openToElement(IPath path, ISchemaElement element)
		//     to directly highlight this particular element in a schema editor
		//      ? too fancy ?
		/*
				IPluginObject parent = XMLUtil.getTopLevelParent(node);
				if (parent == null || !parent.getModel().isEditable())
					return null;
				
				if (parent instanceof IPluginExtension) { 
					ISchemaElement sElement = XMLUtil.getSchemaElement(node, ((IPluginExtension)parent).getPoint());
					if (sElement == null)
						return null;
					URL url = sElement.getSchema().getURL();
					// only have access to URL now - extend SchemaEditor?
					SchemaEditor.openToElement(url, sElement);
				}
		*/
		return null;
	}

	protected IHyperlink[] detectTextNodeHyperlink(IDocumentTextNode node) {
		IDocumentElementNode enclosing = node.getEnclosingElement();
		if (!(enclosing instanceof IPluginObject))
			return null;
		IPluginModelBase base = ((IPluginObject) enclosing).getPluginModel();
		if (node.getText().charAt(0) == '%')
			return new IHyperlink[] {new TranslationHyperlink(new Region(node.getOffset(), node.getLength()), node.getText(), base)};
		return null;
	}
}
