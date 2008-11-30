/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.CompCSFileValidator;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.CompCSInputContext;
import org.eclipse.pde.internal.ua.ui.wizards.cheatsheet.NewSimpleCSFileWizard;
import org.eclipse.pde.internal.ua.ui.wizards.cheatsheet.SimpleCSFileWizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * CompCSTaskDetails
 *
 */
public class CompCSTaskDetails extends CSAbstractDetails {

	private Section fDefinitionSection;

	private FormEntry fNameEntry;

	private FormEntry fPathEntry;

	private Button fSkip;

	private ICompCSTask fDataTask;

	private CompCSEnclosingTextDetails fEnclosingTextSection;

	private final static String F_PATH_SEPARATOR = "/"; //$NON-NLS-1$

	private final static String F_DOT_DOT = ".."; //$NON-NLS-1$

	/**
	 * @param section
	 */
	public CompCSTaskDetails(ICSMaster section) {
		super(section, CompCSInputContext.CONTEXT_ID);
		fDataTask = null;

		fNameEntry = null;
		fPathEntry = null;
		fSkip = null;

		fDefinitionSection = null;
		fEnclosingTextSection = new CompCSEnclosingTextDetails(ICompCSConstants.TYPE_TASK, section);
	}

	/**
	 * @param object
	 */
	public void setData(ICompCSTask object) {
		// Set data
		fDataTask = object;
		// Set data on the enclosing text section
		fEnclosingTextSection.setData(object);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is 
		// registered directly.
		// Initialize managed form for enclosing text section
		fEnclosingTextSection.initialize(form);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fDefinitionSection = getPage().createUISection(parent, DetailsMessages.CompCSTaskDetails_title, DetailsMessages.CompCSTaskDetails_description, style);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fDefinitionSection);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(fDefinitionSection, 3);
		// Create the name entry
		createUINameEntry(sectionClient);
		// Create the kind combo
		createUIPathEntry(sectionClient);
		// Create the skip button
		createUISkipButton(sectionClient);
		// Create the enclosing text section
		fEnclosingTextSection.createDetails(parent);
		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fDefinitionSection.setClient(sectionClient);
		markDetailsPart(fDefinitionSection);
	}

	/**
	 * @param parent
	 */
	private void createUINameEntry(Composite parent) {
		fNameEntry = new FormEntry(parent, getManagedForm().getToolkit(), DetailsMessages.CompCSTaskDetails_name, SWT.NONE);
	}

	/**
	 * @param parent
	 */
	private void createUIPathEntry(Composite parent) {
		fPathEntry = new FormEntry(parent, getManagedForm().getToolkit(), DetailsMessages.CompCSTaskDetails_path, DetailsMessages.CompCSTaskDetails_browse, isEditable());
	}

	/**
	 * @param parent
	 */
	private void createUISkipButton(Composite parent) {
		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		fSkip = getToolkit().createButton(parent, DetailsMessages.CompCSTaskDetails_optional, SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Create listeners for the name entry
		createListenersNameEntry();
		// Create listeners for the path entry
		createListenersPathEntry();
		// Create listeners for the skip button
		createListenersSkipButton();
		// Create listeners within the enclosing text section
		fEnclosingTextSection.hookListeners();
	}

	/**
	 * 
	 */
	private void createListenersNameEntry() {
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fDataTask == null) {
					return;
				}
				fDataTask.setFieldName(fNameEntry.getValue());
			}
		});
	}

	/**
	 * 
	 */
	private void createListenersPathEntry() {
		fPathEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void browseButtonSelected(FormEntry entry) {
				// Ensure data object is defined
				if (fDataTask == null) {
					return;
				}
				handleButtonEventPathEntry(entry);
			}

			public void linkActivated(HyperlinkEvent e) {
				// Ensure data object is defined
				if (fDataTask == null) {
					return;
				}
				handleLinkEventPathEntry(convertPathRelativeToAbs(fPathEntry.getValue(), fDataTask.getModel().getUnderlyingResource().getFullPath().toPortableString()));
			}

			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fDataTask == null) {
					return;
				}
				// TODO: MP: LOW: CompCS: Could validate manual input
				handleTextEventPathEntry(entry.getValue());
			}
		});
	}

	/**
	 * @param entry
	 */
	private void handleButtonEventPathEntry(FormEntry entry) {
		// Create the dialog
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getManagedForm().getForm().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new CompCSFileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(DetailsMessages.CompCSTaskDetails_wizardTitle);
		dialog.setMessage(DetailsMessages.CompCSTaskDetails_wizardDescription);
		dialog.addFilter(new FileExtensionFilter("xml")); //$NON-NLS-1$
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot().getProject(fDataTask.getModel().getUnderlyingResource().getProject().getName()));

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			String newValue = convertPathAbsToRelative(file.getFullPath().toPortableString(), fDataTask.getModel().getUnderlyingResource().getFullPath().toPortableString());
			entry.setValue(newValue);
			handleTextEventPathEntry(newValue);
		}
	}

	/**
	 * @param path
	 * @return
	 */
	private String extractFileName(String path) {
		StringTokenizer tokenizer = new StringTokenizer(path, F_PATH_SEPARATOR);
		while (tokenizer.countTokens() > 1) {
			tokenizer.nextToken();
		}
		return tokenizer.nextToken();
	}

	/**
	 * @param path
	 * @return
	 */
	private String convertPathAbsToRelative(String relativePath, String basePath) {
		StringTokenizer convertPathTokenizer = new StringTokenizer(relativePath, F_PATH_SEPARATOR);
		StringTokenizer basePathTokenizer = new StringTokenizer(basePath, F_PATH_SEPARATOR);
		// First entry is the project name
		String convertPathToken = convertPathTokenizer.nextToken();
		String basePathToken = basePathTokenizer.nextToken();
		// If the project names don't match, then we cannot make a relative
		// path
		if (convertPathToken.equals(basePathToken) == false) {
			return ""; //$NON-NLS-1$
		}
		// Process base and convert path segments to make a relative path
		while (basePathTokenizer.hasMoreTokens() && convertPathTokenizer.hasMoreTokens()) {

			if (basePathTokenizer.countTokens() == 1) {
				// Only the base file name is left
				// No ".." required
				// No last base path segment is required since we did not
				// get the next base path segment token
				return createRelativePath(0, null, convertPathTokenizer);
			} else if (convertPathTokenizer.countTokens() == 1) {
				// Only the convert file name is left
				// Calculate required ".."
				// No last base path segment is required since we did not
				// get the next base path segment token
				return createRelativePath(basePathTokenizer.countTokens() - 1, null, convertPathTokenizer);
			} else {
				// Compare the next path segment
				convertPathToken = convertPathTokenizer.nextToken();
				basePathToken = basePathTokenizer.nextToken();
				if (convertPathToken.equals(basePathToken) == false) {
					// The path segments are not equal
					// Calculate required ".."
					// Last base path segment needs to be included in the 
					// relative path
					return createRelativePath(basePathTokenizer.countTokens(), convertPathToken, convertPathTokenizer);
				}
			}
		}
		// This should never happen
		return ""; //$NON-NLS-1$
	}

	/**
	 * @param dotDotCount
	 * @param tokenizer
	 * @return
	 */
	private String createRelativePath(int dotDotCount, String lastToken, StringTokenizer tokenizer) {
		StringBuffer relativePath = new StringBuffer();
		// Prepend with the number of specified ".."
		for (int i = 0; i < dotDotCount; i++) {
			relativePath.append(F_DOT_DOT);
			relativePath.append(F_PATH_SEPARATOR);
		}
		// Append the last token if specified
		if (lastToken != null) {
			relativePath.append(lastToken);
			relativePath.append(F_PATH_SEPARATOR);
		}
		// Append all the path segments excluding the file itself
		for (int i = 0; i < (tokenizer.countTokens() - 1); i++) {
			relativePath.append(tokenizer.nextToken());
			relativePath.append(F_PATH_SEPARATOR);
		}
		// Append the file itself
		relativePath.append(tokenizer.nextToken());

		return relativePath.toString();
	}

	/**
	 * @param absolutePath
	 */
	private void handleLinkEventPathEntry(String absolutePath) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		Path path = new Path(absolutePath);
		// If the path is empty open the new simple cheat sheet wizard
		if (path.isEmpty()) {
			handleLinkWizardPathEntry();
			return;
		}
		// Try to find the simple cheat sheet in the workspace
		IResource resource = root.findMember(path);
		// If the simple cheat sheet is found open the simple cheat sheet 
		// editor using it as input; otherwise, opne the simple cheat sheet
		// wizard
		if ((resource != null) && (resource instanceof IFile)) {
			try {
				IDE.openEditor(PDEUserAssistanceUIPlugin.getActivePage(), (IFile) resource, true);
			} catch (PartInitException e) {
				// Ignore
			}
		} else {
			handleLinkWizardPathEntry();
		}
	}

	/**
	 * 
	 */
	private void handleLinkWizardPathEntry() {
		NewSimpleCSFileWizard wizard = new NewSimpleCSFileWizard();
		// Select in the tree view the directory this composite cheat sheet is 
		// stored in
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(fDataTask.getModel().getUnderlyingResource()));
		// Create the dialog for the wizard
		WizardDialog dialog = new WizardDialog(PDEUserAssistanceUIPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		// Get the wizard page
		IWizardPage wizardPage = wizard.getPage(SimpleCSFileWizardPage.F_PAGE_NAME);
		if ((wizardPage instanceof SimpleCSFileWizardPage) == false) {
			return;
		}
		SimpleCSFileWizardPage page = (SimpleCSFileWizardPage) wizardPage;
		// Set the initial file name
		String initialValue = fPathEntry.getValue().trim();
		if (initialValue.length() > 0) {
			// It is a relative file name
			page.setFileName(extractFileName(initialValue));
		}
		// Restrict user choices of where to store the new simple cheat sheet
		// to only the project name this composite cheat sheet is stored in
		page.setProjectName(fDataTask.getModel().getUnderlyingResource().getProject().getName());
		// Check the result
		if (dialog.open() == Window.OK) {
			String newValue = convertPathAbsToRelative(page.getAbsoluteFileName(), fDataTask.getModel().getUnderlyingResource().getFullPath().toPortableString());
			fPathEntry.setValue(newValue, true);
			handleTextEventPathEntry(newValue);
		}
	}

	/**
	 * @param relativePath
	 * @return
	 */
	private String convertPathRelativeToAbs(String relativePath, String basePath) {
		StringTokenizer convertPathTokenizer = new StringTokenizer(relativePath, F_PATH_SEPARATOR);
		StringTokenizer basePathTokenizer = new StringTokenizer(basePath, F_PATH_SEPARATOR);
		// Accumulate the non ".." path segments excluding the file name
		// and count the number of ".." path segments
		StringBuffer endPath = new StringBuffer();
		int dotDotCount = 0;
		if (convertPathTokenizer.hasMoreTokens()) {
			while (convertPathTokenizer.countTokens() > 1) {
				String token = convertPathTokenizer.nextToken();
				if (token.equals(F_DOT_DOT)) {
					dotDotCount++;
				} else {
					endPath.append(token);
					endPath.append(F_PATH_SEPARATOR);
				}
			}
			// Append the file name
			endPath.append(convertPathTokenizer.nextToken());
		}
		// Calculate the number of base path segments to accumulate 
		int baseSegementCount = basePathTokenizer.countTokens() - dotDotCount - 1;
		// Check to see if the relative path is bogus
		if (baseSegementCount < 0) {
			return ""; //$NON-NLS-1$
		}
		// Accumulate the initial path segments making up the absolute path
		StringBuffer startPath = new StringBuffer(F_PATH_SEPARATOR);
		for (int i = 0; i < baseSegementCount; i++) {
			startPath.append(basePathTokenizer.nextToken());
			startPath.append(F_PATH_SEPARATOR);
		}
		// Concatenate the start and end paths together to get the absolute
		// paths
		return startPath.toString() + endPath.toString();
	}

	/**
	 * @param newValue
	 */
	private void handleTextEventPathEntry(String newValue) {
		// Check for existing parameters
		if (fDataTask.hasFieldParams()) {
			// There are existing parameters
			// Check for an existing "path" parameter
			ICompCSParam parameter = fDataTask.getFieldParam(ICompCSConstants.ATTRIBUTE_VALUE_PATH);
			if (parameter != null) {
				parameter.setFieldValue(newValue);
			} else {
				// No suitable parameter found
				// Create a new "path" parameter
				createTaskParamPathEntry(newValue);
			}
		} else {
			// No existing parameters
			// Create a new "path" parameter
			createTaskParamPathEntry(newValue);
		}
	}

	/**
	 * @param newValue
	 */
	private void createTaskParamPathEntry(String newValue) {
		ICompCSModelFactory factory = fDataTask.getModel().getFactory();
		// Create parameter
		ICompCSParam parameter = factory.createCompCSParam(fDataTask);
		// Configure parameter
		parameter.setFieldName(ICompCSConstants.ATTRIBUTE_VALUE_PATH);
		parameter.setFieldValue(newValue);
		// Add parameter to the task
		fDataTask.addFieldParam(parameter);
	}

	/**
	 * 
	 */
	private void createListenersSkipButton() {
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fDataTask == null) {
					return;
				}
				fDataTask.setFieldSkip(fSkip.getSelection());
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fDataTask == null) {
			return;
		}
		boolean editable = isEditableElement();
		// Update name entry
		updateNameEntry(editable);
		// Update kind combo
		updatePathEntry(editable);
		// Update skip button
		updateSkipButton(editable);
		// Update fields within enclosing text section		
		fEnclosingTextSection.updateFields();
	}

	/**
	 * @param editable
	 */
	private void updateNameEntry(boolean editable) {
		fNameEntry.setValue(fDataTask.getFieldName(), true);
		fNameEntry.setEditable(editable);
	}

	/**
	 * @param editable
	 */
	private void updatePathEntry(boolean editable) {
		ICompCSParam parameter = fDataTask.getFieldParam(ICompCSConstants.ATTRIBUTE_VALUE_PATH);
		if (parameter != null) {
			fPathEntry.setValue(parameter.getFieldValue(), true);
		} else {
			fPathEntry.setValue("", true); //$NON-NLS-1$
		}
	}

	/**
	 * @param editable
	 */
	private void updateSkipButton(boolean editable) {
		fSkip.setSelection(fDataTask.getFieldSkip());
		fSkip.setEnabled(editable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fNameEntry.commit();
		fPathEntry.commit();
		// No need to call for sub details, because they contain no form entries
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ICompCSTask) == false) {
			return;
		}
		// Set data
		setData((ICompCSTask) object);
		// Update the UI given the new data
		updateFields();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Dispose of the enclosing text section
		if (fEnclosingTextSection != null) {
			fEnclosingTextSection.dispose();
			fEnclosingTextSection = null;
		}
		super.dispose();
	}

}
