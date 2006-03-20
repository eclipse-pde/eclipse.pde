package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.AbstractTextChangeListener;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class XMLTextChangeListener extends AbstractTextChangeListener {
	
	public XMLTextChangeListener(IDocument document) {
		super(document);
	}

	public TextEdit[] getTextOperations() {
		if (fOperationTable.size() == 0)
			return new TextEdit[0];
		
		return (TextEdit[])fOperationTable.values().toArray(new TextEdit[fOperationTable.size()]);
	}
	
	protected void deleteNode(IDocumentNode node) {
		// delete previous op on this node, if any
		TextEdit old = (TextEdit)fOperationTable.get(node);
		if (old != null)
			fOperationTable.remove(node);
		
		// if node has an offset, delete it
		if (node.getOffset() > -1) {
			// Create a delete op for this node
			TextEdit op = getDeleteNodeOperation(node);
			fOperationTable.put(node, op);			
		} else if (old == null){
			// No previous op on this non-offset node, just rewrite highest ancestor with an offset
			insertNode(node);
		}
	}

	protected void insertNode(IDocumentNode node) {
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
		fOperationTable.put(node, op);				
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
		int indent = 0;
		try {
			int line = fDocument.getLineOfOffset(offset + length);
			for (;;) {
				char ch = fDocument.get(offset + length, 1).toCharArray()[0];
				if (fDocument.getLineOfOffset(offset + length) > line || !Character.isWhitespace(ch)) {
					length -= 1;
					break;
				}
				length += 1;
			}
			
			for (indent = 1; indent <= node.getLineIndent(); indent++) {
				char ch = fDocument.get(offset - indent, 1).toCharArray()[0];
				if (!Character.isWhitespace(ch)) {
					indent -= 1;
					break;
				}
					
			}
		} catch (BadLocationException e) {
		}
		return new DeleteEdit(offset - indent, length + indent);
		
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
		return CoreUtility.getWritableString(source);
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects == null)
			return;
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof IDocumentNode))
				continue;
			IDocumentNode node = (IDocumentNode)objects[i];
			fOperationTable.remove(node);
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
					break;
			}
		}
	}
}
