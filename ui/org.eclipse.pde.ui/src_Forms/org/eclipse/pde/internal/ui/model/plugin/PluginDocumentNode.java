package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public abstract class PluginDocumentNode implements IDocumentNode {
	
	private IDocumentNode fParent;
	private ArrayList fChildren = new ArrayList();
	private boolean fIsErrorNode;
	private int fLength = -1;
	private int fOffset = -1;
	protected HashMap fAttributes = new HashMap();
	private String fTag;
	private int fIndent = 0;
	private IDocumentNode fPreviousSibling;
	protected IDocumentTextNode fTextNode;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#getChildNodes()
	 */
	public IDocumentNode[] getChildNodes() {
		return (IDocumentNode[]) fChildren.toArray(new IDocumentNode[fChildren.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#indexOf(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public int indexOf(IDocumentNode child) {
		return fChildren.indexOf(child);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getChildAt(int)
	 */
	public IDocumentNode getChildAt(int index) {
		if (index < fChildren.size())
			return (IDocumentNode)fChildren.get(index);
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#getParentNode()
	 */
	public IDocumentNode getParentNode() {
		return fParent;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#setParentNode(org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode)
	 */
	public void setParentNode(IDocumentNode node) {
		fParent = node;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#addChildNode(org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode)
	 */
	public void addChildNode(IDocumentNode child) {
		addChildNode(child, fChildren.size());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#addChildNode(org.eclipse.pde.internal.ui.model.IDocumentNode, int)
	 */
	public void addChildNode(IDocumentNode child, int position) {
		fChildren.add(position, child);
		if (position > 0 && fChildren.size() > 1)
			child.setPreviousSibling((IDocumentNode)fChildren.get(position - 1));
		if (fChildren.size() > 1 && position < fChildren.size() - 1)
			((IDocumentNode)fChildren.get(position + 1)).setPreviousSibling(child);
		child.setParentNode(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#removeChildNode(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public IDocumentNode removeChildNode(IDocumentNode child) {
		int index = fChildren.indexOf(child);
		if (index != -1) {
			fChildren.remove(child);
			if (index < fChildren.size()) {
				IDocumentNode prevSibling = index == 0 ? null : (IDocumentNode)fChildren.get(index - 1);
				((IDocumentNode)fChildren.get(index)).setPreviousSibling(prevSibling);
				return child;
			}
		}
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#isErrorNode()
	 */
	public boolean isErrorNode() {
		return fIsErrorNode;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#setIsErrorNode(boolean)
	 */
	public void setIsErrorNode(boolean isErrorNode) {
		fIsErrorNode = isErrorNode;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setAttribute(org.eclipse.pde.internal.ui.model.IDocumentAttribute)
	 */
	public void setXMLAttribute(IDocumentAttribute attribute) {
		fAttributes.put(attribute.getAttributeName(), attribute);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getXMLAttributeValue(java.lang.String)
	 */
	public String getXMLAttributeValue(String name) {
		PluginAttribute attr = (PluginAttribute)fAttributes.get(name);
		return attr == null ? null : attr.getValue();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setXMLTagName(java.lang.String)
	 */
	public void setXMLTagName(String tag) {
		fTag = tag;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getXMLTagName()
	 */
	public String getXMLTagName() {
		return fTag;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getDocumentAttribute(java.lang.String)
	 */
	public IDocumentAttribute getDocumentAttribute(String name) {
		return (IDocumentAttribute)fAttributes.get(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getLineIndent()
	 */
	public int getLineIndent() {
		return fIndent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setLineIndent(int)
	 */
	public void setLineIndent(int indent) {
		fIndent = indent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getAttributes()
	 */
	public IDocumentAttribute[] getNodeAttributes() {
		ArrayList list = new ArrayList();
		Iterator iter = fAttributes.values().iterator();
		while (iter.hasNext())
			list.add(iter.next());
		return (IDocumentAttribute[])list.toArray(new IDocumentAttribute[list.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getPreviousSibling()
	 */
	public IDocumentNode getPreviousSibling() {
		return fPreviousSibling;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setPreviousSibling(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setPreviousSibling(IDocumentNode sibling) {
		fPreviousSibling = sibling;
	}
	
	protected String getIndent() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fIndent; i++) {
			buffer.append(" ");
		}
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#swap(org.eclipse.pde.internal.ui.model.IDocumentNode, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void swap(IDocumentNode child1, IDocumentNode child2) {
		int index1 = fChildren.indexOf(child1);
		int index2 = fChildren.indexOf(child2);
		
		fChildren.set(index1, child2);
		fChildren.set(index2, child1);
		
		child1.setPreviousSibling(index2 == 0 ? null : (IDocumentNode)fChildren.get(index2 - 1));
		child2.setPreviousSibling(index1 == 0 ? null : (IDocumentNode)fChildren.get(index1 - 1));
		
		if (index1 < fChildren.size() - 1)
			((IDocumentNode)fChildren.get(index1 + 1)).setPreviousSibling(child2);
		
		if (index2 < fChildren.size() - 1)
			((IDocumentNode)fChildren.get(index2 + 1)).setPreviousSibling(child1);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#addTextNode(org.eclipse.pde.internal.ui.model.IDocumentTextNode)
	 */
	public void addTextNode(IDocumentTextNode textNode) {
		fTextNode = textNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getTextNode()
	 */
	public IDocumentTextNode getTextNode() {
		return fTextNode;
	}
	
}