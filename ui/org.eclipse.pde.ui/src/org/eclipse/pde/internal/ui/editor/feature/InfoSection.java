package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
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

public class InfoSection extends PDEFormSection {
	private static final String SECTION_TITLE =
		"FeatureEditor.InfoSection.title";
	private static final String KEY_APPLY = "Actions.apply.flabel";
	private static final String KEY_RESET = "Actions.reset.flabel";
	private static final String SECTION_DESC = "FeatureEditor.InfoSection.desc";
	private static final String KEY_INFO = "FeatureEditor.InfoSection.info";
	private static final String KEY_URL = "FeatureEditor.InfoSection.url";
	private static final String KEY_TEXT = "FeatureEditor.InfoSection.text";
	private static final String KEY_INFO_DESCRIPTION =
		"FeatureEditor.info.description";
	private static final String KEY_INFO_LICENSE = "FeatureEditor.info.license";
	private static final String KEY_INFO_COPYRIGHT =
		"FeatureEditor.info.copyright";
	private IDocument document;
	private IDocumentPartitioner partitioner;
	private SourceViewerConfiguration sourceConfiguration;
	private SourceViewer sourceViewer;
	private CCombo sectionCombo;
	private Text urlText;
	private Button applyButton;
	private Button resetButton;
	private Object element;
	private int elementIndex;
	private IColorManager colorManager;
	private FormWidgetFactory factory;
	private boolean ignoreChange;

