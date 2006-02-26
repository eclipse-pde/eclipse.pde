/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PDEExtensionRegistry {
	
	protected static boolean DEBUG = false;

	static {
		DEBUG = PDECore.getDefault().isDebugging()
				&& "true".equals(Platform.getDebugOption("org.eclipse.pde.core/cache")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private static String CACHE_EXTENSION = ".extensions";
	
	private static String ROOT_EXTENSIONS = "extensions";
	private static String ELEMENT_BUNDLE = "bundle";
	private static String ATTR_BUNDLE_ID = "bundleID";
	private static String ELEMENT_EXTENSION = "extension";
	private static String ELEMENT_EXTENSION_POINT = "extension-point";
	
	private Map fExtensions = new HashMap();

	protected void saveExtensions(State state, File dir) {
		try {
			File file = new File(dir, CACHE_EXTENSION); //$NON-NLS-1$
			XMLPrintHandler.writeFile(createExtensionDocument(state), file);
		} catch (IOException e) {
		}
	}
	
	public Node[] getExtensions(long bundleID) {
		return getChildren(bundleID, ELEMENT_EXTENSION); //$NON-NLS-1$
	}
	
	public Node[] getExtensionPoints(long bundleID) {
		return getChildren(bundleID, ELEMENT_EXTENSION_POINT); //$NON-NLS-1$
	}
	
	private Node[] getChildren(long bundleID, String tagName) {
		ArrayList list = new ArrayList();
		Element bundle = (Element)fExtensions.get(Long.toString(bundleID));
		if (bundle != null) {
			NodeList children = bundle.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (tagName.equals(children.item(i).getNodeName())) {
					list.add(children.item(i));
				}
			}
		}
		return (Node[])list.toArray(new Node[list.size()]);
	}

	public Node[] getAllExtensions(long bundleID) {
		ArrayList list = new ArrayList();
		Element bundle = (Element)fExtensions.get(Long.toString(bundleID));
		if (bundle != null) {
			NodeList children = bundle.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				String name = children.item(i).getNodeName();
				if (ELEMENT_EXTENSION.equals(name) || ELEMENT_EXTENSION_POINT.equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
					list.add(children.item(i));
				}
			}
		}
		return (Node[])list.toArray(new Node[list.size()]);
	}
	
	private Document createExtensionDocument(State state){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			doc = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			return null;
		}
		Element root = doc.createElement(ROOT_EXTENSIONS); //$NON-NLS-1$

		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription desc = bundles[i];
			Element element = doc.createElement(ELEMENT_BUNDLE); //$NON-NLS-1$
			element.setAttribute(ATTR_BUNDLE_ID, Long.toString(desc.getBundleId())); //$NON-NLS-1$
			PDEStateHelper.parseExtensions(desc, element);
			if (element.hasChildNodes()) {
				root.appendChild(element);
				fExtensions.put(Long.toString(desc.getBundleId()), element);
			}
		}
		doc.appendChild(root);
		return doc;
	}

	protected boolean readExtensionsCache(File dir) {
		long start = System.currentTimeMillis();
		File file = new File(dir, CACHE_EXTENSION); //$NON-NLS-1$
		if (file.exists() && file.isFile()) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList bundles = root.getChildNodes();
					for (int i = 0; i < bundles.getLength(); i++) {
						if (bundles.item(i).getNodeType() == Node.ELEMENT_NODE) {
							Element bundle = (Element)bundles.item(i); 
							String id = bundle.getAttribute(ATTR_BUNDLE_ID); //$NON-NLS-1$
							fExtensions.put(id, bundle.getChildNodes());
						}
					}
				}
				if (DEBUG)
					System.out.println("Time to read extensions: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		}
		return false;
	}
	
	public static void writeExtensions(IPluginModelBase[] models, File destination) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
		
			Element root = doc.createElement(ROOT_EXTENSIONS); //$NON-NLS-1$
			doc.appendChild(root);
		
			for (int i = 0; i < models.length; i++) {
				IPluginBase plugin = models[i].getPluginBase();
				IPluginExtension[] extensions = plugin.getExtensions();
				IPluginExtensionPoint[] extPoints = plugin.getExtensionPoints();
				if (extensions.length == 0 && extPoints.length == 0)
					continue;
				Element element = doc.createElement(ELEMENT_BUNDLE); //$NON-NLS-1$
				element.setAttribute(ATTR_BUNDLE_ID, Long.toString(models[i].getBundleDescription().getBundleId())); //$NON-NLS-1$
				for (int j = 0; j < extensions.length; j++) {
					element.appendChild(writeExtension(doc, extensions[j]));
				}				
				for (int j = 0; j < extPoints.length; j++) {
					element.appendChild(writeExtensionPoint(doc, extPoints[j]));
				}			
				root.appendChild(element);
			}
			XMLPrintHandler.writeFile(doc, new File(destination, CACHE_EXTENSION)); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (IOException e) {
		}	
	}

	public static Element writeExtensionPoint(Document doc, IPluginExtensionPoint extPoint) {
		Element child = doc.createElement("extension-point"); //$NON-NLS-1$
		if (extPoint.getId() != null)
			child.setAttribute("id", CoreUtility.getWritableString(extPoint.getId())); //$NON-NLS-1$
		if (extPoint.getName() != null)
			child.setAttribute("name", CoreUtility.getWritableString(extPoint.getName())); //$NON-NLS-1$
		if (extPoint.getSchema() != null)
			child.setAttribute("schema", CoreUtility.getWritableString(extPoint.getSchema())); //$NON-NLS-1$
		if (extPoint instanceof PluginExtensionPoint)
			child.setAttribute("line", Integer.toString(((PluginExtensionPoint)extPoint).getStartLine())); //$NON-NLS-1$
		return child;	
	}
	
	public static Element writeExtension(Document doc, IPluginExtension extension) {
		Element child = doc.createElement("extension"); //$NON-NLS-1$
		if (extension.getPoint() != null)
			child.setAttribute("point", CoreUtility.getWritableString(extension.getPoint())); //$NON-NLS-1$
		if (extension.getName() != null)
			child.setAttribute("name", CoreUtility.getWritableString(extension.getName())); //$NON-NLS-1$
		if (extension.getId() != null)
			child.setAttribute("id", CoreUtility.getWritableString(extension.getId())); //$NON-NLS-1$
		if (extension instanceof PluginExtension)
			child.setAttribute("line", Integer.toString(((PluginExtension)extension).getStartLine())); //$NON-NLS-1$
		IPluginObject[] children = extension.getChildren();
		for (int i = 0; i < children.length; i++) {
			child.appendChild(writeElement(doc, (IPluginElement)children[i]));
		}
		return child;	
	}

	public static Element writeElement(Document doc, IPluginElement element) {
		Element child = doc.createElement(element.getName());
		IPluginAttribute[] attrs = element.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			child.setAttribute(attrs[i].getName(), CoreUtility.getWritableString(attrs[i].getValue()));
		}
		IPluginObject[] elements = element.getChildren();
		for (int i = 0; i < elements.length; i++) {
			child.appendChild(writeElement(doc, (IPluginElement)elements[i]));
		}
		return child;
	}
	
	protected void clear() {
		fExtensions.clear();
	}

}
