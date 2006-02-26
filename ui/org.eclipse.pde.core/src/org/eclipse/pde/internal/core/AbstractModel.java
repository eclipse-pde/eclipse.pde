/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.ModelChangedEvent;
import org.xml.sax.SAXException;

public abstract class AbstractModel
	extends PlatformObject
	implements IModel, IModelChangeProviderExtension, Serializable {
	private transient List listeners;
	private boolean loaded;
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
	
	public NLResourceHelper getNLResourceHelper() {
		if (nlHelper == null)
			nlHelper = createNLResourceHelper();
		return nlHelper;
	}
	
	public void resetNLResourceHelper() {
		nlHelper = null;
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
				"Error in the manifest file", //$NON-NLS-1$
				e);
		throw new CoreException(status);
	}
	
	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError  {
		return SAXParserFactory.newInstance().newSAXParser();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isReconcilingModel()
	 */
	public boolean isReconcilingModel() {
		return false;
	}

}
