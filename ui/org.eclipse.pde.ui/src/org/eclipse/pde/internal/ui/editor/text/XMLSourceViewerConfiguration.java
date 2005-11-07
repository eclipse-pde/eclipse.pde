/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;

public class XMLSourceViewerConfiguration extends XMLConfiguration {
	private XMLSourcePage fSourcePage;
	private MonoReconciler fReconciler;

	public XMLSourceViewerConfiguration(XMLSourcePage page, IColorManager colorManager) {
		super(colorManager);
		fSourcePage = page;
	}
	
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fReconciler == null) {
			IBaseModel model = fSourcePage.getInputContext().getModel();
			if (model instanceof IReconcilingParticipant) {
				ReconcilingStrategy strategy = new ReconcilingStrategy();
				strategy.addParticipant((IReconcilingParticipant)model);
				if (fSourcePage.getContentOutline() instanceof IReconcilingParticipant)
					strategy.addParticipant((IReconcilingParticipant)fSourcePage.getContentOutline());
				fReconciler = new MonoReconciler(strategy, false);
				fReconciler.setDelay(500);
			}
		}
		return fReconciler;
	}

}
