package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
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
			if (node.getName().equals("export")) {
				String name = children[i].getXMLAttributeValue("name");
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
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode)children[i];
			if (node.getName().equals("packages")) {
				String name = children[i].getXMLAttributeValue("prefixes");
				if (name != null && name.length() > 0) {
					StringTokenizer tok = new StringTokenizer(name, ",");
					int number = tok.countTokens();
					String[] result = new String[number];
					for (int j = 0; j < number; j++)
						result[j] = tok.nextToken();
					return result;
				}
			}
		}
		return new String[0];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#isExported()
	 */
	public boolean isExported() {
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode)children[i];
			if (node.getName().equals("export"))
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
			if (node.getName().equals("export")) {
				String name = children[i].getXMLAttributeValue("name");
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
		String type = getXMLAttributeValue("type");
		return (type != null && type.equals("resource")) ? IPluginLibrary.RESOURCE : IPluginLibrary.CODE;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setContentFilters(java.lang.String[])
	 */
	public void setContentFilters(String[] filters) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setPackages(java.lang.String[])
	 */
	public void setPackages(String[] packages) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setExported(boolean)
	 */
	public void setExported(boolean value) throws CoreException {
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
		return getXMLAttributeValue("name");
	}
}
