/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.schema.DocumentSection;
import org.eclipse.pde.internal.core.schema.EditableSchema;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaComplexType;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.navigator.ResourceSorter;
public abstract class BaseExtensionPointMainPage extends WizardPage {
	public static final String SETTINGS_PLUGIN_ID = "BaseExtensionPoint.settings.pluginId"; //$NON-NLS-1$
	public static final String SCHEMA_DIR = "schema"; //$NON-NLS-1$

	private IContainer fContainer;
	private IProject fProject;
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
		this.fContainer = container;
		if (container != null)
			this.fProject = container.getProject();
		else
			this.fProject = null;
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
			gd.horizontalSpan=1;
			gd.widthHint = 275;
			fPluginIdText.setLayoutData(gd);
			fPluginIdText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage(true);
				}
			});
			fPluginBrowseButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.horizontalSpan =1;
			gd.widthHint = 50;
			fPluginBrowseButton.setLayoutData(gd);
			fPluginBrowseButton.setText(PDEUIMessages.BaseExtensionPointMainPage_pluginBrowse); //$NON-NLS-1$
			fPluginBrowseButton.setToolTipText(PDEUIMessages.BaseExtensionPointMainPage_pluginId_tooltip); //$NON-NLS-1$
			fPluginBrowseButton.addSelectionListener(new SelectionAdapter(){
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
				fSchemaText
						.setText(getSchemaLocation()
								+ (getSchemaLocation().length() > 0 ? "/" : "") + fIdText.getText() + ".exsd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				validatePage(false);
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
				validatePage(false);
			}
		});
		if (isPluginIdNeeded() && !isPluginIdFinal()){
			label = new Label(container, SWT.NONE);
			label.setText(PDEUIMessages.BaseExtensionPoint_schemaLocation);
			fSchemaLocationText = new Text(container, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = 150;
			gd.grabExcessHorizontalSpace = true;
			fSchemaLocationText.setLayoutData(gd);
			fSchemaLocationText.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e){
					validatePage(true);
				}
			});
			fFindLocationButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.widthHint = 50;
			fFindLocationButton.setLayoutData(gd);
			fFindLocationButton.setText(PDEUIMessages.BaseExtensionPointMainPage_findBrowse); //$NON-NLS-1$
			fFindLocationButton.setToolTipText(PDEUIMessages.BaseExtensionPointMainPage_schemaLocation_tooltip); //$NON-NLS-1$
			fFindLocationButton.addSelectionListener(new SelectionAdapter(){
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
		fSchemaText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e){
				validatePage(false);
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
		validatePage(false);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_SCHEMA);
	}
	private InputStream createSchemaStream(String pluginId, String pointId,
			String name, boolean shared) {
		if (name.length() == 0)
			name = pointId;
		EditableSchema schema = new EditableSchema(pluginId, pointId, name, false);
		schema.setDescription(PDEUIMessages.BaseExtensionPoint_sections_overview);
		DocumentSection section;
		section = new DocumentSection(schema, IDocumentSection.SINCE, PDEUIMessages.BaseExtensionPointMainPage_since); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_since);
		schema.addDocumentSection(section);
		SchemaElement element;
		if (!shared) {
			element = new SchemaElement(schema, "extension"); //$NON-NLS-1$
			SchemaComplexType complexType = new SchemaComplexType(schema);
			element.setType(complexType);
			SchemaCompositor compositor = new SchemaCompositor(element,
					ISchemaCompositor.SEQUENCE);
			complexType.setCompositor(compositor);
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
		section = new DocumentSection(schema, IDocumentSection.EXAMPLES,
		"Examples"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_usage);
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.API_INFO,
		"API Information"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_api);
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.IMPLEMENTATION,
		"Supplied Implementation"); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.BaseExtensionPoint_sections_supplied);
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.COPYRIGHT,
		"Copyright"); //$NON-NLS-1$
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
	private IFile generateSchemaFile(String pluginId, String id, String name,
			boolean shared, String schema, IProgressMonitor monitor)
	throws CoreException {
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
		final boolean shared = fSharedSchemaButton != null ? fSharedSchemaButton
				.getSelection() : false;
				IRunnableWithProgress operation = new WorkspaceModifyOperation() {
					public void execute(IProgressMonitor monitor) {
						try {
							String schemaName = schema;
							if (!schema.endsWith(".exsd")) //$NON-NLS-1$
								schemaName = schema + ".exsd"; //$NON-NLS-1$
							
							IFile file = fContainer.getFile(new Path(schema));
							// do not overwrite if schema already exists
							if (!file.exists())
								file = generateSchemaFile(getPluginId(), id, name,
									shared, schemaName, monitor);
							
							if (file != null && openFile){
								fSchemaText.setText(file.getProjectRelativePath().toString());
								openSchemaFile(file);
							}
							
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						} finally {
							monitor.done();
						}
					}
				};
				return operation;
	}
	public String getSchemaLocation() {
		if (fSchemaText!=null){
			String schema = fSchemaText.getText();
			if (schema.length() == 0) {
				if (fSchemaLocationText != null
						&& SCHEMA_DIR.equals(new Path(fSchemaLocationText
								.getText()).lastSegment())) {
					return ""; //$NON-NLS-1$
				}
				return SCHEMA_DIR;
			}
			
			int loc = schema.lastIndexOf("/"); //$NON-NLS-1$
			if (loc!=-1)
				return schema.substring(0,loc);
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
	protected boolean isPluginIdFinal(){
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
					ww.getActivePage().openEditor(new FileEditorInput(file),
							editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	public boolean checkFieldsFilled() {	
		boolean empty = fIdText.getText().length() == 0 || fNameText.getText().length() == 0;
        if (!empty) {
            empty = !IdUtil.isValidExtensionPointId(fIdText.getText());
        }
		if (!empty && isPluginIdNeeded()) {
			empty = getPluginId().length() == 0 || fSchemaText.getText().length() == 0 ;
		}
		if (!empty && !isPluginIdFinal())
			empty = fSchemaLocationText.getText().length() == 0;
		return !empty;
	}

    public boolean isInvalidValidId() {
        return fIdText.getText().length()>0 && !IdUtil.isValidExtensionPointId(fIdText.getText());
    }

	private void validatePage(boolean hasContainerChanged) {
		if (hasContainerChanged && !validateContainer())
			return;
		boolean isFilled = checkFieldsFilled();
		String message = null;
        if (isInvalidValidId())
            message = PDEUIMessages.BaseExtensionPoint_malformedId;                
        else if (!isFilled) {
            if (isPluginIdNeeded())
				message = PDEUIMessages.BaseExtensionPoint_missingId;
			else
				message = PDEUIMessages.BaseExtensionPoint_noPlugin_missingId;
		}
		setPageComplete(isFilled);
		setMessage(message, IMessageProvider.WARNING);
	}
	private boolean validateContainer() {
		if (isPluginIdNeeded() && !isPluginIdFinal()){
			String newContainerName = fSchemaLocationText.getText();
			IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
			IPath workspacePath = root.getLocation();
			if (newContainerName.startsWith(workspacePath.toString()))
				newContainerName = newContainerName.replaceFirst(workspacePath.toString(), ""); //$NON-NLS-1$
			if (newContainerName.length() == 0){
				handleInvalidContainer();
				return false;
			}
			if (root.exists(new Path(newContainerName)))
				fContainer = root.getContainerForLocation(workspacePath.append(newContainerName));
			else if (fProject != null && fProject.exists(new Path(newContainerName)))
				fContainer = root.getContainerForLocation(fProject.getLocation().append(newContainerName));
			else{
				handleInvalidContainer();
				return false;
			}
			handleValidContainer();
			return true;
		}
		
		boolean exists = fContainer != null && fContainer.exists();
		if (!exists)
			handleInvalidContainer();
		return exists;
	}
	private void handleInvalidContainer(){
		setErrorMessage(PDEUIMessages.BaseExtensionPointMainPage_noContainer); //$NON-NLS-1$
		setPageComplete(false);
	}
	private void handleValidContainer(){
		setErrorMessage(null);
	}
	private void handlePluginBrowse(){
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), PDECore.getDefault().getModelManager().getWorkspaceModels(), false);
		dialog.create();
		if (dialog.open() == Window.OK){
			IPluginModelBase workspaceModelBase = (IPluginModelBase)dialog.getFirstResult();
			fPluginIdText.setText(workspaceModelBase.getPluginBase().getId());
		}
	}
	private void handleSchemaLocation(){
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		dialog.setTitle(PDEUIMessages.BaseExtensionPointMainPage_schemaLocation_title); //$NON-NLS-1$
		dialog.setMessage(PDEUIMessages.BaseExtensionPointMainPage_schemaLocation_desc); //$NON-NLS-1$
		dialog.setDoubleClickSelects(false);
		dialog.setAllowMultiple(false);
		dialog.addFilter(new ViewerFilter(){
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile)
					return false;
				else if (isPluginIdFinal())
					return ((IResource)element).getProject().equals(fProject);
				return true;
			}
		});
		
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		dialog.setInitialSelection(fProject);
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			if (elements.length >0){
				IResource elem = (IResource) elements[0];
				String newPath = getWorkspaceRelativePath(elem.getLocation().toString());
				fSchemaLocationText.setText(newPath + "/"); //$NON-NLS-1$
			}
		}
	}
	private String getWorkspaceRelativePath(String path){
		String workspacePath = PDECore.getWorkspace().getRoot().getLocation().toString();
		if (path.startsWith(workspacePath))
			path = path.replaceFirst(workspacePath, ""); //$NON-NLS-1$
		return path;
	}
}
