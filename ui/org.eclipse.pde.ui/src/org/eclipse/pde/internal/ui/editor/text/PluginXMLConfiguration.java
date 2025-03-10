/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLContentAssistProcessor;

public class PluginXMLConfiguration extends XMLConfiguration {

	private ContentAssistant fContentAssistant;
	private XMLContentAssistProcessor fContentAssistProcessor;
	private PluginXMLTextHover fTextHover;

	public PluginXMLConfiguration(IColorManager colorManager, PDESourcePage page) {
		super(colorManager, page);
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (sourceViewer.isEditable() && fContentAssistant == null) {
			fContentAssistProcessor = new XMLContentAssistProcessor(fSourcePage);
			fContentAssistant = new ContentAssistant();
			fContentAssistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
			fContentAssistant.setContentAssistProcessor(fContentAssistProcessor, IDocument.DEFAULT_CONTENT_TYPE);
			fContentAssistant.setContentAssistProcessor(fContentAssistProcessor, XMLPartitionScanner.XML_TAG);
			fContentAssistant.setInformationControlCreator(getInformationControlCreator(true));
			fContentAssistant.setShowEmptyList(false);
			fContentAssistant.addCompletionListener(fContentAssistProcessor);
			fContentAssistant.enableAutoInsert(true);
		}
		return fContentAssistant;
	}

	@Override
	public void dispose() {
		if (fContentAssistProcessor != null) {
			fContentAssistProcessor.dispose();
		}
		super.dispose();
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if (fTextHover == null && fSourcePage != null) {
			fTextHover = new PluginXMLTextHover(fSourcePage);
		}
		return fTextHover;
	}

}
