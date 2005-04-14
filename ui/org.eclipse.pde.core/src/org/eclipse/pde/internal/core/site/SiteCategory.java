/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

public class SiteCategory extends SiteObject implements ISiteCategory {
	private static final long serialVersionUID = 1L;
	private String name;
	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategory#getName()
	 */
	public String getName() {
		return name;
	}
	
	public boolean isValid() {
		return name!=null;
	}
	
	protected void parse(Node node, Hashtable lineTable) {
		name = getNodeAttribute(node, "name"); //$NON-NLS-1$
	}
	
	protected void reset() {
		name = null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategory#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<category"); //$NON-NLS-1$
		if (name!=null)
			writer.print(" name=\""+name+"\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		}
		else super.restoreProperty(name, oldValue, newValue);
	}

	public ISiteCategoryDefinition getDefinition() {
		ISite site = getSite();
		ISiteCategoryDefinition [] definitions = site.getCategoryDefinitions();
		for (int i=0; i<definitions.length; i++) {
			ISiteCategoryDefinition def = definitions[i];
			if (def.getName().equals(getName()))
				return def;
		}
		return null;
	}
}
