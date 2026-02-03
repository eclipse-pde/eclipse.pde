/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.p2inf;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;

public class P2InfModel implements IBaseModel, IModelChangeProvider {

	private final IDocument document;
	private volatile boolean valid;
	private volatile boolean disposed;

	public P2InfModel(IDocument document) {
		this.document = document;
	}

	public void load() {
		valid = document != null;
	}

	public IDocument getDocument() {
		return document;
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
		if (adapter == IDocument.class) {
			return adapter.cast(document);
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