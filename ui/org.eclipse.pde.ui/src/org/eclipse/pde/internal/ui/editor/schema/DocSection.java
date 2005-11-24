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
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DocSection extends PDESection {
	private IDocument fDocument;
	private SourceViewerConfiguration fSourceConfiguration;
	private SourceViewer fSourceViewer;
	private CTabFolder fTabFolder;
	private ISchema fSchema;
	private Object fElement;
	private boolean fIgnoreChange;

	public DocSection(PDEFormPage page, Composite parent, IColorManager colorManager) {
		 		 super(page, parent, Section.DESCRIPTION, true);
		 		 getSection().setText(PDEUIMessages.DocSection_text);
		 		 getSection().setDescription(PDEUIMessages.SchemaEditor_DocSection_desc);
		fSourceConfiguration = new XMLConfiguration(colorManager);
		fDocument = new Document();
		new XMLDocumentSetupParticpant().setup(fDocument);
		fSchema = (ISchema) getPage().getModel();	
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	public void commit(boolean onSave) {
		handleApply();
		super.commit(onSave);
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		fTabFolder = new CTabFolder(container, SWT.FLAT|SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = 2;
		fTabFolder.setLayoutData(gd);
		
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor1, selectedColor2, toolkit.getColors().getBackground()}, new int[] {50, 100}, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection();
			}
		});

		int styles = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL;
		fSourceViewer = new SourceViewer(container, null, styles);
		fSourceViewer.configure(fSourceConfiguration);
		fSourceViewer.setDocument(fDocument);
		fSourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		StyledText styledText = fSourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

		if (SWT.getPlatform().equals("motif") == false) //$NON-NLS-1$
			toolkit.paintBordersFor(container);
		Control[] children = container.getChildren();
		Control control = children[children.length - 1];
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 50;
		gd.heightHint = 50;
		control.setLayoutData(gd);

		createTabs();
		section.setClient(container);
		initialize();
		if (fTabFolder.getItemCount() > 0) {
			fTabFolder.setSelection(0);		
			updateTabSelection();
		}
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			fSourceViewer.doOperation(SourceViewer.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			fSourceViewer.doOperation(SourceViewer.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			fSourceViewer.doOperation(SourceViewer.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fSourceViewer.doOperation(SourceViewer.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			fSourceViewer.doOperation(SourceViewer.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			fSourceViewer.doOperation(SourceViewer.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			fSourceViewer.doOperation(SourceViewer.REDO);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
	}
	public boolean setFormInput(Object input) {
		int index = -1;
		if (input instanceof ISchema) {
			index = 0;
		} else if (input instanceof IDocumentSection) {
			IDocumentSection[] sections = fSchema.getDocumentSections();
			for (int i = 0; i < sections.length; i++) {
				IDocumentSection section = sections[i];
				if (section.equals(input)) {
					index = i + 1;
					break;
				}
			}
		}
		if (index != -1)
			fTabFolder.setSelection(index);
		updateEditorInput(input);
		return true;
	}
	
	private String getTopicName(Object object) {
		if (object instanceof ISchema) {
			return PDEUIMessages.SchemaEditor_topic_overview;
		} else if (object instanceof IDocumentSection) {
			IDocumentSection section = (IDocumentSection) object;
			String sectionId = section.getSectionId();
			if (sectionId.equals(IDocumentSection.EXAMPLES))
				return PDEUIMessages.SchemaEditor_topic_examples;
			if (sectionId.equals(IDocumentSection.SINCE))
				return PDEUIMessages.SchemaEditor_topic_since;
			if (sectionId.equals(IDocumentSection.IMPLEMENTATION))
				return PDEUIMessages.SchemaEditor_topic_implementation;
			if (sectionId.equals(IDocumentSection.API_INFO))
				return PDEUIMessages.SchemaEditor_topic_api;
			if (sectionId.equals(IDocumentSection.COPYRIGHT))
				return PDEUIMessages.SchemaEditor_topic_copyright;
		}
		return "?"; //$NON-NLS-1$
	}

	private void handleApply() {
		if (fElement != null) {
			if (fElement instanceof ISchema)
				 ((Schema) fElement).setDescription(fDocument.get());
			else
				 ((SchemaObject) fElement).setDescription(fDocument.get());
			updateTabImage(fTabFolder.getSelection());
		}
	}

	public void initialize() {
		fSourceViewer.setEditable(fSchema.isEditable());
		fDocument.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				if (!fIgnoreChange && fSchema.isEditable()) {
					markDirty();
				}
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
		updateEditorInput(fSchema);
		fSchema.addModelChangedListener(this);
	}

	public void dispose() {
		fSchema.removeModelChangedListener(this);
		super.dispose();
	}

	private void createTabs() {
		IDocumentSection[] sections = fSchema.getDocumentSections();
		addTab(fSchema);
		for (int i = 0; i < sections.length; i++) {
			IDocumentSection section = sections[i];
			addTab(section);
		}
	}
	
	private void addTab(ISchemaObject section) {
		String label = getTopicName(section);
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(label);
		item.setData(section);
		updateTabImage(item);
	}

	private void updateTabImage(CTabItem item) {
		if (item != null) {
			ISchemaObject section = (ISchemaObject)item.getData();
			if (section != null)
				item.setImage(PDEPlugin.getDefault().getLabelProvider().getImage(section));
		}
	}

	private void updateTabSelection() {
		int index = fTabFolder.getSelectionIndex();
		if (fSchema.isEditable() && isDirty())
			handleApply();
		
		if (index == 0)
			updateEditorInput(fSchema);
		else {
			IDocumentSection[] sections = fSchema.getDocumentSections();
			updateEditorInput(sections[index - 1]);
		}
	}

	public void setFocus() {
		fSourceViewer.getTextWidget().setFocus();
		updateSelection(fSourceViewer.getSelection());
	}

	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	public void updateEditorInput(Object input) {
		fIgnoreChange = true;
		String text = "";
		if (input instanceof ISchemaObject)
			text = ((ISchemaObject) input).getDescription();
		fDocument.set(text == null ? "" : text);
		fElement = input;
		fIgnoreChange = false;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}

	public void refresh() {
		IDocumentSection[] sections = fSchema.getDocumentSections();
		int index = fTabFolder.getSelectionIndex();
		if (index == 0)
			updateEditorInput(fSchema);
		else {
			updateEditorInput(sections[index - 1]);
		}
		super.refresh();
	}

	public boolean canPaste(Clipboard clipboard) {
		return fSourceViewer.canDoOperation(SourceViewer.PASTE);
	}
}
