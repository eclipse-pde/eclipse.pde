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
package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.IRuntimeInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Target extends TargetObject implements ITarget {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fName;
	private TreeMap fPlugins = new TreeMap();
	private TreeMap fFeatures = new TreeMap();
	private IArgumentsInfo fArgsInfo;
	private IEnvironmentInfo fEnvInfo;
	private IRuntimeInfo fRuntimeInfo;
	private ILocationInfo fLocationInfo;
	private boolean fUseAllTargetPlatform = false;
	
	public Target(ITargetModel model) {
		super(model);
	}

	public void reset() {
		fArgsInfo = null;
		fEnvInfo = null;
		fRuntimeInfo = null;
		fLocationInfo = null;
		fPlugins.clear();
		fFeatures.clear();
		fUseAllTargetPlatform = false;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE 
				&& node.getNodeName().equals("target")) { //$NON-NLS-1$
			Element element = (Element)node; 
			fId = element.getAttribute(P_ID); 
			fName = element.getAttribute(P_NAME); 
			NodeList children = node.getChildNodes();
			ITargetModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("launcherArgs")) { //$NON-NLS-1$
						fArgsInfo = factory.createArguments();
						fArgsInfo.parse(child);
					} else if (name.equals("plugins")) { //$NON-NLS-1$
						element = (Element)child;
						fUseAllTargetPlatform =
							"true".equals(element.getAttribute("useAllPlugins"));
						parsePlugins(child.getChildNodes());
					} else if (name.equals("features")) { //$NON-NLS-1$
						parseFeatures(child.getChildNodes());
					} else if (name.equals("environment")) { //$NON-NLS-1$
						fEnvInfo = factory.createEnvironment();
						fEnvInfo.parse(child);
					} else if (name.equals("targetJRE")) { //$NON-NLS-1$
						fRuntimeInfo = factory.createJREInfo();
						fRuntimeInfo.parse(child);
					} else if (name.equals("location")) {
						fLocationInfo = factory.createLocation();
						fLocationInfo.parse(child);
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
					ITargetPlugin plugin = getModel().getFactory().createPlugin();
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
					ITargetFeature feature = getModel().getFactory().createFeature();
					feature.parse(child);
					fFeatures.put(feature.getId(), feature);
				}
			}
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<target"); //$NON-NLS-1$
		if (fId != null && fId.length() > 0)
			writer.print(" " + P_ID + "=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fName != null && fName.length() > 0)
			writer.print(" " + P_NAME + "=\"" + getWritableString(fName) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println(">");
		if (fArgsInfo != null) {
			fArgsInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fEnvInfo != null) {
			fEnvInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fRuntimeInfo != null) {
			fRuntimeInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fLocationInfo != null) {
			fLocationInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		if (fUseAllTargetPlatform) {
			writer.println(indent + "   <plugins useAllPlugins=\"true\">");
		} else {
			writer.println(indent + "   <plugins>");
		}
		Iterator iter = fPlugins.values().iterator();
		while (iter.hasNext()) {
			ITargetPlugin plugin = (ITargetPlugin) iter.next();
			plugin.write(indent + "      ", writer);
		}
		writer.println(indent + "   </plugins>");
		
		writer.println();
		writer.println(indent + "   <features>");
		iter = fFeatures.values().iterator();
		while (iter.hasNext()) {
			ITargetFeature feature = (ITargetFeature) iter.next();
			feature.write(indent + "      ", writer);
		}
		writer.println(indent + "   </features>");
			
		writer.println();
		writer.println(indent + "</target>"); //$NON-NLS-1$
	}
	
	public IArgumentsInfo getArguments() {
		return fArgsInfo;
	}
	
	public void setArguments(IArgumentsInfo info) {
		fArgsInfo = info;
	}

	public IEnvironmentInfo getEnvironment() {
		return fEnvInfo;
	}

	public void setEnvironment(IEnvironmentInfo info) {
		fEnvInfo = info;
	}

	public IRuntimeInfo getTargetJREInfo() {
		return fRuntimeInfo;
	}

	public void setTargetJREInfo(IRuntimeInfo info) {
		fRuntimeInfo = info;
		
	}

	public String getId() {
		return fId;
	}

	public void setId(String id) {
		String oldValue = fId;
		fId = id;
		firePropertyChanged(P_ID, oldValue, fId);
	}

	public String getName() {
		return fName;
	}

	public void setName(String name) {
		String oldValue = fName;
		fName = name;
		firePropertyChanged(P_NAME, oldValue, fName);
	}

	public ILocationInfo getLocationInfo() {
		return fLocationInfo;
	}

	public void setLocationInfo(ILocationInfo info) {
		fLocationInfo = info;
	}

	public void addPlugin(ITargetPlugin plugin) {
		fUseAllTargetPlatform = false;
		String id = plugin.getId();
		if (fPlugins.containsKey(id))
			return;
		plugin.setModel(getModel());
		fPlugins.put(id, plugin);
		if (isEditable())
			fireStructureChanged(plugin, IModelChangedEvent.INSERT);
	}

	public void addFeature(ITargetFeature feature) {
		fUseAllTargetPlatform = false;
		String id = feature.getId();
		if (fFeatures.containsKey(id))
			return;
		feature.setModel(getModel());
		fFeatures.put(id, feature);
		if (isEditable())
			fireStructureChanged(feature, IModelChangedEvent.INSERT);
	}

	public void removePlugin(ITargetPlugin plugin) {
		fUseAllTargetPlatform = false;
		fPlugins.remove(plugin.getId());
		if (isEditable())
			fireStructureChanged(plugin, IModelChangedEvent.REMOVE);
	}

	public void removeFeature(ITargetFeature feature) {
		fUseAllTargetPlatform = false;
		fFeatures.remove(feature.getId());
		if (isEditable())
			fireStructureChanged(feature, IModelChangedEvent.REMOVE);
	}

	public ITargetPlugin[] getPlugins() {
		return (ITargetPlugin[]) fPlugins.values().toArray(new ITargetPlugin[fPlugins.size()]);
	}

	public ITargetFeature[] getFeatures() {
		return (ITargetFeature[]) fFeatures.values().toArray(new ITargetFeature[fFeatures.size()]);
	}

	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}

	public boolean containsFeature(String id) {
		return fFeatures.containsKey(id);
	}

	public boolean useAllPlugins() {
		return fUseAllTargetPlatform;
	}

	public void setUseAllPlugins(boolean value) {
		boolean oldValue = fUseAllTargetPlatform;
		fUseAllTargetPlatform = value;
		if (isEditable())
			firePropertyChanged(P_ALL_PLUGINS, new Boolean(oldValue), new Boolean(fUseAllTargetPlatform));
	}
}
