/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.model.*;


public class BuildModel extends AbstractEditingModel implements IBuildModel {
	
	//private Properties fProperties;
	private BuildModelFactory fFactory;
	private Build fBuild;

	/**
	 * @param document
	 * @param isReconciling
	 */
	public BuildModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fLoaded = true;
			((Build)getBuild()).load(source);
		} catch (IOException e) {
			fLoaded = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#adjustOffsets(org.eclipse.jface.text.IDocument)
	 */
	protected void adjustOffsets(IDocument document) {
		((Build)getBuild()).adjustOffsets(document);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getBuild()
	 */
	public IBuild getBuild() {
		if (fBuild == null)
			fBuild = new Build(this);
		return fBuild;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getFactory()
	 */
	public IBuildModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new BuildModelFactory(this);
		return fFactory;
	}
}
