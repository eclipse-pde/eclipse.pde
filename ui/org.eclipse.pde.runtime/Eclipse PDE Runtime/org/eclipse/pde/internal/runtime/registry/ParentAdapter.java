package org.eclipse.pde.internal.runtime.registry;

public abstract class ParentAdapter extends PluginObjectAdapter {
	Object [] children;

public ParentAdapter(Object object) {
	super(object);
}
protected abstract Object[] createChildren();
public Object[] getChildren() {
	if (children==null) children = createChildren();
	return children;
}
}
