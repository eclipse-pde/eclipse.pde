/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.pde.internal.elements.DefaultContentProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;

public class UpdateBuildpathWizardPage extends StatusWizardPage {
	private IPluginModelBase[] selected;
	private Button showNamesCheck;
	private Image pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
	private Image fragmentImage =
		PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
	private Image errorPluginImage = PDEPluginImages.get(PDEPluginImages.IMG_ERR_PLUGIN_OBJ);
	private Image errorFragmentImage =
		PDEPluginImages.get(PDEPluginImages.IMG_ERR_FRAGMENT_OBJ);
	private boolean block;
	private CheckboxTableViewer pluginListViewer;
	private int counter;
	private Label counterLabel;
	private static final String SETTINGS_SHOW_IDS = "showIds";
	private static final String KEY_TITLE = "UpdateBuildpathWizard.title";
	private static final String KEY_DESC = "UpdateBuildpathWizard.desc";
	private static final String KEY_SHOW_NAMES =
		"ImportWizard.DetailedPage.showNames";
	private static final String KEY_PLUGIN_LIST =
		"ImportWizard.DetailedPage.pluginList";
	private static final String KEY_SELECT_ALL =
		"ImportWizard.DetailedPage.selectAll";
	private static final String KEY_DESELECT_ALL =
		"ImportWizard.DetailedPage.deselectAll";
	private static final String KEY_NO_PLUGINS = "ImportWizard.messages.noPlugins";
	private static final String KEY_NO_SELECTED =
		"ImportWizard.errors.noPluginSelected";
	private static final String KEY_SELECTED = "ImportWizard.DetailedPage.selected";
	private static final String KEY_OUT_OF_SYNC = "WorkspaceModelManager.outOfSync";

	public class BuildpathContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModels();
		}
	}

	public class BuildpathLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				IPluginModelBase model = (IPluginModelBase) obj;
				IPluginBase plugin = model.getPluginBase();
				String name;
				if (showNamesCheck.getSelection())
					name = plugin.getTranslatedName();
				else
					name = plugin.getId();

				String version = plugin.getVersion();
				String result = name + " (" + version + ")";
				if (!model.isInSync()) 
					result += " "+PDEPlugin.getResourceString(KEY_OUT_OF_SYNC);
				return result;
					
			}
			return "";
		}
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0) {
				if (obj instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase)obj;
					boolean error = !(model.isLoaded() && model.isInSync());
					if (model instanceof IFragmentModel) {
						return error?errorFragmentImage:fragmentImage;
					}
					else {
						return error?errorPluginImage:pluginImage;
					}
				}
			}
			return null;
		}
	}

	public UpdateBuildpathWizardPage(IPluginModelBase[] selected) {
		super("UpdateBuildpathWizardPage", true);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		this.selected = selected;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		showNamesCheck = new Button(container, SWT.CHECK);
		showNamesCheck.setText(PDEPlugin.getResourceString(KEY_SHOW_NAMES));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		showNamesCheck.setLayoutData(gd);
		showNamesCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!block)
					pluginListViewer.refresh();
			}
		});
		IDialogSettings settings = getDialogSettings();
		showNamesCheck.setSelection(!settings.getBoolean(SETTINGS_SHOW_IDS));

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PLUGIN_LIST));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		pluginListViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		pluginListViewer.setContentProvider(new BuildpathContentProvider());
		pluginListViewer.setLabelProvider(new BuildpathLabelProvider());
		pluginListViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				modelChecked((IPluginModelBase) event.getElement(), event.getChecked());
			}
		});
		pluginListViewer.setSorter(ListUtil.NAME_SORTER);

		gd =
			new GridData(
				GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint = 300;
		gd.widthHint = 300;
		gd.verticalSpan = 2;

		pluginListViewer.getTable().setLayoutData(gd);
		pluginListViewer.setInput(PDEPlugin.getDefault());
		pluginListViewer.setCheckedElements(selected);
		counter = selected.length;

		Button button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString(KEY_SELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(true);
			}
		});
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);

		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString(KEY_DESELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(false);
			}
		});
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);

		counterLabel = new Label(container, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		dialogChanged();
		setControl(container);
	}

	private void handleSelectAll(boolean selected) {
		pluginListViewer.setAllChecked(selected);
		if (selected)
			counter = pluginListViewer.getCheckedElements().length;
		else
			counter = 0;
		dialogChanged();
	}
	
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(SETTINGS_SHOW_IDS, !showNamesCheck.getSelection());
	}

	public Object[] getSelected() {
		return pluginListViewer.getCheckedElements();
	}

	private void modelChecked(IPluginModelBase model, boolean checked) {
		if (checked)
			counter++;
		else
			counter--;
		dialogChanged();
	}

	private void dialogChanged() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
		updateCounterLabel();
	}

	protected void updateCounterLabel() {
		String[] args = { "" + counter };
		String selectedLabelText = PDEPlugin.getFormattedMessage(KEY_SELECTED, args);
		counterLabel.setText(selectedLabelText);
	}

	private Object[] getModels() {
		IPluginModelBase[] plugins =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
		IPluginModelBase[] fragments =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
		IPluginModelBase[] all =
			new IPluginModelBase[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(fragments, 0, all, plugins.length, fragments.length);
		return all;
	}

	private IStatus validatePlugins() {
		Object[] allModels = getModels();
		if (allModels == null || allModels.length == 0) {
			return createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_NO_PLUGINS));
		}
		if (counter == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_SELECTED));
		}
		return createStatus(IStatus.OK, "");
	}
}