package org.eclipse.pde.internal.model.component;

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import java.net.*;
import org.eclipse.pde.internal.base.model.component.*;

public class ComponentURLElement extends ComponentObject implements IComponentURLElement {
	private int elementType;
	private URL url;

public ComponentURLElement(int elementType) {
	this.elementType = elementType;
}
public ComponentURLElement(int elementType, URL url) {
	this.elementType = elementType;
	this.url = url;
}
public int getElementType() {
	return elementType;
}
public URL getURL() {
	return url;
}
void parse(Node node) {
	label = getNodeAttribute(node, "label");
	String urlName = getNodeAttribute(node, "url");
	try {
		url = new URL(urlName);
	} catch (MalformedURLException e) {
	}
}
public void setURL(URL url) throws CoreException {
	ensureModelEditable();
	this.url = url;
	firePropertyChanged(this, P_URL);
}
public String toString() {
	if (label != null)
		return label;
	if (url != null)
		return url.toString();
	return super.toString();
}
public void write(String indent, PrintWriter writer) {
	String tag = null;
	switch (elementType) {
		case UPDATE :
			tag = "update";
			break;
		case DISCOVERY :
			tag = "discovery";
			break;
	}
	if (tag == null)
		return;
	writer.print(indent + "<" + tag);
	if (label != null) {
		writer.print(" label=\""+label+"\"");
	}
	if (url != null) {
		writer.print(" url=\"" + url.toString() + "\"");
	}
	writer.println("/>");
}
}
