package org.eclipse.pde.internal.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.pde.internal.*;
import java.util.*;
import java.io.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.preferences.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.custom.*;


public class ExternalPluginsBlock implements ICheckStateListener {
	private CheckboxTableViewer pluginListViewer;
	private Control control;
	private Button deselectAllButton;
	private Button reloadButton;
	private Button selectAllButton;
	private ExternalPluginsEditor editor;
	private Label selectedLabel;
	private static final String SAVED_ALL = "[all]";
	private static final String KEY_RELOAD = "ExternalPluginsBlock.reload";
	private static final String KEY_SELECT_ALL = "ExternalPluginsBlock.selectAll";
	private static final String KEY_DESELECT_ALL = "ExternalPluginsBlock.deselectAll";
	private static final String KEY_SELECTED = "ExternalPluginsBlock.selected";
	private static final String SAVED_NONE = "[none]";
	private int selectedCount;
	private ExternalModelManager registry;
	private Image externalPluginImage;

	public static final String CHECKED_PLUGINS = "PluginPath.checkedPlugins";

	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (editor == null || editor.getPlatformPath().length() > 0) {
				long startTime = System.currentTimeMillis();
				Object [] models = registry.getModels();
				long stopTime = System.currentTimeMillis();
				return models;
				
			} else
				return new Object[0];
		}
	}

	public class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				IPluginModel model = (IPluginModel)obj;
				return model.getPlugin().getTranslatedName();
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0) {
				return externalPluginImage;
			}
			return null;
		}
	}
	private final static int SELECT_ALL = 1;
	private final static int DESELECT_ALL = -1;
	private final static int SELECT_SOME = 0;

