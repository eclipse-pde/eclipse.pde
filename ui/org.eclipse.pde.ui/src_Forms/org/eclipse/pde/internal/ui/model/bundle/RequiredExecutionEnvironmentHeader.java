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

import java.util.ArrayList;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class RequiredExecutionEnvironmentHeader extends ManifestHeader {
    
    private static final long serialVersionUID = 1L;
    public static final int TOTAL_JRES = 7;
    public static final int TOTAL_J2MES = 2;
    public static final ArrayList JRES = new ArrayList(TOTAL_JRES);
    public static final ArrayList J2MES = new ArrayList(TOTAL_J2MES);
    static {
    	JRES.add("OSGi/Minimum-1.0");
    	JRES.add("OSGi/Minimum-1.1");
    	JRES.add("JRE-1.1");
    	JRES.add("J2SE-1.2");
    	JRES.add("J2SE-1.3");
    	JRES.add("J2SE-1.4");
    	JRES.add("J2SE-1.5");
    	
    	J2MES.add("CDC-1.0/Foundation-1.0");
    	J2MES.add("CDC-1.0/Foundation-1.1");
    }
    
    public static String[] getJRES() {
    	return (String[])JRES.toArray(new String[JRES.size()]);
    }
    public static String[] getJ2MES() {
    	return (String[])J2MES.toArray(new String[J2MES.size()]);
    }
    
    private String fMinJRE;
    private String fMinJ2ME;
    
    public RequiredExecutionEnvironmentHeader(String name, String value, IBundle bundle,
			String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
		processValue();
	}
    
    private void processValue() {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, fValue);
	        if (elements != null && elements.length > 0) {
	            for (int i = 0; i < TOTAL_JRES; i++) {
	            	for (int j = 0; j < elements.length; j++) {
	            		String value = elements[j].getValue();
	            		if (value.equals(JRES.get(i)) && fMinJRE == null)
	            			fMinJRE = value;
	            	}
	            }
	           	for (int i = 0; i < TOTAL_J2MES; i++) {
	            	for (int j = 0; j < elements.length; j++) {
	            		String value = elements[j].getValue();
	            		if (value.equals(J2MES.get(i)) && fMinJ2ME == null)
	            			fMinJ2ME = value;
	            	}
	            }
	        }
		} catch (BundleException e) {
		}
    }
    
    public String getMinimumJRE() {
        return fMinJRE;
    }
    
    public String getMinimumJ2ME() {
        return fMinJ2ME;
    }
    
    
    public String updateJRE(String newValue) {
    	if (newValue.equals(fMinJRE))
    		return fValue;
    	fMinJRE = newValue.equals("") ? null : newValue;
    	return getUpdatedValue();
    }
    
    public String updateJ2ME(String newValue) {
    	if (newValue.equals(fMinJ2ME))
    		return fValue;
    	fMinJ2ME = newValue.equals("") ? null : newValue;
    	return getUpdatedValue();
    }

	public String getUpdatedValue() {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, fValue);
			StringBuffer sb = new StringBuffer();
			ArrayList nonStandardElements = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				String value = elements[i].getValue();
				if (!J2MES.contains(value) && !JRES.contains(value))
					nonStandardElements.add(value);
			}
			for (int i = 0; i < nonStandardElements.size(); i++) {
				if (sb.length() > 0) sb.append(", ");
				sb.append((String)nonStandardElements.get(i));
			}
			if (fMinJRE != null) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(fMinJRE);
			}
			if (fMinJ2ME != null) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(fMinJ2ME);
			}
			fValue = sb.toString();
		} catch (BundleException e) {
		}
		return fValue;
	}
}
