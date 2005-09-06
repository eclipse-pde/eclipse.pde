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

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

public abstract class BasePackageHeader extends ManifestHeader {

    private static final long serialVersionUID = 1L;
    
    protected TreeMap fPackages = new TreeMap();
    
	public BasePackageHeader(String name, String value, IBundle bundle,
			String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
		processValue();
	}

    protected String getVersionAttribute() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
    
    protected abstract void processValue();

    public void addPackage(PackageObject object) {
        fPackages.put(object.getName(), object);
        updateValue();
        fireStructureChanged(object, IModelChangedEvent.INSERT);
    }
    
    public void removePackage(PackageObject object) {
        fPackages.remove(object.getName());
        updateValue();
        fireStructureChanged(object, IModelChangedEvent.REMOVE);
    }
    
    public Vector getPackageNames() {
        Vector vector = new Vector(fPackages.size());
        Iterator iter = fPackages.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            vector.add(iter.next().toString());
        }
        return vector;
    }
    
    public boolean hasPackage(String packageName) {
        return fPackages.containsKey(packageName);
    }
    
    public boolean removePackage(String name) {
    	return fPackages.remove(name) != null;
    }
    
    public boolean renamePackage(String oldName, String newName) {
    	if (hasPackage(oldName)) {
    		PackageObject object = (PackageObject)fPackages.remove(oldName);
    		object.setName(newName);
    		fPackages.put(newName, object);
    		return true;
    	}
    	return false;
    }
    
    public void updateValue() {
        StringBuffer buffer = new StringBuffer();
        Iterator iter = fPackages.values().iterator();
        while (iter.hasNext()) {
            buffer.append(((PackageObject)iter.next()).write());
            if (iter.hasNext()) {
                buffer.append(","); //$NON-NLS-1$
                buffer.append(getLineLimiter());
                buffer.append(" ");  //$NON-NLS-1$
            }
        }
       fValue = buffer.toString();
    }


}
