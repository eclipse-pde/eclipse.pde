package org.eclipse.pde.internal.ui.model;

/**
 * @author melhem
 *
 */
public class DocumentTextNode implements IDocumentTextNode {
	private int fOffset = -1;
	private int fLength = 0;
	private IDocumentNode fEnclosingElement;
	private String fText;
	private int fFullLength = 0;
	private int fTopOffset = -1;
	/**
	 * 
	 */
	public DocumentTextNode() {
		super();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setEnclosingElement(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentNode node) {
		fEnclosingElement = node;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getEnclosingElement()
	 */
	public IDocumentNode getEnclosingElement() {
		return fEnclosingElement;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setText(java.lang.String)
	 */
	public void setText(String text) {
		fText = text;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getText()
	 */
	public String getText() {
		return fText == null ? "" : fText;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getFullLength()
	 */
	public int getFullLength() {
		return fFullLength;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setFullLength(int)
	 */
	public void setFullLength(int length) {
		fFullLength = length;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getTopOffset()
	 */
	public int getTopOffset() {
		return fTopOffset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setTopOffset(int)
	 */
	public void setTopOffset(int offset) {
		fTopOffset = offset;
	}
}
