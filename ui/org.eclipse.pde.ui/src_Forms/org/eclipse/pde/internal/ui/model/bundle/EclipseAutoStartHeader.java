/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.util.*;


public class EclipseAutoStartHeader extends ManifestHeader {
	
	private ArrayList fExceptions = new ArrayList();
	
	public EclipseAutoStartHeader() {
		setName("Eclipse-AutoStart"); //$NON-NLS-1$
	}
	
	public void addException(String packageName) {
		if (!fExceptions.contains(packageName))
			fExceptions.add(packageName);
	}
	
	public void removeException(String packageName) {
		fExceptions.remove(packageName);
	}
	
	public String[] getExceptions() {
		return (String[])fExceptions.toArray(new String[fExceptions.size()]);
	}
}
