package org.eclipse.pde.internal.core.product;

import java.io.*;
import java.util.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class Product extends ProductObject implements IProduct {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fName;
	private String fApplication;
	private IAboutInfo fAboutInfo;
	
	private ArrayList fPlugins = new ArrayList();
	private IConfigurationFileInfo fConfigIniInfo;

	public Product(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getApplication()
	 */
	public String getApplication() {
		return fApplication;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setId(java.lang.String)
	 */
	public void setId(String id) {
		String old = fId;
		fId = id;
		if (isEditable())
			firePropertyChanged(P_ID, old, fId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setName(java.lang.String)
	 */
	public void setName(String name) {
		String old = fName;
		fName = name;
		if (isEditable())
			firePropertyChanged(P_NAME, old, fName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setAboutInfo(org.eclipse.pde.internal.core.iproduct.IAboutInfo)
	 */
	public void setAboutInfo(IAboutInfo info) {
		fAboutInfo = info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setApplication(java.lang.String)
	 */
	public void setApplication(String application) {
		String old = fApplication;
		fApplication = application;
		if (isEditable())
			firePropertyChanged(P_APPLICATION, old, fApplication);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<product"); //$NON-NLS-1$
		if (fName != null && fName.length() > 0)
			writer.print(" name=\"" + fName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fId != null && fId.length() > 0)
			writer.print(" id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fApplication != null && fApplication.length() > 0)
			writer.print(" application=\"" + fApplication + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(">"); //$NON-NLS-1$
		
		if (fAboutInfo != null) {
			writer.println();
			fAboutInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fConfigIniInfo != null) {
			writer.println();
			fConfigIniInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fPlugins.size() > 0) {
			writer.println();
			writer.println(indent + "   <plugins>"); //$NON-NLS-1$
			for (int i = 0; i < fPlugins.size(); i++) {
				IProductPlugin plugin = (IProductPlugin)fPlugins.get(i);
				plugin.write(indent + "      ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "   </plugins>"); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println("</product>"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getAboutInfo()
	 */
	public IAboutInfo getAboutInfo() {
		return fAboutInfo;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#reset()
	 */
	public void reset() {
		fAboutInfo = null;
		fApplication = null;
		fId = null;
		fName = null;
		fPlugins.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE 
				&& node.getNodeName().equals("product")) { //$NON-NLS-1$
			Element element = (Element)node;
			fApplication = element.getAttribute("application"); //$NON-NLS-1$
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fName = element.getAttribute("name"); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					if (child.getNodeName().equals("aboutInfo")) { //$NON-NLS-1$
						fAboutInfo = getModel().getFactory().createAboutInfo();
						fAboutInfo.parse(child);
					} else if (child.getNodeName().equals("plugins")) { //$NON-NLS-1$
						parsePlugins(child.getChildNodes());
					} else if (child.getNodeName().equals("configIni")) { //$NON-NLS-1$
						fConfigIniInfo = getModel().getFactory().createConfigFileInfo();
						fConfigIniInfo.parse(child);
					}
				}
			}
		}
	}
	
	private void parsePlugins(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("plugin")) { //$NON-NLS-1$
					IProductPlugin plugin = getModel().getFactory().createPlugin();
					plugin.parse(child);
					fPlugins.add(plugin);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#addPlugin(org.eclipse.pde.internal.core.iproduct.IProductPlugin)
	 */
	public void addPlugin(IProductPlugin plugin) {
		fPlugins.add(plugin);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#removePlugin(org.eclipse.pde.internal.core.iproduct.IProductPlugin)
	 */
	public void removePlugin(IProductPlugin plugin) {
		fPlugins.remove(plugin);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getPlugins()
	 */
	public IProductPlugin[] getPlugins() {
		return (IProductPlugin[])fPlugins.toArray(new IProductPlugin[fPlugins.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getConfigurationFileInfo()
	 */
	public IConfigurationFileInfo getConfigurationFileInfo() {
		return fConfigIniInfo;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#setConfigurationFileInfo(org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo)
	 */
	public void setConfigurationFileInfo(IConfigurationFileInfo info) {
		fConfigIniInfo = info;
	}

}
