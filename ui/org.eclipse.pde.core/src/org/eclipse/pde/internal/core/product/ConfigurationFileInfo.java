package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class ConfigurationFileInfo extends ProductObject implements
		IConfigurationFileInfo {

	private static final long serialVersionUID = 1L;
	
	private int fUse;

	private String fPath;

	public ConfigurationFileInfo(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setUse(int)
	 */
	public void setUse(int use) {
		fUse = use;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getUse()
	 */
	public int getUse() {
		return fUse;
	}
	
	private String getUsage() {
		switch (fUse) {
			case USE_WORKSPACE:
				return "workspace";
			case USE_FILESYSTEM:
				return "filesystem";
			default:
				return "default";
		}
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
			String usage = element.getAttribute("use");
			if ("workspace".equals(usage)) {
				fUse = USE_WORKSPACE;
			} else if ("filesystem".equals(usage)) {
				fUse = USE_FILESYSTEM;
			} else {
				fUse = USE_DEFAULT;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<configIni");
		writer.print(" use=\"" + getUsage() + "\"");
		if (fPath != null && getUse() != USE_DEFAULT)
			writer.print(" path=\"" + fPath.trim() + "\"");
		writer.println("/>");
	}

}
