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

import java.util.ArrayList;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class RequiredExecutionEnvironmentHeader extends CompositeManifestHeader {
    
    private static final long serialVersionUID = 1L;
    public static final int TOTAL_JRES = 7;
    public static final int TOTAL_J2MES = 2;
    public static final ArrayList JRES = new ArrayList(TOTAL_JRES);
    public static final ArrayList J2MES = new ArrayList(TOTAL_J2MES);
    static {
    	JRES.add("OSGi/Minimum-1.0"); //$NON-NLS-1$
    	JRES.add("OSGi/Minimum-1.1"); //$NON-NLS-1$
    	JRES.add("JRE-1.1"); //$NON-NLS-1$
    	JRES.add("J2SE-1.2"); //$NON-NLS-1$
    	JRES.add("J2SE-1.3"); //$NON-NLS-1$
    	JRES.add("J2SE-1.4"); //$NON-NLS-1$
    	JRES.add("J2SE-1.5"); //$NON-NLS-1$
    	
    	J2MES.add("CDC-1.0/Foundation-1.0"); //$NON-NLS-1$
    	J2MES.add("CDC-1.1/Foundation-1.1"); //$NON-NLS-1$
    }
    
    public static String[] getJRES() {
    	return (String[])JRES.toArray(new String[JRES.size()]);
    }
    public static String[] getJ2MES() {
    	return (String[])J2MES.toArray(new String[J2MES.size()]);
    }
    
    private PDEManifestElement fMinJRE;
    private PDEManifestElement fMinJ2ME;
    
    public RequiredExecutionEnvironmentHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
    
    protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, value);
			ArrayList extra = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				String name = elements[i].getValue();
				int index = JRES.indexOf(name);
				if (index == -1) {
					index = J2MES.indexOf(name);
					if (index > -1) {
						if (fMinJ2ME == null || index < J2MES.indexOf(fMinJ2ME.getValue()))
							fMinJ2ME = createElement(elements[i]);
					}
				} else if (fMinJRE == null || index < JRES.indexOf(fMinJRE.getValue())) {
					fMinJRE = createElement(elements[i]);
				}			
				if (index == -1)
					extra.add(createElement(elements[i]));
			}
			if (fMinJRE != null)
				addManifestElement(fMinJRE, false);
			if (fMinJ2ME != null)
				addManifestElement(fMinJ2ME, false);
			for (int i = 0; i < extra.size(); i++)
				addManifestElement((PDEManifestElement)extra.get(i), false);
		} catch (BundleException e) {
		}
    }
    
    public String getMinimumJRE() {
        return (fMinJRE != null) ? fMinJRE.getValue() : ""; //$NON-NLS-1$
    }
    
    public String getMinimumJ2ME() {
        return (fMinJ2ME != null) ? fMinJ2ME.getValue() : ""; //$NON-NLS-1$
    }
    
    public void updateJRE(String newValue) {
       	if (fMinJRE != null && newValue.equals(fMinJRE.getValue()))
    		return;
    	
    	if (newValue == null || newValue.length() == 0) {
    		if (fMinJRE != null) {
    			removeManifestElement(fMinJRE);
    			fMinJRE = null;
    		}
    	} else {
    		if (fMinJRE == null) {
    			fMinJRE = new PDEManifestElement(this, newValue);
    			addManifestElement(fMinJRE);
    		} else {
    			fMinJRE.setValue(newValue);
    		}
    	}
    	update(true);
    }
    
    public void updateJ2ME(String newValue) {
    	if (fMinJ2ME != null && newValue.equals(fMinJ2ME.getValue()))
    		return;
    	
    	if (newValue == null || newValue.length() == 0) {
    		if (fMinJ2ME != null) {
    			removeManifestElement(fMinJ2ME);
    			fMinJ2ME = null;
    		}
    	} else {
    		if (fMinJ2ME == null) {
    			fMinJ2ME = new PDEManifestElement(this, newValue);
    			addManifestElement(fMinJ2ME);
    		} else {
    			fMinJ2ME.setValue(newValue);
    		}
    	}
    	update(true);
    }
}
