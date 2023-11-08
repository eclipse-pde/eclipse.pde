/*******************************************************************************
 *  Copyright (c) 2003, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.text.DocumentHandler;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.xml.sax.SAXException;

public class PluginDocumentHandler extends DocumentHandler {

	private final PluginModelBase fModel;
	private String fSchemaVersion;
	protected PluginDocumentNodeFactory fFactory;

	/**
	 * @param model
	 */
	public PluginDocumentHandler(PluginModelBase model, boolean reconciling) {
		super(reconciling);
		fModel = model;
		fFactory = (PluginDocumentNodeFactory) getModel().getPluginFactory();
	}

	@Override
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		IPluginBase pluginBase = fModel.getPluginBase(false);
		try {
			if (pluginBase != null) {
				pluginBase.setSchemaVersion(fSchemaVersion);
			}
		} catch (CoreException e) {
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			// Data should be of the form: version="<version>"
			if (data.length() > 10 && data.substring(0, 9).equals("version=\"") && data.charAt(data.length() - 1) == '\"') { //$NON-NLS-1$
				fSchemaVersion = TargetPlatformHelper.getSchemaVersionForTargetVersion(data.substring(9, data.length() - 1));
			} else {
				fSchemaVersion = TargetPlatformHelper.getSchemaVersion();
			}
		}
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		fSchemaVersion = null;
	}

	protected PluginModelBase getModel() {
		return fModel;
	}

	@Override
	protected IDocumentElementNode getDocumentNode(String name, IDocumentElementNode parent) {
		IDocumentElementNode node = null;
		if (parent == null) {
			node = (IDocumentElementNode) getModel().getPluginBase(false);
			if (node != null) {
				node.setOffset(-1);
				node.setLength(-1);
			}
		} else {
			IDocumentElementNode[] children = parent.getChildNodes();
			for (IDocumentElementNode childNode : children) {
				if (childNode.getOffset() < 0) {
					if (name.equals(childNode.getXMLTagName())) {
						node = childNode;
					}
					break;
				}
			}
		}

		if (node == null) {
			return fFactory.createDocumentNode(name, parent);
		}

		IDocumentAttributeNode[] attrs = node.getNodeAttributes();
		for (IDocumentAttributeNode attrNode : attrs) {
			attrNode.setNameOffset(-1);
			attrNode.setNameLength(-1);
			attrNode.setValueOffset(-1);
			attrNode.setValueLength(-1);
		}

		for (int i = 0; i < node.getChildNodes().length; i++) {
			IDocumentElementNode child = node.getChildAt(i);
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

	@Override
	protected IDocumentAttributeNode getDocumentAttribute(String name, String value, IDocumentElementNode parent) {
		IDocumentAttributeNode attr = parent.getDocumentAttribute(name);
		try {
			if (attr == null) {
				attr = fFactory.createAttribute(name, value, parent);
			} else {
				if (!name.equals(attr.getAttributeName())) {
					attr.setAttributeName(name);
				}
				if (!value.equals(attr.getAttributeValue())) {
					attr.setAttributeValue(value);
				}
			}
		} catch (CoreException e) {
		}
		return attr;
	}

	@Override
	protected IDocumentTextNode getDocumentTextNode(String content, IDocumentElementNode parent) {

		IDocumentTextNode textNode = parent.getTextNode();
		if (textNode == null) {
			if (content.trim().length() > 0) {
				textNode = fFactory.createDocumentTextNode(content, parent);
			}
		} else {
			String newContent = textNode.getText() + content;
			textNode.setText(newContent);
		}
		return textNode;
	}

}
