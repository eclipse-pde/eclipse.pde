package org.eclipse.pde.internal.core.bundle;

import java.util.*;
import org.eclipse.pde.internal.core.ibundle.*;

/**
 * @author melhem
 *
 */
public class Bundle extends BundleObject implements IBundle {
	private Properties fProperties;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String key, String value) {
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
		return (String)fProperties.get(key);
	}
	
	public void load(Properties properties) {
		fProperties = properties;
	}
}
