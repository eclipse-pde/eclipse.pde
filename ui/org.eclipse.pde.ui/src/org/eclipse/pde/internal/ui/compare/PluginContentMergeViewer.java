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

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

public class PluginContentMergeViewer extends TextMergeViewer {

	private IColorManager fColorManager;
	private ArrayList<IPropertyChangeListener> fPropertyChangedListeners;

	public PluginContentMergeViewer(Composite parent, CompareConfiguration config) {
		super(parent, SWT.LEFT_TO_RIGHT, config);
	}

	@Override
	protected void configureTextViewer(final TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			if (fColorManager == null)
				fColorManager = ColorManager.getDefault();
			final XMLConfiguration configuration = new XMLConfiguration(fColorManager);
			textViewer.getControl().addDisposeListener(e -> configuration.dispose());
			IPropertyChangeListener propertyChangedListener = event -> {
				// the configuration will test if the properties affect the presentation also,
				// but checking it here allows us to prevent the viewer from being invalidated
				// and saves some unnecessary work
				if (configuration.affectsColorPresentation(event) || configuration.affectsTextPresentation(event)) {
					configuration.adaptToPreferenceChange(event);
					textViewer.invalidateTextPresentation();
				}
			};
			PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangedListener);

			if (fPropertyChangedListeners == null)
				fPropertyChangedListeners = new ArrayList<>(3);
			fPropertyChangedListeners.add(propertyChangedListener);
			((SourceViewer) textViewer).configure(configuration);
			Font font = JFaceResources.getFont(PluginContentMergeViewer.class.getName());
			if (font != null)
				((SourceViewer) textViewer).getTextWidget().setFont(font);
		}
	}

	@Override
	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new XMLPartitionScanner(), XMLPartitionScanner.PARTITIONS);
	}

	@Override
	protected String getDocumentPartitioning() {
		return XMLDocumentSetupParticpant.XML_PARTITIONING;
	}

	@Override
	public String getTitle() {
		return PDEUIMessages.PluginContentMergeViewer_title;
	}

	@Override
	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		if (fColorManager != null)
			fColorManager.dispose();
		if (fPropertyChangedListeners != null) {
			Iterator<IPropertyChangeListener> iter = fPropertyChangedListeners.iterator();
			while (iter.hasNext())
				PDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(iter.next());
			fPropertyChangedListeners = null;
		}
	}
}
