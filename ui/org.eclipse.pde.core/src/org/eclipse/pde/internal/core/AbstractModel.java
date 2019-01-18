/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
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
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.ModelChangedEvent;
import org.xml.sax.SAXException;

public abstract class AbstractModel extends PlatformObject implements IModel, IModelChangeProviderExtension, Serializable {

	private static final long serialVersionUID = 1L;

	private transient List<IModelChangedListener> fListeners;

	private boolean fLoaded;

	protected boolean fDisposed;

	private long fTimestamp;

	private Exception fException;

	protected static String getLineDelimiterPreference(IFile file) {
		if (file != null) {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
			if (buffer != null) {
				return TextUtilities.getDefaultLineDelimiter(buffer.getDocument());
			}
		}
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null,
				null);
	}

	/**
	 * Replaces all line delimiters to the same characters based on preference settings.  If no project
	 * or workspace preference has been set then the string will not be modified.  If the
	 * delimiter matches the current system setting, the string will not be modified.
	 *
	 * @param string the string to replace line delimiters in
	 * @param file the file to lookup specific project preference settings for, can be <code>null</code> to use workspace settings
	 * @return the provided string with line delimiters replaced
	 */
	public static String fixLineDelimiter(String string, IFile file) {
		String lineDelimiter = getLineDelimiterPreference(file);
		if (lineDelimiter == null) {
			return string;
		}

		String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
		if (lineDelimiter.equals(lineSeparator)) {
			return string;
		}

		return string.replace(lineSeparator, lineDelimiter);
	}

	public AbstractModel() {
		fListeners = Collections.synchronizedList(new ArrayList<IModelChangedListener>());
	}

	@Override
	public void addModelChangedListener(IModelChangedListener listener) {
		fListeners.add(listener);
	}

	@Override
	public void transferListenersTo(IModelChangeProviderExtension target, IModelChangedListenerFilter filter) {
		ArrayList<IModelChangedListener> removed = new ArrayList<>();
		for (int i = 0; i < fListeners.size(); i++) {
			IModelChangedListener listener = fListeners.get(i);
			if (filter == null || filter.accept(listener)) {
				target.addModelChangedListener(listener);
				removed.add(listener);
			}
		}
		fListeners.removeAll(removed);
	}

	@Override
	public void dispose() {
		fDisposed = true;
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		IModelChangedListener[] list = fListeners.toArray(new IModelChangedListener[fListeners.size()]);
		for (IModelChangedListener listener : list) {
			listener.modelChanged(event);
		}
	}

	@Override
	public void fireModelObjectChanged(Object object, String property, Object oldValue, Object newValue) {
		fireModelChanged(new ModelChangedEvent(this, object, property, oldValue, newValue));
	}

	@Override
	public String getResourceString(String key) {
		return key;
	}

	@Override
	public IResource getUnderlyingResource() {
		return null;
	}

	protected boolean isInSync(File localFile) {
		return localFile.exists() && localFile.lastModified() == getTimeStamp();
	}

	@Override
	public boolean isValid() {
		return !isDisposed() && isLoaded();
	}

	@Override
	public final long getTimeStamp() {
		return fTimestamp;
	}

	protected abstract void updateTimeStamp();

	protected void updateTimeStamp(File localFile) {
		if (localFile.exists()) {
			fTimestamp = localFile.lastModified();
		}
	}

	@Override
	public boolean isDisposed() {
		return fDisposed;
	}

	@Override
	public boolean isLoaded() {
		return fLoaded;
	}

	public void setLoaded(boolean loaded) {
		fLoaded = loaded;
	}

	public void setException(Exception e) {
		fException = e;
	}

	public Exception getException() {
		return fException;
	}

	@Override
	public void removeModelChangedListener(IModelChangedListener listener) {
		fListeners.remove(listener);
	}

	public void throwParseErrorsException(Throwable e) throws CoreException {
		Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, "Error in the manifest file", //$NON-NLS-1$
				e);
		throw new CoreException(status);
	}

	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
		return SAXParserFactory.newInstance().newSAXParser();
	}

	@Override
	public boolean isReconcilingModel() {
		return false;
	}

}
