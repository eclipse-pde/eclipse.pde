package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.text.edits.*;

public class TextEditUtilities {

	public static TextEdit getInsertOperation(IDocumentNode node, IDocument doc) {
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
	
	public static TextEdit addAttributeOperation(IDocumentAttribute attr, String newValue, IDocument doc) {
		int offset = attr.getValueOffset();
		if (offset > -1)
			return new ReplaceEdit(offset, attr.getValueLength(), CoreUtility.getWritableString(newValue));

		IDocumentNode node = attr.getEnclosingElement();
		if (node.getOffset() > -1) {
			int len = getNextPosition(doc, node.getOffset(), '>');
			return new ReplaceEdit(node.getOffset(), len + 1, node.writeShallow(shouldTerminateElement(doc, node.getOffset()+ len)));
		}
		return getInsertOperation(node, doc);
	}
	
	private static boolean shouldTerminateElement(IDocument doc, int offset) {
		try {
			return doc.get(offset-1, 1).toCharArray()[0] == '/';
		} catch (BadLocationException e) {
		}
		return false;
	}

	private static IDocumentNode getHighestNodeToBeWritten(IDocumentNode node, IDocument doc) {
		IDocumentNode parent = node.getParentNode();
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

	private static InsertEdit insertAfterSibling(IDocumentNode node, IDocument doc) {
		IDocumentNode sibling = node.getPreviousSibling();
		for (;;) {
			if (sibling == null)
				break;
			if (sibling.getOffset() > -1) {
				node.setLineIndent(sibling.getLineIndent());
				String sep = TextUtilities.getDefaultLineDelimiter(doc);
				return new InsertEdit(sibling.getOffset() + sibling.getLength(), sep + node.write(true)); //$NON-NLS-1$
			}
			sibling = sibling.getPreviousSibling();
		}
		return null;
	}
	
	private static InsertEdit insertAsFirstChild(IDocumentNode node, IDocument doc) {
		int offset = node.getParentNode().getOffset();
		int length = getNextPosition(doc, offset, '>');
		node.setLineIndent(node.getParentNode().getLineIndent() + 3);
		String sep = TextUtilities.getDefaultLineDelimiter(doc);
		return new InsertEdit(offset+ length + 1, sep + node.write(true));	 //$NON-NLS-1$
	}
	
	private static int getNextPosition(IDocument doc, int offset, char ch) {
		int i = 0;
		try {
			for (i = 0; i + offset < doc.getLength() ;i++) {
				if (ch == doc.get(offset + i, 1).toCharArray()[0])
					break;
			}
		} catch (BadLocationException e) {
		}
		return i;
	}

}
