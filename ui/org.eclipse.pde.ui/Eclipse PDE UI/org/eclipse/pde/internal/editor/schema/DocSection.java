package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.jface.text.*;
import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.graphics.*;

public class DocSection extends PDEFormSection {
	public static final String SECTION_TITLE = "SchemaEditor.DocSection.title";
	public static final String KEY_APPLY = "Actions.apply.label";
	public static final String KEY_RESET = "Actions.reset.label";
	public static final String SECTION_DESC = "SchemaEditor.DocSection.desc";
	public static final String KEY_TOPIC_OVERVIEW = "SchemaEditor.topic.overview";
	public static final String KEY_TOPIC_EXAMPLES = "SchemaEditor.topic.examples";
	public static final String KEY_TOPIC_IMPLEMENTATION =
		"SchemaEditor.topic.implementation";
	public static final String KEY_TOPIC_API = "SchemaEditor.topic.api";
	private SourceViewer editor;
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
		new RuleBasedPartitioner(
			new PDEPartitionScanner(),
			new String[] { PDEPartitionScanner.XML_TAG, PDEPartitionScanner.XML_COMMENT });
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
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory =factory;
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = factory.BORDER_STYLE==SWT.NULL? 2 : 0;
	layout.marginHeight = factory.BORDER_STYLE==SWT.NULL? 2 : 0;
	container.setLayout(layout);
	GridData gd;

	sectionCombo = new CCombo(container, SWT.FLAT | SWT.READ_ONLY);
	sectionCombo.setBackground(factory.getBackgroundColor());
	gd = new GridData(GridData.FILL_HORIZONTAL);
	sectionCombo.setLayoutData(gd);
	factory.createLabel(container, null);
	
	int styles = SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL /*| SWT.WRAP */ | factory.BORDER_STYLE;
	sourceViewer = new SourceViewer(container, null, styles);
	sourceViewer.configure(sourceConfiguration);
	sourceViewer.setDocument(document);
	sourceViewer.setEditable(isEditable());
	StyledText styledText= sourceViewer.getTextWidget();
	styledText.setFont(JFaceResources.getTextFont());
	factory.paintBordersFor(container);
	Control [] children = container.getChildren();
	Control control = children[children.length-1];
	gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	//gd.widthHint = 600;
	//gd.heightHint = 600;
	control.setLayoutData(gd);
	Composite buttonContainer = factory.createComposite(container);
	layout = new GridLayout();
	layout.marginHeight=0;
	buttonContainer.setLayout(layout);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	
	applyButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_APPLY), SWT.PUSH);
	applyButton.setEnabled(false);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	applyButton.setLayoutData(gd);
	applyButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleApply();
		}
	});

	resetButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_RESET), SWT.PUSH);
	resetButton.setEnabled(false);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	resetButton.setLayoutData(gd);
	resetButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleReset();
		}
	});
	return container;
}
public void doGlobalAction(String actionId) {
	PDEHackFinder.fixMe("Global operation mapping must be done better");
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.CUT)) {
		sourceViewer.doOperation(sourceViewer.CUT);
	}
	else if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.COPY)) {
		sourceViewer.doOperation(sourceViewer.COPY);
	}
	else if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.PASTE)) {
		sourceViewer.doOperation(sourceViewer.PASTE);
	}
	else if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		sourceViewer.doOperation(sourceViewer.DELETE);
	}
	else if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.UNDO)) {
		sourceViewer.doOperation(sourceViewer.UNDO);
	}
	else if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.REDO)) {
		sourceViewer.doOperation(sourceViewer.REDO);
	}
}
public void expandTo(Object input) {
	int index = -1;
	if (input instanceof ISchema) {
		index = 0;
	} else
		if (input instanceof IDocumentSection) {
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
	} else
		if (object instanceof IDocumentSection) {
			IDocumentSection section = (IDocumentSection) object;
			String sectionId = section.getSectionId();
			if (sectionId.equals(IDocumentSection.EXAMPLES))
				return PDEPlugin.getResourceString(KEY_TOPIC_EXAMPLES);
			if (sectionId.equals(IDocumentSection.IMPLEMENTATION))
				return PDEPlugin.getResourceString(KEY_TOPIC_IMPLEMENTATION);
			if (sectionId.equals(IDocumentSection.API_INFO))
				return PDEPlugin.getResourceString(KEY_TOPIC_API);
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
	schema = (ISchema) model;
	initializeSectionCombo();
	document.addDocumentListener(new IDocumentListener() {
		public void documentChanged(DocumentEvent e) {
			if (!ignoreChange && schema instanceof IEditable) {
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
}
private void initializeSectionCombo() {
	IDocumentSection[] sections = schema.getDocumentSections();
	sectionCombo.add(getTopicName(schema));
	for (int i = 0; i < sections.length; i++) {
		IDocumentSection section = sections[i];
		sectionCombo.add(getTopicName(section));
	}
	sectionCombo.select(0);
	sectionCombo.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			int index = sectionCombo.getSelectionIndex();
			if (index == 0)
				updateEditorInput(schema);
			else {
				IDocumentSection[] sections = schema.getDocumentSections();
				updateEditorInput(sections[index - 1]);
			}
		}
	});
}
public boolean isEditable() {
	return editable;
}
private String resolveObjectName(Object object) {
	if (object instanceof ISchemaObject) {
		return ((ISchemaObject)object).getName();
	}
	return object.toString();
}
public void setEditable(boolean newEditable) {
	editable = newEditable;
}
public void setFocus() {
	sourceViewer.getTextWidget().setFocus();
}
public void updateEditorInput(Object input) {
	ignoreChange=true;
	String text = "";
	if (input instanceof ISchemaObject) {
		text = ((ISchemaObject) input).getDescription();
	}
	if (text == null)
		text = "";
	else
		text = TextUtil.createMultiLine(text, 60, false);
	//updateHeading(input);
	document.set(text);
	applyButton.setEnabled(false);
	resetButton.setEnabled(false);
	element = input;
	ignoreChange=false;
}
}
