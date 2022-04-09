/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class XMLEditingModel extends AbstractEditingModel {

	private IStatus status;

	public XMLEditingModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	@Override
	public void load(InputStream source, boolean outOfSync) {
		try {
			fLoaded = true;
			status = Status.OK_STATUS;
			SAXParserWrapper.parse(source, createDocumentHandler(this, true));
		} catch (SAXException e) {
			fLoaded = false;
			status = Status.error(e.getMessage(), e);
		} catch (IOException | ParserConfigurationException | FactoryConfigurationError e) {
			fLoaded = false;
		}
	}

	// TODO move this later when we re-work the text editing model
	public IStatus getStatus() {
		return status;
	}

	protected abstract DefaultHandler createDocumentHandler(IModel model, boolean reconciling);

	@Override
	public void adjustOffsets(IDocument document) {
		try {
			SAXParserWrapper.parse(getInputStream(document), createDocumentHandler(this, false));
		} catch (SAXException | IOException | ParserConfigurationException | FactoryConfigurationError e) {
		}
	}

	private boolean isResourceFile() {
		if (getUnderlyingResource() == null) {
			return false;
		} else if ((getUnderlyingResource() instanceof IFile) == false) {
			return false;
		}
		return true;
	}

	public void save() {
		if (isResourceFile() == false) {
			return;
		}
		IFile file = (IFile) getUnderlyingResource();
		String contents = getContents();
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
				file.create(stream, false, null);
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
		}
	}

	public void reload() {
		if (isResourceFile() == false) {
			return;
		}
		IFile file = (IFile) getUnderlyingResource();
		// Underlying file has to exist in order to reload the model
		if (file.exists()) {
			InputStream stream = null;
			try {
				// Get the file contents
				stream = new BufferedInputStream(file.getContents(true));
				// Load the model using the last saved file contents
				reload(stream, false);
				// Remove the dirty (*) indicator from the editor window
				setDirty(false);
			} catch (CoreException e) {
				// Ignore
			}
		}
	}

	public void reload(IDocument document) {
		// Get the document's text
		String text = document.get();
		InputStream stream = null;

		try {
			// Turn the document's text into a stream
			stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
			// Reload the model using the stream
			reload(stream, false);
			// Remove the dirty (*) indicator from the editor window
			setDirty(false);
		} catch (CoreException e) {
			// Ignore
		}
	}

	public String getContents() {
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			setLoaded(true);
			save(writer);
			writer.flush();
			return swriter.toString();
		} catch (IOException e) {
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			getRoot().write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		// by default, don't create one
		return null;
	}

	protected abstract IWritable getRoot();
}