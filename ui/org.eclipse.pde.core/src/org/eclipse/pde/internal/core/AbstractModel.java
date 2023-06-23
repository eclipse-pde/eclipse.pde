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
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.util.XmlParserFactory;
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

		String lineSeparator = System.lineSeparator();
		if (lineDelimiter.equals(lineSeparator)) {
			return string;
		}

		return string.replace(lineSeparator, lineDelimiter);
	}

	public AbstractModel() {
		fListeners = Collections.synchronizedList(new ArrayList<>());
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

	final protected boolean isResourceInSync() {
		Long timeStamp = getResourceTimeStamp();
		return timeStamp != null && timeStamp.longValue() == getTimeStamp();
	}

	protected Long getResourceTimeStamp() {
		IResource underlyingResource = getUnderlyingResource();
		if (underlyingResource == null) {
			return null;
		} else if (underlyingResource.getLocation() == null) {
			// If we have no underlying resource, it probably got deleted from
			// right
			// underneath us; thus, the model is not in sync
			return null;
		}
		long modificationStamp = underlyingResource.getModificationStamp();
		return modificationStamp == IResource.NULL_STAMP ? null : modificationStamp;
	}

	@Override
	public boolean isInSync() {
		return isResourceInSync();
	}

	@Override
	public boolean isValid() {
		return !isDisposed() && isLoaded();
	}

	@Override
	public final long getTimeStamp() {
		return fTimestamp;
	}

	static protected Long getTimeStamp(File localFile) {
		try {
			long lastModified = Files.getLastModifiedTime(localFile.toPath()).toMillis();
			return lastModified;
		} catch (IOException e) {
			return null; // NoSuchFileException -> does not exist
		}
	}

	protected final void updateTimeStampWith(Long resourceTimeStamp) {
		// If we have no underlying resource, it probably got deleted from right
		// underneath us; thus, there is nothing to update the time stamp for
		if (resourceTimeStamp != null) {
			fTimestamp = resourceTimeStamp;
		}
	}

	protected void updateTimeStamp() {
		updateTimeStampFromResource();
	}

	protected void updateTimeStampFromResource() {
		updateTimeStampWith(getResourceTimeStamp());
	}

	protected void updateTimeStamp(File localFile) {
		updateTimeStampWith(getTimeStamp(localFile));
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
		throw new CoreException(Status.error("Error in the manifest file", e)); //$NON-NLS-1$
	}

	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
		return XmlParserFactory.createSAXParserWithErrorOnDOCTYPE();
	}

	@Override
	public boolean isReconcilingModel() {
		return false;
	}

}