public ExternalPluginsBlock(ExternalPluginsEditor editor) {
	registry = PDEPlugin.getDefault().getExternalModelManager();
	externalPluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
	this.editor = editor;
}
public void checkStateChanged(CheckStateChangedEvent event) {
	IPluginModel model = (IPluginModel)event.getElement();
	model.setEnabled(event.getChecked());
	updateSelectedCount();
}
public Control createContents(Composite parent) {
	Composite container = new Composite(parent, SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginHeight = 0;
	layout.marginWidth = 5;
	container.setLayout(layout);

	pluginListViewer = new CheckboxTableViewer(container, SWT.BORDER);
	pluginListViewer.setContentProvider(new PluginContentProvider());
	pluginListViewer.setLabelProvider(new PluginLabelProvider());

	GridData gd =
		new GridData(
			GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	gd.heightHint = 200;
	if (editor==null) { 
		gd.heightHint = 300;
		gd.widthHint = 300;
	}
	pluginListViewer.getTable().setLayoutData(gd);

	Composite buttonContainer = new Composite(container, SWT.NONE);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	GridLayout buttonLayout = new GridLayout();
	buttonLayout.marginWidth = 0;
	buttonLayout.marginHeight = 0;
	buttonContainer.setLayout(buttonLayout);

	selectedLabel = new Label(container, SWT.NONE);
	gd = new GridData();
	gd.horizontalAlignment = GridData.FILL;
	gd.horizontalSpan = 2;
	selectedLabel.setLayoutData(gd);

	if (editor != null) {
		reloadButton = new Button(buttonContainer, SWT.PUSH);
		reloadButton.setText(PDEPlugin.getResourceString(KEY_RELOAD));
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		reloadButton.setLayoutData(gd);
		reloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReload();
			}
		});
	}

	selectAllButton = new Button(buttonContainer, SWT.PUSH);
	selectAllButton.setText(PDEPlugin.getResourceString(KEY_SELECT_ALL));
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	selectAllButton.setLayoutData(gd);
	selectAllButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			pluginListViewer.setAllChecked(true);
			updateSelectedCount(1);
		}
	});

	deselectAllButton = new Button(buttonContainer, SWT.PUSH);
	deselectAllButton.setText(PDEPlugin.getResourceString(KEY_DESELECT_ALL));
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	deselectAllButton.setLayoutData(gd);
	deselectAllButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			pluginListViewer.setAllChecked(false);
			updateSelectedCount(-1);
		}
	});
	this.control = container;
	return container;
}
private static Vector createSavedList(String saved) {
	Vector result = new Vector();
	StringTokenizer stok = new StringTokenizer(saved);
	while (stok.hasMoreTokens()) {
		result.add(stok.nextToken());
	}
	return result;
}
public void dispose() {
	externalPluginImage.dispose();
}
public Control getControl() {
	return control;
}
private void globalSelect(IPluginModel[] models, boolean selected) {
	for (int i = 0; i < models.length; i++) {
		models[i].setEnabled(selected);
	}
}
private void handleReload() {
	final String platformPath = editor.getPlatformPath();
	if (platformPath != null && platformPath.length() > 0) {
		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
			public void run() {
				registry.reload(platformPath);
				pluginListViewer.setInput(registry);
				initializeDefault(true);
			}
		});
	} else {
		registry.clear();
		pluginListViewer.setInput(null);
	}
}
public void initialize(IPreferenceStore store) {
	String platformPath=null;
	if (editor!=null) platformPath = editor.getPlatformPath();
	if (platformPath!=null && platformPath.length()==0) return;
	int mode;


	pluginListViewer.setInput(registry);
	String saved = store.getString(CHECKED_PLUGINS);
	if (saved.length() == 0 || saved.equals(SAVED_ALL)) {
		initializeDefault(true);
		mode = SELECT_ALL;
	} else
		if (saved.equals(SAVED_NONE)) {
			initializeDefault(false);
			mode = DESELECT_ALL;
		} else {
			Vector savedList = createSavedList(saved);

			IPluginModel[] models = registry.getModels();
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				String id = model.getPlugin().getId();
				model.setEnabled(isChecked(id, savedList));
				pluginListViewer.setChecked(model, model.isEnabled());
			}
			mode = SELECT_SOME;
		}
	pluginListViewer.addCheckStateListener(this);
	updateSelectedCount(mode);
}
public static void initialize(ExternalModelManager registry, IPreferenceStore store) {
	String saved = store.getString(CHECKED_PLUGINS);

	if (saved.length() == 0 || saved.equals(SAVED_ALL)) {
		initializeDefault(registry, true);
	} else
		if (saved.equals(SAVED_NONE)) {
			initializeDefault(registry, false);
		} else {
			Vector savedList = createSavedList(saved);

			IPluginModel[] models = registry.getModels();
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				String id = model.getPlugin().getId();
				model.setEnabled(isChecked(id, savedList));
			}
		}
}
private static int initializeDefault(ExternalModelManager registry, boolean enabled) {
	IPluginModel [] models = registry.getModels();
	for (int i=0; i<models.length; i++) {
		IPluginModel model = models[i];
		model.setEnabled(enabled);
	}
	return enabled ? models.length : 0;
}
public void initializeDefault(boolean enabled) {
	selectedCount = initializeDefault(registry, enabled);
	pluginListViewer.setAllChecked(enabled);
}
private static boolean isChecked(String name, Vector list) {
	for (int i = 0; i < list.size(); i++) {
		if (name.equals(list.elementAt(i)))
			return true;
	}
	return false;
}
public void save(IPreferenceStore store) {
	String saved = "";
	IPluginModel[] models = registry.getModels();
	if (selectedCount == models.length) {
		saved = SAVED_ALL;
	} else
		if (selectedCount == 0) {
			saved = SAVED_NONE;
		} else {
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				if (model.isEnabled()) {
					if (i > 0)
						saved += " ";
					saved += model.getPlugin().getId();
				}
			}
		}
	store.setValue(CHECKED_PLUGINS, saved);
	registry.fireModelProviderEvent(
		new ModelProviderEvent(IModelProviderEvent.MODEL_CHANGED, null));
}
private void updateSelectedCount() {
	updateSelectedCount(SELECT_SOME);
}
private void updateSelectedCount(int mode) {
	selectedCount = 0;

	IPluginModel[] models = registry.getModels();
	if (mode == SELECT_ALL) {
		selectedCount = models.length;
		globalSelect(models, true);
	} else
		if (mode == DESELECT_ALL) {
			globalSelect(models, false);
			selectedCount = 0;
		} else {
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				if (model.isEnabled())
					selectedCount++;
			}
		}
	String[] args = { ""+selectedCount };
	String selectedLabelText= PDEPlugin.getFormattedMessage(KEY_SELECTED, args);
	selectedLabel.setText(selectedLabelText);
}
}
