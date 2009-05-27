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
package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.eclipse.text.edits.*;

public class TextEditUtilities {

	public static TextEdit getInsertOperation(IDocumentElementNode node, IDocument doc) {
		node = getHighestNodeToBeWritten(node, doc);
		if (node.getParentNode() == null)
			return new InsertEdit(0, node.write(true));

		if (node.getOffset() > -1) {
			// this is an element that was of the form <element/>
			// it now needs to be broken up into <element><new/></element>
			return new ReplaceEdit(node.getOffset(), node.getLength(), node.write(false));
		}
		// try to insert after last sibling that has an offset
		TextEdit op = insertAfterSibling(node, doc);

		// insert as first child of its parent if op is null
		return (op != null) ? op : insertAsFirstChild(node, doc);
	}

	public static TextEdit addAttributeOperation(IDocumentAttributeNode attr, String newValue, IDocument doc) {
		int offset = attr.getValueOffset();
		if (offset > -1)
			return new ReplaceEdit(offset, attr.getValueLength(), PDEXMLHelper.getWritableString(newValue));

		IDocumentElementNode node = attr.getEnclosingElement();
		if (node.getOffset() > -1) {
			int len = getNextPosition(doc, node.getOffset(), '>');
			return new ReplaceEdit(node.getOffset(), len + 1, node.writeShallow(shouldTerminateElement(doc, node.getOffset() + len)));
		}
		return getInsertOperation(node, doc);
	}

	private static boolean shouldTerminateElement(IDocument doc, int offset) {
		try {
			return doc.get(offset - 1, 1).toCharArray()[0] == '/';
		} catch (BadLocationException e) {
		}
		return false;
	}

	private static IDocumentElementNode getHighestNodeToBeWritten(IDocumentElementNode node, IDocument doc) {
		IDocumentElementNode parent = node.getParentNode();
		if (parent == null)
			return node;
		if (parent.getOffset() > -1) {
			try {
				String endChars = doc.get(parent.getOffset() + parent.getLength() - 2, 2);
				return ("/>".equals(endChars)) ? parent : node; //$NON-NLS-1$
			} catch (BadLocationException e) {
				return node;
			}

		}
		return getHighestNodeToBeWritten(parent, doc);
	}

	private static InsertEdit insertAfterSibling(IDocumentElementNode node, IDocument doc) {
		IDocumentElementNode sibling = node.getPreviousSibling();
		for (;;) {
			if (sibling == null)
				break;
			if (sibling.getOffset() > -1) {
				node.setLineIndent(sibling.getLineIndent());
				String sep = TextUtilities.getDefaultLineDelimiter(doc);
				return new InsertEdit(sibling.getOffset() + sibling.getLength(), sep + node.write(true));
			}
			sibling = sibling.getPreviousSibling();
		}
		return null;
	}

	private static InsertEdit insertAsFirstChild(IDocumentElementNode node, IDocument doc) {
		int offset = node.getParentNode().getOffset();
		int length = getNextPosition(doc, offset, '>');
		node.setLineIndent(node.getParentNode().getLineIndent() + 3);
		String sep = TextUtilities.getDefaultLineDelimiter(doc);
		return new InsertEdit(offset + length + 1, sep + node.write(true));
	}

	private static int getNextPosition(IDocument doc, int offset, char ch) {
		int i = 0;
		try {
			for (i = 0; i + offset < doc.getLength(); i++) {
				if (ch == doc.get(offset + i, 1).toCharArray()[0])
					break;
			}
		} catch (BadLocationException e) {
		}
		return i;
	}

}
