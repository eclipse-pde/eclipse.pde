/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

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
	protected void parse(Node node, Hashtable lineTable) {
		bindSourceLocation(node, lineTable);
		String urlName = getNodeAttribute(node, "url"); //$NON-NLS-1$
		if (urlName != null) {
			try {
				url = new URL(urlName);
			} catch (MalformedURLException e) {
			}
		}
		library = getNodeAttribute(node, "library"); //$NON-NLS-1$
		handlerName = getNodeAttribute(node, "handler"); //$NON-NLS-1$
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<install-handler"); //$NON-NLS-1$
		if (url != null) {
			writer.print(" url=\"" + url.toString() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (library != null) {
			writer.print(" library=\"" + library + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (handlerName != null) {
			writer.print(" handler=\"" + handlerName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
		//writer.println(indent + "</install-handler>");
	}
}
