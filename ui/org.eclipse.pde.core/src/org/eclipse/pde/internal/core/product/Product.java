package org.eclipse.pde.internal.core.product;

import java.io.*;
import java.util.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;


public class Product extends ProductObject implements IProduct {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fName;
	private String fApplication;
	private IAboutInfo fAboutInfo;
	
	private TreeMap fPlugins = new TreeMap();
	private IConfigurationFileInfo fConfigIniInfo;
	private boolean fUseFeatures;
	private String fExportDestination;
	private boolean fIncludeFragments;

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
			writer.print(" " + P_NAME + "=\"" + getWritableString(fName) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fId != null && fId.length() > 0)
			writer.print(" " + P_ID + "=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fApplication != null && fApplication.length() > 0)
			writer.print(" " + P_APPLICATION + "=\"" + fApplication + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(" " + P_USEFEATURES + "=\"" + Boolean.toString(fUseFeatures) + "\"");
		writer.println(">"); //$NON-NLS-1$

		if (fExportDestination != null && fExportDestination.length() > 0) {
			writer.println();
			writer.println(indent + "   <export " + P_DESTINATION + "=\"" + getWritableString(fExportDestination) + "\"/>");
		}
		
		if (fAboutInfo != null) {
			writer.println();
			fAboutInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fConfigIniInfo != null) {
			writer.println();
			fConfigIniInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println(indent + "   <plugins " + P_INCLUDE_FRAGMENTS + "=\"" + Boolean.toString(fIncludeFragments) + "\">"); //$NON-NLS-1$
		Iterator iter = fPlugins.values().iterator();
		while (iter.hasNext()) {
			IProductPlugin plugin = (IProductPlugin)iter.next();
			plugin.write(indent + "      ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "   </plugins>"); //$NON-NLS-1$
		
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
			fApplication = element.getAttribute(P_APPLICATION); //$NON-NLS-1$
			fId = element.getAttribute(P_ID); //$NON-NLS-1$
			fName = element.getAttribute(P_NAME); //$NON-NLS-1$
			fUseFeatures = "true".equals(element.getAttribute(P_USEFEATURES));
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("aboutInfo")) { //$NON-NLS-1$
						fAboutInfo = getModel().getFactory().createAboutInfo();
						fAboutInfo.parse(child);
					} else if (name.equals("plugins")) { //$NON-NLS-1$
						fIncludeFragments = "true".equals(((Element)child).getAttribute(P_INCLUDE_FRAGMENTS));
						parsePlugins(child.getChildNodes());
					} else if (name.equals("configIni")) { //$NON-NLS-1$
						fConfigIniInfo = getModel().getFactory().createConfigFileInfo();
						fConfigIniInfo.parse(child);
					} else if (name.equals("export")) {
						fExportDestination = ((Element)child).getAttribute(P_DESTINATION);
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
					fPlugins.put(plugin.getId(), plugin);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#addPlugin(org.eclipse.pde.internal.core.iproduct.IProductPlugin)
	 */
	public void addPlugin(IProductPlugin plugin) {
		String id = plugin.getId();
		if (fPlugins.containsKey(id))
			return;
		
		fPlugins.put(plugin.getId(), plugin);
		plugin.setInTheModel(true);
		if (isEditable())
			fireStructureChanged(plugin, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#removePlugin(org.eclipse.pde.internal.core.iproduct.IProductPlugin)
	 */
	public void removePlugin(IProductPlugin plugin) {
		fPlugins.remove(plugin.getId());
		plugin.setInTheModel(false);
		if (isEditable())
			fireStructureChanged(plugin, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#getPlugins()
	 */
	public IProductPlugin[] getPlugins() {
		return (IProductPlugin[])fPlugins.values().toArray(new IProductPlugin[fPlugins.size()]);
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

	public boolean useFeatures() {
		return fUseFeatures;
	}

	public void setUseFeatures(boolean use) {
		boolean old = fUseFeatures;
		fUseFeatures = use;
		if (isEditable())
			firePropertyChanged(P_USEFEATURES, Boolean.toString(old), Boolean.toString(fUseFeatures));
	}

	public String getExportDestination() {
		return fExportDestination;
	}

	public void setExportDestination(String destination) {
		String old = fExportDestination;
		fExportDestination = destination;
		if (isEditable())
			firePropertyChanged(P_DESTINATION, old, fExportDestination);
	}

	public boolean includeFragments() {
		return fIncludeFragments;
	}

	public void setIncludeFragments(boolean include) {
		boolean old = fIncludeFragments;
		fIncludeFragments = include;
		if (isEditable())
			firePropertyChanged(P_USEFEATURES, Boolean.toString(old), Boolean.toString(fIncludeFragments));
	}

	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}

}
