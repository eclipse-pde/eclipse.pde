/*
 * Created on Sep 3, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.standalone.parser;

import java.util.*;

import org.w3c.dom.*;

/**
 * @author melhem
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DocumentNode implements IDocumentNode {
	
	private IDocumentNode[] fChildren;
	private IDocumentNode fParentNode;
	private Node fNode;
	private Hashtable fLines;
	
	public DocumentNode(IDocumentNode[] children, Node domNode, Hashtable lines) {
		fChildren = children;
		fNode = domNode;
		fLines = lines;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.parser.IDocumentNode#getChildren()
	 */
	public IDocumentNode[] getChildren() {
		return fChildren;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.parser.IDocumentNode#getParent()
	 */
	public IDocumentNode getParent() {
		return fParentNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.parser.IDocumentNode#setParent(org.eclipse.pde.internal.ui.editor.parser.IDocumentNode)
	 */
	public void setParent(IDocumentNode parentNode) {
		fParentNode = parentNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.parser.IDocumentNode#getSourceRange()
	 */
	public ISourceRange getSourceRange() {
		return (ISourceRange)fLines.get(fNode);
	}
	
	public String getText() {
		return getTagName();
	}
	
	public String getTagName() {
		return fNode.getNodeName();		
	}
	
	public Node getContent() {
		return fNode;
	}

}
