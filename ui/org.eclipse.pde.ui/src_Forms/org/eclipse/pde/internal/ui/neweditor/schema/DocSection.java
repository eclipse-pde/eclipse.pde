/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.schema;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.XMLConfiguration;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.*;

public class DocSection extends PDESection {
	public static final String SECTION_TITLE = "SchemaEditor.DocSection.title";
	public static final String KEY_APPLY = "Actions.apply.flabel";
	public static final String KEY_RESET = "Actions.reset.flabel";
	public static final String SECTION_DESC = "SchemaEditor.DocSection.desc";
	public static final String KEY_TOPIC_OVERVIEW =
		"SchemaEditor.topic.overview";
	public static final String KEY_TOPIC_SINCE = "SchemaEditor.topic.since";
	public static final String KEY_TOPIC_EXAMPLES =
		"SchemaEditor.topic.examples";
	public static final String KEY_TOPIC_IMPLEMENTATION =
		"SchemaEditor.topic.implementation";
	public static final String KEY_TOPIC_API = "SchemaEditor.topic.api";
	public static final String KEY_TOPIC_COPYRIGHT =
		"SchemaEditor.topic.copyright";
	private IDocument document;
	private IDocumentPartitioner partitioner;
	private SourceViewerConfiguration sourceConfiguration;
	private SourceViewer sourceViewer;
	private CTabFolder tabFolder;
	private ISchema schema;
	private Button applyButton;
	private Button resetButton;
	private Object element;
	private boolean ignoreChange;

