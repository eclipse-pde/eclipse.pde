/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.net.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.w3c.dom.Node;

/**
 * @version 	1.0
 * @author
 */
public class FeatureInstallHandler
	extends FeatureObject
	implements IFeatureInstallHandler {
		private URL url;
		private String library;
		private String handlerName;

	/*
	 * @see IFeatureInstallHandler#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/*
	 * @see IFeatureInstallHandler#getLibrary()
	 */
	public String getLibrary() {
		return library;
	}

	/*
	 * @see IFeatureInstallHandler#getClassName()
	 */
	public String getHandlerName() {
		return handlerName;
	}

	/*
	 * @see IFeatureInstallHandler#setURL(URL)
	 */
	public void setURL(URL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	/*
	 * @see IFeatureInstallHandler#setLibrary(String)
	 */
	public void setLibrary(String library) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.library;
		this.library = library;
		firePropertyChanged(P_URL, oldValue, library);
	}

	/*
	 * @see IFeatureInstallHandler#setClassName(String)
	 */
	public void setHandlerName(String handlerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.handlerName;
		this.handlerName = handlerName;
		firePropertyChanged(P_URL, oldValue, handlerName);
	}
	protected void parse(Node node) {
		String urlName = getNodeAttribute(node, "url");
		if (urlName!=null) {
			try {
				url = new URL(urlName);
			}
			catch (MalformedURLException e) {
			}
		}
		library = getNodeAttribute(node, "library");
		handlerName = getNodeAttribute(node, "handler");
	}
public void write(String indent, PrintWriter writer) {
	writer.print(indent+"<install-handler");
	if (url!=null) {
		writer.print(" url=\""+url.toString()+"\"");
	}
	if (library!=null) {
		writer.print(" library=\""+library+"\"");
	}
	if (handlerName!=null) {
		writer.print(" handler=\""+handlerName+"\"");
	}
	writer.println(">");
	writer.println(indent+"</install-handler>");
}
}
