package org.eclipse.pde.internal.preferences;

import java.util.*;
import org.eclipse.swt.widgets.*;
/**
 * Insert the type's description here.
 * Creation date: (5/14/2001 8:41:14 PM)
 * @author: Dejan Glozic
 */
public abstract class RuntimeOption {
	private String key;
	private String label;
/**
 * RuntimeOption constructor comment.
 */
public RuntimeOption(String key, String label) {
	this.key = key;
	this.label = label;
}

public abstract Control createControl(Composite parent);
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 8:42:13 PM)
 * @return java.lang.String
 */
public java.lang.String getKey() {
	return key;
}
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 8:42:13 PM)
 * @return java.lang.String
 */
public java.lang.String getLabel() {
	return label;
}
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 8:43:39 PM)
 * @param value java.lang.Object
 */
public abstract String getValue();
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 9:13:59 PM)
 */
public void load(Properties store) {
	Object value = store.getProperty(key);
	if (value != null)
		setValue(value);
}
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 8:43:39 PM)
 * @param value java.lang.Object
 */
public abstract void setValue(Object value);
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 9:14:06 PM)
 */
public void store(Properties store) {
	store.setProperty(getKey(), getValue());
}
}
