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
 
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.*;
/**
 * @version 	1.0
 * @author
 */
public interface IFeatureInfo extends IFeatureObject {
	String P_URL = "p_url"; //$NON-NLS-1$
	String P_DESC = "p_desc"; //$NON-NLS-1$
	
	public String getURL();
	public String getDescription();
	
	public void setURL(String url) throws CoreException;
	public void setDescription(String desc) throws CoreException;
	
	public boolean isEmpty();
	public int getIndex();
}
