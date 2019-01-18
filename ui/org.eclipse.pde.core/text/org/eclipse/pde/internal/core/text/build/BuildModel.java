/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text.build;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;

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

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	@Override
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fLoaded = true;
			((Build) getBuild()).load(source);
		} catch (IOException e) {
			fLoaded = false;
		}
	}

	@Override
	public void adjustOffsets(IDocument document) {
		((Build) getBuild()).adjustOffsets(document);
	}

	@Override
	public IBuild getBuild() {
		if (fBuild == null) {
			fBuild = new Build(this);
		}
		return fBuild;
	}

	@Override
	public IBuildModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new BuildModelFactory(this);
		}
		return fFactory;
	}
}