package org.eclipse.pde.internal.ui.neweditor.context;
import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.text.edits.*;
import org.eclipse.ui.IEditorInput;

public abstract class XMLInputContext extends UTF8InputContext {
	private HashMap fOperationTable = new HashMap();

	/**
	 * @param editor
	 * @param input
	 */
	public XMLInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
	}

	protected IDocumentPartitioner createDocumentPartitioner() {
		DefaultPartitioner partitioner = new DefaultPartitioner(
				new XMLPartitionScanner(), new String[]{
						XMLPartitionScanner.XML_TAG,
						XMLPartitionScanner.XML_COMMENT});
		return partitioner;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects != null ) {
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				switch (event.getChangeType()) {
					case IModelChangedEvent.REMOVE:
						if (object instanceof IDocumentNode)
							removeNode((IDocumentNode)object, ops);
						break;
					case IModelChangedEvent.INSERT:
						if (object instanceof IDocumentNode)
							insertNode((IDocumentNode)object, ops);
						break;
					case IModelChangedEvent.CHANGE:
						if (object instanceof IDocumentAttribute)
							addAttributeOperation((IDocumentAttribute)object, ops, event);
						else if (object instanceof IDocumentNode)
							modifyNode((IDocumentNode)object, ops, event);
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
		// if node has an offset, delete it
		if (node.getOffset() > -1) {
			// Create a delete op for this node
			IRegion region = getNodeRegion(node);
			TextEdit op = new DeleteEdit(region.getOffset(), region.getLength());
			ops.add(op);
			fOperationTable.put(node, op);			
		} else if (old == null){
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
				return new InsertEdit(sibling.getOffset() + sibling.getLength(), System.getProperty("line.separator") + node.write(true));
			}
			sibling = sibling.getPreviousSibling();
		}
		return null;
	}
	
	private InsertEdit insertAsFirstChild(IDocumentNode node) {
		int offset = node.getParentNode().getOffset();
		int length = getNextPosition(getDocumentProvider().getDocument(getInput()), offset, '>');
		node.setLineIndent(node.getParentNode().getLineIndent() + 3);
		return new InsertEdit(offset+ length + 1, System.getProperty("line.separator") + node.write(true));	
	}
	

	private void modifyNode(IDocumentNode node, ArrayList ops, IModelChangedEvent event) {
		IDocumentNode node1 = (IDocumentNode)event.getOldValue();
		IDocumentNode node2 = (IDocumentNode)event.getNewValue();
		
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
			MoveSourceEdit source = new MoveSourceEdit(node1.getOffset(), node1.getLength());
			MoveTargetEdit target = new MoveTargetEdit(node2.getOffset(), source);
			ops.add(0, source);
			fOperationTable.put(node1, source);
			ops.add(0, target);
			fOperationTable.put(node2, source);			
		} else {
			// one node with offset, the other without offset.  Delete/reinsert the one without offset
			insertNode((node1.getOffset() < 0) ? node1 : node2, ops);
		}		
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
	

	private IRegion getNodeRegion(IDocumentNode node) {
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
		return new Region(offset - indent, length + indent);
		
	}
/**
	 * @param node
	 * @return
	 */
	private IDocumentNode getHighestNodeToBeWritten(IDocumentNode node) {
		IDocumentNode parent = node.getParentNode();
		if (parent == null)
			return node;
		if (parent.getOffset() > -1) {
			IDocument doc = getDocumentProvider().getDocument(getInput());
			try {
				String endChars = doc.get(parent.getOffset() + parent.getLength() - 2, 2);
				return ("/>".equals(endChars)) ? parent : node;
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
				}
			}
		}		
	}
	
	
	public String getWritableString(String source) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}
	
	protected HashMap getOperationTable() {
		return fOperationTable;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getMoveOperations()
	 */
	protected TextEdit[] getMoveOperations() {
		return new TextEdit[0];
	}


}