/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
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
