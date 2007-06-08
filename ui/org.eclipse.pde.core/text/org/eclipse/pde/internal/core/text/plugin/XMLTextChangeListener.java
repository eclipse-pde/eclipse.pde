/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.AbstractTextChangeListener;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class XMLTextChangeListener extends AbstractTextChangeListener {
	
	private ArrayList fOperationList = new ArrayList();
	
	public XMLTextChangeListener(IDocument document) {
		super(document);
	}

	public TextEdit[] getTextOperations() {
		if (fOperationList.size() == 0)
			return new TextEdit[0];
		
		MultiTextEdit edit = new MultiTextEdit();
		try {
			if (PropertiesUtil.isNewlineNeeded(fDocument))
				insert(edit, new InsertEdit(fDocument.getLength(), TextUtilities.getDefaultLineDelimiter(fDocument)));
		} catch (BadLocationException e) {
		}
		Object[] operations = fOperationList.toArray();
		for (int i = 0; i < operations.length; i++)
			insert(edit, (TextEdit)operations[i]);
		
		return new TextEdit[] {edit};
	}
	
	protected static void insert(TextEdit parent, TextEdit edit) {
		if (!parent.hasChildren()) {
			parent.addChild(edit);
			if (edit instanceof MoveSourceEdit) {
				parent.addChild(((MoveSourceEdit)edit).getTargetEdit());
			}
			return;
		}
		TextEdit[] children= parent.getChildren();
		// First dive down to find the right parent.
		for (int i= 0; i < children.length; i++) {
			TextEdit child= children[i];
			if (covers(child, edit)) {
				insert(child, edit);
				return;
			}
		}
		// We have the right parent. Now check if some of the children have to
		// be moved under the new edit since it is covering it.
		for (int i= children.length - 1; i >= 0; i--) {
			TextEdit child= children[i];
			if (covers(edit, child)) {
				parent.removeChild(i);
				edit.addChild(child);
			}
		}
		parent.addChild(edit);
		if (edit instanceof MoveSourceEdit) {
			parent.addChild(((MoveSourceEdit)edit).getTargetEdit());
		}
	}
	
	protected static boolean covers(TextEdit thisEdit, TextEdit otherEdit) {
		if (thisEdit.getLength() == 0)	// an insertion point can't cover anything
			return false;
		
		int thisOffset= thisEdit.getOffset();
		int thisEnd= thisEdit.getExclusiveEnd();	
		if (otherEdit.getLength() == 0) {
			int otherOffset= otherEdit.getOffset();
			return thisOffset < otherOffset && otherOffset < thisEnd;
		}
		int otherOffset= otherEdit.getOffset();
		int otherEnd= otherEdit.getExclusiveEnd();
		return thisOffset <= otherOffset && otherEnd <= thisEnd;
	}
	
	
	protected void deleteNode(IDocumentNode node) {
		// delete previous op on this node, if any
		TextEdit old = (TextEdit)fOperationTable.get(node);
		if (old != null) {
			Object op = fOperationTable.remove(node);
			fOperationList.remove(op);
		}
		
		// if node has an offset, delete it
		if (node.getOffset() > -1) {
			// Create a delete op for this node
			TextEdit op = getDeleteNodeOperation(node);
			fOperationTable.put(node, op);
			fOperationList.add(op);
		} else if (old == null){
			// No previous op on this non-offset node, just rewrite highest ancestor with an offset
			insertNode(node);
		}
	}

	protected void insertNode(IDocumentNode node) {
		TextEdit op = null;
		node = getHighestNodeToBeWritten(node);
		if (node.getParentNode() == null) {
			// Only add the insertion edit operation if the node is a root node
			// Otherwise the insertion edit operation will specify to add the
			// node to the beginning of the file and corrupt it
			// See Bugs 163161, 166520
			if (node.isRoot()) {
				op = new InsertEdit(0, node.write(true));
			}
		} else {
			if (node.getOffset() > -1) {
				// this is an element that was of the form <element/>
				// it now needs to be broken up into <element><new/></element>
				op = new ReplaceEdit(node.getOffset(), node.getLength(), node.write(false));
			} else {
				// try to insert after last sibling that has an offset
				op = insertAfterSibling(node);
				// insert as first child of its parent
				if (op == null) {
					op = insertAsFirstChild(node);
				}
			}
		}
		fOperationTable.put(node, op);
		fOperationList.add(op);
	}

	private InsertEdit insertAfterSibling(IDocumentNode node) {
		IDocumentNode sibling = node.getPreviousSibling();
		for (;;) {
			if (sibling == null)
				break;
			if (sibling.getOffset() > -1) {
				node.setLineIndent(sibling.getLineIndent());
				return new InsertEdit(sibling.getOffset() + sibling.getLength(), fSep + node.write(true)); 
			}
			sibling = sibling.getPreviousSibling();
		}
		return null;
	}
	
	private InsertEdit insertAsFirstChild(IDocumentNode node) {
		int offset = node.getParentNode().getOffset();
		int length = getNextPosition(fDocument, offset, '>');
		node.setLineIndent(node.getParentNode().getLineIndent() + 3);
		return new InsertEdit(offset+ length + 1, fSep + node.write(true));	 
	}
	

	protected void modifyNode(IDocumentNode node, IModelChangedEvent event) {
		IDocumentNode oldNode = (IDocumentNode)event.getOldValue();
		IDocumentNode newNode = (IDocumentNode)event.getNewValue();
		
		IDocumentNode node1 = (oldNode.getPreviousSibling() == null || oldNode.equals(newNode.getPreviousSibling())) ? oldNode : newNode;
		IDocumentNode node2 = node1.equals(oldNode) ? newNode : oldNode;
		
		if (node1.getOffset() < 0 && node2.getOffset() < 0) {
			TextEdit op = (TextEdit)fOperationTable.get(node1);
			if (op == null) {
				// node 1 has no rule, so node 2 has no rule, therefore rewrite parent/ancestor
				insertNode(node);
			}
		} else if (node1.getOffset() > -1 && node2.getOffset() > -1) {
			// both nodes have offsets, so create a move target/source combo operation
			IRegion region = getMoveRegion(node1);
			MoveSourceEdit source = new MoveSourceEdit(region.getOffset(), region.getLength());
			region = getMoveRegion(node2);
			source.setTargetEdit(new MoveTargetEdit(region.getOffset()));		
			fOperationTable.put(node, source);
			fOperationList.add(source);
		} else {
			// one node with offset, the other without offset.  Delete/reinsert the one without offset
			insertNode((node1.getOffset() < 0) ? node1 : node2);
		}		
	}
	
	private IRegion getMoveRegion(IDocumentNode node) {
		int offset = node.getOffset();
		int length = node.getLength();
		int i = 1;
		try {
			for (;;i++) {
				char ch = fDocument.get(offset - i, 1).toCharArray()[0];
				if (!Character.isWhitespace(ch)) {
					i -= 1;
					break;
				}
			}
		} catch (BadLocationException e) {
		}
		return new Region(offset - i, length + i);		
	}

	protected void addAttributeOperation(IDocumentAttribute attr, IModelChangedEvent event) {
		int offset = attr.getValueOffset();
		Object newValue = event.getNewValue();
		Object changedObject = attr;
		TextEdit op = null;
		if (offset > -1) {
			if (newValue == null || newValue.toString().length() == 0) {
				int length = attr.getValueOffset() + attr.getValueLength() + 1 - attr.getNameOffset();
				op = getAttributeDeleteEditOperation(attr.getNameOffset(), length);
			} else {
				op = new ReplaceEdit(offset, attr.getValueLength(), getWritableString(event.getNewValue().toString()));
			}
		} 
				
		if (op == null) {
			IDocumentNode node = attr.getEnclosingElement();
			if (node.getOffset() > -1) {
				changedObject = node;
				int len = getNextPosition(fDocument, node.getOffset(), '>');
				op = new ReplaceEdit(node.getOffset(), len + 1, node.writeShallow(shouldTerminateElement(fDocument, node.getOffset() + len)));		
			} else {
				insertNode(node);
				return;
			}		
		}
		fOperationTable.put(changedObject, op);
		fOperationList.add(op);
	}
	
	protected void addElementContentOperation(IDocumentTextNode textNode) {
		TextEdit op = null;
		Object changedObject = textNode;
		if (textNode.getOffset() > -1) {
			String newText = getWritableString(textNode.getText());
			op = new ReplaceEdit(textNode.getOffset(), textNode.getLength(), newText);
		} else {
			IDocumentNode parent = textNode.getEnclosingElement();
			if (parent.getOffset() > -1) {
				try {
					String endChars = fDocument.get(parent.getOffset() + parent.getLength() - 2, 2);
					if ("/>".equals(endChars)) { //$NON-NLS-1$
						// parent element is of the form <element/>, rewrite it
						insertNode(parent);
						return;
					}
				} catch (BadLocationException e) {
				}
				// add text as first child
				changedObject = parent;
				StringBuffer buffer = new StringBuffer(fSep); 
				for (int i = 0; i < parent.getLineIndent(); i++) 
					buffer.append(" "); //$NON-NLS-1$
				buffer.append("   " + getWritableString(textNode.getText())); //$NON-NLS-1$
				int offset = parent.getOffset();
				int length = getNextPosition(fDocument, offset, '>');
				op = new InsertEdit(offset+ length + 1, buffer.toString());	
			} else {
				insertNode(parent);
				return;
			}
		}
		fOperationTable.put(changedObject, op);
		fOperationList.add(op);
	}

	private boolean shouldTerminateElement(IDocument doc, int offset) {
		try {
			return doc.get(offset-1, 1).toCharArray()[0] == '/';
		} catch (BadLocationException e) {
		}
		return false;
	}
	
	private int getNextPosition(IDocument doc, int offset, char ch) {
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
	
	private DeleteEdit getAttributeDeleteEditOperation(int offset, int length) {
		try {
			for (;;) {
				char ch = fDocument.get(offset + length, 1).toCharArray()[0];
				if (!Character.isWhitespace(ch)) {
					break;
				}
				
				length += 1;
			}
		} catch (BadLocationException e) {
		}
		return new DeleteEdit(offset, length);		
	}
	

	private DeleteEdit getDeleteNodeOperation(IDocumentNode node) {
		int offset = node.getOffset();
		int length = node.getLength();
		try {
			// node starts on this line:
			int startLine = fDocument.getLineOfOffset(offset);
			// 1st char on startLine has this offset:
			int startLineOffset = fDocument.getLineOffset(startLine);
			// hunt down 1st whitespace/start of line with startOffset:
			int startOffset;
			// loop backwards to the beginning of the line, stop if we find non-whitespace
			for (startOffset = offset - 1; startOffset >= startLineOffset; startOffset -= 1)
				if (!Character.isWhitespace(fDocument.getChar(startOffset)))
					break;
			
			// move forward one (loop stopped after reaching too far)
			startOffset += 1;
			
			// node ends on this line:
			int endLine = fDocument.getLineOfOffset(offset + length);
			// length of last line's delimiter:
			int endLineDelimLength = fDocument.getLineDelimiter(endLine).length();
			// hunt last whitespace/end of line with extraLength:
			int extraLength = length;
			while (true) {
				extraLength += 1;
				if (!Character.isWhitespace(fDocument.getChar(offset + extraLength))) {
					// found non-white space, move back one
					extraLength -= 1;
					break;
				}
				if (fDocument.getLineOfOffset(offset + extraLength) > endLine) {
					// don't want to touch the lineDelimeters
					extraLength -= endLineDelimLength;
					break;
				}
			}
			
			// if we reached start of line, remove newline
			if (startOffset == startLineOffset)
				startOffset -= fDocument.getLineDelimiter(startLine).length();
			
			// add difference of new offset
			length = extraLength + (offset - startOffset);
			offset = startOffset;
//			printDeletionRange(offset, length);
		} catch (BadLocationException e) {
		}
		return new DeleteEdit(offset, length);
	}

	protected void printDeletionRange(int offset, int length) {
		try {
			// newlines printed as \n
			// carriage returns printed as \r
			// tabs printed as \t
			// spaces printed as *
			String string = fDocument.get(offset, length);
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < string.length(); i++) {
				char c = string.charAt(i);
				if (c == '\n')
					buffer.append("\\n"); //$NON-NLS-1$
				else if (c == '\r')
					buffer.append("\\r"); //$NON-NLS-1$
				else if (c == '\t')
					buffer.append("\\t"); //$NON-NLS-1$
				else if (c == ' ')
					buffer.append('*');
				else
					buffer.append(c);
			}
			System.out.println(buffer.toString());
		} catch (BadLocationException e) {
		}
	}
	
	private IDocumentNode getHighestNodeToBeWritten(IDocumentNode node) {
		IDocumentNode parent = node.getParentNode();
		if (parent == null)
			return node;
		if (parent.getOffset() > -1) {
			try {
				String endChars = fDocument.get(parent.getOffset() + parent.getLength() - 2, 2);
				return ("/>".equals(endChars)) ? parent : node; //$NON-NLS-1$
			} catch (BadLocationException e) {
				return node;
			}
			
		}
		return getHighestNodeToBeWritten(parent);
	}
	
	private String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects == null)
			return;
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof IDocumentNode))
				continue;
			IDocumentNode node = (IDocumentNode)objects[i];
			Object op = fOperationTable.remove(node);
			fOperationList.remove(op);
			switch (event.getChangeType()) {
				case IModelChangedEvent.REMOVE:
					deleteNode(node);
					break;
				case IModelChangedEvent.INSERT:
					insertNode(node);
					break;
				case IModelChangedEvent.CHANGE:
					IDocumentAttribute attr = node.getDocumentAttribute(event.getChangedProperty());
					if (attr != null) {
						addAttributeOperation(attr, event);
					} else {
						if (event.getOldValue() instanceof IDocumentTextNode) {
							addElementContentOperation((IDocumentTextNode)event.getOldValue());
						} else if (event.getOldValue() instanceof IDocumentNode && event.getNewValue() instanceof IDocumentNode){
							// swapping of nodes
							modifyNode(node, event);
						}
					}
			}
		}
	}
}
