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

public interface IWindowImages extends IProductObject {
	
	public static final String P_LARGE = "large"; //$NON-NLS-1$
	public static final String P_SMALL = "small"; //$NON-NLS-1$
	
	String getLargeImagePath();
	String getSmallImagePath();
	
	void setLargeImagePath(String path);
	void setSmallImagePath(String path);

}
