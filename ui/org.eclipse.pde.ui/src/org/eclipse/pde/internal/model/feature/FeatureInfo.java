/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.model.feature;

import org.eclipse.pde.internal.base.model.feature.IFeatureInfo;
import java.net.*;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Node;
import java.io.PrintWriter;

/**
 * @version 	1.0
 * @author
 */
public class FeatureInfo extends FeatureObject implements IFeatureInfo {
	private URL url;
	private String description;
	private String tag;
	
	public FeatureInfo(String tag) {
		this.tag = tag;
	}

	/*
	 * @see IFeatureInfo#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/*
	 * @see IFeatureInfo#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/*
	 * @see IFeatureInfo#setURL(URL)
	 */
	public void setURL(URL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	/*
	 * @see IFeatureInfo#setDescription(String)
	 */
	public void setDescription(String description) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_DESC, oldValue, description);
	}
	protected void parse(Node node) {
		String urlName = getNodeAttribute(node, "url");
		try {
			url = new URL(urlName);
		}
		catch (MalformedURLException e) {
		}
		description = getNormalizedText(node.getFirstChild().getNodeValue());
	}
	
	public void write(String indent, PrintWriter writer) {
		String indent2 = indent+Feature.INDENT;
		String desc = getWritableString(description.trim());
		writer.println();
		writer.print(indent+"<"+tag);
		if (url!=null) {
			writer.print("url=\""+url.toString()+"\"");
		}
		writer.println(">");
		writer.println(indent2+description);
		writer.println(indent+"</"+tag+">");
	}
}
