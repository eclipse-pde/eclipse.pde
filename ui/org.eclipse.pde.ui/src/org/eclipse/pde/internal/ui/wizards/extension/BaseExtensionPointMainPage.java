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
package org.eclipse.pde.internal.ui.wizards.extension;
import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;
public abstract class BaseExtensionPointMainPage extends WizardPage {
	public static final String KEY_PLUGIN_ID = "BaseExtensionPoint.pluginId"; //$NON-NLS-1$
	public static final String KEY_ID = "BaseExtensionPoint.id"; //$NON-NLS-1$
	public static final String KEY_NAME = "BaseExtensionPoint.name"; //$NON-NLS-1$
	public static final String KEY_SCHEMA = "BaseExtensionPoint.schema"; //$NON-NLS-1$
	public static final String KEY_EDIT = "BaseExtensionPoint.edit"; //$NON-NLS-1$
	public static final String KEY_SHARED = "BaseExtensionPoint.shared"; //$NON-NLS-1$
	public static final String KEY_MISSING_ID = "BaseExtensionPoint.missingId"; //$NON-NLS-1$
	public static final String KEY_NO_PLUGIN_MISSING_ID = "BaseExtensionPoint.noPlugin.missingId"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_OVERVIEW = "BaseExtensionPoint.sections.overview"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_SINCE = "BaseExtensionPoint.sections.since"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_USAGE = "BaseExtensionPoint.sections.usage"; //$NON-NLS-1$
	public static final String KEY_GENERATING = "BaseExtensionPoint.generating"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_API = "BaseExtensionPoint.sections.api"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_SUPPLIED = "BaseExtensionPoint.sections.supplied"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_COPYRIGHT = "BaseExtensionPoint.sections.copyright"; //$NON-NLS-1$
	public static final String SETTINGS_PLUGIN_ID = "BaseExtensionPoint.settings.pluginId";
	private IContainer container;
	protected Text idText;
	protected Text pluginIdText;
	protected Text nameText;
	protected Text schemaText;
	protected Button openSchemaButton;
	protected Button sharedSchemaButton;
	public BaseExtensionPointMainPage(IContainer container) {
		super("newExtensionPoint"); //$NON-NLS-1$
		this.container = container;
	}
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		if (isPluginIdNeeded()) {
			Label label = new Label(container, SWT.NONE);
			label.setText(PDEPlugin.getResourceString(KEY_PLUGIN_ID));
			pluginIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan=1;
			gd.widthHint = 275;
			pluginIdText.setLayoutData(gd);
			pluginIdText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			});
			Button browseButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.horizontalSpan =1;
			gd.widthHint = 50;
			browseButton.setLayoutData(gd);
			browseButton.setText(PDEPlugin.getResourceString("BaseExtensionPointMainPage.browse"));
			browseButton.setToolTipText(PDEPlugin.getResourceString("BaseExtensionPointMainPage.pluginId.tooltip"));
			browseButton.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					handlePluginBrowse();
				}
			});
			SWTUtil.setButtonDimensionHint(browseButton);
		}
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_ID));
		idText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		idText.setLayoutData(gd);
		idText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_NAME));
		nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		nameText.setLayoutData(gd);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_SCHEMA));
		schemaText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		schemaText.setLayoutData(gd);
		//schemaText.setEditable(false);
		if (isSharedSchemaSwitchNeeded()) {
			sharedSchemaButton = new Button(container, SWT.CHECK);
			sharedSchemaButton.setText(PDEPlugin.getResourceString(KEY_SHARED));
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			sharedSchemaButton.setLayoutData(gd);
		}
		openSchemaButton = new Button(container, SWT.CHECK);
		openSchemaButton.setText(PDEPlugin.getResourceString(KEY_EDIT));
		openSchemaButton.setSelection(true);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		openSchemaButton.setLayoutData(gd);
		if (isPluginIdNeeded())
			pluginIdText.setFocus();
		else
			idText.setFocus();
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_SCHEMA);
	}
	private InputStream createSchemaStream(String pluginId, String pointId,
			String name, boolean shared) {
		if (name.length() == 0)
			name = pointId;
		EditableSchema schema = new EditableSchema(pluginId, pointId, name);
		schema.setDescription(PDEPlugin
				.getResourceString(KEY_SECTIONS_OVERVIEW));
		DocumentSection section;
		section = new DocumentSection(schema, IDocumentSection.SINCE, "Since:");
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_SINCE));
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
			complexType.addAttribute(attribute);
			schema.addElement(element);
		}
		section = new DocumentSection(schema, IDocumentSection.EXAMPLES,
				"Examples"); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_USAGE));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.API_INFO,
				"API Information"); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_API));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.IMPLEMENTATION,
				"Supplied Implementation"); //$NON-NLS-1$
		section.setDescription(PDEPlugin
				.getResourceString(KEY_SECTIONS_SUPPLIED));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.COPYRIGHT,
				"Copyright"); //$NON-NLS-1$
		section.setDescription(PDEPlugin
				.getResourceString(KEY_SECTIONS_COPYRIGHT));
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
		monitor.subTask(PDEPlugin.getResourceString(KEY_GENERATING));
		IWorkspace workspace = container.getWorkspace();
		IPath path = new Path(schema).removeLastSegments(1);
		if (path.isEmpty() == false)
			JavaCodeGenerator.ensureFoldersExist(container.getProject(), path
					.toString(), "/"); //$NON-NLS-1$
		InputStream source = createSchemaStream(pluginId, id, name, shared);
		IPath filePath = container.getFullPath().append(schema);
		schemaFile = workspace.getRoot().getFile(filePath);
		if (schemaFile.exists() == false) {
			// create for the first time
			schemaFile.create(source, true, monitor);
		} else {
			schemaFile.setContents(source, true, false, monitor);
		}
		monitor.done();
		IDE.setDefaultEditor(schemaFile, PDEPlugin.SCHEMA_EDITOR_ID);
		return schemaFile;
	}
	public IRunnableWithProgress getOperation() {
		final boolean openFile = openSchemaButton.getSelection();
		final String id = idText.getText();
		final String name = nameText.getText();
		final String schema = schemaText.getText();
		final boolean shared = sharedSchemaButton != null ? sharedSchemaButton
				.getSelection() : false;
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					IFile file = generateSchemaFile(getPluginId(), id, name,
							shared, schema, monitor);
					if (file != null && openFile)
						openSchemaFile(file);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		return operation;
	}
	public String getPluginId() {
		if (pluginIdText != null) {
			return pluginIdText.getText();
		}
		return ""; //$NON-NLS-1$
	}
	public String getSchemaLocation() {
		return null;
	}
	protected boolean isPluginIdNeeded() {
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
					String editorId = PDEPlugin.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(new FileEditorInput(file),
							editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	public boolean isPageComplete() {
		boolean empty = idText.getText().length() == 0 || nameText.getText().length() == 0;
		if (!empty && pluginIdText != null) {
			empty = pluginIdText.getText().length() == 0;
		}
		return !empty;
	}
	private void validatePage() {
		if (!validateContainer())
			return;
		boolean isComplete = isPageComplete();
		setPageComplete(isComplete);
		String message = null;
		if (!isComplete) {
			if (isPluginIdNeeded())
				message = PDEPlugin.getResourceString(KEY_MISSING_ID);
			else
				message = PDEPlugin.getResourceString(KEY_NO_PLUGIN_MISSING_ID);
		}
		setMessage(message, IMessageProvider.WARNING);
		String location = getSchemaLocation();
		String prefix = location != null ? location + "/" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		String schema = (idText.getText().length() > 0) ? (prefix
				+ idText.getText() + ".exsd") : ""; //$NON-NLS-1$ //$NON-NLS-2$
		schemaText.setText(schema);
	}
	private boolean validateContainer() {
		boolean exists = container != null && container.exists();
		if (!exists) {
			setErrorMessage(PDEPlugin
					.getResourceString("BaseExtensionPointMainPage.noContainer")); //$NON-NLS-1$
			setPageComplete(false);
		}
		return exists;
	}
	private void handlePluginBrowse(){
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), PDECore.getDefault().getWorkspaceModelManager().getAllModels(), false);
		dialog.create();
		if (dialog.open() == Dialog.OK){
			WorkspacePluginModelBase workspaceModelBase = (WorkspacePluginModelBase)dialog.getFirstResult();
			pluginIdText.setText(workspaceModelBase.getPluginBase().getId());
		}
			
	}
}