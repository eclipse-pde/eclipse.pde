package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.IModel;
import org.eclipse.pde.internal.base.model.IModelChangeProvider;
import org.eclipse.pde.internal.base.model.IModelChangedEvent;
import org.eclipse.pde.internal.base.model.IModelChangedListener;
import org.eclipse.pde.internal.base.model.ModelChangedEvent;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.*;
import org.eclipse.core.runtime.PlatformObject;

public abstract class AbstractModel extends PlatformObject implements IModel, IModelChangeProvider {
	private Vector listeners = new Vector();
	protected boolean loaded;
	protected NLResourceHelper nlHelper;
	protected boolean disposed;

public AbstractModel() {
	super();
}
public void addModelChangedListener(IModelChangedListener listener) {
	listeners.add(listener);
}
protected NLResourceHelper createNLResourceHelper() {
	return null;
}
public void dispose() {
	if (nlHelper!=null) {
		nlHelper.dispose();
		nlHelper = null;
	}
	disposed = true;
}
public void fireModelChanged(IModelChangedEvent event) {
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
		IModelChangedListener listener = (IModelChangedListener) iter.next();
		listener.modelChanged(event);
	}
}

public void fireModelObjectChanged(Object object, String property, Object oldValue, Object newValue) {
	fireModelChanged(
		new ModelChangedEvent(object, property, oldValue, newValue));
}

public String getResourceString(String key) {
	if (nlHelper==null) {
		nlHelper = createNLResourceHelper();
	}
	if (nlHelper==null) return key;
	if (key==null) return "";
	return nlHelper.getResourceString(key);
}
public IResource getUnderlyingResource() {
	return null;
}
public boolean isDisposed() {
	return disposed;
}
public boolean isLoaded() {
	return loaded;
}
public void release() {}
public void removeModelChangedListener(IModelChangedListener listener) {
	listeners.remove(listener);
}
}
