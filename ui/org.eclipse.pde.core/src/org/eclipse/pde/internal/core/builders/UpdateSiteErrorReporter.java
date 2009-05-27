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
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class UpdateSiteErrorReporter extends ManifestErrorReporter {

	private IProgressMonitor fMonitor;

	public UpdateSiteErrorReporter(IFile file) {
		super(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.XMLErrorReporter#validateContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void validateContent(IProgressMonitor monitor) {
		fMonitor = monitor;
		Element root = getDocumentRoot();
		if (root == null)
			return;
		String elementName = root.getNodeName();
		if (!"site".equals(elementName)) { //$NON-NLS-1$
			reportIllegalElement(root, CompilerFlags.ERROR);
		} else {
			NamedNodeMap attributes = root.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Attr attr = (Attr) attributes.item(i);
				String name = attr.getName();
				if (!name.equals("type") && //$NON-NLS-1$
						!name.equals("url") && //$NON-NLS-1$
						!name.equals("mirrorsURL") && //$NON-NLS-1$
						!name.equals("digestURL") && //$NON-NLS-1$
						!name.equals("pack200") && //$NON-NLS-1$
						!name.equals("availableLocales") && //$NON-NLS-1$
						!name.equals("associateSitesURL")) { //$NON-NLS-1$
					reportUnknownAttribute(root, name, CompilerFlags.ERROR);
				}
			}
			validateDescription(root);
			validateFeatures(root);
			validateCategoryDefinitions(root);
			validateArchives(root);
		}
	}

	/**
	 * @param root
	 */
	private void validateArchives(Element root) {
		NodeList list = getChildrenByName(root, "archive"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element) list.item(i);
			assertAttributeDefined(element, "path", CompilerFlags.ERROR); //$NON-NLS-1$
			assertAttributeDefined(element, "url", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = element.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr) attributes.item(j);
				String name = attr.getName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(element, "url"); //$NON-NLS-1$
				} else if (!name.equals("path")) { //$NON-NLS-1$
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
		}
	}

	/**
	 * @param root
	 */
	private void validateCategoryDefinitions(Element root) {
		NodeList list = getChildrenByName(root, "category-def"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element) list.item(i);
			assertAttributeDefined(element, "name", CompilerFlags.ERROR); //$NON-NLS-1$
			assertAttributeDefined(element, "label", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = element.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr) attributes.item(j);
				String name = attr.getName();
				if (!name.equals("name") && !name.equals("label")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			validateDescription(element);
		}
	}

	/**
	 * @param root
	 */
	private void validateCategories(Element root) {
		NodeList list = getChildrenByName(root, "category"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element) list.item(i);
			assertAttributeDefined(element, "name", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = element.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr) attributes.item(j);
				String name = attr.getName();
				if (!name.equals("name")) { //$NON-NLS-1$
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
		}
	}

	private void validateFeatures(Element parent) {
		NodeList list = getChildrenByName(parent, "feature"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);
			assertAttributeDefined(element, "url", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = element.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr) attributes.item(j);
				String name = attr.getName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(element, "url"); //$NON-NLS-1$
				} else if (name.equals("patch")) { //$NON-NLS-1$
					validateBoolean(element, attr);
				} else if (name.equals("version")) { //$NON-NLS-1$
					validateVersionAttribute(element, attr);
				} else if (!name.equals("type") && !name.equals("id") //$NON-NLS-1$ //$NON-NLS-2$
						&& !name.equals("os") && !name.equals("ws") //$NON-NLS-1$ //$NON-NLS-2$
						&& !name.equals("nl") && !name.equals("arch")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			validateCategories(element);
		}
	}

	/**
	 * @param root
	 */
	private void validateDescription(Element parent) {
		NodeList list = getChildrenByName(parent, "description"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element) list.item(0);
			validateElementWithContent((Element) list.item(0), true);
			if (element.getAttributeNode("url") != null) //$NON-NLS-1$
				validateURL(element, "url"); //$NON-NLS-1$
			reportExtraneousElements(list, 1);
		}
	}

}
