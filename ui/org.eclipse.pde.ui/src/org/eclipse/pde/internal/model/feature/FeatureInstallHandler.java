/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.model.feature;

import org.eclipse.pde.internal.base.model.feature.*;
import java.net.*;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Node;
import java.io.PrintWriter;

/**
 * @version 	1.0
 * @author
 */
public class FeatureInstallHandler
	extends FeatureObject
	implements IFeatureInstallHandler {
		private URL url;
		private String library;
		private String className;

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
	public String getClassName() {
		return className;
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
	public void setClassName(String className) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.className;
		this.className = className;
		firePropertyChanged(P_URL, oldValue, className);
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
		className = getNodeAttribute(node, "class");
	}
public void write(String indent, PrintWriter writer) {
	writer.print(indent+"<install-handler");
	if (url!=null) {
		writer.print(" url=\""+url.toString()+"\"");
	}
	if (library!=null) {
		writer.print(" library=\""+library+"\"");
	}
	if (className!=null) {
		writer.print(" class=\""+className+"\"");
	}
	writer.println(">");
	writer.println(indent+"</install-handler>");
}
}
