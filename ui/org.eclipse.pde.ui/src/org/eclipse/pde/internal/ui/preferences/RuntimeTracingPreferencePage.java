package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;
import java.util.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.*;
/**
 */
public class RuntimeTracingPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {
	public static final String KEY_DESC = "Preferences.RuntimeTracingPage.desc";
	public static final String KEY_TRACE_FILTERS =
		"Preferences.RuntimeTracingPage.traceFilters";
	public static final String KEY_TRACE_OPTIONS =
		"Preferences.RuntimeTracingPage.traceOptions";

	public static final String KEY_MASTER = "Preferences.RuntimeTracingPage.master";
	public static final String KEY_CREATE = "Preferences.RuntimeTracingPage.create";
	public static final String KEY_ACTIVATEPLUGIN =
		"Preferences.RuntimeTracingPage.activateplugin";
	public static final String KEY_ACTIONS =
		"Preferences.RuntimeTracingPage.actions";
	public static final String KEY_SUCCESS =
		"Preferences.RuntimeTracingPage.success";
	public static final String KEY_FAILURE =
		"Preferences.RuntimeTracingPage.failure";
	public static final String KEY_LOADER = "Preferences.RuntimeTracingPage.loader";
	public static final String KEY_CLASS = "Preferences.RuntimeTracingPage.class";
	public static final String KEY_RESOURCE =
		"Preferences.RuntimeTracingPage.resource";
	public static final String KEY_NATIVE = "Preferences.RuntimeTracingPage.native";

	private CheckOption master;
	private Properties options;
	private boolean wasHidden = false;
	private static final String PLUGIN_ID = "org.eclipse.core.runtime";
	private Vector checks = new Vector();
	private Vector filters = new Vector();
/**
 * RuntimeTracingPreferencePage constructor comment.
 */
public RuntimeTracingPreferencePage() {
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
	initializeOptions();
}
/**
 * Creates and returns the SWT control for the customized body 
 * of this preference page under the given parent composite.
 * <p>
 * This framework method must be implemented by concrete
 * subclasses.
 * </p>
 *
 * @param parent the parent composite
 * @return the new control
 */
protected Control createContents(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);

	master.createControl(container);

	Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
	layout = new GridLayout();
	group.setLayout(layout);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	group.setLayoutData(gd);
	group.setText(PDEPlugin.getResourceString(KEY_TRACE_OPTIONS));
	for (int i=0; i<checks.size(); i++) {
		CheckOption check = (CheckOption)checks.elementAt(i);
		check.createControl(group);
	}

	group = new Group(container, SWT.SHADOW_ETCHED_IN);
	layout = new GridLayout();
	layout.numColumns = 2;
	group.setLayout(layout);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	group.setLayoutData(gd);
	group.setText(PDEPlugin.getResourceString(KEY_TRACE_FILTERS));
	for (int i=0; i<filters.size(); i++) {
		TextOption filter = (TextOption)filters.elementAt(i);
		filter.createControl(group);
	}
	initializeValues(options);
	return container;
}
/**
 * Initializes this preference page for the given workbench.
 * <p>
 * This method is called automatically as the preference page is being created
 * and initialized. Clients must not call this method.
 * </p>
 *
 * @param workbench the workbench
 */
public void init(IWorkbench workbench) {}
/**
 */
private void initializeCheck(String key, String label) {
	checks.add(new CheckOption(key, label));
}
/**
 */
