/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.pde.internal.ui.editor.XMLConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;

/**
 * ManifestConfiugration.java
 */
public class XMLViewerConfiguration extends XMLConfiguration {

	private ManifestSourcePageNew fPage;

	public XMLViewerConfiguration(ManifestSourcePageNew page, IColorManager colorManager) {
		super(colorManager);
		fPage= page;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		IReconciler result;
		if (fPage == null) {
			result= super.getReconciler(sourceViewer);
		} else {
			result= new NotifyingReconciler(new XMLReconcilingStrategy(fPage), false);
			((NotifyingReconciler)result).setDelay(500);
		}
		return result;
	}
}
