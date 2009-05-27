/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.util.Properties;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.context.ManifestDocumentSetupParticipant;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class ExternalizeStringsWizardPage extends UserInputWizardPage {

	public static final String PAGE_NAME = "ExternalizeStringsWizardPage"; //$NON-NLS-1$

	public static final int EXTERN = 0;
	public static final int VALUE = 1;
	public static final int KEY = 2;
	private static final int SIZE = 3; // column counter
	private static final String[] TABLE_PROPERTIES = new String[SIZE];
	private static final String[] TABLE_COLUMNS = new String[SIZE];

	static {
		TABLE_PROPERTIES[EXTERN] = "extern"; //$NON-NLS-1$
		TABLE_PROPERTIES[VALUE] = "value"; //$NON-NLS-1$
		TABLE_PROPERTIES[KEY] = "key"; //$NON-NLS-1$
		TABLE_COLUMNS[EXTERN] = ""; //$NON-NLS-1$
		TABLE_COLUMNS[VALUE] = PDEUIMessages.ExternalizeStringsWizardPage_value;
		TABLE_COLUMNS[KEY] = PDEUIMessages.ExternalizeStringsWizardPage_subKey;
	}

	private class ModelChangeContentProvider implements ITreeContentProvider, IContentProvider {

		public Object[] getElements(Object parent) {
			return fModelChangeTable.getAllModelChanges().toArray();
		}

		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ModelChange))
				return new Object[0];
			return ((ModelChange) parentElement).getModelChangeFiles();
		}

		public Object getParent(Object element) {
			if (element instanceof ModelChangeFile) {
				return ((ModelChangeFile) element).getModel();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			return element instanceof ModelChange;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class ExternalizeStringsCellModifier implements ICellModifier {

		public boolean canModify(Object element, String property) {
			return (property != null && (element instanceof ModelChangeElement) && !TABLE_PROPERTIES[VALUE].equals(property) && (isPageComplete() || element.equals(fErrorElement)) && (TABLE_PROPERTIES[KEY].equals(property) && ((ModelChangeElement) element).isExternalized()));

		}

		public Object getValue(Object element, String property) {
			if (element instanceof ModelChangeElement) {
				ModelChangeElement changeElement = (ModelChangeElement) element;
				if (TABLE_PROPERTIES[KEY].equals(property)) {
					return StringHelper.unwindEscapeChars(changeElement.getKey());
				}
			}
			return ""; //$NON-NLS-1$
		}

		public void modify(Object element, String property, Object value) {
			if (element instanceof TableItem) {
				Object data = ((TableItem) element).getData();
				if (data instanceof ModelChangeElement) {
					ModelChangeElement changeElement = (ModelChangeElement) data;
					if (TABLE_PROPERTIES[KEY].equals(property)) {
						String newKey = StringHelper.windEscapeChars((String) value);
						validateKey(newKey, changeElement);
						changeElement.setKey(newKey);
						fPropertiesViewer.update(data, null);
					}
				}
			}
		}
	}

	private ModelChangeTable fModelChangeTable;

	private ContainerCheckedTreeViewer fInputViewer;
	private Button fSelectAll;
	private Button fDeselectAll;
	private Label fProjectLabel;
	private Text fLocalizationText;
	private CheckboxTableViewer fPropertiesViewer;
	private Table fTable;
	private SourceViewer fSourceViewer;

	private ViewerFilter fErrorElementFilter;
	private ModifyListener fModifyListener;

	private Object fCurrSelection;
	private ModelChangeElement fErrorElement;
	private String fPreErrorKey;

	private IDocument fEmptyDoc;
	private IColorManager fColorManager;
	private XMLConfiguration fXMLConfig;
	private XMLDocumentSetupParticpant fXMLSetupParticipant;
	private ManifestDocumentSetupParticipant fManifestSetupParticipant;

	private ManifestConfiguration fManifestConfig;

	protected ExternalizeStringsWizardPage(ModelChangeTable changeTable) {
		super(PAGE_NAME);
		setTitle(PDEUIMessages.ExternalizeStringsWizardPage_pageTitle);
		setDescription(PDEUIMessages.ExternalizeStringsWizardPage_pageDescription);
		fModelChangeTable = changeTable;
		fErrorElementFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (!(element instanceof ModelChangeElement))
					return false;
				ModelChangeElement change = (ModelChangeElement) element;
				return change.equals(fErrorElement);
			}
		};
		fModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String localization = fLocalizationText.getText();
				if (StringHelper.isValidLocalization(localization)) {
					setEnabled(fLocalizationText, true);
					setPageComplete(hasCheckedElements());
					setErrorMessage(null);
					if (fCurrSelection instanceof ModelChange) {
						((ModelChange) fCurrSelection).setBundleLocalization(fLocalizationText.getText());
					} else if (fCurrSelection instanceof ModelChangeFile) {
						((ModelChangeFile) fCurrSelection).getModel().setBundleLocalization(fLocalizationText.getText());
					}
				} else {
					setEnabled(fLocalizationText, false);
					fLocalizationText.setEditable(true);
					setPageComplete(false);
					setErrorMessage(PDEUIMessages.ExternalizeStringsWizardPage_badLocalizationError);
				}
			}
		};
		fColorManager = ColorManager.getDefault();
		fXMLConfig = new XMLConfiguration(fColorManager);
		fXMLSetupParticipant = new XMLDocumentSetupParticpant();
		fManifestConfig = new ManifestConfiguration(fColorManager);
		fManifestSetupParticipant = new ManifestDocumentSetupParticipant();
	}

	public void dispose() {
		fColorManager.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {

		SashForm superSash = new SashForm(parent, SWT.HORIZONTAL);
		superSash.setFont(parent.getFont());
		superSash.setLayoutData(new GridData(GridData.FILL_BOTH));

		createInputContents(superSash);

		SashForm sash = new SashForm(superSash, SWT.VERTICAL);
		sash.setFont(superSash.getFont());
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTableViewer(sash);
		createSourceViewer(sash);
		initialize();

		setPageComplete(hasCheckedElements());

		superSash.setWeights(new int[] {4, 7});
		setControl(superSash);
		Dialog.applyDialogFont(superSash);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(superSash, IHelpContextIds.EXTERNALIZE_STRINGS_PAGE);
	}

	private void createInputContents(Composite composite) {
		Composite fileComposite = new Composite(composite, SWT.NONE);
		fileComposite.setLayout(new GridLayout());
		fileComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(fileComposite, SWT.NONE);
		label.setText(PDEUIMessages.ExternalizeStringsWizardPage_resourcelabel);
		fInputViewer = new ContainerCheckedTreeViewer(fileComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.BORDER);
		fInputViewer.setContentProvider(new ModelChangeContentProvider());
		fInputViewer.setLabelProvider(new ModelChangeLabelProvider());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fInputViewer.getTree().setLayoutData(gd);
		fInputViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged(event);
			}
		});
		fInputViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(hasCheckedElements());
			}
		});
		fInputViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);

		Composite buttonComposite = new Composite(fileComposite, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = layout.marginWidth = 0;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fSelectAll = new Button(buttonComposite, SWT.PUSH);
		fSelectAll.setText(PDEUIMessages.ExternalizeStringsWizardPage_selectAllButton);
		fSelectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fSelectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInputViewer.setCheckedElements(fModelChangeTable.getAllModelChanges().toArray());
				setPageComplete(hasCheckedElements());
			}
		});
		fDeselectAll = new Button(buttonComposite, SWT.PUSH);
		fDeselectAll.setText(PDEUIMessages.ExternalizeStringsWizardPage_deselectAllButton);
		fDeselectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fDeselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInputViewer.setCheckedElements(new Object[0]);
				setPageComplete(hasCheckedElements());
			}
		});

		Composite infoComposite = new Composite(fileComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		infoComposite.setLayout(layout);
		infoComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label project = new Label(infoComposite, SWT.NONE);
		project.setText(PDEUIMessages.ExternalizeStringsWizardPage_projectLabel);
		fProjectLabel = new Label(infoComposite, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 10;
		fProjectLabel.setLayoutData(gd);
		fProjectLabel.setText(PDEUIMessages.ExternalizeStringsWizardPage_noUnderlyingResource);

		Label properties = new Label(infoComposite, SWT.NONE);
		properties.setText(PDEUIMessages.ExternalizeStringsWizardPage_localizationLabel);
		fLocalizationText = new Text(infoComposite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 10;
		fLocalizationText.setLayoutData(gd);
		fLocalizationText.setText(PDEUIMessages.ExternalizeStringsWizardPage_noUnderlyingResource);
		fLocalizationText.addModifyListener(fModifyListener);

		fInputViewer.setInput(PDEPlugin.getDefault());
	}

	private void createTableViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout());

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.ExternalizeStringsWizardPage_propertiesLabel);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPropertiesViewer = CheckboxTableViewer.newCheckList(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
		fTable = fPropertiesViewer.getTable();
		fTable.setFont(composite.getFont());
		fTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTable.setLayout(new GridLayout());
		fTable.setLinesVisible(true);
		fTable.setHeaderVisible(true);

		for (int i = 0; i < TABLE_COLUMNS.length; i++) {
			TableColumn tc = new TableColumn(fTable, SWT.NONE);
			tc.setText(TABLE_COLUMNS[i]);
			tc.setResizable(i != 0);
			tc.setWidth(i == 0 ? 20 : 200);
		}

		fPropertiesViewer.setUseHashlookup(true);
		fPropertiesViewer.setCellEditors(createCellEditors());
		fPropertiesViewer.setColumnProperties(TABLE_PROPERTIES);
		fPropertiesViewer.setCellModifier(new ExternalizeStringsCellModifier());
		fPropertiesViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				if (fInputViewer.getSelection() instanceof IStructuredSelection) {
					Object selection = ((IStructuredSelection) fInputViewer.getSelection()).getFirstElement();
					if (selection instanceof ModelChangeFile) {
						ModelChangeFile cf = (ModelChangeFile) selection;
						return (cf).getModel().getChangesInFile(cf.getFile()).toArray();
					}
				}
				return new Object[0];
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		fPropertiesViewer.setLabelProvider(new ExternalizeStringsLabelProvider());
		fPropertiesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handlePropertySelection();
			}
		});
		fPropertiesViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof ModelChangeElement) {
					((ModelChangeElement) element).setExternalized(event.getChecked());
					fPropertiesViewer.update(element, null);
				}
			}
		});
		fPropertiesViewer.setInput(new Object());
	}

	private void createSourceViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout());

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.ExternalizeStringsWizardPage_sourceLabel);
		label.setLayoutData(new GridData());

		fSourceViewer = new SourceViewer(composite, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fSourceViewer.setEditable(false);
		fSourceViewer.getTextWidget().setFont(JFaceResources.getTextFont());
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

		fEmptyDoc = new Document();
		fSourceViewer.setDocument(fEmptyDoc);
	}

	// must set selection after source viewer is created, otherwise you get an NPE.
	private void initialize() {
		Object[] preSelect = fModelChangeTable.getPreSelected();
		fInputViewer.setSelection(new StructuredSelection(preSelect));
		fInputViewer.setCheckedElements(fModelChangeTable.getPreSelected());
	}

	private void handleSelectionChanged(SelectionChangedEvent event) {
		if (!(event.getSelection() instanceof IStructuredSelection))
			return;
		Object selection = (((IStructuredSelection) event.getSelection()).getFirstElement());
		if (selection == null) {
			fCurrSelection = null;
			fSourceViewer.setDocument(fEmptyDoc);
		} else if (selection.equals(fCurrSelection)) {
			return;
		} else if (selection instanceof ModelChangeFile) {
			fCurrSelection = selection;
			IFile file = ((ModelChangeFile) fCurrSelection).getFile();
			NullProgressMonitor monitor = new NullProgressMonitor();
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				try {
					manager.connect(file.getFullPath(), LocationKind.IFILE, monitor);
					updateSourceViewer(manager, file);
				} catch (MalformedTreeException e) {
				} finally {
					manager.disconnect(file.getFullPath(), LocationKind.IFILE, monitor);
				}
			} catch (CoreException e) {
			}
		} else if (selection instanceof ModelChange) {
			fCurrSelection = selection;
			fSourceViewer.setDocument(fEmptyDoc);
			updatePropertiesLabel(((ModelChange) fCurrSelection).getParentModel());
		}
		refreshPropertiesViewer(false);
	}

	private void refreshPropertiesViewer(boolean updateLabels) {
		fPropertiesViewer.refresh(updateLabels);
		TableItem[] items = fTable.getItems();
		for (int i = 0; i < items.length; i++) {
			if (!(items[i].getData() instanceof ModelChangeElement))
				continue;
			ModelChangeElement element = (ModelChangeElement) items[i].getData();
			fPropertiesViewer.setChecked(element, element.isExternalized());
		}
	}

	private void updateSourceViewer(ITextFileBufferManager manager, IFile sourceFile) {
		IDocument document = manager.getTextFileBuffer(sourceFile.getFullPath(), LocationKind.IFILE).getDocument();
		TreeItem item = fInputViewer.getTree().getSelection()[0];
		IPluginModelBase model = ((ModelChange) item.getParentItem().getData()).getParentModel();

		if (fSourceViewer.getDocument() != null)
			fSourceViewer.unconfigure();
		if (sourceFile.getFileExtension().equalsIgnoreCase("xml")) { //$NON-NLS-1$
			fSourceViewer.configure(fXMLConfig);
			fXMLSetupParticipant.setup(document);
		} else {
			fSourceViewer.configure(fManifestConfig);
			fManifestSetupParticipant.setup(document);
		}

		fSourceViewer.setDocument(document);
		updatePropertiesLabel(model);
	}

	private void updatePropertiesLabel(IPluginModelBase model) {
		ModelChange modelChange = fModelChangeTable.getModelChange(model);
		fProjectLabel.setText(model.getUnderlyingResource().getProject().getName());
		fLocalizationText.setEditable(!modelChange.localizationSet());
		fLocalizationText.setText(modelChange.getBundleLocalization());
	}

	private void handlePropertySelection() {
		if (!(fPropertiesViewer.getSelection() instanceof IStructuredSelection))
			return;
		Object selection = (((IStructuredSelection) fPropertiesViewer.getSelection()).getFirstElement());
		if (selection instanceof ModelChangeElement && fSourceViewer.getDocument() != null) {
			ModelChangeElement element = (ModelChangeElement) selection;
			int offset = element.getOffset();
			int length = element.getLength();
			fSourceViewer.setSelectedRange(offset, length);
			fSourceViewer.revealRange(offset, length);
		}
	}

	private CellEditor[] createCellEditors() {
		final CellEditor editors[] = new CellEditor[SIZE];
		editors[EXTERN] = null;
		editors[VALUE] = null;
		editors[KEY] = new TextCellEditor(fTable);
		return editors;
	}

	private void validateKey(String key, ModelChangeElement element) {
		ModelChange modelChange = ((ModelChangeFile) fCurrSelection).getModel();
		Properties properties = modelChange.getProperties();
		String error = null;
		String oldKey = (fPreErrorKey != null) ? fPreErrorKey : element.getKey();
		if (key.equals(fPreErrorKey)) {
			error = null;
		} else if (key.trim().length() < 1) {
			error = getErrorMessage(PDEUIMessages.ExternalizeStringsWizardPage_keyEmptyError, oldKey);
		} else if (key.charAt(0) == '#' || key.charAt(0) == '!' || key.charAt(0) == '%') {
			error = getErrorMessage(PDEUIMessages.ExternalizeStringsWizardPage_keyCommentError, oldKey);
		} else if (key.indexOf(':') != -1 || key.indexOf('=') != -1 || key.indexOf(' ') != -1) {
			error = getErrorMessage(PDEUIMessages.ExternalizeStringsWizardPage_keyError, oldKey);
		} else if ((!key.equals(oldKey) || fPreErrorKey != null) && properties.containsKey(key)) {
			error = getErrorMessage(PDEUIMessages.ExternalizeStringsWizardPage_keyDuplicateError, oldKey);
		}

		setErrorMessage(error);
		setPageComplete(error == null && hasCheckedElements());
		if (error == null) {
			fErrorElement = null;
			fPreErrorKey = null;
			setEnabled(fPropertiesViewer.getControl(), true);
			fPropertiesViewer.removeFilter(fErrorElementFilter);
			refreshPropertiesViewer(true);
			properties.setProperty(key, element.getValue());
		} else if (fPreErrorKey == null) {
			fErrorElement = element;
			fPreErrorKey = oldKey;
			setEnabled(fPropertiesViewer.getControl(), false);
			fPropertiesViewer.addFilter(fErrorElementFilter);
		}
	}

	private String getErrorMessage(String error, String suggestion) {
		StringBuffer sb = new StringBuffer(error);
		if (suggestion != null) {
			sb.append(PDEUIMessages.ExternalizeStringsWizardPage_keySuggested);
			sb.append(suggestion);
		}
		return sb.toString();
	}

	public Object[] getChangeFiles() {
		return fInputViewer.getCheckedElements();
	}

	private boolean hasCheckedElements() {
		return fInputViewer.getCheckedElements().length > 0;
	}

	private void setEnabled(Control exception, boolean enabled) {
		if (!exception.equals(fInputViewer.getControl()))
			fInputViewer.getControl().setEnabled(enabled);
		if (!exception.equals(fPropertiesViewer.getControl()))
			fPropertiesViewer.getControl().setEnabled(enabled);
		if (!exception.equals(fLocalizationText))
			fLocalizationText.setEnabled(enabled);
		if (!exception.equals(fSelectAll))
			fSelectAll.setEnabled(enabled);
		if (!exception.equals(fDeselectAll))
			fDeselectAll.setEnabled(enabled);
	}

	public void setPageComplete(boolean complete) {
		super.setPageComplete(complete);
		// if the page is ready to be completed set the selection on the processor so it knows
		// what work needs to be done
		if (complete)
			((ExternalizeStringsProcessor) ((PDERefactor) getRefactoring()).getProcessor()).setChangeFiles(fInputViewer.getCheckedElements());
	}
}
