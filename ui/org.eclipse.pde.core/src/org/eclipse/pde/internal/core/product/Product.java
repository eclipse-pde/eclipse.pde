/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
	private TreeMap fFeatures = new TreeMap();
	private IConfigurationFileInfo fConfigIniInfo;
	private boolean fUseFeatures;
	private IWindowImages fWindowImages;
	private ISplashInfo fSplashInfo;
	private ILauncherInfo fLauncherInfo;
	private IArgumentsInfo fLauncherArgs;

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
			writer.print(" " + P_NAME + "=\"" + getWritableString(fName) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fId != null && fId.length() > 0)
			writer.print(" " + P_ID + "=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fApplication != null && fApplication.length() > 0)
			writer.print(" " + P_APPLICATION + "=\"" + fApplication + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.print(" " + P_USEFEATURES + "=\"" + Boolean.toString(fUseFeatures) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println(">"); //$NON-NLS-1$

		if (fAboutInfo != null) {
			writer.println();
			fAboutInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fConfigIniInfo != null) {
			writer.println();
			fConfigIniInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fLauncherArgs != null) {
			writer.println();
			fLauncherArgs.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fWindowImages != null) {
			writer.println();
			fWindowImages.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fSplashInfo != null) {
			writer.println();
			fSplashInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		if (fLauncherInfo != null) {
			writer.println();
			fLauncherInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println(indent + "   <plugins>"); //$NON-NLS-1$  
		Iterator iter = fPlugins.values().iterator();
		while (iter.hasNext()) {
			IProductPlugin plugin = (IProductPlugin)iter.next();
			plugin.write(indent + "      ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "   </plugins>"); //$NON-NLS-1$
		
		if (fFeatures.size() > 0) {
			writer.println();
			writer.println(indent + "   <features>"); //$NON-NLS-1$
			iter = fFeatures.values().iterator();
			while (iter.hasNext()) {
				IProductFeature feature = (IProductFeature)iter.next();
				feature.write(indent + "      ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "   </features>"); //$NON-NLS-1$
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
			fApplication = element.getAttribute(P_APPLICATION); 
			fId = element.getAttribute(P_ID); 
			fName = element.getAttribute(P_NAME); 
			fUseFeatures = "true".equals(element.getAttribute(P_USEFEATURES)); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			IProductModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("aboutInfo")) { //$NON-NLS-1$
						fAboutInfo = factory.createAboutInfo();
						fAboutInfo.parse(child);
					} else if (name.equals("plugins")) { //$NON-NLS-1$
						parsePlugins(child.getChildNodes());
					} else if (name.equals("features")) { //$NON-NLS-1$
						parseFeatures(child.getChildNodes());
					} else if (name.equals("configIni")) { //$NON-NLS-1$
						fConfigIniInfo = factory.createConfigFileInfo();
						fConfigIniInfo.parse(child);
					} else if (name.equals("windowImages")) { //$NON-NLS-1$
						fWindowImages = factory.createWindowImages();
						fWindowImages.parse(child);
					} else if (name.equals("splash")) { //$NON-NLS-1$
						fSplashInfo = factory.createSplashInfo();
						fSplashInfo.parse(child);
					} else if (name.equals("launcher")) { //$NON-NLS-1$
						fLauncherInfo = factory.createLauncherInfo();
						fLauncherInfo.parse(child);
					} else if (name.equals("launcherArgs")) { //$NON-NLS-1$
						fLauncherArgs = factory.createLauncherArguments();
						fLauncherArgs.parse(child);
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
	
	private void parseFeatures(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("feature")) { //$NON-NLS-1$
					IProductFeature feature = getModel().getFactory().createFeature();
					feature.parse(child);
					fFeatures.put(feature.getId(), feature);
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
		
		plugin.setModel(getModel());
		fPlugins.put(id, plugin);
		if (isEditable())
			fireStructureChanged(plugin, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProduct#removePlugin(org.eclipse.pde.internal.core.iproduct.IProductPlugin)
	 */
	public void removePlugin(IProductPlugin plugin) {
		fPlugins.remove(plugin.getId());
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

	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}
	
	public boolean containsFeature(String id) {
		return fFeatures.containsKey(id);
	}

	public IWindowImages getWindowImages() {
		return fWindowImages;
	}

	public void setWindowImages(IWindowImages images) {
		fWindowImages = images;
	}

	public ISplashInfo getSplashInfo() {
		return fSplashInfo;
	}

	public void setSplashInfo(ISplashInfo info) {
		fSplashInfo = info;
	}

	public ILauncherInfo getLauncherInfo() {
		return fLauncherInfo;
	}

	public void setLauncherInfo(ILauncherInfo info) {
		fLauncherInfo = info;
	}

	public void addFeature(IProductFeature feature) {
		String id = feature.getId();
		if (fFeatures.containsKey(id))
			return;
		
		feature.setModel(getModel());
		fFeatures.put(id, feature);
		if (isEditable())
			fireStructureChanged(feature, IModelChangedEvent.INSERT);
	}

	public void removeFeature(IProductFeature feature) {
		fFeatures.remove(feature.getId());
		if (isEditable())
			fireStructureChanged(feature, IModelChangedEvent.REMOVE);
	}

	public IProductFeature[] getFeatures() {
		return (IProductFeature[])fFeatures.values().toArray(new IProductFeature[fFeatures.size()]);
	}

	public IArgumentsInfo getLauncherArguments() {
		return fLauncherArgs;
	}

	public void setLauncherArguments(IArgumentsInfo info) {
		fLauncherArgs = info;
	}

}
