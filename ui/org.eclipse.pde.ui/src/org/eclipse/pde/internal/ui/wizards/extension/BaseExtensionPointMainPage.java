package org.eclipse.pde.internal.ui.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.part.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.schema.*;

import java.io.*;
import java.util.ArrayList;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.codegen.*;

public abstract class BaseExtensionPointMainPage extends WizardPage {
	public static final String KEY_PLUGIN_ID = "BaseExtensionPoint.pluginId"; //$NON-NLS-1$
	public static final String KEY_ID = "BaseExtensionPoint.id"; //$NON-NLS-1$
	public static final String KEY_NAME = "BaseExtensionPoint.name"; //$NON-NLS-1$
	public static final String KEY_SCHEMA = "BaseExtensionPoint.schema"; //$NON-NLS-1$
	public static final String KEY_EDIT = "BaseExtensionPoint.edit"; //$NON-NLS-1$
	public static final String KEY_MISSING_ID = "BaseExtensionPoint.missingId"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_OVERVIEW = "BaseExtensionPoint.sections.overview"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_SINCE = "BaseExtensionPoint.sections.since"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_USAGE = "BaseExtensionPoint.sections.usage"; //$NON-NLS-1$
	public static final String KEY_GENERATING = "BaseExtensionPoint.generating"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_API = "BaseExtensionPoint.sections.api"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_SUPPLIED = "BaseExtensionPoint.sections.supplied"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_COPYRIGHT = "BaseExtensionPoint.sections.copyright"; //$NON-NLS-1$
	public static final String SETTINGS_PLUGIN_ID =
		"BaseExtensionPoint.settings.pluginId";
	private IContainer container;
	protected Text idText;
	protected Combo pluginIdText;
	protected Text nameText;
	protected Text schemaText;
	protected Button openSchemaButton;

