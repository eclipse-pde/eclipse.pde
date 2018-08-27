/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.toc.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.toc.ITocConstants;

public class TocDocumentFactory extends DocumentNodeFactory {
	private TocModel fModel;

	public TocDocumentFactory(TocModel model) {
		fModel = model;
	}

	@Override
	public IDocumentElementNode createDocumentNode(String name,
			IDocumentElementNode parent) {

		// Semantics:
		// org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_help_toc.html

		if (isToc(name)) { // Root
			return createToc();
		}

		if (isTopic(name)) { // Topic
			return createTocTopic();
		}

		if (isLink(name)) { // Link
			return createTocLink();
		}

		if (isAnchor(name)) { // Anchor
			return createTocAnchor();
		}

		return super.createDocumentNode(name, parent);
	}

	private boolean isTocElement(String name, String elementName) {
		if (name.equals(elementName)) {
			return true;
		}
		return false;
	}

	private boolean isToc(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_TOC);
	}

	private boolean isAnchor(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_ANCHOR);
	}

	private boolean isTopic(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_TOPIC);
	}

	private boolean isLink(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_LINK);
	}

	public Toc createToc() {
		return new Toc(fModel);
	}

	public TocTopic createTocTopic() {
		return new TocTopic(fModel);
	}

	public TocLink createTocLink() {
		return new TocLink(fModel);
	}

	public TocTopic createTocTopic(IFile file) {
		return new TocTopic(fModel, file);
	}

	public TocLink createTocLink(IFile file) {
		return new TocLink(fModel, file);
	}

	public TocAnchor createTocAnchor() {
		return new TocAnchor(fModel);
	}
}
