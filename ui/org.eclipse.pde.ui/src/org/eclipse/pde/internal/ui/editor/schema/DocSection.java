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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class DocSection extends PDEFormSection {
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
	private boolean editable = true;
	private SourceViewerConfiguration sourceConfiguration;
	private SourceViewer sourceViewer;
	private CCombo sectionCombo;
	private ISchema schema;
	private Button applyButton;
	private Button resetButton;
	private Object element;
	private IColorManager colorManager;
	//private TableTreeViewer topicTree;
	private FormWidgetFactory factory;
	private boolean ignoreChange;
	private boolean updateNeeded;

	public DocSection(PDEFormPage page, IColorManager colorManager) {
		super(page);
		setHeaderPainted(false);
		setAddSeparator(false);
		String description = PDEPlugin.getResourceString(SECTION_DESC);
		setDescription(TextUtil.createMultiLine(description, 80));
		this.colorManager = colorManager;
		sourceConfiguration = new XMLConfiguration(colorManager);
		document = new Document();
		partitioner =
			new DefaultPartitioner(
				new PDEPartitionScanner(),
				new String[] {
					PDEPartitionScanner.XML_TAG,
					PDEPartitionScanner.XML_COMMENT });
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
	}
	public void commitChanges(boolean onSave) {
		handleApply();
		if (onSave) {
			setDirty(false);
			resetButton.setEnabled(false);
		}
	}
	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = FormWidgetFactory.BORDER_STYLE == SWT.NULL ? 2 : 0;
		layout.marginHeight =
			FormWidgetFactory.BORDER_STYLE == SWT.NULL ? 2 : 0;
		layout.verticalSpacing = 6;
		container.setLayout(layout);
		GridData gd;

		schema = (ISchema) getFormPage().getModel();

		Label label = factory.createLabel(container, null);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		int comboStyle = SWT.READ_ONLY;
		if (SWT.getPlatform().equals("motif") == false)
			comboStyle |= SWT.FLAT;
		else
			comboStyle |= SWT.BORDER;
		sectionCombo = new CCombo(container, comboStyle);
		sectionCombo.setBackground(factory.getBackgroundColor());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		sectionCombo.setLayoutData(gd);
		factory.createLabel(container, null);

		int styles =
			SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL /*| SWT.WRAP */
		| FormWidgetFactory.BORDER_STYLE;
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
		styledText.setMenu(getFormPage().getEditor().getContextMenu());

		if (SWT.getPlatform().equals("motif") == false)
			factory.paintBordersFor(container);
		Control[] children = container.getChildren();
		Control control = children[children.length - 1];
		gd =
			new GridData(
				GridData.FILL_BOTH
					| GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL);
		//gd.widthHint = 600;
		//gd.heightHint = 600;
		control.setLayoutData(gd);
		Composite buttonContainer = factory.createComposite(container);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);

		applyButton =
			factory.createButton(
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
			factory.createButton(
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
		initializeSectionCombo();
		return container;
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.CUT)) {
			sourceViewer.doOperation(SourceViewer.CUT);
			return true;
		} else if (
			actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.COPY)) {
			sourceViewer.doOperation(SourceViewer.COPY);
			return true;
		} else if (
			actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.PASTE)) {
			sourceViewer.doOperation(SourceViewer.PASTE);
			return true;
		} else if (
			actionId.equals(
				org.eclipse.ui.IWorkbenchActionConstants.SELECT_ALL)) {
			sourceViewer.doOperation(SourceViewer.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			sourceViewer.doOperation(SourceViewer.DELETE);
			return true;
		} else if (
			actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.UNDO)) {
			sourceViewer.doOperation(SourceViewer.UNDO);
			return true;
		} else if (
			actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.REDO)) {
			sourceViewer.doOperation(SourceViewer.REDO);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}
	public void expandTo(Object input) {
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
			sectionCombo.select(index);
		updateEditorInput(input);
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
		}
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
	}
	private void handleReset() {
		updateEditorInput(element);
	}
	public void initialize(Object model) {
		sourceViewer.setEditable(schema.isEditable());
		document.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				if (!ignoreChange && schema.isEditable()) {
					setDirty(true);
					((IEditable) schema).setDirty(true);
					getFormPage().getEditor().fireSaveNeeded();
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

	private void initializeSectionCombo() {
		IDocumentSection[] sections = schema.getDocumentSections();
		loadSectionCombo(sections);

		sectionCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = sectionCombo.getSelectionIndex();
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
		});
	}

	private void loadSectionCombo(IDocumentSection[] sections) {
		sectionCombo.add(getTopicName(schema));
		for (int i = 0; i < sections.length; i++) {
			IDocumentSection section = sections[i];
			sectionCombo.add(getTopicName(section));
		}
		sectionCombo.pack();
		sectionCombo.select(0);
	}

	private String resolveObjectName(Object object) {
		if (object instanceof ISchemaObject) {
			return ((ISchemaObject) object).getName();
		}
		return object.toString();
	}
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
		updateSelection(sourceViewer.getSelection());
	}

	private void updateSelection(ISelection selection) {
		getFormPage().getEditor().setSelection(selection);
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
			updateNeeded = true;
			if (getFormPage().isVisible()) {
				update();
			}
		}
	}

	public void update() {
		int itemCount = sectionCombo.getItemCount();
		IDocumentSection[] sections = schema.getDocumentSections();
		if (itemCount != sections.length+1) {
			// sections added or removed - reload combo
			sectionCombo.removeAll();
			loadSectionCombo(sections);
			sectionCombo.getParent().layout();
			updateEditorInput(schema);
			return;
		}
		int index = sectionCombo.getSelectionIndex();
		if (index == 0)
			updateEditorInput(schema);
		else {
			updateEditorInput(sections[index - 1]);
		}
	}

	public boolean canPaste(Clipboard clipboard) {
		return sourceViewer.canDoOperation(SourceViewer.PASTE);
	}
}
