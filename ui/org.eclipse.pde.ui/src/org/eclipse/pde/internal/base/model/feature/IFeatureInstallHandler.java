/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.base.model.feature;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
/**
 * @version 	1.0
 * @author
 */
public interface IFeatureInstallHandler extends IFeatureObject {
	String P_URL = "url";
	String P_LIBRARY = "library";
	String P_CLASS_NAME = "className";

	public URL getURL();
	public String getLibrary();
	public String getClassName();
	
	public void setURL(URL url) throws CoreException;
	public void setLibrary(String library) throws CoreException;
	public void setClassName(String className) throws CoreException;
}