	public DocSection(PDEFormPage page, Composite parent, IColorManager colorManager) {
		super(page, parent, Section.DESCRIPTION|Section.NO_TITLE, false);
		String description = PDEPlugin.getResourceString(SECTION_DESC);
		getSection().setDescription(description);
		sourceConfiguration = new XMLConfiguration(colorManager);
		document = new Document();
		partitioner =
			new DefaultPartitioner(
				new XMLPartitionScanner(),
				new String[] {
					XMLPartitionScanner.XML_TAG,
					XMLPartitionScanner.XML_COMMENT });
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		//getSection().clientVerticalSpacing = 3;
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	public void commit(boolean onSave) {
		handleApply();
		if (onSave) {
			resetButton.setEnabled(false);
		}
		super.commit(onSave);
	}
	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		layout.verticalSpacing = 6;
		container.setLayout(layout);
		GridData gd;

		schema = (ISchema) getPage().getModel();
/*
		Label label = toolkit.createLabel(container, null);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
*/
		
		tabFolder = new CTabFolder(container, SWT.FLAT|SWT.TOP);
		toolkit.adapt(tabFolder, true, true);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		gd.heightHint = tabFolder.getTabHeight();
		tabFolder.setLayoutData(gd);
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		tabFolder.setSelectionBackground(new Color[] {selectedColor1, selectedColor2, toolkit.getColors().getBackground()}, new int[] {50, 100}, true);

		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection();
			}
		});

		int styles =
			SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL;
		sourceViewer = new SourceViewer(container, null, styles);
		sourceViewer.configure(sourceConfiguration);
		sourceViewer.setDocument(document);
		sourceViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		StyledText styledText = sourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

		if (SWT.getPlatform().equals("motif") == false)
			toolkit.paintBordersFor(container);
		Control[] children = container.getChildren();
		Control control = children[children.length - 1];
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 50;
		gd.heightHint = 50;
		control.setLayoutData(gd);
		Composite buttonContainer = toolkit.createComposite(container);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);

		applyButton =
			toolkit.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_APPLY),
				SWT.PUSH);
		applyButton.setEnabled(false);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		applyButton.setLayoutData(gd);
		applyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleApply();
			}
		});

		resetButton =
			toolkit.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_RESET),
				SWT.PUSH);
		resetButton.setEnabled(false);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		resetButton.setLayoutData(gd);
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReset();
			}
		});
		createTabs();
		section.setClient(container);
		initialize();
		if (tabFolder.getItemCount()>0) {
			tabFolder.setSelection(0);		
			updateTabSelection();
		}
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
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
	}
	public boolean setFormInput(Object input) {
		int index = -1;
		if (input instanceof ISchema) {
			index = 0;
		} else if (input instanceof IDocumentSection) {
			IDocumentSection[] sections = schema.getDocumentSections();
			for (int i = 0; i < sections.length; i++) {
				IDocumentSection section = sections[i];
				if (section.equals(input)) {
					index = i + 1;
					break;
				}
			}
		}
		if (index != -1)
			tabFolder.setSelection(index);
		updateEditorInput(input);
		return true;
	}
	
	private String getTopicName(Object object) {
		if (object instanceof ISchema) {
			return PDEPlugin.getResourceString(KEY_TOPIC_OVERVIEW);
		} else if (object instanceof IDocumentSection) {
			IDocumentSection section = (IDocumentSection) object;
			String sectionId = section.getSectionId();
			if (sectionId.equals(IDocumentSection.EXAMPLES))
				return PDEPlugin.getResourceString(KEY_TOPIC_EXAMPLES);
			if (sectionId.equals(IDocumentSection.SINCE))
				return PDEPlugin.getResourceString(KEY_TOPIC_SINCE);
			if (sectionId.equals(IDocumentSection.IMPLEMENTATION))
				return PDEPlugin.getResourceString(KEY_TOPIC_IMPLEMENTATION);
			if (sectionId.equals(IDocumentSection.API_INFO))
				return PDEPlugin.getResourceString(KEY_TOPIC_API);
			if (sectionId.equals(IDocumentSection.COPYRIGHT))
				return PDEPlugin.getResourceString(KEY_TOPIC_COPYRIGHT);
		}
		return "?";
	}
	private void handleApply() {
		if (element != null) {
			if (element instanceof ISchema)
				 ((Schema) element).setDescription(document.get());
			else
				 ((SchemaObject) element).setDescription(document.get());
			updateTabImage(tabFolder.getSelection());
		}
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
	}
	private void handleReset() {
		updateEditorInput(element);
		updateTabImage(tabFolder.getSelection());		
	}
	public void initialize() {
		sourceViewer.setEditable(schema.isEditable());
		document.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				if (!ignoreChange && schema.isEditable()) {
					markDirty();
				}
				applyButton.setEnabled(true);
				resetButton.setEnabled(true);
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
		updateEditorInput(schema);
		schema.addModelChangedListener(this);
	}

	public void dispose() {
		schema.removeModelChangedListener(this);
		super.dispose();
	}

	private void createTabs() {
		IDocumentSection[] sections = schema.getDocumentSections();
		addTab(schema);
		for (int i = 0; i < sections.length; i++) {
			IDocumentSection section = sections[i];
			addTab(section);
		}
	}
	
	private void addTab(ISchemaObject section) {
		String label = getTopicName(section);
		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setText(label);
		item.setData(section);
		updateTabImage(item);
	}

	private void updateTabImage(CTabItem item) {
		if (item==null) return;
		ISchemaObject section = (ISchemaObject)item.getData();
		if (section==null) return;
		item.setImage(PDEPlugin.getDefault().getLabelProvider().getImage(section));
	}

	private void updateTabSelection() {
		int index = tabFolder.getSelectionIndex();
		if (schema.isEditable() && isDirty()) {
			handleApply();
		}
		if (index == 0)
			updateEditorInput(schema);
		else {
			IDocumentSection[] sections = schema.getDocumentSections();
			updateEditorInput(sections[index - 1]);
		}
	}

	/*private String resolveObjectName(Object object) {
		if (object instanceof ISchemaObject) {
			return ((ISchemaObject) object).getName();
		}
		return object.toString();
	}*/
	
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
		updateSelection(sourceViewer.getSelection());
	}

	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	public void updateEditorInput(Object input) {
		ignoreChange = true;
		String text = "";
		if (input instanceof ISchemaObject) {
			text = ((ISchemaObject) input).getDescription();
		}
		if (text == null)
			text = "";
		/*
		else
			text = TextUtil.createMultiLine(text, 60, false);
		*/

		document.set(text);
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
		element = input;
		ignoreChange = false;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}

	public void refresh() {
		int itemCount = tabFolder.getItemCount();
		IDocumentSection[] sections = schema.getDocumentSections();
		if (itemCount != sections.length+1) {
			// sections added or removed - reload combo
			disposeAllTabs();
			createTabs();
			getPage().getManagedForm().getForm().reflow(true);
			updateEditorInput(schema);
		}
		else {
			int index = tabFolder.getSelectionIndex();
			if (index == 0)
				updateEditorInput(schema);
			else {
				updateEditorInput(sections[index - 1]);
			}
		}
		super.refresh();
	}

	private void disposeAllTabs() {
		CTabItem [] items = tabFolder.getItems();
		for (int i=0; i<items.length; i++) 
			items[i].dispose();
	}

	public boolean canPaste(Clipboard clipboard) {
		return sourceViewer.canDoOperation(SourceViewer.PASTE);
	}
}