	public BaseExtensionPointMainPage(IContainer container) {
		super("newExtensionPoint"); //$NON-NLS-1$
		this.container = container;
	}
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		if (isPluginIdNeeded()) {
			Label label = new Label(container, SWT.NONE);
			label.setText(PDEPlugin.getResourceString(KEY_PLUGIN_ID));
			pluginIdText = new Combo(container, SWT.SINGLE | SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			pluginIdText.setLayoutData(gd);
			loadSettings();
			pluginIdText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			});
		}

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_ID));
		idText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
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
		nameText.setLayoutData(gd);

		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_SCHEMA));
		schemaText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		schemaText.setLayoutData(gd);
		//schemaText.setEditable(false);

		openSchemaButton = new Button(container, SWT.CHECK);
		openSchemaButton.setText(PDEPlugin.getResourceString(KEY_EDIT));
		openSchemaButton.setSelection(true);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		openSchemaButton.setLayoutData(gd);
		validatePage();
		if (isPluginIdNeeded())
			pluginIdText.setFocus();
		else
			idText.setFocus();
		setControl(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_SCHEMA);
	}
	private InputStream createSchemaStream(
		String pluginId,
		String pointId,
		String name) {
		if (name.length() == 0)
			name = pointId;
		EditableSchema schema = new EditableSchema(pluginId, pointId, name);
		schema.setDescription(
			PDEPlugin.getResourceString(KEY_SECTIONS_OVERVIEW));
		DocumentSection section;

		section = new DocumentSection(schema, IDocumentSection.SINCE, "Since:");
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_SINCE));
		schema.addDocumentSection(section);

		SchemaElement element = new SchemaElement(schema, "extension"); //$NON-NLS-1$
		SchemaComplexType complexType = new SchemaComplexType(schema);
		element.setType(complexType);
		SchemaCompositor compositor =
			new SchemaCompositor(element, ISchemaCompositor.SEQUENCE);
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

		section = new DocumentSection(schema, IDocumentSection.EXAMPLES, "Examples"); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_USAGE));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.API_INFO, "API Information"); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_API));
		schema.addDocumentSection(section);

		section = new DocumentSection(schema, IDocumentSection.IMPLEMENTATION, "Supplied Implementation"); //$NON-NLS-1$
		section.setDescription(
			PDEPlugin.getResourceString(KEY_SECTIONS_SUPPLIED));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.COPYRIGHT, "Copyright"); //$NON-NLS-1$
		section.setDescription(
			PDEPlugin.getResourceString(KEY_SECTIONS_COPYRIGHT));
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
	private IFile generateSchemaFile(
		String pluginId,
		String id,
		String name,
		String schema,
		IProgressMonitor monitor)
		throws CoreException {
		IFile schemaFile = null;
		monitor.subTask(PDEPlugin.getResourceString(KEY_GENERATING));
		IWorkspace workspace = container.getWorkspace();

		IPath path = new Path(schema).removeLastSegments(1);

		if (path.isEmpty() == false)
			JavaCodeGenerator.ensureFoldersExist(container.getProject(), path.toString(), "/"); //$NON-NLS-1$

		InputStream source = createSchemaStream(pluginId, id, name);

		IPath filePath = container.getFullPath().append(schema);

		schemaFile = workspace.getRoot().getFile(filePath);
		if (schemaFile.exists() == false) {
			// create for the first time
			schemaFile.create(source, true, monitor);
		} else {
			schemaFile.setContents(source, true, false, monitor);
		}
		monitor.done();
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			schemaFile,
			PDEPlugin.SCHEMA_EDITOR_ID);
		return schemaFile;
	}
	public IRunnableWithProgress getOperation() {
		final boolean openFile = openSchemaButton.getSelection();
		final String id = idText.getText();
		final String name = nameText.getText();
		final String schema = schemaText.getText();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					IFile file =
						generateSchemaFile(
							getPluginId(),
							id,
							name,
							schema,
							monitor);
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

	protected void loadSettings() {
		if (pluginIdText != null) {
			ArrayList items = new ArrayList();
			IDialogSettings settings = getDialogSettings();
			for (int i = 0; i < 6; i++) {
				String curr =
					settings.get(SETTINGS_PLUGIN_ID + String.valueOf(i));
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			pluginIdText.setItems((String[]) items.toArray(new String[items.size()]));
		}
	}

	protected void saveSettings() {
		if (pluginIdText != null) {
			IDialogSettings settings = getDialogSettings();
			settings.put(
				SETTINGS_PLUGIN_ID + String.valueOf(0),
				pluginIdText.getText());
			String[] items = pluginIdText.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(
					SETTINGS_PLUGIN_ID + String.valueOf(i + 1),
					items[i]);
			}
		}
	}

	public String getSchemaLocation() {
		return null;
	}
	protected boolean isPluginIdNeeded() {
		return false;
	}
	private void openSchemaFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId = PDEPlugin.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(
						new FileEditorInput(file),
						editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	private void validatePage() {
		if (!validateContainer())
			return;
		String id = idText.getText();
		boolean empty = id.length() == 0;
		if (!empty && pluginIdText != null) {
			empty = pluginIdText.getText().length() == 0;
		}
		setPageComplete(!empty);
		String message = null;
		if (empty) {
			message = PDEPlugin.getResourceString(KEY_MISSING_ID);
		}
		setMessage(message, IMessageProvider.WARNING);
		String location = getSchemaLocation();
		String prefix = location != null ? location + "/" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		String schema = (!empty) ? (prefix + id + ".exsd") : ""; //$NON-NLS-1$ //$NON-NLS-2$
		schemaText.setText(schema);
	}
	private boolean validateContainer() {
		boolean exists = container != null && container.exists();
		if (!exists) {
			setErrorMessage(PDEPlugin.getResourceString("BaseExtensionPointMainPage.noContainer")); //$NON-NLS-1$
			setPageComplete(false);
		}
		return exists;
	}
}