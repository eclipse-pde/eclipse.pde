package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
import java.net.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;
import org.w3c.dom.Node;

public class FeatureURLElement extends FeatureObject implements IFeatureURLElement {
	private int elementType;
	private URL url;

public FeatureURLElement(int elementType) {
	this.elementType = elementType;
}
public FeatureURLElement(int elementType, URL url) {
	this.elementType = elementType;
	this.url = url;
}
public int getElementType() {
	return elementType;
}
public URL getURL() {
	return url;
}
protected void parse(Node node) {
	super.parse(node);
	String urlName = getNodeAttribute(node, "url");
	try {
		url = new URL(urlName);
	} catch (MalformedURLException e) {
	}
}
public void setURL(URL url) throws CoreException {
	ensureModelEditable();
	Object oldValue = this.url;
	this.url = url;
	firePropertyChanged(this, P_URL, oldValue, url);
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
		writer.print(" label=\""+getWritableString(label)+"\"");
	}
	if (url != null) {
		writer.print(" url=\"" + getWritableString(url.toString()) + "\"");
	}
	writer.println("/>");
}
}
