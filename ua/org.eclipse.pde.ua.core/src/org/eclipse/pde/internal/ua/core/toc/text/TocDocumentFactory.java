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

package org.eclipse.pde.internal.ua.core.toc.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.ua.core.toc.ITocConstants;

public class TocDocumentFactory extends DocumentNodeFactory implements
		IDocumentNodeFactory {
	private TocModel fModel;

	/**
	 * @param model
	 */
	public TocDocumentFactory(TocModel model) {
		fModel = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode
	 * (java.lang.String,
	 * org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentElementNode createDocumentNode(String name,
			IDocumentElementNode parent) {

		// Semantics:
		// org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_help_toc.html

		if (isToc(name)) { // Root
			return (IDocumentElementNode) createToc();
		}

		if (isTopic(name)) { // Topic
			return (IDocumentElementNode) createTocTopic();
		}

		if (isLink(name)) { // Link
			return (IDocumentElementNode) createTocLink();
		}

		if (isAnchor(name)) { // Anchor
			return (IDocumentElementNode) createTocAnchor();
		}

		return super.createDocumentNode(name, parent);
	}

	/**
	 * @param name
	 * @param elementName
	 * @return
	 */
	private boolean isTocElement(String name, String elementName) {
		if (name.equals(elementName)) {
			return true;
		}
		return false;
	}

	/**
	 * @param name
	 * @return
	 */
	private boolean isToc(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_TOC);
	}

	/**
	 * @param name
	 * @return
	 */
	private boolean isAnchor(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_ANCHOR);
	}

	/**
	 * @param name
	 * @return
	 */
	private boolean isTopic(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_TOPIC);
	}

	/**
	 * @param name
	 * @return
	 */
	private boolean isLink(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_LINK);
	}

	/**
	 * @return
	 */
	public Toc createToc() {
		return new Toc(fModel);
	}

	/**
	 * @param parent
	 * @return
	 */
	public TocTopic createTocTopic() {
		return new TocTopic(fModel);
	}

	/**
	 * @param parent
	 * @return
	 */
	public TocLink createTocLink() {
		return new TocLink(fModel);
	}

	/**
	 * @param parent
	 * @return
	 */
	public TocTopic createTocTopic(IFile file) {
		return new TocTopic(fModel, file);
	}

	/**
	 * @param parent
	 * @return
	 */
	public TocLink createTocLink(IFile file) {
		return new TocLink(fModel, file);
	}

	/**
	 * @return
	 */
	public TocAnchor createTocAnchor() {
		return new TocAnchor(fModel);
	}
}
