package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public class PluginLibraryNode extends PluginObjectNode
		implements
			IPluginLibrary {
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#getContentFilters()
	 */
	public String[] getContentFilters() {
		IDocumentNode[] children = getChildNodes();
		ArrayList result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode)children[i];
			if (node.getName().equals(P_EXPORTED)) {
				String name = children[i].getXMLAttributeValue(P_NAME);
				if (name != null && !name.equals("*"))
					result.add(name);
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#getPackages()
	 */
	public String[] getPackages() {
		return new String[0];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#isExported()
	 */
	public boolean isExported() {
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode)children[i];
			if (node.getName().equals(P_EXPORTED))
				return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#isFullyExported()
	 */
	public boolean isFullyExported() {
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode)children[i];
			if (node.getName().equals(P_EXPORTED)) {
				String name = children[i].getXMLAttributeValue(P_NAME);
				if (name != null && name.equals("*"))
					return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#getType()
	 */
	public String getType() {
		String type = getXMLAttributeValue(P_TYPE);
		return (type != null && type.equals("resource")) ? IPluginLibrary.RESOURCE : IPluginLibrary.CODE;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setContentFilters(java.lang.String[])
	 */
	public void setContentFilters(String[] filters) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#addContentFilter(java.lang.String)
	 */
	public void addContentFilter(String filter) throws CoreException {
		PluginElementNode node = new PluginElementNode();
		node.setXMLTagName(P_EXPORTED);
		node.setParentNode(this);
		node.setModel(getModel());
		node.setXMLAttribute(P_NAME, filter);
		addChildNode(node);
		if (isInTheModel())
			fireStructureChanged((IPluginElement)node, IModelChangedEvent.INSERT);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setPackages(java.lang.String[])
	 */
	public void setPackages(String[] packages) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setExported(boolean)
	 */
	public void setExported(boolean exported) throws CoreException {
		IDocumentNode[] children = getChildNodes();
		boolean alreadyExported = false;
		for (int i = 0; i < children.length; i++) {
			if (children[i].getXMLTagName().equals(P_EXPORTED)) {
				if (!"*".equals(children[i].getXMLAttributeValue(P_NAME))) {
					removeChildNode(children[i]);
					if (isInTheModel())
						fireStructureChanged((IPluginElement)children[i], IModelChangedEvent.REMOVE);
				} else {
					alreadyExported = true;
					if (!exported) {
						removeChildNode(children[i]);
						if (isInTheModel())
							fireStructureChanged((IPluginElement)children[i], IModelChangedEvent.REMOVE);
					}
				}
			}
		}
		if (exported && !alreadyExported) {
			addContentFilter("*");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setType(java.lang.String)
	 */
	public void setType(String type) throws CoreException {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String sep = System.getProperty("line.separator");
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());
		
		IDocumentNode[] children = getChildNodes();
		if (children.length > 0) {
			buffer.append(writeShallow(false) + sep);		
			for (int i = 0; i < children.length; i++) {
				children[i].setLineIndent(getLineIndent() + 3);
				buffer.append(children[i].write(true) + sep);
			}
			buffer.append(getIndent() + "</" + getXMLTagName() + ">");
		} else {
			buffer.append(writeShallow(true));
		}
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		StringBuffer buffer = new StringBuffer("<" + getXMLTagName());

		IDocumentAttribute[] attrs = getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			buffer.append(" " + attrs[i].write());
		}
		if (terminate)
			buffer.append("/");
		buffer.append(">");
		return buffer.toString();
	}

}
