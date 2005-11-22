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
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
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
	private IDocument document;
	private boolean editable = true;
	private SourceViewerConfiguration sourceConfiguration;
	private ISchemaObject element;
	private SourceViewer sourceViewer;
	private IDocumentPartitioner partitioner;
	private ISchema schema;
	private boolean ignoreChange = false;

	public DescriptionSection(PDEFormPage page, Composite parent, IColorManager colorManager) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.SchemaEditor_DescriptionSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_DescriptionSection_desc);
		sourceConfiguration = new XMLConfiguration(colorManager);
		document = new Document();
		partitioner =
			new FastPartitioner(
				new XMLPartitionScanner(),
				new String[] {
					XMLPartitionScanner.XML_TAG,
					XMLPartitionScanner.XML_COMMENT });
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
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
		int styles =
			SWT.MULTI
				| SWT.WRAP
				| SWT.V_SCROLL
				| SWT.H_SCROLL;
		sourceViewer = new SourceViewer(container, null, styles);
		sourceViewer.configure(sourceConfiguration);
		sourceViewer.setDocument(document);
		sourceViewer.setEditable(isEditable());
		sourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		Control styledText = sourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		if (SWT.getPlatform().equals("motif") == false) //$NON-NLS-1$
			toolkit.paintBordersFor(container);
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		Control[] children = container.getChildren();
		Control control = children[children.length - 1];
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		gd.heightHint = 90;
		control.setLayoutData(gd);
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				updateSelection(sourceViewer.getSelection());
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
			sourceViewer.doOperation(SourceViewer.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			sourceViewer.doOperation(SourceViewer.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			sourceViewer.doOperation(SourceViewer.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			sourceViewer.doOperation(SourceViewer.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			sourceViewer.doOperation(SourceViewer.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			sourceViewer.doOperation(SourceViewer.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			sourceViewer.doOperation(SourceViewer.REDO);
			return true;
		}
		return false;
	}
	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
	private void updateDescription() {
		if (element != null) {
			if (element == schema)
				((Schema)schema).setDescription(document.get());
			else
				((SchemaObject) element).setDescription(document.get());
		}
	}
	public void initialize() {
		schema = (ISchema) getPage().getModel();
		element = schema;
		updateDocument();
		document.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				if (!ignoreChange && schema instanceof IEditable) {
					markDirty();
				}
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
	}
	public boolean isEditable() {
		return editable;
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (!(part instanceof ElementSection))
			return;
		Object changeObject = ((IStructuredSelection)selection).getFirstElement();
		if (changeObject != element && isDirty())
			updateDescription();
		element = (ISchemaObject) changeObject;
		if (element instanceof ISchemaObjectReference)
			element = ((ISchemaObjectReference)element).getReferencedObject();
		if (element == null)
			element = schema;
		updateDocument();
	}
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}
	public void setEditable(boolean newEditable) {
		editable = newEditable;
	}
	public void updateDocument() {
		ignoreChange = true;
		String text = element.getDescription();
		if (text == null)
			text = ""; //$NON-NLS-1$
		document.set(text);
		ignoreChange = false;
	}

	public boolean canPaste(Clipboard clipboard) {
		return sourceViewer.canDoOperation(SourceViewer.PASTE);
	}
}
