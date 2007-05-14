/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * CSSourceViewerFactory
 *
 */
public class CSSourceViewer {

	private static XMLConfiguration fSourceConfiguration = null;
	
	private static IColorManager fColorManager = null;
	
	private static int fSourceViewerCount = 0;
	
	private SourceViewer fViewer;
	
	private PDEFormPage fPage;
	
	private IDocument fDocument;
	
	/**
	 * @param page
	 */
	public CSSourceViewer(PDEFormPage page) {
		// Create the underlying document
		fDocument = new Document();	
		fPage = page;
	}
	
	/**
	 * @return
	 */
	public IDocument getDocument() {
		return fDocument;
	}
	
	/**
	 * @return
	 */
	public SourceViewer getViewer() {
		return fViewer;
	}
	
	/**
	 * @return
	 */
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
		configureUIStyledText(heightHint, widthHint, fViewer.getTextWidget());
		// Create style text listeners
		createUIListenersStyledText(fViewer.getTextWidget());	
	}

	/**
	 * 
	 */
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
	
	/**
	 * @param textWidget
	 */
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

	/**
	 * 
	 */
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
	
	/**
	 * @param heightHint
	 * @param widthHint
	 * @param styledText
	 */
	private void configureUIStyledText(int heightHint,
			int widthHint, StyledText styledText) {
		// Configure the underlying styled text widget
		styledText.setMenu(fPage.getPDEEditor().getContextMenu());
		// Force borders
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = heightHint;
		data.widthHint = widthHint;
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
	 * @return
	 */
	public boolean doGlobalAction(String actionId) {
		// Ensure the viewer was created
		if (fViewer == null) {
			return false;
		} else if (actionId.equals(ActionFactory.CUT.getId())) {
			fViewer.doOperation(ITextOperationTarget.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			fViewer.doOperation(ITextOperationTarget.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			fViewer.doOperation(ITextOperationTarget.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fViewer.doOperation(ITextOperationTarget.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			fViewer.doOperation(ITextOperationTarget.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			fViewer.doOperation(ITextOperationTarget.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			fViewer.doOperation(ITextOperationTarget.REDO);
			return true;
		}
		return false;		
	}	
	
	/**
	 * @param viewer
	 * @return
	 */
	public boolean canPaste() {
		// Ensure the viewer was created
		if (fViewer == null) {
			return false;
		}
		return fViewer.canDoOperation(ITextOperationTarget.PASTE);
	}
}
