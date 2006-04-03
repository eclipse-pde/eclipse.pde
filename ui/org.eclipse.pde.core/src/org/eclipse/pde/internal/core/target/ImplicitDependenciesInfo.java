/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImplicitDependenciesInfo extends TargetObject implements
		IImplicitDependenciesInfo {
	
	private static final long serialVersionUID = 1L;
	
	Map fPlugins = new HashMap();

	public ImplicitDependenciesInfo(ITargetModel model) {
		super(model);
	}

	public ITargetPlugin[] getPlugins() {
		return (ITargetPlugin[]) fPlugins.values().toArray(new ITargetPlugin[fPlugins.size()]);
	}

	public void addPlugin(ITargetPlugin plugin) {
		addPlugins(new ITargetPlugin[] {plugin});
	}

	public void addPlugins(ITargetPlugin[] plugins) {
		ArrayList addedPlugins = new ArrayList();
		for (int i = 0; i < plugins.length; i ++ ) {
			String id = plugins[i].getId();
			if (fPlugins.containsKey(id))
				continue;
			plugins[i].setModel(getModel());
			fPlugins.put(id, plugins[i]);
			addedPlugins.add(plugins[i]);
		}
		if (isEditable() && (addedPlugins.size() > 0)) {
			firePropertyChanged(P_IMPLICIT_PLUGINS, new ITargetPlugin[0], 
					(ITargetPlugin[])addedPlugins.toArray(new ITargetPlugin[addedPlugins.size()]));
		}

	}

	public void removePlugin(ITargetPlugin plugin) {
		removePlugins(new ITargetPlugin[] {plugin});
	}

	public void removePlugins(ITargetPlugin[] plugins) {
		ArrayList removedPlugins = new ArrayList();
		for (int i =0; i < plugins.length; i++) 
			if (fPlugins.remove(plugins[i].getId()) != null)
				removedPlugins.add(plugins[i]);
		if (isEditable() && (removedPlugins.size() > 0))
			firePropertyChanged(P_IMPLICIT_PLUGINS, (ITargetPlugin[])removedPlugins.toArray(new ITargetPlugin[removedPlugins.size()]), 
					new ITargetPlugin[0]);
	}

	public void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals("plugin")) { //$NON-NLS-1$
				ITargetPlugin plugin = getModel().getFactory().createPlugin();
				plugin.parse(child);
				fPlugins.put(plugin.getId(), plugin);
			}
		}

	}

	public void write(String indent, PrintWriter writer) {
		if (fPlugins.size() == 0)
			return;
		writer.println();
		writer.println(indent + "<implicitDependencies>"); //$NON-NLS-1$
		Iterator it = fPlugins.values().iterator();
		while (it.hasNext()) {
			TargetPlugin plugin = (TargetPlugin) it.next();
			plugin.write(indent + "   ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "</implicitDependencies>"); //$NON-NLS-1$
	}

	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}

}
