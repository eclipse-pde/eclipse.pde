/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LocationInfo extends TargetObject implements ILocationInfo{

	private static final long serialVersionUID = 1L;
	
	private String fPath = ""; //$NON-NLS-1$
	private boolean fUseDefault = true;

	public LocationInfo(ITargetModel model) {
		super(model);
	}

	public void parse(Node node) {
		Element element = (Element)node; 
		fPath = element.getAttribute("path");  //$NON-NLS-1$
		fUseDefault = "true".equalsIgnoreCase(element.getAttribute("useDefault")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void write(String indent, PrintWriter writer) {
		writer.println();
		writer.print(indent + "<location"); //$NON-NLS-1$
		if (fUseDefault)
			writer.print(" useDefault=\"true\""); //$NON-NLS-1$
		else if (fPath != null && fPath.trim().length() > 0)
			writer.print(" path=\"" + getWritableString(fPath.trim()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>");  //$NON-NLS-1$
	}

	public boolean useDefault() {
		return fUseDefault;
	}

	public void setDefault(boolean value) {
		fUseDefault = value;
		if (value) 
			firePropertyChanged(P_LOC, fPath, ""); //$NON-NLS-1$
		else
			firePropertyChanged(P_LOC, "", fPath); //$NON-NLS-1$
	}

	public String getPath() {
		return fPath;
	}

	public void setPath(String path) {
		fUseDefault = false;
		String oldValue = fPath;
		fPath = (path != null) ? path : ""; //$NON-NLS-1$
		firePropertyChanged(P_LOC, oldValue, fPath);
	}

}
