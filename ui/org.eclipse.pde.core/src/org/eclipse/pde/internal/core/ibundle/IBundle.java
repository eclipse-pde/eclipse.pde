/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ibundle;

public interface IBundle {
	
	void setHeader(String key, String value);
	
	void renameHeader(String key, String newKey);
	
	String getHeader(String key);
	
	IManifestHeader getManifestHeader(String key);
    
    IBundleModel getModel();
    
    String getLocalization();
    
    void setLocalization(String localization);
    
}
