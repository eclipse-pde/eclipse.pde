/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.util.*;
import org.eclipse.pde.internal.core.ibundle.*;

public class Bundle extends BundleObject implements IBundle {
    private static final long serialVersionUID = 1L;
    private Properties fProperties;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String key, String value) {
        if (fProperties == null)
            return;
		Object oldValue = fProperties.get(key);
		if (value == null || value.trim().length() == 0)
			fProperties.remove(key);
		else 
			fProperties.put(key, value);
		getModel().fireModelObjectChanged(this, key, oldValue, value);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#getHeader(java.lang.String)
	 */
	public String getHeader(String key) {
		if (fProperties == null) {
			return null;
		}
		return (String)fProperties.get(key);
	}
	
	public void load(Properties properties) {
		fProperties = properties;
	}
}
