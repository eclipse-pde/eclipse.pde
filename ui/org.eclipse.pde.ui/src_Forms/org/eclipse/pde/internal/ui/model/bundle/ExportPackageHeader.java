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

import java.util.*;

import org.eclipse.osgi.util.*;
import org.eclipse.pde.internal.core.*;
import org.osgi.framework.*;

public class ExportPackageHeader extends ManifestHeader {
    
    private TreeMap fPackages = new TreeMap();

    public ExportPackageHeader(int manifestVersion, String value) {
       this(getExportedPackageHeader(manifestVersion), value);
    }
    
    public ExportPackageHeader(String name, String value) {
        super(name, value);
        processValue();
    }

    private void processValue() {
        try {
            if (fValue != null) {
                ManifestElement[] elements = ManifestElement.parseHeader(fName, fValue);
                for (int i = 0; i < elements.length; i++) {
                    ExportPackageObject p = new ExportPackageObject(elements[i], getVersionAttribute());
                    fPackages.put(p.getName(), p);
                }
            }
        } catch (BundleException e) {
        }
    }
    
    private String getVersionAttribute() {
        int manifestVersion = fName.equals(Constants.EXPORT_PACKAGE) ? 2 : 1;
        return (manifestVersion < 2) ? Constants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }

    private static String getExportedPackageHeader(int manifestVersion) {
         return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
    }
    
    public void addPackage(ExportPackageObject object) {
        fPackages.put(object.getName(), object);
        getBundle().setHeader(fName, write());
    }
    
    public void removePackage(ExportPackageObject object) {
        fPackages.remove(object.getName());
        getBundle().setHeader(fName, write());
    }
    
    public ExportPackageObject[] getPackages() {
        return (ExportPackageObject[])fPackages.values().toArray(new ExportPackageObject[fPackages.size()]);
    }
    
    public Vector getPackageNames() {
        Vector vector = new Vector(fPackages.size());
        Iterator iter = fPackages.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            vector.add(iter.next().toString());
        }
        return vector;
    }
    
    public String write() {
        StringBuffer buffer = new StringBuffer(fName);
        buffer.append(": ");
        Iterator iter = fPackages.values().iterator();
        while (iter.hasNext()) {
            buffer.append(((ExportPackageObject)iter.next()).write());
            if (iter.hasNext()) {
                buffer.append("," + System.getProperty("line.separator") + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        return buffer.toString();
    }

}
