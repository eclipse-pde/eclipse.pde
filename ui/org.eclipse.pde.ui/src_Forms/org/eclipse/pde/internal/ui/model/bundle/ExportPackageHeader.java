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
package org.eclipse.pde.internal.ui.model.bundle;

import org.eclipse.osgi.util.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.*;

public class ExportPackageHeader extends BasePackageHeader {
    
    private static final long serialVersionUID = 1L;

    private static String getExportedPackageHeader(int manifestVersion) {
        return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
   }
   
    public ExportPackageHeader(int manifestVersion, String value, IBundle bundle) {
       super(getExportedPackageHeader(manifestVersion), value, bundle);
    }
 
    public ExportPackageHeader(String name, String value, IBundle bundle) {
        super(name, value, bundle);
    }
    
    protected void processValue() {
        try {
            if (fValue != null) {
                ManifestElement[] elements = ManifestElement.parseHeader(fName, fValue);
                for (int i = 0; i < elements.length; i++) {
                    ExportPackageObject p = new ExportPackageObject(this, elements[i], getVersionAttribute());
                    fPackages.put(p.getName(), p);
                }
            }
        } catch (BundleException e) {
        }
    }
    
    public ExportPackageObject[] getPackages() {
        return (ExportPackageObject[])fPackages.values().toArray(new ExportPackageObject[fPackages.size()]);
    }
    
}
