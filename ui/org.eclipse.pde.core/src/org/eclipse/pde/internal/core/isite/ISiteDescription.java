/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.*;

public interface ISiteDescription extends ISiteObject {
	String P_URL = "url"; //$NON-NLS-1$
	String P_TEXT = "text"; //$NON-NLS-1$
	
	String getURL();
	String getText();
	
	void setURL(String url) throws CoreException;
	void setText(String text) throws CoreException;

}
