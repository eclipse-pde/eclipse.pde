/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.Vector;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;

public class ExportPackageHeader extends BasePackageHeader {
    
    private static final long serialVersionUID = 1L;
   
    public ExportPackageHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
    
     protected PDEManifestElement createElement(ManifestElement element) {
    	return new ExportPackageObject(this, element, getVersionAttribute());
    }
    
    public Vector getPackageNames() {
        return getElementNames();
    }
    
    public ExportPackageObject[] getPackages() {
    	PDEManifestElement[] elements = getElements();
    	ExportPackageObject[] result = new ExportPackageObject[elements.length];
    	System.arraycopy(elements, 0, result, 0, elements.length);
        return result;
    }
    
}
