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

import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.*;
import org.osgi.framework.*;

public class ImportPackageObject extends PackageObject {
    
    private static final long serialVersionUID = 1L;
    
    private boolean fOptional;
    
    private static String getVersion(ExportPackageDescription desc) {
        String version = desc.getVersion().toString();
        if (!version.equals(Version.emptyVersion.toString()))
            return desc.getVersion().toString();
        return null;
    }
    
    public ImportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        super(header, element, versionAttribute);
        fOptional = Constants.RESOLUTION_OPTIONAL.equals(element.getDirective(Constants.RESOLUTION_DIRECTIVE));
    }
    
    public ImportPackageObject(ManifestHeader header, ExportPackageDescription desc, String versionAttribute) {       
        super(header, desc.getName(), getVersion(desc), versionAttribute);
        fOptional = Constants.RESOLUTION_OPTIONAL.equals(desc.getDirective(Constants.RESOLUTION_DIRECTIVE));
    }
   
    protected void appendSupportedAttributes(StringBuffer buffer) {
        super.appendSupportedAttributes(buffer);
        if (fOptional) {
            buffer.append(";");
            buffer.append(Constants.RESOLUTION_DIRECTIVE);
            buffer.append(":=");
            buffer.append(Constants.RESOLUTION_OPTIONAL);
        }
    }
    public boolean isOptional() {
        return fOptional;
    }

    public void setOptional(boolean optional) {
        fOptional = optional;
    }

}
