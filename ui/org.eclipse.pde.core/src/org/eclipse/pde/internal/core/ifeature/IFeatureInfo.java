/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
 
package org.eclipse.pde.internal.core.ifeature;

import java.net.URL;
import org.eclipse.core.runtime.CoreException;
/**
 * @version 	1.0
 * @author
 */
public interface IFeatureInfo extends IFeatureObject {
	String P_URL = "p_url";
	String P_DESC = "p_desc";
	
	public URL getURL();
	public String getDescription();
	
	public void setURL(URL url) throws CoreException;
	public void setDescription(String desc) throws CoreException;
	
	public boolean isEmpty();
	public int getIndex();
}
