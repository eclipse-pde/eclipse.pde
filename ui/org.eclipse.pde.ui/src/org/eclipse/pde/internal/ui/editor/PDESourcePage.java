/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.*;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.texteditor.*;

public abstract class PDESourcePage extends TextEditor implements IFormPage, IGotoMarker {
	/**
	 * Updates the OutlinePage selection and this editor's range indicator.
	 * 
	 * @since 3.0
	 */
	private class PDESourcePageChangedListener implements
			ISelectionChangedListener {

		/**
		 * Installs this selection changed listener with the given selection
		 * provider. If the selection provider is a post selection provider,
		 * post selection changed events are the preferred choice, otherwise
		 * normal selection changed events are requested.
		 * 
		 * @param selectionProvider
		 */
		public void install(ISelectionProvider selectionProvider) {
			if (selectionProvider == null) {
				return;
			}

			if (selectionProvider instanceof IPostSelectionProvider) {
				IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
				provider.addPostSelectionChangedListener(this);
			} else {
				selectionProvider.addSelectionChangedListener(this);
			}
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = event.getSelection();
			if (!selection.isEmpty() && selection instanceof ITextSelection) {
				IDocumentRange rangeElement = getRangeElement((ITextSelection) selection);
				if (rangeElement != null) {
					setHighlightRange(rangeElement, false);
				} else {
					resetHighlightRange();
				}
				// notify outline page
				if (PDEPlugin.getDefault().getPreferenceStore().getBoolean(
						"ToggleLinkWithEditorAction.isChecked")) { //$NON-NLS-1$
					outlinePage
							.removeSelectionChangedListener(outlineSelectionChangedListener);
					if (rangeElement != null) {
						outlinePage.setSelection(new StructuredSelection(
								rangeElement));
					} else {
						outlinePage.setSelection(StructuredSelection.EMPTY);
					}
					outlinePage
							.addSelectionChangedListener(outlineSelectionChangedListener);
				}
			}
		}

		/**
		 * Removes this selection changed listener from the given selection
		 * provider.
		 * 
		 * @param selectionProviderstyle
		 */
		public void uninstall(ISelectionProvider selectionProvider) {
			if (selectionProvider == null) {
				return;
			}

			if (selectionProvider instanceof IPostSelectionProvider) {
				IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
				provider.removePostSelectionChangedListener(this);
			} else {
				selectionProvider.removeSelectionChangedListener(this);
			}
		}

	}

	/**
	 * The editor selection changed listener.
	 * 
	 * @since 3.0
	 */
	private PDESourcePageChangedListener fEditorSelectionChangedListener;
	private PDEFormEditor editor;
	private Control control;
	private int index;
	private String id;
	private InputContext inputContext;
	private ISortableContentOutlinePage outlinePage;
	private ISelectionChangedListener outlineSelectionChangedListener;
	/**
	 * 
	 */
	public PDESourcePage(PDEFormEditor editor, String id, String title) {
		this.id = id;
		initialize(editor);
		IPreferenceStore[] stores = new IPreferenceStore[2];
		stores[0] = PDEPlugin.getDefault().getPreferenceStore();
		stores[1] = EditorsUI.getPreferenceStore();
		setPreferenceStore(new ChainedPreferenceStore(stores));
		setRangeIndicator(new DefaultRangeIndicator());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#initialize(org.eclipse.ui.forms.editor.FormEditor)
	 */
	public void initialize(FormEditor editor) {
		this.editor = (PDEFormEditor)editor;
	}
	public void dispose() {
		if (fEditorSelectionChangedListener != null)  {
			fEditorSelectionChangedListener.uninstall(getSelectionProvider());
			fEditorSelectionChangedListener= null;
		}
		if (outlinePage != null) {
			outlinePage.dispose();
			outlinePage = null;
		}
		super.dispose();
	}

	protected void editorSaved() {
		super.editorSaved();
	}
	
	protected abstract ILabelProvider createOutlineLabelProvider();
	protected abstract ITreeContentProvider createOutlineContentProvider();
	protected abstract ViewerSorter createOutlineSorter();
	protected abstract void outlineSelectionChanged(SelectionChangedEvent e);
	protected ViewerSorter createDefaultOutlineSorter() {
		return null;
	}
	protected ISortableContentOutlinePage createOutlinePage() {
		SourceOutlinePage sourceOutlinePage=
		new SourceOutlinePage(
				(IEditingModel) getInputContext().getModel(),
				createOutlineLabelProvider(), createOutlineContentProvider(),
				createDefaultOutlineSorter(), createOutlineSorter());
		outlinePage = sourceOutlinePage;
		outlineSelectionChangedListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				outlineSelectionChanged(event);
			}
		};
		outlinePage.addSelectionChangedListener(outlineSelectionChangedListener);
		getSelectionProvider().addSelectionChangedListener(sourceOutlinePage);
		fEditorSelectionChangedListener= new PDESourcePageChangedListener();
		fEditorSelectionChangedListener.install(getSelectionProvider());
		return outlinePage;
	}

	public ISortableContentOutlinePage getContentOutline() {
		if (outlinePage==null)
			outlinePage = createOutlinePage();
		return outlinePage;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getEditor()
	 */
	public FormEditor getEditor() {
		return editor;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getManagedForm()
	 */
	public IManagedForm getManagedForm() {
		// not a form page
		return null;
	}
	protected void firePropertyChange(int type) {
		if (type == PROP_DIRTY) {
			editor.fireSaveNeeded(getEditorInput(), true);
		} else
			super.firePropertyChange(type);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#setActive(boolean)
	 */
	public void setActive(boolean active) {
		inputContext.setSourceEditingMode(active);
	}

	public boolean canLeaveThePage() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#isActive()
	 */
	public boolean isActive() {
		return this.equals(editor.getActivePageInstance());
	}
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		Control[] children = parent.getChildren();
		control = children[children.length - 1];
		
		WorkbenchHelp.setHelp(control, IHelpContextIds.MANIFEST_SOURCE_PAGE);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getPartControl()
	 */
	public Control getPartControl() {
		return control;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getId()
	 */
	public String getId() {
		return id;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getIndex()
	 */
	public int getIndex() {
		return index;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#setIndex(int)
	 */
	public void setIndex(int index) {
		this.index = index;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#isSource()
	 */
	public boolean isEditor() {
		return true;
	}
	/**
	 * @return Returns the inputContext.
	 */
	public InputContext getInputContext() {
		return inputContext;
	}
	/**
	 * @param inputContext The inputContext to set.
	 */
	public void setInputContext(InputContext inputContext) {
		this.inputContext = inputContext;
		setDocumentProvider(inputContext.getDocumentProvider());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#focusOn(java.lang.Object)
	 */
	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker)object);
			return true;
		}
		return false;
	}
	
	protected IDocumentRange getRangeElement(ITextSelection selection) {
		return null;
	}

	public void setHighlightRange(IDocumentRange node, boolean moveCursor) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset = node.getOffset();
		int length = node.getLength();
		setHighlightRange(offset, length == -1 ? 1 : length, moveCursor);
	}
}