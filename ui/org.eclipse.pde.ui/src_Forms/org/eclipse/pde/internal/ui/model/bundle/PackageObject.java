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

import java.util.Enumeration;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.bundle.BundleObject;

public class PackageObject extends BundleObject {

    private static final long serialVersionUID = 1L;

    private String fVersionAttribute;
    private String fName;
    private String fVersion;
    private transient ManifestElement fElement;

    private ManifestHeader fHeader;
    
    public PackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        fHeader = header;
        fVersionAttribute = versionAttribute;
        fName = element.getValue();
        fVersion = element.getAttribute(fVersionAttribute);
        fElement = element;
        setModel(fHeader.getBundle().getModel());
    }
    
    public PackageObject(ManifestHeader header, String name, String version, String versionAttribute) {
        fHeader = header;
        fVersion = version;
        fVersionAttribute = versionAttribute;
        fName = name.length() > 0 ? name : "."; //$NON-NLS-1$
        setModel(fHeader.getBundle().getModel());
    }
    
     public String toString() {
        StringBuffer buffer = new StringBuffer(fName);
        if (fVersion != null && fVersion.length() > 0) {
            buffer.append(" "); //$NON-NLS-1$
            boolean wrap = Character.isDigit(fVersion.charAt(0));
            if (wrap)
                buffer.append("("); //$NON-NLS-1$
            buffer.append(fVersion);
            if (wrap)
                buffer.append(")"); //$NON-NLS-1$
        }
        return buffer.toString();
    }
    
    public String write() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(fName);
        
        appendSupportedAttributes(buffer);

        if (fElement == null)
            return buffer.toString();
        
        Enumeration attrs = fElement.getKeys();
        if (attrs != null) {
            while (attrs.hasMoreElements()) {
                String attr = attrs.nextElement().toString();
                if (attr.equals(fVersionAttribute))
                    continue;
                buffer.append(";"); //$NON-NLS-1$
                buffer.append(attr);
                buffer.append("=\""); //$NON-NLS-1$
                buffer.append(fElement.getAttribute(attr));
                buffer.append("\""); //$NON-NLS-1$
            }
        }
        
        Enumeration directives = fElement.getDirectiveKeys();
        if (directives != null) {
            while (directives.hasMoreElements()) {
                String directive = directives.nextElement().toString();
                if (skipDirective(directive))
                    continue;
                buffer.append(";"); //$NON-NLS-1$
                buffer.append(directive);
                buffer.append(":="); //$NON-NLS-1$
                buffer.append("\""); //$NON-NLS-1$
                buffer.append(fElement.getDirective(directive));
                buffer.append("\""); //$NON-NLS-1$
            }
        }
        return buffer.toString();
    }
    
    protected void appendSupportedAttributes(StringBuffer buffer) {
        if (fVersion != null && fVersion.length() > 0) {
            buffer.append(";"); //$NON-NLS-1$
            buffer.append(fVersionAttribute);
            buffer.append("=\""); //$NON-NLS-1$
            buffer.append(fVersion.trim());
            buffer.append("\""); //$NON-NLS-1$
        }
    }
    
    protected boolean skipDirective(String directive) {
        return false;
    }

    public String getVersion() {
        return fVersion;
    }

    public String getName() {
        return fName;
    }
    
    public void setName(String name) {
    	fName = name;
    }

    public void setVersion(String version) {
        String old = fVersion;
        fVersion = version;
        firePropertyChanged(this, fVersionAttribute, old, version);
    }

    public ManifestHeader getHeader() {
        return fHeader;
    }
    
    public ManifestElement getManifestElement() {
    	return fElement;
    }

}
