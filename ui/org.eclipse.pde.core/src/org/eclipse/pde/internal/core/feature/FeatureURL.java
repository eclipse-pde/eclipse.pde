/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class FeatureURL extends FeatureObject implements IFeatureURL {
	private Vector updates = new Vector();
	private Vector discoveries = new Vector();

	public void addDiscovery(IFeatureURLElement discovery) throws CoreException {
		ensureModelEditable();
		discoveries.add(discovery);
		((FeatureURLElement)discovery).setInTheModel(true);
		fireStructureChanged(discovery, IModelChangedEvent.INSERT);
	}
	public void addUpdate(IFeatureURLElement update) throws CoreException {
		ensureModelEditable();
		updates.add(update);
		((FeatureURLElement)update).setInTheModel(true);
		fireStructureChanged(update, IModelChangedEvent.INSERT);
	}
	public IFeatureURLElement[] getDiscoveries() {
		IFeatureURLElement[] result = new IFeatureURLElement[discoveries.size()];
		discoveries.copyInto(result);
		return result;
	}
	public IFeatureURLElement[] getUpdates() {
		IFeatureURLElement[] result = new IFeatureURLElement[updates.size()];
		updates.copyInto(result);
		return result;
	}
	protected void parse(Node node, Hashtable lineTable) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase();
				int urlType = -1;
				if (tag.equals("update")) {
					urlType = IFeatureURLElement.UPDATE;
				} else if (tag.equals("discovery")) {
					urlType = IFeatureURLElement.DISCOVERY;
				}
				if (urlType != -1) {
					IFeatureURLElement element =
						getModel().getFactory().createURLElement(this, urlType);
					((FeatureURLElement) element).parse(child, lineTable);
					if (urlType == IFeatureURLElement.UPDATE) {
						((FeatureURLElement)element).setInTheModel(true);
						updates.add(element);
					}
					else if (urlType == IFeatureURLElement.DISCOVERY) {
						((FeatureURLElement)element).setInTheModel(true);			
						discoveries.add(element);
					}
				}
			}
		}
	}
	public void removeDiscovery(IFeatureURLElement discovery)
		throws CoreException {
		ensureModelEditable();
		discoveries.remove(discovery);
		((FeatureURLElement)discovery).setInTheModel(false);
		fireStructureChanged(discovery, IModelChangedEvent.REMOVE);
	}
	public void removeUpdate(IFeatureURLElement update) throws CoreException {
		ensureModelEditable();
		((FeatureURLElement)update).setInTheModel(false);
		updates.remove(update);
		fireStructureChanged(update, IModelChangedEvent.REMOVE);
	}
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<url>");
		String indent2 = indent + Feature.INDENT;
		for (int i = 0; i < updates.size(); i++) {
			IFeatureURLElement element = (IFeatureURLElement) updates.elementAt(i);
			element.write(indent2, writer);
		}
		for (int i = 0; i < discoveries.size(); i++) {
			IFeatureURLElement element = (IFeatureURLElement) discoveries.elementAt(i);
			element.write(indent2, writer);
		}
		writer.println(indent + "</url>");
	}
}
