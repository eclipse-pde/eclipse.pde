/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.net.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.Node;

/**
 * @version 	1.0
 * @author
 */
public class FeatureInfo extends FeatureObject implements IFeatureInfo {
	private static final String KEY_INFO_DESCRIPTION ="FeatureEditor.info.description";
	private static final String KEY_INFO_LICENSE = "FeatureEditor.info.license";
	private static final String KEY_INFO_COPYRIGHT = "FeatureEditor.info.copyright";
	private URL url;
	private String description;
	private int index;
	
	public FeatureInfo(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	private String getTag() {
		return IFeature.INFO_TAGS[index];
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
		writer.print(indent+"<"+getTag());
		if (url!=null) {
			writer.print(" url=\""+url.toString()+"\"");
		}
		writer.println(">");
		writer.println(indent2+desc);
		writer.println(indent+"</"+getTag()+">");
	}
	
	public boolean isEmpty() {
		if (url!=null) return false;
		String desc = description!=null ? description.trim() : null;
		if (desc!=null && desc.length()>0) return false;
		return true;
	}
	
	public String toString() {
		switch (index) {
			case IFeature.INFO_DESCRIPTION:
				return PDECore.getResourceString(KEY_INFO_DESCRIPTION);
			case IFeature.INFO_LICENSE:
				return PDECore.getResourceString(KEY_INFO_LICENSE);
			case IFeature.INFO_COPYRIGHT:
				return PDECore.getResourceString(KEY_INFO_COPYRIGHT);
		}
		return super.toString();
	}
}