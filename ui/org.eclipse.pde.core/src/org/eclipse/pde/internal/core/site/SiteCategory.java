package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteCategory extends SiteObject implements ISiteCategory {
	private String name;
	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategory#getName()
	 */
	public String getName() {
		return name;
	}
	
	protected void parse(Node node) {
		name = getNodeAttribute(node, "name");
	}
	
	protected void reset() {
		name = null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategory#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<category");
		if (name!=null)
			writer.print(" name=\""+name+"\"");
		writer.println("/>");
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		}
		else super.restoreProperty(name, oldValue, newValue);
	}

}
