package org.eclipse.pde.internal.runtime.registry;

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
