/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
		firePropertyChanged(P_LIBRARY, oldValue, library);
	}

	/*
	 * @see IFeatureInstallHandler#setClassName(String)
	 */
	public void setHandlerName(String handlerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.handlerName;
		this.handlerName = handlerName;
		firePropertyChanged(P_HANDLER_NAME, oldValue, handlerName);
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_URL)) {
			setURL((URL) newValue);
		} else if (name.equals(P_LIBRARY)) {
			setLibrary((String) newValue);
		} else if (name.equals(P_HANDLER_NAME)) {
			setHandlerName((String) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}
	protected void parse(Node node) {
		String urlName = getNodeAttribute(node, "url");
		if (urlName != null) {
			try {
				url = new URL(urlName);
			} catch (MalformedURLException e) {
			}
		}
		library = getNodeAttribute(node, "library");
		handlerName = getNodeAttribute(node, "handler");
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<install-handler");
		if (url != null) {
			writer.print(" url=\"" + url.toString() + "\"");
		}
		if (library != null) {
			writer.print(" library=\"" + library + "\"");
		}
		if (handlerName != null) {
			writer.print(" handler=\"" + handlerName + "\"");
		}
		writer.println("/>");
		//writer.println(indent + "</install-handler>");
	}
}