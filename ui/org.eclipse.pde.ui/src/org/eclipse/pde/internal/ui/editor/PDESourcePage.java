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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.model.IEditingModel;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public abstract class PDESourcePage extends TextEditor implements IFormPage, IGotoMarker {
	private PDEFormEditor editor;
	private Control control;
	private int index;
	private String id;
	private InputContext inputContext;
	private IContentOutlinePage outlinePage;
	
	/**
	 * 
	 */
	public PDESourcePage(PDEFormEditor editor, String id, String title) {
		this.id = id;
		initialize(editor);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setRangeIndicator(new DefaultRangeIndicator());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#initialize(org.eclipse.ui.forms.editor.FormEditor)
	 */
	public void initialize(FormEditor editor) {
		this.editor = (PDEFormEditor)editor;
	}
	public void dispose() {
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
	protected abstract void outlineSelectionChanged(SelectionChangedEvent e);
	protected ViewerSorter createViewerSorter() {
		return null;
	}
	protected IContentOutlinePage createOutlinePage() {
		SourceOutlinePage outline = new SourceOutlinePage(
				(IEditingModel) getInputContext().getModel(),
				createOutlineLabelProvider(), createOutlineContentProvider(),
				createViewerSorter());
		outline.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				outlineSelectionChanged(event);
			}
		});
		getSelectionProvider().addSelectionChangedListener(outline);
		return outline;
	}

	public IContentOutlinePage getContentOutline() {
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
}