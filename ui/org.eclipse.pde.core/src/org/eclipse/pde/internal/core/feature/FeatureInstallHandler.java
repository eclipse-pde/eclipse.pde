/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.w3c.dom.Node;

public class FeatureInstallHandler extends FeatureObject implements IFeatureInstallHandler {
	private static final long serialVersionUID = 1L;
	private String fLibrary;
	private String fHandlerName;

	/*
	 * @see IFeatureInstallHandler#getLibrary()
	 */
	@Override
	public String getLibrary() {
		return fLibrary;
	}

	/*
	 * @see IFeatureInstallHandler#getClassName()
	 */
	@Override
	public String getHandlerName() {
		return fHandlerName;
	}

	/*
	 * @see IFeatureInstallHandler#setLibrary(String)
	 */
	@Override
	public void setLibrary(String library) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fLibrary;
		this.fLibrary = library;
		firePropertyChanged(P_LIBRARY, oldValue, library);
	}

	/*
	 * @see IFeatureInstallHandler#setClassName(String)
	 */
	@Override
	public void setHandlerName(String handlerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fHandlerName;
		this.fHandlerName = handlerName;
		firePropertyChanged(P_HANDLER_NAME, oldValue, handlerName);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_LIBRARY)) {
			setLibrary((String) newValue);
		} else if (name.equals(P_HANDLER_NAME)) {
			setHandlerName((String) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	@Override
	protected void parse(Node node) {
		fLibrary = getNodeAttribute(node, "library"); //$NON-NLS-1$
		fHandlerName = getNodeAttribute(node, "handler"); //$NON-NLS-1$
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<install-handler"); //$NON-NLS-1$
		if (fLibrary != null) {
			writer.print(" library=\"" + fLibrary + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fHandlerName != null) {
			writer.print(" handler=\"" + fHandlerName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
		//writer.println(indent + "</install-handler>");
	}
}
