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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.*;
import java.util.*;

import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.*;
import org.osgi.framework.*;

public class ExportPackageObject implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String fVersionAttribute;
    private String name;
    private String version;
    private transient ManifestElement element;
    
    public ExportPackageObject(ManifestElement element, String versionAttribute) {
        fVersionAttribute = versionAttribute;
        name = element.getValue();
        version = element.getAttribute(fVersionAttribute);
        this.element = element;
    }
    
    public ExportPackageObject(IPackageFragment fragment, String versionAttribute) {
        fVersionAttribute = versionAttribute;
        name = fragment.getElementName();
    }
    
     public String toString() {
        StringBuffer buffer = new StringBuffer(name);
        if (version != null && version.length() > 0) {
            buffer.append(" ");
            boolean wrap = Character.isDigit(version.charAt(0));
            if (wrap)
                buffer.append("(");
            buffer.append(version);
            if (wrap)
                buffer.append(")");
        }
        return buffer.toString();
    }
    
    public String write() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(name);
        if (version != null && version.length() > 0) {
            buffer.append(";");
            buffer.append(fVersionAttribute);
            buffer.append("=\"");
            buffer.append(version.trim());
            buffer.append("\"");
        }
        if (element == null)
            return buffer.toString();
        
        Enumeration attrs = element.getKeys();
        if (attrs != null) {
            while (attrs.hasMoreElements()) {
                String attr = attrs.nextElement().toString();
                if (attr.equals(fVersionAttribute))
                    continue;
                buffer.append(";");
                buffer.append(attr);
                buffer.append("=\"");
                buffer.append(element.getAttribute(attr));
                buffer.append("\"");
            }
        }
        Enumeration directives = element.getDirectiveKeys();
        if (directives != null) {
            while (directives.hasMoreElements()) {
                String directive = directives.nextElement().toString();
                if (directive.equals(Constants.RESOLUTION_DIRECTIVE))
                    continue;
                buffer.append(";");
                buffer.append(directive);
                buffer.append(":=");
                buffer.append("\"");
                buffer.append(element.getDirective(directive));
                buffer.append("\"");
            }
        }
        return buffer.toString();
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
