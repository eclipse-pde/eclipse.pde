package org.eclipse.pde.internal.model.component;

import java.io.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.component.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;

public class ComponentURL extends ComponentObject implements IComponentURL {
	private Vector updates=new Vector();
	private Vector discoveries = new Vector();

public void addDiscovery(IComponentURLElement discovery) throws CoreException {
	ensureModelEditable();
	discoveries.add(discovery);
	fireStructureChanged(discovery, IModelChangedEvent.INSERT);
}
public void addUpdate(IComponentURLElement update) throws CoreException {
	ensureModelEditable();
	updates.add(update);
	fireStructureChanged(update, IModelChangedEvent.INSERT);
}
public IComponentURLElement[] getDiscoveries() {
	IComponentURLElement [] result = new IComponentURLElement[discoveries.size()];
	discoveries.copyInto(result);
	return result;
}
public IComponentURLElement[] getUpdates() {
	IComponentURLElement [] result = new IComponentURLElement[updates.size()];
	updates.copyInto(result);
	return result;
}
void parse(Node node) {
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			String tag = child.getNodeName().toLowerCase();
			int urlType = -1;
			if (tag.equals("update")) {
				urlType = IComponentURLElement.UPDATE;
			} else
				if (tag.equals("discovery")) {
					urlType = IComponentURLElement.DISCOVERY;
				}
			if (urlType != -1) {
				IComponentURLElement element =
					getModel().getFactory().createURLElement(this, urlType);
				((ComponentURLElement)element).parse(child);
				if (urlType == IComponentURLElement.UPDATE)
					updates.add(element);
				else
					if (urlType == IComponentURLElement.DISCOVERY)
						discoveries.add(element);
			}
		}
	}
}
public void removeDiscovery(IComponentURLElement discovery) throws CoreException {
	ensureModelEditable();
	discoveries.remove(discovery);
	fireStructureChanged(discovery, IModelChangedEvent.REMOVE);
}
public void removeUpdate(IComponentURLElement update) throws CoreException {
	ensureModelEditable();
	updates.remove(update);
	fireStructureChanged(update, IModelChangedEvent.REMOVE);
}
public void write(String indent, PrintWriter writer) {
	writer.println(indent+"<url>");
	String indent2 = indent + Component.INDENT;
	for (int i=0; i<updates.size(); i++) {
		IComponentURLElement element = (IComponentURLElement)updates.elementAt(i);
		element.write(indent2, writer);
	}
	for (int i=0; i<discoveries.size(); i++) {
		IComponentURLElement element = (IComponentURLElement)discoveries.elementAt(i);
		element.write(indent2, writer);
	}
	writer.println(indent+"</url>");
}
}
