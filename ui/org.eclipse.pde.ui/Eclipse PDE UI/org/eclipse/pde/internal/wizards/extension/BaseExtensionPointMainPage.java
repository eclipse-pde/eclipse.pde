package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.part.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.*;
import org.eclipse.core.runtime.OperationCanceledException;
import java.lang.reflect.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.schema.*;
import java.io.*;
import org.eclipse.pde.internal.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.jface.window.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.editor.schema.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.codegen.*;

public abstract class BaseExtensionPointMainPage extends WizardPage {
	public static final String KEY_PLUGIN_ID = "BaseExtensionPoint.pluginId";
	public static final String KEY_ID = "BaseExtensionPoint.id";
	public static final String KEY_NAME = "BaseExtensionPoint.name";
	public static final String KEY_SCHEMA = "BaseExtensionPoint.schema";
	public static final String KEY_EDIT = "BaseExtensionPoint.edit";
	public static final String KEY_MISSING_ID = "BaseExtensionPoint.missingId";
	public static final String KEY_SECTIONS_OVERVIEW = "BaseExtensionPoint.sections.overview";
	public static final String KEY_SECTIONS_USAGE = "BaseExtensionPoint.sections.usage";
	public static final String KEY_GENERATING = "BaseExtensionPoint.generating";
	public static final String KEY_SECTIONS_API = "BaseExtensionPoint.sections.api";
	public static final String KEY_SECTIONS_SUPPLIED = "BaseExtensionPoint.sections.supplied";
	public static final String KEY_SECTIONS_COPYRIGHT = "BaseExtensionPoint.sections.copyright";
	private IContainer container;
	protected Text idText;
	protected Text pluginIdText;
	protected Text nameText;
	protected Text schemaText;
	protected Button openSchemaButton;

public BaseExtensionPointMainPage(IContainer container) {
	super("newExtensionPoint");
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
		pluginIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		pluginIdText.setLayoutData(gd);
		pluginIdText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyIdNotEmpty();
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
			verifyIdNotEmpty();
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
	verifyIdNotEmpty();
	idText.setFocus();
	setControl(container);
}
private InputStream createSchemaStream(String plugin, String id, String name) {
	String fullId = plugin + "." + id;
	EditableSchema schema = new EditableSchema(fullId, name);
	schema.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_OVERVIEW));
	DocumentSection section;

	SchemaElement element = new SchemaElement(schema, "extension");
	SchemaComplexType complexType = new SchemaComplexType(schema);
	element.setType(complexType);
	SchemaCompositor compositor =
		new SchemaCompositor(element, ISchemaCompositor.SEQUENCE);
	complexType.setCompositor(compositor);

	SchemaAttribute attribute = new SchemaAttribute(element, "point");
	attribute.setType(new SchemaSimpleType(schema, "string"));
	attribute.setUse(ISchemaAttribute.REQUIRED);
	complexType.addAttribute(attribute);

	attribute = new SchemaAttribute(element, "id");
	attribute.setType(new SchemaSimpleType(schema, "string"));
	complexType.addAttribute(attribute);

	attribute = new SchemaAttribute(element, "name");
	attribute.setType(new SchemaSimpleType(schema, "string"));
	complexType.addAttribute(attribute);

	schema.addElement(element);

	section = new DocumentSection(schema, IDocumentSection.EXAMPLES, "Examples");
	section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_USAGE));
	schema.addDocumentSection(section);
	section = new DocumentSection(schema, IDocumentSection.API_INFO, "API Information");
	section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_API));
	schema.addDocumentSection(section);

	section =
		new DocumentSection(
			schema,
			IDocumentSection.IMPLEMENTATION,
			"Supplied Implementation");
	section.setDescription(
		PDEPlugin.getResourceString(KEY_SECTIONS_SUPPLIED));
	schema.addDocumentSection(section);
	section =
		new DocumentSection(
			schema,
			IDocumentSection.COPYRIGHT,
			"Copyright");
	section.setDescription(
		PDEPlugin.getResourceString(KEY_SECTIONS_COPYRIGHT));
	schema.addDocumentSection(section);

	ByteArrayOutputStream bstream = new ByteArrayOutputStream();
	try {
		PrintWriter writer = new PrintWriter(bstream, true);
		schema.save(writer);
		bstream.close();
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}

	return new ByteArrayInputStream(bstream.toByteArray());
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
	
	if (path.isEmpty()==false)
	   JavaCodeGenerator.ensureFoldersExist(container.getProject(), path.toString(), "/");

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
	workbench.getEditorRegistry().setDefaultEditor(schemaFile, PDEPlugin.SCHEMA_EDITOR_ID);
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
				IFile file = generateSchemaFile(getPluginId(), id, name, schema, monitor);
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
	return "";
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
				ww.getActivePage().openEditor(new FileEditorInput(file), editorId);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
	});
}
private void verifyIdNotEmpty() {
	String id = idText.getText();
	boolean empty = id.length()==0;
	if (!empty && pluginIdText!=null) {
		empty = pluginIdText.getText().length()==0;
	}
	setPageComplete(!empty);
	String message=null;
	if (empty) {
		message = PDEPlugin.getResourceString(KEY_MISSING_ID);
	}
	setErrorMessage(message);
	String location = getSchemaLocation();
	String prefix = location!=null ? location + "/" : "";
	String schema = (!empty)?(prefix+id+".xsd"):"";
	schemaText.setText(schema);
}
}
