package org.eclipse.pde.internal.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class ExternalPluginsEditor extends FieldEditor {
	private ExternalPluginsBlock pluginsBlock;
	private final static String KEY_LABEL = "ExternalPluginsEditor.label";
	private Label label;

protected ExternalPluginsEditor(Composite parent) {
	pluginsBlock = new ExternalPluginsBlock(this);
	createControl(parent);
	setPreferenceName("ExternalPlugins");
}
protected void adjustForNumColumns(int numColumns) {
	Control control = pluginsBlock.getControl();
	GridData gd = (GridData)control.getLayoutData();
	gd.horizontalSpan = numColumns;
	gd = (GridData)label.getLayoutData();
	gd.horizontalSpan = numColumns;
}
public void dispose() {
	pluginsBlock.dispose();
}
protected void doFillIntoGrid(Composite parent, int numColumns) {
	label = new Label(parent, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(KEY_LABEL));
	GridData gd = new GridData(GridData.FILL_HORIZONTAL | 
							GridData.VERTICAL_ALIGN_BEGINNING);
	gd.horizontalSpan = numColumns;
	label.setLayoutData(gd);
	
	Control control = pluginsBlock.createContents(parent);
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = numColumns;
	control.setLayoutData(gd);
}
protected void doLoad() {
	IPreferenceStore store = getPreferenceStore();
	pluginsBlock.initialize(store);
}
protected void doLoadDefault() {
	pluginsBlock.initializeDefault(true);
}
protected void doStore() {
	pluginsBlock.save(getPreferenceStore());
}

public int getNumberOfControls() {
	return 1;
}
public String getPlatformPath() {
	return ((PDEBasePreferencePage)getPreferencePage()).getPlatformPath();
}
}
