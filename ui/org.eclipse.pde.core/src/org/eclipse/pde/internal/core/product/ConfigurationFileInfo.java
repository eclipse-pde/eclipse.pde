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
		fPath = path;
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
			fPath = element.getAttribute("path");
			fUse = element.getAttribute("use");
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<configIni");
		if (fUse != null)
			writer.print(" use=\"" + fUse + "\"");
		if (fPath != null)
			writer.print(" path=\"" + fPath.trim() + "\"");
		writer.println("/>");
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setUse(java.lang.String)
	 */
	public void setUse(String use) {
		fUse = use;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getUse()
	 */
	public String getUse() {
		return fUse;
	}

}
