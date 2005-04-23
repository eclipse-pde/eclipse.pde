/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
import org.eclipse.pde.internal.ui.model.IDocumentAttribute;
import org.eclipse.pde.internal.ui.model.IDocumentNode;
import org.eclipse.pde.internal.ui.model.IDocumentTextNode;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;

public abstract class XMLInputContext extends UTF8InputContext {
	protected HashMap fOperationTable = new HashMap();
	protected HashMap fMoveOperations = new HashMap();

	/**
	 * @param editor
	 * @param input
	 */
	public XMLInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
	}	

	protected IDocumentPartitioner createDocumentPartitioner() {
		FastPartitioner partitioner = new FastPartitioner(
				new XMLPartitionScanner(), new String[]{
						XMLPartitionScanner.XML_TAG,
						XMLPartitionScanner.XML_COMMENT});
		return partitioner;
	}
	
	protected IDocumentSetupParticipant getDocumentSetupParticipant() {
		return new XMLDocumentSetupParticpant();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects != null) {
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				switch (event.getChangeType()) {
					case IModelChangedEvent.REMOVE :
						if (object instanceof IDocumentNode)
							removeNode((IDocumentNode) object, ops);
						break;
					case IModelChangedEvent.INSERT :
						if (object instanceof IDocumentNode)
							insertNode((IDocumentNode) object, ops);
						break;
					case IModelChangedEvent.CHANGE :
						if (object instanceof IDocumentNode) {
							IDocumentNode node = (IDocumentNode) object;
							IDocumentAttribute attr = node.getDocumentAttribute(event.getChangedProperty());
							if (attr != null) {
								addAttributeOperation(attr, ops, event);
							} else {
								if (event.getOldValue() instanceof IDocumentTextNode) {
									addElementContentOperation((IDocumentTextNode)event.getOldValue(), ops);
								} else if (event.getOldValue() instanceof IDocumentNode && event.getNewValue() instanceof IDocumentNode){
									// swapping of nodes
									modifyNode(node, ops, event);
								}
							}
						}
					default:
						break;
				}
			}
		}
	}
	
	private void removeNode(IDocumentNode node, ArrayList ops) {
		// delete previous op on this node, if any
		TextEdit old = (TextEdit)fOperationTable.get(node);
		if (old != null) {
			ops.remove(old);
			fOperationTable.remove(node);
		}
		TextEdit oldMove= (TextEdit)fMoveOperations.get(node);
		if (oldMove != null) {
			ops.remove(oldMove);
			fMoveOperations.remove(node);
		}
		// if node has an offset, delete it
		if (node.getOffset() > -1) {
			// Create a delete op for this node
			TextEdit op = getDeleteNodeOperation(node);
			ops.add(op);
			fOperationTable.put(node, op);			
		} else if (old == null && oldMove == null){
			// No previous op on this non-offset node, just rewrite highest ancestor with an offset
			insertNode(node, ops);
		}
	}

	private void insertNode(IDocumentNode node, ArrayList ops) {
		TextEdit op = null;
		node = getHighestNodeToBeWritten(node);
		if (node.getParentNode() == null) {
			op = new InsertEdit(0, node.write(true));
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
		TextEdit old = (TextEdit) fOperationTable.get(node);
		if (old != null)
			ops.remove(old);
		ops.add(op);
		fOperationTable.put(node, op);				
	}

	private InsertEdit insertAfterSibling(IDocumentNode node) {
		IDocumentNode sibling = node.getPreviousSibling();
		for (;;) {
			if (sibling == null)
				break;
			if (sibling.getOffset() > -1) {
				node.setLineIndent(sibling.getLineIndent());
				String sep = TextUtilities.getDefaultLineDelimiter(getDocumentProvider().getDocument(getInput()));
				return new InsertEdit(sibling.getOffset() + sibling.getLength(), sep + node.write(true)); //$NON-NLS-1$
			}
			sibling = sibling.getPreviousSibling();
		}
		return null;
	}
	
	private InsertEdit insertAsFirstChild(IDocumentNode node) {
		int offset = node.getParentNode().getOffset();
		int length = getNextPosition(getDocumentProvider().getDocument(getInput()), offset, '>');
		node.setLineIndent(node.getParentNode().getLineIndent() + 3);
		String sep = TextUtilities.getDefaultLineDelimiter(getDocumentProvider().getDocument(getInput()));
		return new InsertEdit(offset+ length + 1, sep + node.write(true));	 //$NON-NLS-1$
	}
	

	private void modifyNode(IDocumentNode node, ArrayList ops, IModelChangedEvent event) {
		IDocumentNode oldNode = (IDocumentNode)event.getOldValue();
		IDocumentNode newNode = (IDocumentNode)event.getNewValue();
		
		IDocumentNode node1 = (oldNode.getPreviousSibling() == null || oldNode.equals(newNode.getPreviousSibling())) ? oldNode : newNode;
		IDocumentNode node2 = node1.equals(oldNode) ? newNode : oldNode;
		
		if (node1.getOffset() < 0 && node2.getOffset() < 2) {
			TextEdit op = (TextEdit)fOperationTable.get(node1);
			if (op == null) {
				// node 1 has no rule, so node 2 has no rule, therefore rewrite parent/ancestor
				insertNode(node, ops);
			} else {
				// swap order of insert operations
				TextEdit op2 = (TextEdit)fOperationTable.get(node2);
				ops.set(ops.indexOf(op), op2);
				ops.set(ops.indexOf(op2), op);
			}
		} else if (node1.getOffset() > -1 && node2.getOffset() > -1) {
			// both nodes have offsets, so create a move target/source combo operation
			IRegion region = getMoveRegion(node1);
			MoveSourceEdit source = new MoveSourceEdit(region.getOffset(), region.getLength());
			region = getMoveRegion(node2);
			source.setTargetEdit(new MoveTargetEdit(region.getOffset()));
			MoveSourceEdit op = (MoveSourceEdit)fMoveOperations.get(node1);
			if (op != null) {
				ops.set(ops.indexOf(op), source);
			} else {
				op = (MoveSourceEdit)fMoveOperations.get(node2);
				if (op != null && op.getTargetEdit().getOffset() == source.getOffset()) {
					fMoveOperations.remove(node2);
					ops.remove(op);
					return;
				} 
				ops.add(source);
			}			
			fMoveOperations.put(node1, source);
		} else {
			// one node with offset, the other without offset.  Delete/reinsert the one without offset
			insertNode((node1.getOffset() < 0) ? node1 : node2, ops);
		}		
	}
	
	private IRegion getMoveRegion(IDocumentNode node) {
		int offset = node.getOffset();
		int length = node.getLength();
		int i = 1;
		try {
			IDocument doc = getDocumentProvider().getDocument(getInput());
			for (;;i++) {
				char ch = doc.get(offset - i, 1).toCharArray()[0];
				if (!Character.isWhitespace(ch)) {
					i -= 1;
					break;
				}
			}
		} catch (BadLocationException e) {
		}
		return new Region(offset - i, length + i);		
	}

	private void addAttributeOperation(IDocumentAttribute attr, ArrayList ops, IModelChangedEvent event) {
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
			IDocument doc = getDocumentProvider().getDocument(getInput());
			if (node.getOffset() > -1) {
				changedObject = node;
				int len = getNextPosition(doc, node.getOffset(), '>');
				op = new ReplaceEdit(node.getOffset(), len + 1, node.writeShallow(shouldTerminateElement(doc, node.getOffset() + len)));		
			} else {
				insertNode(node, ops);
				return;
			}		
		}
		TextEdit oldOp = (TextEdit)fOperationTable.get(changedObject);
		if (oldOp != null)
			ops.remove(oldOp);
		ops.add(op);
		fOperationTable.put(changedObject, op);
	}
	
	private void addElementContentOperation(IDocumentTextNode textNode, ArrayList ops) {
		TextEdit op = null;
		Object changedObject = textNode;
		if (textNode.getOffset() > -1) {
			String newText = getWritableString(textNode.getText());
			op = new ReplaceEdit(textNode.getOffset(), textNode.getLength(), newText);
		} else {
			IDocumentNode parent = textNode.getEnclosingElement();
			if (parent.getOffset() > -1) {
				IDocument doc = getDocumentProvider().getDocument(getInput());
				try {
					String endChars = doc.get(parent.getOffset() + parent.getLength() - 2, 2);
					if ("/>".equals(endChars)) { //$NON-NLS-1$
						// parent element is of the form <element/>, rewrite it
						insertNode(parent, ops);
						return;
					}
				} catch (BadLocationException e) {
				}
				// add text as first child
				changedObject = parent;
				String sep = TextUtilities.getDefaultLineDelimiter(getDocumentProvider().getDocument(getInput()));
				StringBuffer buffer = new StringBuffer(sep); //$NON-NLS-1$
				for (int i = 0; i < parent.getLineIndent(); i++) 
					buffer.append(" "); //$NON-NLS-1$
				buffer.append("   " + getWritableString(textNode.getText())); //$NON-NLS-1$
				int offset = parent.getOffset();
				int length = getNextPosition(doc, offset, '>');
				op = new InsertEdit(offset+ length + 1, buffer.toString());	
			} else {
				insertNode(parent, ops);
				return;
			}
		}
		TextEdit oldOp = (TextEdit)fOperationTable.get(changedObject);
		if (oldOp != null)
			ops.remove(oldOp);
		ops.add(op);
		fOperationTable.put(changedObject, op);		
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
			IDocument doc = getDocumentProvider().getDocument(getInput());
			for (;;) {
				char ch = doc.get(offset + length, 1).toCharArray()[0];
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
		int indent = 0;
		try {
			IDocument doc = getDocumentProvider().getDocument(getInput());
			int line = doc.getLineOfOffset(offset + length);
			for (;;) {
				char ch = doc.get(offset + length, 1).toCharArray()[0];
				if (doc.getLineOfOffset(offset + length) > line || !Character.isWhitespace(ch)) {
					length -= 1;
					break;
				}
				length += 1;
			}
			
			for (indent = 1; indent <= node.getLineIndent(); indent++) {
				char ch = doc.get(offset - indent, 1).toCharArray()[0];
				if (!Character.isWhitespace(ch)) {
					indent -= 1;
					break;
				}
					
			}
			//System.out.println("\"" + getDocumentProvider().getDocument(getInput()).get(offset-indent, length + indent) + "\"");
		} catch (BadLocationException e) {
		}
		return new DeleteEdit(offset - indent, length + indent);
		
	}

	private IDocumentNode getHighestNodeToBeWritten(IDocumentNode node) {
		IDocumentNode parent = node.getParentNode();
		if (parent == null)
			return node;
		if (parent.getOffset() > -1) {
			IDocument doc = getDocumentProvider().getDocument(getInput());
			try {
				String endChars = doc.get(parent.getOffset() + parent.getLength() - 2, 2);
				return ("/>".equals(endChars)) ? parent : node; //$NON-NLS-1$
			} catch (BadLocationException e) {
				return node;
			}
			
		}
		return getHighestNodeToBeWritten(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#flushModel(org.eclipse.jface.text.IDocument)
	 */
	protected void flushModel(IDocument doc) {
		removeUnnecessaryOperations();
		if (fOperationTable.size() == 1) {
			Object object = fOperationTable.keySet().iterator().next();
			if (object instanceof IDocumentNode && fEditOperations.get(0) instanceof InsertEdit) {
				if (((IDocumentNode)object).getParentNode() == null) {
					doc.set(((IDocumentNode)object).write(true));
					fOperationTable.clear();
					fEditOperations.clear();
					return;
				}
			} 
		}
		reorderInsertEdits(fEditOperations);
		fOperationTable.clear();
		fMoveOperations.clear();
		super.flushModel(doc);
	}
	
	protected abstract void reorderInsertEdits(ArrayList ops);

	protected void removeUnnecessaryOperations() {
		Iterator iter = fOperationTable.values().iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof IDocumentNode) {
				IDocumentNode node = (IDocumentNode)object;
				if (node.getOffset() > -1) {
					IDocumentAttribute[] attrs = node.getNodeAttributes();
					for (int i = 0; i < attrs.length; i++) {
						Object op = fOperationTable.remove(attrs[i]);
						if (op != null)
							fEditOperations.remove(op);
					}
					IDocumentTextNode textNode = node.getTextNode();
					if (textNode != null) {
						Object op = fOperationTable.remove(textNode);
						if (op != null)
							fEditOperations.remove(op);						
					}
				}
			}
		}		
	}
	
	
	public String getWritableString(String source) {
		return CoreUtility.getWritableString(source);
	}
	
	protected HashMap getOperationTable() {
		return fOperationTable;
	}

}
