package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.model.ifeature.*;
import org.eclipse.pde.core.*;

import java.util.*;
import org.eclipse.pde.internal.base.model.*;

public class FeatureURL extends FeatureObject implements IFeatureURL {
	private Vector updates=new Vector();
	private Vector discoveries = new Vector();

public void addDiscovery(IFeatureURLElement discovery) throws CoreException {
	ensureModelEditable();
	discoveries.add(discovery);
	fireStructureChanged(discovery, IModelChangedEvent.INSERT);
}
public void addUpdate(IFeatureURLElement update) throws CoreException {
	ensureModelEditable();
	updates.add(update);
	fireStructureChanged(update, IModelChangedEvent.INSERT);
}
public IFeatureURLElement[] getDiscoveries() {
	IFeatureURLElement [] result = new IFeatureURLElement[discoveries.size()];
	discoveries.copyInto(result);
	return result;
}
public IFeatureURLElement[] getUpdates() {
	IFeatureURLElement [] result = new IFeatureURLElement[updates.size()];
	updates.copyInto(result);
	return result;
}
protected void parse(Node node) {
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			String tag = child.getNodeName().toLowerCase();
			int urlType = -1;
			if (tag.equals("update")) {
				urlType = IFeatureURLElement.UPDATE;
			} else
				if (tag.equals("discovery")) {
					urlType = IFeatureURLElement.DISCOVERY;
				}
			if (urlType != -1) {
				IFeatureURLElement element =
					getModel().getFactory().createURLElement(this, urlType);
				((FeatureURLElement)element).parse(child);
				if (urlType == IFeatureURLElement.UPDATE)
					updates.add(element);
				else
					if (urlType == IFeatureURLElement.DISCOVERY)
						discoveries.add(element);
			}
		}
	}
}
public void removeDiscovery(IFeatureURLElement discovery) throws CoreException {
	ensureModelEditable();
	discoveries.remove(discovery);
	fireStructureChanged(discovery, IModelChangedEvent.REMOVE);
}
public void removeUpdate(IFeatureURLElement update) throws CoreException {
	ensureModelEditable();
	updates.remove(update);
	fireStructureChanged(update, IModelChangedEvent.REMOVE);
}
public void write(String indent, PrintWriter writer) {
	writer.println(indent+"<url>");
	String indent2 = indent + Feature.INDENT;
	for (int i=0; i<updates.size(); i++) {
		IFeatureURLElement element = (IFeatureURLElement)updates.elementAt(i);
		element.write(indent2, writer);
	}
	for (int i=0; i<discoveries.size(); i++) {
		IFeatureURLElement element = (IFeatureURLElement)discoveries.elementAt(i);
		element.write(indent2, writer);
	}
	writer.println(indent+"</url>");
}
}
