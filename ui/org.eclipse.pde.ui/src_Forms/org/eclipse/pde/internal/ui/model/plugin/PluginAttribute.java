package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public class PluginAttribute extends PluginObjectNode
		implements
			IPluginAttribute , IDocumentAttribute{
	
	private IDocumentNode fEnclosingElement;
	private String fValue;
	private int fNameOffset;
	private int fNameLength;
	private int fValueOffset;
	private int fValueLength;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginAttribute#getValue()
	 */
	public String getValue() {
		return fValue;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginAttribute#setValue(java.lang.String)
	 */
	public void setValue(String value) throws CoreException {
		fValue = value;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setEnclosingElement(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentNode node) {
		fEnclosingElement = node;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getEnclosingElement()
	 */
	public IDocumentNode getEnclosingElement() {
		return fEnclosingElement;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setNameOffset(int)
	 */
	public void setNameOffset(int offset) {
		fNameOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getNameOffset()
	 */
	public int getNameOffset() {
		return fNameOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setNameLength(int)
	 */
	public void setNameLength(int length) {
		fNameLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getNameLength()
	 */
	public int getNameLength() {
		return fNameLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setValueOffset(int)
	 */
	public void setValueOffset(int offset) {
		fValueOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getValueOffset()
	 */
	public int getValueOffset() {
		return fValueOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setValueLength(int)
	 */
	public void setValueLength(int length) {
		fValueLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getValueLength()
	 */
	public int getValueLength() {
		return fValueLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getAttributeName()
	 */
	public String getAttributeName() {
		return getName();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getAttributeValue()
	 */
	public String getAttributeValue() {
		return getValue();
	}
}
