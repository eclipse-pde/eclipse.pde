/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;

public abstract class ManifestHeaderErrorResolution extends AbstractPDEMarkerResolution {	
	
	public ManifestHeaderErrorResolution(int type) {
		super(type);
	}

	protected AbstractEditingModel createModel(IDocument document) {
		return new BundleModel(document, true);
	}
	
	protected abstract void createChange(BundleModel model);
	
	protected void createChange(IBaseModel model) {
		if (model instanceof BundleModel)
			createChange((BundleModel)model);
	}

	protected IModelTextChangeListener createListener(IDocument doc) {
		return new BundleTextChangeListener(doc);
	}
}
