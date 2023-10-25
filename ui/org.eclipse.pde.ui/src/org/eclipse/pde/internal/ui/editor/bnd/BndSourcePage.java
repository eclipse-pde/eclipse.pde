/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.bnd;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.GenericSourcePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class BndSourcePage extends GenericSourcePage {

	public BndSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(new SourceViewerConfiguration() {
			private ContentAssistant fContentAssistant;

			@Override
			public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
				if (isEditable()) {
					if (fContentAssistant == null) {
						// Initialize in SWT thread before using in background
						// thread:
						PDEPluginImages.get(null);
						fContentAssistant = new ContentAssistant(true);
						fContentAssistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
						fContentAssistant.setContentAssistProcessor(new BndAutoCompleteProcessor(),
								IDocument.DEFAULT_CONTENT_TYPE);
						fContentAssistant.enableAutoInsert(true);
						fContentAssistant
								.setInformationControlCreator(parent -> new DefaultInformationControl(parent, false));
						fContentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
						fContentAssistant.enableAutoActivation(true);
					}
					return fContentAssistant;
				}
				return null;
			}
		});
	}

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	@Override
	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	@Override
	public void updateSelection(Object object) {

	}

}
