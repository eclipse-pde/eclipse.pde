package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class ConfigurationFileInfo extends ProductObject implements
		IConfigurationFileInfo {

	private static final long serialVersionUID = 1L;
	
	private String fUse;

	private String fPath;

	public ConfigurationFileInfo(IProductModel model) {
		super(model);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setPath(java.lang.String)
	 */
	public void setPath(String path) {
		String old = fPath;
		fPath = path;
		if (isEditable())
			firePropertyChanged(P_PATH, old, fPath);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getPath()
	 */
	public String getPath() {
		return fPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			fPath = element.getAttribute("path"); //$NON-NLS-1$
			fUse = element.getAttribute("use"); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<configIni"); //$NON-NLS-1$
		if (fUse != null)
			writer.print(" " + P_USE + "=\"" + fUse + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fPath != null)
			writer.print(" " + P_PATH + "=\"" + getWritableString(fPath.trim()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setUse(java.lang.String)
	 */
	public void setUse(String use) {
		String old = fUse;
		fUse = use;
		if (isEditable())
			firePropertyChanged(P_USE, old, fUse);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getUse()
	 */
	public String getUse() {
		return fUse;
	}

}
