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
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.xml.sax.*;

public abstract class AbstractModel
	extends PlatformObject
	implements IModel, IModelChangeProviderExtension, Serializable {
	private static final String KEY_ERROR = "Errors.modelError"; //$NON-NLS-1$
	private transient List listeners;
	protected boolean loaded;
	protected transient NLResourceHelper nlHelper;
	protected boolean disposed;
	private long timeStamp;

	public AbstractModel() {
		super();
		listeners = Collections.synchronizedList(new ArrayList());
	}
	public void addModelChangedListener(IModelChangedListener listener) {
		listeners.add(listener);
	}
	public void transferListenersTo(IModelChangeProviderExtension target, IModelChangedListenerFilter filter) {
		ArrayList removed=new ArrayList();
		for (int i=0; i<listeners.size(); i++) {
			IModelChangedListener listener = (IModelChangedListener)listeners.get(i);
			if (filter==null || filter.accept(listener)) {
				target.addModelChangedListener(listener);
				removed.add(listener);
			}
		}
		listeners.removeAll(removed);
	}

	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}
	public void dispose() {
		if (nlHelper != null) {
			nlHelper.dispose();
			nlHelper = null;
		}
		disposed = true;
	}
	public void fireModelChanged(IModelChangedEvent event) {
		IModelChangedListener [] list = (IModelChangedListener[])listeners.toArray(new IModelChangedListener[listeners.size()]);
		for (int i=0; i<list.length; i++) {
			IModelChangedListener listener = list[i];
			listener.modelChanged(event);
		}
	}

	public void fireModelObjectChanged(
		Object object,
		String property,
		Object oldValue,
		Object newValue) {
		fireModelChanged(
			new ModelChangedEvent(this, object, property, oldValue, newValue));
	}

	public String getResourceString(String key) {
		if (nlHelper == null) {
			nlHelper = createNLResourceHelper();
		}
		if (nlHelper == null)
			return key;
		if (key == null)
			return ""; //$NON-NLS-1$
		return nlHelper.getResourceString(key);
	}
	public IResource getUnderlyingResource() {
		return null;
	}

	protected boolean isInSync(File localFile) {
		return localFile.exists() && localFile.lastModified() == getTimeStamp();
	}
	
	public boolean isValid() {
		return !isDisposed() && isLoaded();
	}

	public final long getTimeStamp() {
		return timeStamp;
	}

	protected abstract void updateTimeStamp();

	protected void updateTimeStamp(File localFile) {
		if (localFile.exists())
			this.timeStamp = localFile.lastModified();
	}

	public boolean isDisposed() {
		return disposed;
	}
	public boolean isLoaded() {
		return loaded;
	}
	
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public void removeModelChangedListener(IModelChangedListener listener) {
		listeners.remove(listener);
	}

	public void throwParseErrorsException(Throwable e) throws CoreException {
		Status status =
			new Status(
				IStatus.ERROR,
				PDECore.getPluginId(),
				IStatus.OK,
				PDECore.getResourceString(KEY_ERROR),
				e);
		throw new CoreException(status);
	}
	
	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError  {
		return SAXParserFactory.newInstance().newSAXParser();
	}

}
