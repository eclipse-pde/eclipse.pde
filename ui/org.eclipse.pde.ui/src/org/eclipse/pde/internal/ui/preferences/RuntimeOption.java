package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.swt.widgets.*;
/**
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
 * @return java.lang.String
 */
public java.lang.String getKey() {
	return key;
}
/**
 * @return java.lang.String
 */
public java.lang.String getLabel() {
	return label;
}
/**
 * @param value java.lang.Object
 */
public abstract String getValue();
/**
 */
public void load(Properties store) {
	Object value = store.getProperty(key);
	if (value != null)
		setValue(value);
}
/**
 * @param value java.lang.Object
 */
public abstract void setValue(Object value);
/**
 */
public void store(Properties store) {
	store.setProperty(getKey(), getValue());
}
}