private void initializeOptions() {
	TracingOptionsManager mng = PDEPlugin.getDefault().getTracingOptionsManager();
	options = mng.getTracingOptions();
	String prefix = PLUGIN_ID+"/loader/debug";

	master = new CheckOption(prefix, PDEPlugin.getResourceString(KEY_MASTER));

	prefix += "/";

	initializeCheck(prefix + "create", PDEPlugin.getResourceString(KEY_CREATE));
	initializeCheck(prefix + "activateplugin", PDEPlugin.getResourceString(KEY_ACTIVATEPLUGIN));
	initializeCheck(prefix + "actions", PDEPlugin.getResourceString(KEY_ACTIONS));
	initializeCheck(prefix + "success", PDEPlugin.getResourceString(KEY_SUCCESS));
	initializeCheck(prefix + "failure", PDEPlugin.getResourceString(KEY_FAILURE));

	prefix += "filter/";
	initializeText(prefix + "loader", PDEPlugin.getResourceString(KEY_LOADER));
	initializeText(prefix + "class", PDEPlugin.getResourceString(KEY_CLASS));
	initializeText(prefix + "resource", PDEPlugin.getResourceString(KEY_RESOURCE));
	initializeText(prefix + "native", PDEPlugin.getResourceString(KEY_NATIVE));
}
/**
 */
private void initializeText(String key, String label) {
	filters.add(new TextOption(key, label));
}
/**
 */
private void initializeValues(Properties store) {
	master.load(store);
	for (int i=0; i<checks.size(); i++) {
		RuntimeOption option = (RuntimeOption)checks.elementAt(i);
		option.load(store);
	}
	for (int i=0; i<filters.size(); i++) {
		RuntimeOption option = (RuntimeOption)filters.elementAt(i);
		option.load(store);
	}
}
/**
 * Performs special processing when this page's Defaults button has been pressed.
 * <p>
 * This is a framework hook method for sublcasses to do special things when
 * the Defaults button has been pressed.
 * Subclasses may override, but should call <code>super.performDefaults</code>.
 * </p>
 */
protected void performDefaults() {
	super.performDefaults();
	resetValues();
}
/** 
 * Method declared on IPreferencePage.
 * Subclasses should override
 */
public boolean performOk() {
	storeValues(options);
	TracingOptionsManager mng = PDEPlugin.getDefault().getTracingOptionsManager();
	mng.setTracingOptions(options);
	mng.save();
	return true;
}
/**
 * @param option org.eclipse.pde.ui.internal.preferences.RuntimeOption
 */
private void resetValue(RuntimeOption option, Hashtable template) {
	option.setValue(template.get(option.getKey()));
}
/**
 */
private void resetValues() {
	Hashtable template =
		PDEPlugin.getDefault().getTracingOptionsManager().getTemplateTable(PLUGIN_ID);
	resetValue(master, template);
	for (int i = 0; i < checks.size(); i++) {
		RuntimeOption option = (RuntimeOption) checks.elementAt(i);
		resetValue(option, template);
	}
	for (int i = 0; i < filters.size(); i++) {
		RuntimeOption option = (RuntimeOption) filters.elementAt(i);
		resetValue(option, template);
	}
}
/* (non-Javadoc)
 * Method declared on IDialogPage.
 */
public void setVisible(boolean visible) {
	if (visible) {
		if (wasHidden) {
			// we are coming back to this page - must update
			updateValues();
		}
		wasHidden = false;
	} else {
		wasHidden = true;
	}
	super.setVisible(visible);
}
/**
 */
private void storeValues(Properties store) {
	syncUpMasters(store);
	for (int i=0; i<checks.size(); i++) {
		RuntimeOption option = (RuntimeOption)checks.elementAt(i);
		option.store(store);
	}
	for (int i=0; i<filters.size(); i++) {
		RuntimeOption option = (RuntimeOption)filters.elementAt(i);
		option.store(store);
	}
}
/**
 */
private void syncUpMasters(Properties store) {
	master.store(store);
	String value = master.getValue().toLowerCase();
	if (value.equals("true")) {
		String key = PLUGIN_ID+"/debug";
		store.setProperty(key, "true");
	}
}
/**
 */
private void updateValues() {
	TracingOptionsManager mng = PDEPlugin.getDefault().getTracingOptionsManager();
	options = mng.getTracingOptions();
	initializeValues(options);
}
}
