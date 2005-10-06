/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.model.IDocumentKey;
import org.osgi.framework.Constants;

public class Bundle implements IBundle {
	
	private BundleModel fModel;
	private Hashtable fDocumentHeaders = new Hashtable();
	
	public Bundle(BundleModel model) {
		fModel = model;
	}
	
	public void clearHeaders() {
		fDocumentHeaders.clear();
	}
	
	public void load(Manifest manifest) {
		Map attributes = manifest.getMainAttributes();
		Iterator iter = attributes.keySet().iterator();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			if (key.toString().equals(Constants.BUNDLE_MANIFESTVERSION)) {				
	            String value = (String)attributes.get(key);
				ManifestHeader header = createHeader(key.toString(), value);
				fDocumentHeaders.put(key.toString(), header);
				break;
			}
		}
		iter = attributes.keySet().iterator();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			if (key.toString().equals(Constants.BUNDLE_MANIFESTVERSION))
				continue;
            String value = (String)attributes.get(key);
			ManifestHeader header = createHeader(key.toString(), value);
			fDocumentHeaders.put(key.toString(), header);
		}
		adjustOffsets(fModel.getDocument());		
	}
	
	public void clearOffsets() {
		Iterator iter = fDocumentHeaders.values().iterator();
		while (iter.hasNext()) {
			ManifestHeader header = (ManifestHeader)iter.next();
			header.setOffset(-1);
			header.setLength(-1);
		}
	}
	
	protected void adjustOffsets(IDocument document) {
		int lines = document.getNumberOfLines();
		try {
			IDocumentKey currentKey = null;
			for (int i = 0; i < lines; i++) {
				int offset = document.getLineOffset(i);
				int length = document.getLineLength(i);
				String line = document.get(offset, length);
				
				if (currentKey != null) {
					int lineNumber = line.startsWith(" ") ? i : i - 1; //$NON-NLS-1$
					IRegion region = document.getLineInformation(lineNumber);
					String delimiter = document.getLineDelimiter(lineNumber);
					int keyLength = region.getOffset() + region.getLength() - currentKey.getOffset();
					currentKey.setLength(delimiter != null ? keyLength + delimiter.length() : keyLength);
					if (!line.startsWith(" ")) { //$NON-NLS-1$
						currentKey = null;
					} 
				}  
				
				if (currentKey == null) {
					int index = line.indexOf(':');				
					String name = (index != -1) ? line.substring(0, index) : line;
					currentKey = (IDocumentKey)fDocumentHeaders.get(name);
					if (currentKey != null) {
						IRegion region = document.getLineInformation(i);
						currentKey.setOffset(region.getOffset());
						String delimiter = document.getLineDelimiter(i);
						currentKey.setLength(delimiter != null ? region.getLength() + delimiter.length() : region.getLength());
					}
				}
			}
		} catch (BadLocationException e) {
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String key, String value) {
		ManifestHeader header = (ManifestHeader)fDocumentHeaders.get(key);
        String old = header == null ? null : header.getValue();
		if (header == null) {
			header = createHeader(key, value);
		} else {
            header.setValue(value);
        }
		fDocumentHeaders.put(key, header);
		
		fModel.fireModelObjectChanged(header, key, old, value);
	}
    
    private ManifestHeader createHeader(String key, String value) {
        ManifestHeader header = null;
       	String newLine = TextUtilities.getDefaultLineDelimiter(fModel.getDocument());
        if (key.equals(Constants.EXPORT_PACKAGE) || key.equals(ICoreConstants.PROVIDE_PACKAGE)) {
 			header = new ExportPackageHeader(key, value, this, newLine);
        } else if (key.equals(Constants.IMPORT_PACKAGE)){
 			header = new ImportPackageHeader(key, value, this, newLine);
        } else if (key.equals(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)) {
        	header = new RequiredExecutionEnvironmentHeader(key, value, this, newLine);
        } else if (key.equals(ICoreConstants.ECLIPSE_LAZYSTART) || key.equals(ICoreConstants.ECLIPSE_AUTOSTART)) {
        	header = new LazyStartHeader(key, value, this, newLine);
        } else {
            header = new ManifestHeader(key, value, this, newLine);
        }
        return header;
    }
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#getHeader(java.lang.String)
	 */
	public String getHeader(String key) {
		ManifestHeader header = (ManifestHeader)fDocumentHeaders.get(key);
		return (header != null) ? header.getValue() : null;
	}
    
    public ManifestHeader getManifestHeader(String key) {
        return (ManifestHeader)fDocumentHeaders.get(key);
    }
	
	public Dictionary getHeaders() {
		return fDocumentHeaders;
	}

    public IBundleModel getModel() {
        return fModel;
    }

	public String getLocalization() {
		return getHeader(Constants.BUNDLE_LOCALIZATION);
	}

	public void setLocalization(String localization) {
		setHeader(Constants.BUNDLE_LOCALIZATION, localization);
	}
}
