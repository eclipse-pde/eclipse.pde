package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;

public class PluginObjectAdapter extends PlatformObject {
	private Object object;

public PluginObjectAdapter(Object object) {
	this.object = object;
}
public Object getObject() {
	return object;
}
}
