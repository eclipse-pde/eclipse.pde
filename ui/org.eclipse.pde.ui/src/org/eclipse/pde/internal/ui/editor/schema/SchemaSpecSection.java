package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;


public class SchemaSpecSection extends PDEFormSection {
	public static final String SECTION_TITLE = "SchemaEditor.SpecSection.title";
	public static final String SECTION_DESC = "SchemaEditor.SpecSection.desc";
	public static final String SECTION_PLUGIN = "SchemaEditor.SpecSection.plugin";
	public static final String SECTION_POINT = "SchemaEditor.SpecSection.point";
	public static final String SECTION_NAME = "SchemaEditor.SpecSection.name";
	
	private FormEntry pluginText;
	private FormEntry pointText;
	private FormEntry nameText;
	private boolean updateNeeded;

public SchemaSpecSection(SchemaFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	setCollapsable(true);
	setCollapsed(true);
}
public void commitChanges(boolean onSave) {
	pluginText.commit();
	pointText.commit();
	nameText.commit();
}

public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	layout.horizontalSpacing = 6;
	container.setLayout(layout);

	final Schema schema = (Schema)getFormPage().getModel();

	pluginText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_PLUGIN), factory));
	pluginText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			schema.setPluginId(text.getValue());
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});

	pointText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_POINT), factory));
	pointText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			schema.setPointId(text.getValue());
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});

	nameText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_NAME), factory));
	nameText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			schema.setName(text.getValue());
			getFormPage().getForm().setHeadingText(schema.getName());
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});

	GridData gd = (GridData) pointText.getControl().getLayoutData();
	gd.widthHint = 150;
	
	factory.paintBordersFor(container);
	return container;
}

private void forceDirty() {
	setDirty(true);
	ISchema schema = (ISchema)getFormPage().getModel();
	if (schema instanceof IEditable) {
		IEditable editable = (IEditable)schema;
		editable.setDirty(true);
		getFormPage().getEditor().fireSaveNeeded();
	}
}

public void dispose() {
	ISchema schema = (ISchema) getFormPage().getModel();
	schema.removeModelChangedListener(this);
	super.dispose();
}

public void initialize(Object input) {
	ISchema schema = (ISchema)input;
	update(input);
	if (!(schema instanceof IEditable)) {
		pluginText.getControl().setEnabled(false);
		pointText.getControl().setEnabled(false);
		nameText.getControl().setEnabled(false);
	}
	schema.addModelChangedListener(this);
}
public boolean isDirty() {
	return pluginText.isDirty()
		|| pointText.isDirty()
		|| nameText.isDirty();
}

public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		updateNeeded=true;
	}
}
public void setFocus() {
	if (pointText != null)
		pointText.getControl().setFocus();
}
private void setIfDefined(FormEntry formText, String value) {
	if (value != null) {
		formText.setValue(value, true);
	}
}
private void setIfDefined(Text text, String value) {
	if (value != null)
		text.setText(value);
}
public void update() {
	if (updateNeeded) {
		this.update(getFormPage().getModel());
	}
}
public void update(Object input) {
	ISchema schema = (ISchema)input;
	setIfDefined(pluginText, schema.getPluginId());
	setIfDefined(pointText, schema.getPointId());
	setIfDefined(nameText, schema.getName());
	getFormPage().getForm().setHeadingText(schema.getName());
	updateNeeded=false;
}
}
