/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class ManifestConfiguration extends TextSourceViewerConfiguration {

	private AbstractManifestScanner fManifestNameScanner;
	private AbstractManifestScanner fManifestValueScanner;
	private IColorManager fColorManager;
	
	public ManifestConfiguration(IColorManager colorManager, IPreferenceStore store) {
		super(store);
		fColorManager = colorManager;
		initializeScanners();
	}


	private void initializeScanners() {
		fManifestNameScanner = new ManifestHeaderScanner(fColorManager, fPreferenceStore);
		fManifestValueScanner = new ManifestValueScanner(fColorManager, fPreferenceStore);
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(fManifestNameScanner);
		reconciler.setDamager(dr, ManifestPartitionScanner.HEADER_NAME);
		reconciler.setRepairer(dr, ManifestPartitionScanner.HEADER_NAME);
		
		dr = new DefaultDamagerRepairer(fManifestValueScanner);
		reconciler.setDamager(dr, ManifestPartitionScanner.HEADER_VALUE);
		reconciler.setRepairer(dr, ManifestPartitionScanner.HEADER_VALUE);

		return reconciler;
	}

	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		fColorManager.handlePropertyChangeEvent(event);
		if (fManifestNameScanner.affectsBehavior(event))
			fManifestNameScanner.adaptToPreferenceChange(event);
		if (fManifestValueScanner.affectsBehavior(event))
			fManifestValueScanner.adaptToPreferenceChange(event);
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return ManifestPartitionScanner.PARTITIONS;
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return ManifestPartitionScanner.MANIFEST_FILE_PARTITIONING;
	}
	
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return fManifestNameScanner.affectsBehavior(event)
			|| fManifestValueScanner.affectsBehavior(event);
	}

}

