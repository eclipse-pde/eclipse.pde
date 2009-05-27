/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.navigator.ResourceComparator;

public abstract class BaseExtensionPointMainPage extends WizardPage {
	public static final String SETTINGS_PLUGIN_ID = "BaseExtensionPoint.settings.pluginId"; //$NON-NLS-1$
	public static final String SCHEMA_DIR = "schema"; //$NON-NLS-1$

	protected IContainer fContainer;
	protected Text fIdText;
	protected Text fPluginIdText;
	protected Text fNameText;
	protected Text fSchemaText;
	protected Text fSchemaLocationText;
	protected Button fOpenSchemaButton;
	protected Button fSharedSchemaButton;
	protected Button fPluginBrowseButton;
	protected Button fFindLocationButton;

	public BaseExtensionPointMainPage(IContainer container) {
		super("newExtensionPoint"); //$NON-NLS-1$
		fContainer = container;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		Label label;
		GridData gd;
		if (isPluginIdNeeded()) {
			label = new Label(container, SWT.NONE);
			label.setText(PDEUIMessages.BaseExtensionPoint_pluginId);
			fPluginIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			gd.widthHint = 275;
			fPluginIdText.setLayoutData(gd);
			fPluginIdText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			});
			fPluginBrowseButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.horizontalSpan = 1;
			gd.widthHint = 50;
			fPluginBrowseButton.setLayoutData(gd);
			fPluginBrowseButton.setText(PDEUIMessages.BaseExtensionPointMainPage_pluginBrowse);
			fPluginBrowseButton.setToolTipText(PDEUIMessages.BaseExtensionPointMainPage_pluginId_tooltip);
			fPluginBrowseButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handlePluginBrowse();
				}
			});
			SWTUtil.setButtonDimensionHint(fPluginBrowseButton);
		}
		label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.BaseExtensionPoint_id);
		fIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fIdText.setLayoutData(gd);
		fIdText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// setting the text will trigger validation
				// do not implicitly validate here
				fSchemaText.setText(getSchemaLocation() + (getSchemaLocation().length() > 0 ? "/" : "") + fIdText.getText() + ".exsd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		});
		label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.BaseExtensionPoint_name);
		fNameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fNameText.setLayoutData(gd);
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		if (isPluginIdNeeded() && !isPluginIdFinal()) {
			label = new Label(container, SWT.NONE);
			label.setText(PDEUIMessages.BaseExtensionPoint_schemaLocation);
			fSchemaLocationText = new Text(container, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = 150;
			gd.grabExcessHorizontalSpace = true;
			fSchemaLocationText.setLayoutData(gd);
			fSchemaLocationText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			});
			fFindLocationButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.widthHint = 50;
			fFindLocationButton.setLayoutData(gd);
			fFindLocationButton.setText(PDEUIMessages.BaseExtensionPointMainPage_findBrowse);
			fFindLocationButton.setToolTipText(PDEUIMessages.BaseExtensionPointMainPage_schemaLocation_tooltip);
			fFindLocationButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleSchemaLocation();
				}
			});
			SWTUtil.setButtonDimensionHint(fFindLocationButton);
		}
		label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.BaseExtensionPoint_schema);
		fSchemaText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fSchemaText.setLayoutData(gd);
		fSchemaText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		if (isSharedSchemaSwitchNeeded()) {
			fSharedSchemaButton = new Button(container, SWT.CHECK);
			fSharedSchemaButton.setText(PDEUIMessages.BaseExtensionPoint_shared);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			fSharedSchemaButton.setLayoutData(gd);
		}
		fOpenSchemaButton = new Button(container, SWT.CHECK);
		fOpenSchemaButton.setText(PDEUIMessages.BaseExtensionPoint_edit);
		fOpenSchemaButton.setSelection(true);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fOpenSchemaButton.setLayoutData(gd);
		if (isPluginIdNeeded())
			fPluginIdText.setFocus();
		else
			fIdText.setFocus();
		setControl(container);
		initializeValues();
		validatePage();
		// do not start with an error message, convert to regular message
		String error = getErrorMessage();
		if (error != null) {
			setMessage(error);
			setErrorMessage(null);
		}
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_SCHEMA);
	}

	private InputStream createSchemaStream(String pluginId, String pointId, String name, boolean shared) {
		if (name.length() == 0)
			name = pointId;
		EditableSchema schema = new EditableSchema(pluginId, pointId, name, false);
		schema.setDescription(PDEUIMessages.BaseExtensionPoint_sections_overview);
		DocumentSection section;
		section = new DocumentSection(schema, IDocumentSection.SINCE, PDEUIMessages.BaseExtensionPointMainPage_since);
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_since);
		schema.addDocumentSection(section);
		SchemaElement element;
		if (!shared) {
			element = new SchemaRootElement(schema, "extension"); //$NON-NLS-1$
			SchemaComplexType complexType = new SchemaComplexType(schema);
			element.setType(complexType);
			SchemaAttribute attribute = new SchemaAttribute(element, "point"); //$NON-NLS-1$
			attribute.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
			attribute.setUse(ISchemaAttribute.REQUIRED);
			complexType.addAttribute(attribute);
			attribute = new SchemaAttribute(element, "id"); //$NON-NLS-1$
			attribute.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
			complexType.addAttribute(attribute);
			attribute = new SchemaAttribute(element, "name"); //$NON-NLS-1$
			attribute.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
			attribute.setTranslatableProperty(true);
			complexType.addAttribute(attribute);
			schema.addElement(element);
		}
		section = new DocumentSection(schema, IDocumentSection.EXAMPLES, "Examples"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_usage);
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.API_INFO, "API Information"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_api);
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.IMPLEMENTATION, "Supplied Implementation"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_supplied);
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.COPYRIGHT, "Copyright"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_copyright);
		schema.addDocumentSection(section);
		StringWriter swriter = new StringWriter();
		try {
			PrintWriter writer = new PrintWriter(swriter, true);
			schema.save(writer);
			swriter.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		try {
			return new ByteArrayInputStream(swriter.toString().getBytes("UTF8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new ByteArrayInputStream(new byte[0]);
		}
	}

	private IFile generateSchemaFile(String pluginId, String id, String name, boolean shared, String schema, IProgressMonitor monitor) throws CoreException {
		IFile schemaFile = null;

		IWorkspace workspace = fContainer.getWorkspace();
		IPath schemaPath = new Path(schema).removeLastSegments(1);
		IPath newSchemaPath = fContainer.getProjectRelativePath().append(schemaPath);
		monitor.subTask(PDEUIMessages.BaseExtensionPoint_generating);
		if (newSchemaPath.isEmpty() == false) {
			IFolder folder = fContainer.getProject().getFolder(newSchemaPath);
			CoreUtility.createFolder(folder);
		}
		InputStream source = createSchemaStream(pluginId, id, name, shared);
		IPath filePath = fContainer.getFullPath().append(schema);
		schemaFile = workspace.getRoot().getFile(filePath);
		if (!schemaFile.exists()) {
			// create for the first time
			schemaFile.create(source, true, monitor);
		} else {
			schemaFile.setContents(source, true, false, monitor);
		}
		IDE.setDefaultEditor(schemaFile, IPDEUIConstants.SCHEMA_EDITOR_ID);
		return schemaFile;
	}

	public IRunnableWithProgress getOperation() {
		final boolean openFile = fOpenSchemaButton.getSelection();
		final String id = fIdText.getText();
		final String name = fNameText.getText();
		final String schema = fSchemaText.getText();
		final boolean shared = fSharedSchemaButton != null ? fSharedSchemaButton.getSelection() : false;
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(final IProgressMonitor monitor) {
				try {
					Display.getDefault().asyncExec(new Runnable() {

						public void run() {
							String schemaName = schema;
							if (!schema.endsWith(".exsd")) //$NON-NLS-1$
								schemaName = schema + ".exsd"; //$NON-NLS-1$

							IFile file = fContainer.getFile(new Path(schema));
							// do not overwrite if schema already exists
							if (!file.exists())
								try {
									file = generateSchemaFile(getPluginId(), id, name, shared, schemaName, monitor);
								} catch (CoreException e) {
									PDEPlugin.logException(e);
								}

							if (file != null && openFile) {
								fSchemaText.setText(file.getProjectRelativePath().toString());
								openSchemaFile(file);
							}
						}

					});

				} finally {
					monitor.done();
				}
			}
		};
		return operation;
	}

	public String getSchemaLocation() {
		if (fSchemaText != null) {
			String schema = fSchemaText.getText();
			if (schema.length() == 0) {
				if (fSchemaLocationText != null && SCHEMA_DIR.equals(new Path(fSchemaLocationText.getText()).lastSegment())) {
					return ""; //$NON-NLS-1$
				}
				return SCHEMA_DIR;
			}

			int loc = schema.lastIndexOf("/"); //$NON-NLS-1$
			if (loc != -1)
				return schema.substring(0, loc);
		}
		return ""; //$NON-NLS-1$
	}

	public String getPluginId() {
		if (fPluginIdText != null) {
			return fPluginIdText.getText();
		}
		return ""; //$NON-NLS-1$
	}

	protected boolean isPluginIdNeeded() {
		return false;
	}

	protected boolean isPluginIdFinal() {
		return false;
	}

	protected boolean isSharedSchemaSwitchNeeded() {
		return false;
	}

	private void openSchemaFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId = IPDEUIConstants.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(new FileEditorInput(file), editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}

	private void validatePage() {
		// clear opening message
		setMessage(null);
		String message = validateFieldContents();
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	protected abstract String validateFieldContents();

	protected abstract void initializeValues();

	protected String validateExtensionPointID() {

		// Verify not zero length
		String id = fIdText.getText();
		if (id.length() == 0)
			return PDEUIMessages.BaseExtensionPointMainPage_missingExtensionPointID;

		// For 3.2 or greater plug-ins verify that it is a valid composite ID
		// and that it has a valid namespace
		// For 3.1 and lower plug-ins verify that it is a valid simple ID
		String pluginID = getPluginId();
		IPluginModelBase model = PluginRegistry.findModel(pluginID);
		// Verify that the plugin was found
		if (model == null) {
			return NLS.bind(PDEUIMessages.BaseExtensionPointMainPage_errorMsgPluginNotFound, pluginID);
		}

		String schemaVersion = model.getPluginBase().getSchemaVersion();
		if (schemaVersion == null || Float.parseFloat(schemaVersion) >= 3.2) {
			if (!IdUtil.isValidCompositeID(id))
				return PDEUIMessages.BaseExtensionPointMainPage_invalidCompositeID;

		} else if (!IdUtil.isValidSimpleID(id))
			return PDEUIMessages.BaseExtensionPointMainPage_invalidSimpleID;

		return null;
	}

	protected String validateExtensionPointName() {
		// Verify not zero length
		if (fNameText.getText().length() == 0)
			return PDEUIMessages.BaseExtensionPointMainPage_missingExtensionPointName;

		return null;
	}

	protected String validateExtensionPointSchema() {
		// Verify not zero length
		if (fSchemaText.getText().length() == 0)
			return PDEUIMessages.BaseExtensionPointMainPage_missingExtensionPointSchema;

		return null;
	}

	private void handlePluginBrowse() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), PluginRegistry.getWorkspaceModels(), false);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IPluginModelBase workspaceModelBase = (IPluginModelBase) dialog.getFirstResult();
			fPluginIdText.setText(workspaceModelBase.getPluginBase().getId());
		}
	}

	private void handleSchemaLocation() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setTitle(PDEUIMessages.BaseExtensionPointMainPage_schemaLocation_title);
		dialog.setMessage(PDEUIMessages.BaseExtensionPointMainPage_schemaLocation_desc);
		dialog.setDoubleClickSelects(false);
		dialog.setAllowMultiple(false);
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile)
					return false;
				return true;
			}
		});

		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setInitialSelection(fContainer);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.CONTAINER_SELECTION);
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			if (elements.length > 0) {
				IResource elem = (IResource) elements[0];
				String newPath = getWorkspaceRelativePath(elem.getLocation().toString());
				fSchemaLocationText.setText(newPath + "/"); //$NON-NLS-1$
			}
		}
	}

	private String getWorkspaceRelativePath(String path) {
		String workspacePath = PDECore.getWorkspace().getRoot().getLocation().toString();
		if (path.startsWith(workspacePath))
			path = path.replaceFirst(workspacePath, ""); //$NON-NLS-1$
		return path;
	}

	public String getInvalidIdMessage() {
		// No validation done (other than making sure id is not blank)
		return null;
	}

}
