/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.ui.model.IDocumentKey;
import org.osgi.framework.BundleException;

public class ManifestHeader extends BundleObject implements IDocumentKey {
    private static final long serialVersionUID = 1L;
    private int fOffset = -1;
	private int fLength = -1;
    
	protected String fName;
	protected String fValue;
    private IBundle fBundle;
	private String fLineDelimiter;
	private Hashtable fAttributes = new Hashtable();
	private Hashtable fDirectives = new Hashtable();
	private String[] fValueComponents;
    
    public ManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
        fName = name;
        fValue = value;
        fBundle = bundle;
        fLineDelimiter = lineDelimiter;
        setModel(fBundle.getModel());
        try {
        	// Attribute and Directive support 
			// meant for headers with a single element
        	ManifestElement[] elements = ManifestElement.parseHeader(fName, fValue);
			if (elements == null || elements.length != 1)
				return;
			Enumeration keys = elements[0].getKeys();
			while (keys != null && keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				fAttributes.put(key, elements[0].getAttributes(key));
			}
			Enumeration dkeys = elements[0].getDirectiveKeys();
			while (dkeys != null && dkeys.hasMoreElements()) {
				String dkey = (String)dkeys.nextElement();
				fDirectives.put(dkey, elements[0].getDirectives(dkey));
			}
			fValueComponents = elements[0].getValueComponents();
		} catch (BundleException e) {
		}
    }
    
    public String getLineLimiter() {
    	return fLineDelimiter;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getName()
	 */
	public String getName() {
		return fName;
	}
	
	public void setValue(String value) {
		fValue = value;
	}
	
	public String getValue() {
		return fValue;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#write()
	 */
	public String write() {
        updateValue();
		return fName + ": " + fValue + fLineDelimiter; //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
    
    public void setBundle(IBundle bundle) {
        fBundle = bundle;
    }
    
    public IBundle getBundle() {
        return fBundle;
    }
    
    public void updateValue() {
    }
    
    public void setAttribute(String key, String[] value) {
    	setHashtableValue(fAttributes, key, value);
    }
    public void setDirective(String key, String[] value) {
    	setHashtableValue(fDirectives, key, value);
    }
    
    private void setHashtableValue(Hashtable table, String key, String[] value) {
    	if (key == null) return;
    	String old = fValue;
    	if (value == null || value.length == 0)
    		table.remove(key);
    	else
    		table.put(key, value);
    	refreshValue();
    	getModel().fireModelObjectChanged(this, fName, old, fValue);
    }
    private void refreshValue() {
    	if (fValueComponents.length == 0)
    		return;
    	StringBuffer sb = new StringBuffer();
    	int i = 0;
    	for (; i < fValueComponents.length; i++) {
    		if (i != 0) sb.append("; ");  //$NON-NLS-1$
    		sb.append(fValueComponents[i]);
    	}
    	appendValuesToBuffer(sb, fAttributes);
    	appendValuesToBuffer(sb, fDirectives);
    	fValue = sb.toString();
    }
    private void appendValuesToBuffer(StringBuffer sb, Hashtable table) {
    	Enumeration dkeys = table.keys();
    	while (dkeys.hasMoreElements()) {
    		String dkey = (String)dkeys.nextElement();
    		sb.append("; "); //$NON-NLS-1$
			sb.append(dkey);
			sb.append(table.equals(fDirectives) ? ":=" : "="); //$NON-NLS-1$ //$NON-NLS-2$
    		String[] values = (String[])table.get(dkey);
    		if (values.length > 0)sb.append("\""); //$NON-NLS-1$
    		for (int i = 0; i < values.length; i++) {
    			if (i != 0) sb.append(", "); //$NON-NLS-1$
    			sb.append(values[i]);
    		}
    		if (values.length > 0)sb.append("\""); //$NON-NLS-1$
    	}
    }
}

