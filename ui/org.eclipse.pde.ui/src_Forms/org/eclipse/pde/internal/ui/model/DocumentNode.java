package org.eclipse.pde.internal.ui.model;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.model.plugin.*;

/**
 * @author melhem
 *
 */
public class DocumentNode implements IDocumentNode {
	
	private IDocumentNode fParent;
	private ArrayList fChildren = new ArrayList();
	private boolean fIsErrorNode;
	private int fLength;
	private int fOffset;
	protected HashMap fAttributes = new HashMap();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#getChildNodes()
	 */
	public IDocumentNode[] getChildNodes() {
		return (IDocumentNode[]) fChildren.toArray(new IDocumentNode[fChildren.size()]);
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
		fChildren.add(child);
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
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setXMLAttribute(java.lang.String, java.lang.String)
	 */
	public void setXMLAttribute(String name, String value) {
		try {
			PluginAttribute attr = (PluginAttribute)fAttributes.get(name);
			if (attr == null) {
				attr = new PluginAttribute();
				attr.setName(name);
				attr.setEnclosingElement(this);
				fAttributes.put(name, attr);
			}
			attr.setValue(value);
		} catch (CoreException e) {
		}	
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getXMLAttributeValue(java.lang.String)
	 */
	public String getXMLAttributeValue(String name) {
		PluginAttribute attr = (PluginAttribute)fAttributes.get(name);
		return attr == null ? null : attr.getValue();
	}
}