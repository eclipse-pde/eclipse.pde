/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.compare;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.ManifestConfiguration;
import org.eclipse.pde.internal.ui.editor.text.ManifestPartitionScanner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

public class ManifestContentMergeViewer extends TextMergeViewer {

	private IColorManager fColorManager;
	private Set<ManifestConfiguration> manifestConfigurations;

	public ManifestContentMergeViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, SWT.LEFT_TO_RIGHT, configuration);
		manifestConfigurations = Collections.newSetFromMap(new IdentityHashMap<>());
	}

	@Override
	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			if (fColorManager == null) {
				fColorManager = ColorManager.getDefault();
			}
			ManifestConfiguration manifestConfiguration = new ManifestConfiguration(fColorManager, null,
					getDocumentPartitioning());
			manifestConfigurations.add(manifestConfiguration);
			((SourceViewer) textViewer).configure(manifestConfiguration);

			Font font = JFaceResources.getFont(ManifestContentMergeViewer.class.getName());
			if (font != null) {
				((SourceViewer) textViewer).getTextWidget().setFont(font);
			}
		}
	}

	@Override
	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new ManifestPartitionScanner(), ManifestPartitionScanner.PARTITIONS);
	}

	@Override
	protected String getDocumentPartitioning() {
		return ManifestPartitionScanner.MANIFEST_FILE_PARTITIONING;
	}

	@Override
	public String getTitle() {
		return PDEUIMessages.ManifestContentMergeViewer_title;
	}

	@Override
	protected void handleDispose(DisposeEvent event) {
		if (fColorManager != null) {
			fColorManager.dispose();
		}
		manifestConfigurations.forEach(c -> c.dispose());
		manifestConfigurations.clear();
		super.handleDispose(event);
	}

}
