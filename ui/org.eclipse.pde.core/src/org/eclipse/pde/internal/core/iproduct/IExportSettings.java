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
package org.eclipse.pde.internal.core.iproduct;


public interface IExportSettings extends IProductObject {
	
	public static final String P_LAST_ROOT = "lastRoot"; //$NON-NLS-1$
	public static final String P_LAST_DEST = "lastDest"; //$NON-NLS-1$
	public static final String P_DIRECTORY_DEST = "dirDest"; //$NON-NLS-1$
	
	void setLastRoot(String path);
	
	String getLastRoot();
	
	void setLastDest(String path);
	
	String getLastDest();
	
	void setIsDirectory(boolean isDir);
	
	boolean isDirectory();
}
