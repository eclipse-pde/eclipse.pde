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
package org.eclipse.pde.internal.ui.model.bundle;

import java.io.*;
import java.util.jar.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.model.*;
import org.osgi.framework.*;

public class BundleModel extends AbstractEditingModel implements IBundleModel {

	private IBundle fBundle;
	/**
	 * @param document
	 * @param isReconciling
	 */
	public BundleModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
		fBundle = new Bundle(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundleModel#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return getBundle().getHeader(Constants.FRAGMENT_HOST) != null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fLoaded = true;
			((Bundle)getBundle()).clearHeaders();
			((Bundle)getBundle()).load(new Manifest(source));
		} catch (IOException e) {
			fLoaded = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#adjustOffsets(org.eclipse.jface.text.IDocument)
	 */
	protected void adjustOffsets(IDocument document) {
		((Bundle)getBundle()).clearOffsets();
		((Bundle)getBundle()).adjustOffsets(document);
		((Bundle)getBundle()).trim();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundleModel#getBundle()
	 */
	public IBundle getBundle() {
		return fBundle;
	}
	
}
