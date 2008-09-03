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

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.PDETextHover;

public class DSTextHover extends PDETextHover {

	private PDESourcePage fSourcePage;

	public DSTextHover(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		int offset = hoverRegion.getOffset();
		IDocumentRange range = fSourcePage.getRangeElement(offset, true);
		if (range instanceof IDocumentTextNode)
			return checkTranslatedValue((IDocumentTextNode) range);
		if (!(range instanceof IDSObject))
			return null;

		return ((IDSObject) range).getName();

	}

	private String checkTranslatedValue(IDocumentTextNode node) {
		String value = node.getText();
		if (value.startsWith("%")) //$NON-NLS-1$
			return ((IPluginObject) node.getEnclosingElement())
					.getResourceString(value);

		return null;
	}
}