	public InfoSection(PDEFormPage page, IColorManager colorManager) {
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
		layout.numColumns = 3;
		layout.marginWidth = FormWidgetFactory.BORDER_STYLE == SWT.NULL ? 2 : 0;
		layout.marginHeight =
			FormWidgetFactory.BORDER_STYLE == SWT.NULL ? 2 : 0;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		GridData gd;

		Label label = factory.createLabel(container, null);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		factory.createLabel(container, PDEPlugin.getResourceString(KEY_INFO));
		int borderStyle;
		if (SWT.getPlatform().equals("motif") == false)
			borderStyle = SWT.FLAT;
		else
			borderStyle = SWT.BORDER;
		int comboStyle = SWT.READ_ONLY | borderStyle;
		sectionCombo = new CCombo(container, comboStyle);
		sectionCombo.setBackground(factory.getBackgroundColor());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		sectionCombo.setLayoutData(gd);
		initializeSectionCombo();
		factory.createLabel(container, null);

		factory.createLabel(container, PDEPlugin.getResourceString(KEY_URL));

		if (SWT.getPlatform().equals("motif") == false) {
			urlText =
				factory.createText(container, null, SWT.SINGLE | borderStyle);
			urlText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					infoModified();
				}
			});
			gd = new GridData(GridData.FILL_HORIZONTAL);
			urlText.setLayoutData(gd);
		} else {
			Composite textContainer = createText(container, factory);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			textContainer.setLayoutData(gd);
		}

		factory.createLabel(container, null);

		label =
			factory.createLabel(
				container,
				PDEPlugin.getResourceString(KEY_TEXT));
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);

		int styles = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL
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
		return container;
	}

	private Composite createText(Composite parent, FormWidgetFactory factory) {
		Composite textContainer = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 1;
		layout.marginHeight = 2;
		textContainer.setLayout(layout);
		factory.paintBordersFor(textContainer);
		urlText = factory.createText(textContainer, null);
		urlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				infoModified();
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		urlText.setLayoutData(gd);
		return textContainer;
	}
	private void updateSelection(ISelection selection) {
		getFormPage().getEditor().setSelection(selection);
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
	public void expandTo(Object input) {
		if (input instanceof IFeatureInfo) {
			IFeatureInfo info = (IFeatureInfo) input;
			int index = info.getIndex();
			sectionCombo.select(index);
			updateEditorInput(info, true);
		}
	}

	private void handleApply() {
		handleApply(null, sectionCombo.getSelectionIndex());
	}

	private void handleApply(IFeatureInfo info, int index) {
		String urlName = urlText.getText();
		String text = document.get();
		updateInfoText(info, urlName, text, index);
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
	}

	private void updateInfoText(
		IFeatureInfo targetInfo,
		String urlText,
		String text,
		int index) {
		String url = null;

		if (urlText.length() > 0) {
			url = urlText;
		}
		try {
			IFeatureModel model = (IFeatureModel) getFormPage().getModel();
			IFeature feature = model.getFeature();
			IFeatureInfo info = targetInfo;

			if (info == null) {
				info = feature.getFeatureInfo(index);
			}

			if (targetInfo == null && info == null) {
				info = model.getFactory().createInfo(index);
				feature.setFeatureInfo(info, index);
			}
			info.setURL(url);
			info.setDescription(text);
		} catch (CoreException e) {
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	private void updateInfoURL(URL url) {
	}

	private void handleReset() {
		updateEditorInput(element, false);
	}

	public void initialize(Object model) {
		IFeatureModel featureModel = (IFeatureModel) model;
		document.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				infoModified();
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
		urlText.setEditable(featureModel.isEditable());
		sourceViewer.getTextWidget().setEditable(featureModel.isEditable());
		featureModel.addModelChangedListener(this);
		updateEditorInput(featureModel.getFeature().getFeatureInfo(0), false);
	}

	public void dispose() {
		IFeatureModel featureModel = (IFeatureModel) getFormPage().getModel();
		featureModel.removeModelChangedListener(this);
		super.dispose();
	}

	private void infoModified() {
		IFeatureModel featureModel = (IFeatureModel) getFormPage().getModel();
		if (!ignoreChange && featureModel instanceof IEditable) {
			setDirty(true);
			((IEditable) featureModel).setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
		applyButton.setEnabled(true);
		resetButton.setEnabled(true);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			IFeatureModel model = (IFeatureModel) getFormPage().getModel();
			int index = sectionCombo.getSelectionIndex();
			IFeatureInfo info = model.getFeature().getFeatureInfo(index);
			setDirty(false);
			element = null;
			elementIndex = -1;
			updateEditorInput(info, false);
		}
	}

	private void initializeSectionCombo() {
		sectionCombo.setItems(
			new String[] {
				PDEPlugin.getResourceString(KEY_INFO_DESCRIPTION),
				PDEPlugin.getResourceString(KEY_INFO_COPYRIGHT),
				PDEPlugin.getResourceString(KEY_INFO_LICENSE)});

		sectionCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IFeatureModel model = (IFeatureModel) getFormPage().getModel();
				IFeature feature = model.getFeature();
				int index = sectionCombo.getSelectionIndex();
				IFeatureInfo info = feature.getFeatureInfo(index);
				updateEditorInput(info, true);
			}
		});
		sectionCombo.pack();
		sectionCombo.select(0);
	}
	private String resolveObjectName(Object object) {
		if (object instanceof IFeatureObject) {
			return ((IFeatureObject) object).getLabel();
		}
		return object.toString();
	}
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}

	private void commitPrevious() {
		IFeatureInfo previous = (IFeatureInfo) element;
		handleApply(previous, elementIndex);
	}

	public void updateEditorInput(Object input, boolean commitPrevious) {
		if (isDirty()
			&& commitPrevious /*
			&& element != null
			&& element != input */) {
			commitPrevious();
		}
		ignoreChange = true;
		String text = "";
		String url = null;
		if (input instanceof IFeatureInfo) {
			IFeatureInfo info = (IFeatureInfo) input;
			text = info.getDescription();
			url = info.getURL();
		}
		if (text == null)
			text = "";
		else
			text = TextUtil.createMultiLine(text, 60, false);
		document.set(text);
		if (url == null)
			urlText.setText("");
		else
			urlText.setText(url.toString());
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
		element = input;
		elementIndex = sectionCombo.getSelectionIndex();
		ignoreChange = false;
	}

	public boolean canPaste(Clipboard clipboard) {
		return sourceViewer.canDoOperation(SourceViewer.PASTE);
	}
}