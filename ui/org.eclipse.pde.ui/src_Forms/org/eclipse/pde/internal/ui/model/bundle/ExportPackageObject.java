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

import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.*;

public class ExportPackageObject extends PackageObject {
    
    private static final String INTERNAL = "x-internal";
    
    private static final long serialVersionUID = 1L;
    
    private boolean fInternal;

     
    public ExportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        super(header, element, versionAttribute);
        fInternal = "true".equals(element.getDirective(INTERNAL));
    }
    
    public ExportPackageObject(ManifestHeader header, IPackageFragment fragment, String versionAttribute) {
        super(header, fragment.getElementName(), null, versionAttribute);
    }
    
    protected void appendSupportedAttributes(StringBuffer buffer) {
       super.appendSupportedAttributes(buffer);
       if (fInternal) {
           buffer.append(";");
           buffer.append(INTERNAL);
           buffer.append(":=true");
       }          
    }

    public boolean isInternal() {
        return fInternal;
    }

    public void setInternal(boolean internal) {
        boolean old = fInternal;
        fInternal = internal;
        firePropertyChanged(this, INTERNAL, Boolean.valueOf(old), Boolean.valueOf(internal));
    }
    
    public String[] getFriends() {
        return new String[0];
    }
    
    protected boolean skipDirective(String directive) {
        return INTERNAL.equals(directive);
    }

}
