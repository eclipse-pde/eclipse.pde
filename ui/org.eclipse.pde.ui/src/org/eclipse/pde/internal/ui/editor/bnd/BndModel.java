/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.bnd;

import java.io.IOException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.bnd.BndDocument;

import aQute.bnd.build.model.BndEditModel;

/**
 * Extension to a bnd edit model that implements the base model methods and
 * maintain the document to load/store changes
 */
public class BndModel extends BndEditModel implements IBaseModel, IModelChangeProvider {

	private final BndDocument bndDocument;
	private volatile boolean valid;
	private volatile boolean disposed;

	public BndModel(IDocument document) {
		bndDocument = new BndDocument(document);
	}

	@Override
	public void load() {
		try {
			loadFrom(bndDocument);
			valid = true;
		} catch (IOException e) {
			valid = false;
		}
	}

	@Override
	public void saveChanges() throws IOException {
		super.saveChangesTo(bndDocument);
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	@Override
	public void dispose() {
		disposed = true;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == aQute.bnd.properties.IDocument.class) {
			return adapter.cast(bndDocument);
		}
		return null;
	}

	@Override
	public void addModelChangedListener(IModelChangedListener listener) {

	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {

	}

	@Override
	public void fireModelObjectChanged(Object object, String property, Object oldValue, Object newValue) {

	}

	@Override
	public void removeModelChangedListener(IModelChangedListener listener) {

	}

}
