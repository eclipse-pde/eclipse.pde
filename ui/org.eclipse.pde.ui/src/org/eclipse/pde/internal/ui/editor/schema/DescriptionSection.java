/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.schema.SchemaObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DescriptionSection extends PDESection implements IPartSelectionListener {
	private SourceViewer fSourceViewer;
	private IDocument fDocument;
	private ISchemaObject fElement;
	private boolean fIgnoreChange;
	private IColorManager fColorManager;

	public DescriptionSection(PDEFormPage page, Composite parent, IColorManager colorManager) {
		super(page, parent, Section.DESCRIPTION);
		fColorManager = colorManager;
		getSection().setText(PDEUIMessages.SchemaEditor_DescriptionSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_DescriptionSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	public void commit(boolean onSave) {
		updateDescription();
		super.commit(onSave);
	}
	
	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		container.setLayout(layout);
		GridData gd;
		fSourceViewer = new SourceViewer(container, null, SWT.MULTI|SWT.WRAP|SWT.V_SCROLL| SWT.H_SCROLL);
		fSourceViewer.configure(new XMLConfiguration(fColorManager));
		fDocument = new Document();
		new XMLDocumentSetupParticpant().setup(fDocument);
		fSourceViewer.setDocument(fDocument);
		fSourceViewer.setEditable(isEditable());
		fSourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		
		Control styledText = fSourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		if (SWT.getPlatform().equals("motif") == false) //$NON-NLS-1$
			toolkit.paintBordersFor(container);
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		Control[] children = container.getChildren();
		Control control = children[children.length - 1];
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		gd.heightHint = 120;
		control.setLayoutData(gd);
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				updateSelection(fSourceViewer.getSelection());
			}
		});
		section.setClient(container);
		initialize();
	}

	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.REDO);
			return true;
		}
		return false;
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
	
	private void updateDescription() {
		if (fElement instanceof SchemaObject) {
			((SchemaObject)fElement).setDescription(fDocument.get());
		}
	}
	
	public void initialize() {
		updateDocument();
		fDocument.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				if (!fIgnoreChange && getPage().getPDEEditor().getAggregateModel().isEditable()) {
					markDirty();
				}
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (!(part instanceof ElementSection))
			return;
		Object changeObject = ((IStructuredSelection)selection).getFirstElement();
		if (changeObject != fElement && isDirty())
			updateDescription();
		fElement = (ISchemaObject) changeObject;
		if (fElement instanceof ISchemaObjectReference)
			fElement = ((ISchemaObjectReference)fElement).getReferencedObject();
		updateDocument();
	}
	
	public void setFocus() {
		fSourceViewer.getTextWidget().setFocus();
	}

	public void updateDocument() {
		if (fElement != null) {
			fIgnoreChange = true;
			String text = fElement.getDescription();
			fDocument.set(text == null ? "" : text); //$NON-NLS-1$
			fIgnoreChange = false;
		}
	}

	public boolean canPaste(Clipboard clipboard) {
		return fSourceViewer.canDoOperation(ITextOperationTarget.PASTE);
	}
}
