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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.model.*;

public abstract class KeyValueSourcePage extends PDESourcePage {

	class KeyValueSourceViewerConfiguration extends SourceViewerConfiguration {
		private AnnotationHover fAnnotationHover;
		public IReconciler getReconciler(ISourceViewer sourceViewer) {
			ReconcilingStrategy strategy = new ReconcilingStrategy();
			strategy.addParticipant((IReconcilingParticipant) getInputContext()
					.getModel());
			strategy.addParticipant((SourceOutlinePage)getContentOutline());
			MonoReconciler reconciler = new MonoReconciler(strategy, false);
			reconciler.setDelay(500);
			return reconciler;
		}
		public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
			if (fAnnotationHover == null)
				fAnnotationHover = new AnnotationHover();
			return fAnnotationHover;
		}
	}
	public KeyValueSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(new KeyValueSourceViewerConfiguration());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createViewerSorter()
	 */
	protected ViewerSorter createDefaultOutlineSorter() {
		return new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IDocumentKey key1 = (IDocumentKey)e1;
				IDocumentKey key2 = (IDocumentKey)e2;
				return key1.getOffset() < key2.getOffset() ? -1 : 1;
			}
		};
	}
	
	protected void outlineSelectionChanged(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IDocumentKey) {
				setHighlightRange((IDocumentKey)first);				
			} else {
				resetHighlightRange();
			}
		}
	}
	
	public void setHighlightRange(IDocumentKey key) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset = key.getOffset();
		int length = key.getLength();
		setHighlightRange(offset, length, true);
		sourceViewer.setSelectedRange(offset, key.getName().length());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	protected ViewerSorter createOutlineSorter() {
		return new ViewerSorter();
	}

}
