/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.parts;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class PDESourceViewer {

	private static XMLConfiguration fSourceConfiguration = null;

	private static IColorManager fColorManager = null;

	private static int fSourceViewerCount = 0;

	private SourceViewer fViewer;

	private PDEFormPage fPage;

	private IDocument fDocument;

	public PDESourceViewer(PDEFormPage page) {
		// Create the underlying document
		fDocument = new Document();
		fPage = page;
	}

	public IDocument getDocument() {
		return fDocument;
	}

	public SourceViewer getViewer() {
		return fViewer;
	}

	private XMLConfiguration getConfiguration() {
		if (fSourceConfiguration == null) {
			// Get the color manager
			fColorManager = ColorManager.getDefault();
			// Create the source configuration
			fSourceConfiguration = new XMLConfiguration(fColorManager);
		}
		return fSourceConfiguration;
	}

	/**
	 * Utility method for creating a field for syntax highlighting
	 * @param parent
	 * @param heightHint
	 * @param widthHint
	 */
	public void createUI(Composite parent, int heightHint, int widthHint) {
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = heightHint;
		data.widthHint = widthHint;
		createUI(parent, data);
	}

	public void createUI(Composite parent, GridData data) {
		// Create the source viewer
		int style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
		fViewer = new SourceViewer(parent, null, style);
		// Configure the source viewer
		fViewer.configure(getConfiguration());
		// Setup the underlying document
		IDocumentSetupParticipant participant = new XMLDocumentSetupParticpant();
		participant.setup(fDocument);
		// Set the document on the source viewer
		fViewer.setDocument(fDocument);
		// Configure the underlying styled text widget
		configureUIStyledText(data, fViewer.getTextWidget());
		// Create style text listeners
		createUIListenersStyledText(fViewer.getTextWidget());
	}

	public void createUIListeners() {
		// Ensure the viewer was created
		if (fViewer == null) {
			return;
		}
		// Create source viewer listeners
		// Create selection listener
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fPage.getPDEEditor().setSelection(event.getSelection());
			}
		});
		// Create focus listener
		fViewer.getTextWidget().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				fPage.getPDEEditor().getContributor().updateSelectableActions(null);
			}
		});
	}

	private void createUIListenersStyledText(StyledText textWidget) {
		// Track the number of source viewers created
		fSourceViewerCount++;
		// The color manager and source viewer configuration should be disposed
		// When the last source viewer is diposed, dispose of the color manager
		// and source viewer configuration
		textWidget.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fSourceViewerCount--;
				if (fSourceViewerCount == 0) {
					dispose();
				}
			}
		});
	}

	private void dispose() {
		// TODO: MP: CompCS: Profile Sleek when making static to ensure no leaks
		// Dispose of the color manager
		if (fColorManager != null) {
			fColorManager.dispose();
			fColorManager = null;
		}
		// Dispose of the source configuration
		if (fSourceConfiguration != null) {
			fSourceConfiguration.dispose();
			fSourceConfiguration = null;
		}
	}

	private void configureUIStyledText(GridData data, StyledText styledText) {
		// Configure the underlying styled text widget
		styledText.setMenu(fPage.getPDEEditor().getContextMenu());
		// Force borders
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		styledText.setLayoutData(data);
	}

	/**
	 * The menu set on the underlying styled text widget of the source viewer
	 * needs to be set to null before being diposed; otherwise, the menu will
	 * be disposed along with the widget.
	 * @param viewer
	 */
	public void unsetMenu() {
		if (fViewer == null) {
			return;
		}
		StyledText styledText = fViewer.getTextWidget();
		if (styledText == null) {
			return;
		} else if (styledText.isDisposed()) {
			return;
		}
		styledText.setMenu(null);
	}

	/**
	 * Utility method used to tie global actions into source viewers.
	 * 
	 * @param actionId
	 * @param viewer
	 */
	public boolean doGlobalAction(String actionId) {
		// Ensure the viewer was created
		if (fViewer == null) {
			return false;
		} else if (actionId.equals(ActionFactory.CUT.getId())) {
			fViewer.doOperation(ITextOperationTarget.CUT);
			return true;
		} else if (actionId.equals(ActionFactory.COPY.getId())) {
			fViewer.doOperation(ITextOperationTarget.COPY);
			return true;
		} else if (actionId.equals(ActionFactory.PASTE.getId())) {
			fViewer.doOperation(ITextOperationTarget.PASTE);
			return true;
		} else if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fViewer.doOperation(ITextOperationTarget.SELECT_ALL);
			return true;
		} else if (actionId.equals(ActionFactory.DELETE.getId())) {
			fViewer.doOperation(ITextOperationTarget.DELETE);
			return true;
		} else if (actionId.equals(ActionFactory.UNDO.getId())) {
			// TODO: MP: Undo: Re-enable Undo
			//fViewer.doOperation(ITextOperationTarget.UNDO);
			return false;
		} else if (actionId.equals(ActionFactory.REDO.getId())) {
			// TODO: MP: Undo: Re-enable Redo
			//fViewer.doOperation(ITextOperationTarget.REDO);
			return false;
		}
		return false;
	}

	public boolean canPaste() {
		// Ensure the viewer was created
		if (fViewer == null) {
			return false;
		}
		return fViewer.canDoOperation(ITextOperationTarget.PASTE);
	}
}
