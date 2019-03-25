/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404 and bug 207064
 *     EclipseSource Corporation - ongoing enhancements
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Karsten Thoms <karsten.thoms@itemis.de> - Bug 522332
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.dialogs.PatternFilter;

public abstract class AbstractPluginBlock {

	protected AbstractLauncherTab fTab;

	private FilteredCheckboxTree fPluginFilteredTree;
	protected CachedCheckboxTreeViewer fPluginTreeViewer;
	private NamedElement fWorkspacePlugins;
	private NamedElement fExternalPlugins;
	private IPluginModelBase[] fExternalModels;
	private IPluginModelBase[] fWorkspaceModels;
	protected int fNumExternalChecked;
	protected int fNumWorkspaceChecked;

	private Button fIncludeOptionalButton;
	protected Button fAddWorkspaceButton;
	private Button fAutoValidate;

	private Button fSelectAllButton;
	private Button fDeselectButton;
	private Button fWorkingSetButton;
	private Button fAddRequiredButton;
	private Button fDefaultsButton;
	private Button fFilterButton;

	private Listener fListener = new Listener();

	private Label fCounter;

	private LaunchValidationOperation fOperation;

	private Button fValidateButton;

	private HashMap<Object, String> levelColumnCache = new HashMap<>();
	private HashMap<Object, String> autoColumnCache = new HashMap<>();
	private TreeEditor levelColumnEditor = null;
	private TreeEditor autoColumnEditor = null;
	private boolean fIsDisposed = false;

	private PluginStatusDialog fDialog;

	class PluginModelNameBuffer {
		private List<String> nameList;

		PluginModelNameBuffer() {
			super();
			nameList = new ArrayList<>();
		}

		void add(IPluginModelBase model) {
			nameList.add(getPluginName(model));
		}

		private String getPluginName(IPluginModelBase model) {
			String startLevel = null;
			String autoStart = null;
			if (fPluginTreeViewer.isCheckedLeafElement(model)) {
				startLevel = levelColumnCache.get(model) != null ? levelColumnCache.get(model).toString() : null;
				autoStart = autoColumnCache.get(model) != null ? autoColumnCache.get(model).toString() : null;
			}
			return BundleLauncherHelper.writeBundleEntry(model, startLevel, autoStart);
		}

		@Override
		public String toString() {
			Collections.sort(nameList);
			StringBuilder result = new StringBuilder();
			for (String name : nameList) {
				if (result.length() > 0) {
					result.append(',');
				}
				result.append(name);
			}

			if (result.length() == 0) {
				return null;
			}

			return result.toString();
		}
	}

	/**
	 * Label provider for the tree.
	 */
	class OSGiLabelProvider extends PDELabelProvider {

		@Override
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}

		@Override
		public String getColumnText(Object obj, int index) {
			boolean isChecked = fPluginTreeViewer.getChecked(obj);
			switch (index) {
				case 0 :
					return super.getColumnText(obj, index);
				case 1 :
					if (isChecked && levelColumnCache != null && levelColumnCache.containsKey(obj)) {
						return levelColumnCache.get(obj);
					}
					return ""; //$NON-NLS-1$
				case 2 :
					if (isChecked && autoColumnCache != null && autoColumnCache.containsKey(obj)) {
						return autoColumnCache.get(obj);
					}
					return ""; //$NON-NLS-1$
				default :
					return ""; //$NON-NLS-1$
			}
		}
	}

	class Listener extends SelectionAdapter {

		private void filterAffectingControl(SelectionEvent e) {
			boolean resetFilterButton = false;
			Object source = e.getSource();

			// If the filter is on, turn it off, apply the action, and turn it back on.
			// This has to happen this way because there is no real model behind
			// the view.  The only model is the actual plug-in model, and the state
			// does not get set on that model until an apply is performed.
			if (fFilterButton.getSelection()) {
				fFilterButton.setSelection(false);
				handleFilterButton();
				resetFilterButton = true;
			}
			if (source == fSelectAllButton) {
				toggleGroups(true);
			} else if (source == fDeselectButton) {
				toggleGroups(false);
			} else if (source == fWorkingSetButton) {
				handleWorkingSets();
			} else if (source == fAddRequiredButton) {
				addRequiredPlugins();
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			}

			if (resetFilterButton) {
				resetFilterButton = false;
				fFilterButton.setSelection(true);
				handleFilterButton();
			}
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();

			if (source == fFilterButton) {
				handleFilterButton();
			} else if (source == fSelectAllButton || source == fDeselectButton || source == fWorkingSetButton || source == fAddRequiredButton || source == fDefaultsButton) {
				// These are all the controls that may affect the filtering.  For example, the filter
				// is enabled only to show selected bundles, and the user invokes "select all", we need
				// to update the filter.
				filterAffectingControl(e);
			} else if (source == fValidateButton) {
				handleValidate();
			}
			if (!fIsDisposed) {
				fTab.updateLaunchConfigurationDialog();
			}
		}
	}

	class PluginContentProvider implements ITreeContentProvider {
		@Override
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IPluginModelBase);
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent == fExternalPlugins) {
				return getExternalModels();
			}
			if (parent == fWorkspacePlugins) {
				return getWorkspaceModels();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object child) {
			if (child instanceof IPluginModelBase) {
				IResource resource = ((IPluginModelBase) child).getUnderlyingResource();
				return resource == null ? fExternalPlugins : fWorkspacePlugins;
			}
			return null;
		}

		@Override
		public Object[] getElements(Object input) {
			ArrayList<NamedElement> list = new ArrayList<>();
			if (getWorkspaceModels().length > 0) {
				list.add(fWorkspacePlugins);
			}
			if (getExternalModels().length > 0) {
				list.add(fExternalPlugins);
			}
			return list.toArray();
		}
	}

	public AbstractPluginBlock(AbstractLauncherTab tab) {
		fTab = tab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/**
	 * Returns an array of external plugins that are currently enabled.
	 * @return array of external enabled plugins, possibly empty
	 */
	protected IPluginModelBase[] getExternalModels() {
		if (fExternalModels == null) {
			fExternalModels = collectModelsToDisplay(PluginRegistry.getExternalModels());
		}
		return fExternalModels;
	}

	/**
	 * Returns an array of plugins from the workspace.  Non-OSGi plugins (no valid bundle
	 * manifest) will be filtered out.
	 * @return array of workspace OSGi plugins, possibly empty
	 */
	protected IPluginModelBase[] getWorkspaceModels() {
		if (fWorkspaceModels == null) {
			fWorkspaceModels = collectModelsToDisplay(PluginRegistry.getWorkspaceModels());
		}
		return fWorkspaceModels;
	}

	private IPluginModelBase[] collectModelsToDisplay(IPluginModelBase[] models) {
		SourcePluginFilter sourcePluginFilter = new SourcePluginFilter();
		return Arrays.stream(models)
				.filter(model -> model.getBundleDescription() != null)
				.filter(sourcePluginFilter)
				.toArray(IPluginModelBase[]::new);
	}

	protected void updateCounter() {
		if (fCounter != null) {
			int checked = fNumExternalChecked + fNumWorkspaceChecked;
			int total = getWorkspaceModels().length + getExternalModels().length;
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, Integer.valueOf(checked), Integer.valueOf(total)));
		}
	}

	public void createControl(Composite parent, int span, int indent) {
		createPluginViewer(parent, span - 1, indent);
		createButtonContainer(parent);
		if (fTab instanceof PluginsTab) {
			fIncludeOptionalButton = createButton(parent, span, indent,PDEUIMessages.AdvancedLauncherTab_includeOptional_plugins);
		}else if (fTab instanceof BundlesTab) {
			fIncludeOptionalButton = createButton(parent, span, indent, PDEUIMessages.AdvancedLauncherTab_includeOptional_bundles);
		}else{
			fIncludeOptionalButton = createButton(parent, span, indent, NLS.bind(PDEUIMessages.AdvancedLauncherTab_includeOptional, fTab.getName().toLowerCase(Locale.ENGLISH)));
		}
		if (fTab instanceof PluginsTab) {
			fAddWorkspaceButton = createButton(parent, span, indent, PDEUIMessages.AdvancedLauncherTab_addNew_plugins);
		}else if (fTab instanceof BundlesTab) {
			fAddWorkspaceButton = createButton(parent, span, indent,PDEUIMessages.AdvancedLauncherTab_addNew_bundles);
		}else{
			fAddWorkspaceButton = createButton(parent, span, indent, NLS.bind(PDEUIMessages.AdvancedLauncherTab_addNew, fTab.getName().toLowerCase(Locale.ENGLISH)));
		}


		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gd);

		if (fTab instanceof PluginsTab) {
			fAutoValidate = createButton(parent, span - 1, indent, PDEUIMessages.PluginsTabToolBar_auto_validate_plugins);
		} else if (fTab instanceof BundlesTab) {
			fAutoValidate = createButton(parent, span - 1, indent, PDEUIMessages.PluginsTabToolBar_auto_validate_bundles);
		} else{
			fAutoValidate = createButton(parent, span - 1, indent, NLS.bind(PDEUIMessages.PluginsTabToolBar_auto_validate, fTab.getName().replaceAll("&", "").toLowerCase(Locale.ENGLISH))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		fValidateButton = new Button(parent, SWT.PUSH);
		fValidateButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		if (fTab instanceof PluginsTab) {
			fValidateButton.setText(PDEUIMessages.PluginsTabToolBar_validate_plugins);
		} else if (fTab instanceof BundlesTab) {
			fValidateButton.setText(PDEUIMessages.PluginsTabToolBar_validate_bundles);
		} else {
			fValidateButton.setText(NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName().replaceAll("&", ""))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		SWTUtil.setButtonDimensionHint(fValidateButton);
		fValidateButton.addSelectionListener(fListener);
	}

	private Button createButton(Composite parent, int span, int indent, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);

		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		button.setLayoutData(gd);
		button.addSelectionListener(fListener);
		button.setFont(parent.getFont());
		return button;
	}

	protected ILabelProvider getLabelProvider() {
		return new OSGiLabelProvider();
	}

	protected void createPluginViewer(Composite composite, int span, int indent) {
		PatternFilter patternFilter = new PatternFilter();
		patternFilter.setIncludeLeadingWildcard(true);
		fPluginFilteredTree = new FilteredCheckboxTree(composite, null, getTreeViewerStyle(), patternFilter);
		fPluginTreeViewer = fPluginFilteredTree.getCheckboxTreeViewer();

		fPluginTreeViewer.addCheckStateListener(event -> {
			// Since a check on the root of a CheckBoxTreeViewer selects all its children
			// (hidden or not), we need to ensure that all items are shown
			// if this happens.  Since it not clear what the best behaviour is here
			// this just "un-selects" the filter button.

			if (!event.getChecked())
			 {
				return; // just return if the check state goes to false
			}
			// It is not clear if this is the best approach, but it
			// is hard to tell without user feedback.
			TreeItem[] items = fPluginTreeViewer.getTree().getItems();
			for (TreeItem item : items) {
				if (event.getElement() == item.getData()) {
					// If the even happens on the root of the tree
					fFilterButton.setSelection(false);
					handleFilterButton();
					return;
				}
			}
		});
		fPluginTreeViewer.setContentProvider(new PluginContentProvider());
		fPluginTreeViewer.setLabelProvider(getLabelProvider());
		fPluginTreeViewer.setAutoExpandLevel(2);
		fPluginTreeViewer.addCheckStateListener(event -> {
			Object element = event.getElement();
			if (element instanceof IPluginModelBase) {
				handleCheckStateChanged(event);
			} else {
				countSelectedModels();
			}
			fTab.updateLaunchConfigurationDialog();
		});
		fPluginTreeViewer.setComparator(new ListUtil.PluginComparator() {
			@Override
			public int category(Object obj) {
				if (obj == fWorkspacePlugins) {
					return -1;
				}
				return 0;
			}
		});

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fPluginFilteredTree.setLayoutData(gd);

		Image siteImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_SITE_OBJ);

		fWorkspacePlugins = new NamedElement(PDEUIMessages.AdvancedLauncherTab_workspacePlugins, siteImage);
		fExternalPlugins = new NamedElement(PDEUIMessages.PluginsTab_target, siteImage);

		fPluginTreeViewer.addFilter(new Filter());

		Tree tree = fPluginTreeViewer.getTree();

		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText(fTab.getName());
		column1.setWidth(400);

		TreeColumn column2 = new TreeColumn(tree, SWT.CENTER);
		column2.setText(PDEUIMessages.EquinoxPluginBlock_levelColumn);
		column2.setWidth(80);

		TreeColumn column3 = new TreeColumn(tree, SWT.CENTER);
		column3.setText(PDEUIMessages.EquinoxPluginBlock_autoColumn);
		column3.setWidth(80);
		tree.setHeaderVisible(true);

		tree.setFont(composite.getFont());
		fPluginFilteredTree.getFilterControl().setFont(composite.getFont());

		createEditors();
	}

	private void createEditors() {
		final Tree tree = fPluginTreeViewer.getTree();

		levelColumnEditor = new TreeEditor(tree);
		levelColumnEditor.horizontalAlignment = SWT.CENTER;
		levelColumnEditor.minimumWidth = 60;
		if (Util.isMac()) {
			levelColumnEditor.minimumHeight = 27;
		}

		autoColumnEditor = new TreeEditor(tree);
		autoColumnEditor.horizontalAlignment = SWT.CENTER;
		autoColumnEditor.grabHorizontal = true;
		autoColumnEditor.minimumWidth = 60;

		tree.addSelectionListener(widgetSelectedAdapter(e -> {
			// Clean up any previous editor control
			Control oldEditor = levelColumnEditor.getEditor();
			if (oldEditor != null && !oldEditor.isDisposed()) {
				oldEditor.dispose();
			}

			oldEditor = autoColumnEditor.getEditor();
			if (oldEditor != null && !oldEditor.isDisposed()) {
				oldEditor.dispose();
			}

			// Identify the selected row
			final TreeItem item = (TreeItem) e.item;
			if (item != null && !isEditable(item)) {
				return;
			}

			if (item != null && !isFragment(item)) { // only display editing controls if we're not a fragment
				final Spinner spinner = new Spinner(tree, SWT.BORDER);
				spinner.setMinimum(0);
				String level = item.getText(1);
				int defaultLevel = level.length() == 0 || "default".equals(level) ? 0 : Integer.parseInt(level); //$NON-NLS-1$
				spinner.setSelection(defaultLevel);
				spinner.addModifyListener(e1 -> {
					if (item.getChecked()) {
						int selection = spinner.getSelection();
						item.setText(1, selection == 0 ? "default" //$NON-NLS-1$
								: Integer.toString(selection));
						levelColumnCache.put(item.getData(), item.getText(1));
						fTab.updateLaunchConfigurationDialog();
					}
				});
				levelColumnEditor.setEditor(spinner, item, 1);

				final CCombo combo = new CCombo(tree, SWT.BORDER | SWT.READ_ONLY);
				combo.setItems(new String[] {"default", Boolean.toString(true), Boolean.toString(false)}); //$NON-NLS-1$
				combo.setText(item.getText(2));
				combo.pack();
				combo.addSelectionListener(widgetSelectedAdapter(event -> {
					if (item.getChecked()) {
						item.setText(2, combo.getText());
						autoColumnCache.put(item.getData(), item.getText(2));
						fTab.updateLaunchConfigurationDialog();
					}
				}));
				autoColumnEditor.setEditor(combo, item, 2);
			}
		}));
	}

	private boolean isEditable(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) obj;
			String systemBundleId = PDECore.getDefault().getModelManager().getSystemBundleId();
			if (!(systemBundleId.equals(model.getPluginBase().getId()))) {
				return fPluginTreeViewer.getChecked(model);
			}
		}
		return false;
	}

	private boolean isFragment(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			return ((IPluginModelBase) obj).isFragmentModel();
		}
		return false;
	}

	/**
	 * The view filter for the tree view.  Currently this filter only
	 * filters unchecked items if the fFilterButton is selected.
	 *
	 * @author Ian Bull
	 *
	 */
	class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (fFilterButton.getSelection()) {
				return fPluginTreeViewer.getChecked(element);
			}
			return true;
		}
	}

	private void createButtonContainer(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll, SWT.PUSH);
		fDeselectButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll, SWT.PUSH);
		fWorkingSetButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_workingSet, SWT.PUSH);
		if (fTab instanceof PluginsTab) {
			fAddRequiredButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_subset_plugins, SWT.PUSH);
		} else if (fTab instanceof BundlesTab) {
			fAddRequiredButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_subset_bundles, SWT.PUSH);
		} else {
			fAddRequiredButton = createButton(composite, NLS.bind(PDEUIMessages.AdvancedLauncherTab_subset, fTab.getName()), SWT.PUSH);
		}

		fDefaultsButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_defaults, SWT.PUSH);
		fFilterButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectedBundles, SWT.CHECK);
		GridData filterButtonGridData = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_END);
		fFilterButton.setLayoutData(filterButtonGridData);

		fCounter = SWTFactory.createLabel(composite, "", 1); //$NON-NLS-1$
		fCounter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END));
		updateCounter();
	}

	protected int getTreeViewerStyle() {
		return SWT.BORDER | SWT.FULL_SELECTION;
	}

	private Button createButton(Composite composite, String text, int style) {
		Button button = new Button(composite, style);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(fListener);
		return button;
	}

	protected void handleCheckStateChanged(CheckStateChangedEvent event) {
		countSelectedModels();
		resetText((IPluginModelBase) event.getElement());
	}

	protected void setText(IPluginModelBase model, String value) {
		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (widget instanceof TreeItem) {
			TreeItem item = (TreeItem) widget;
			int index = value == null ? -1 : value.indexOf(':');
			String levelValue = index == -1 ? "" : value.substring(0, index); //$NON-NLS-1$
			String autoValue = null;
			String modelName = model.getBundleDescription().getSymbolicName();
			item.setText(1, levelValue);
			if (model.isFragmentModel()) {
				autoValue = "false"; //$NON-NLS-1$
				item.setText(2, autoValue);
				// FIXME is this the right place for this logic?
			} else if (IPDEBuildConstants.BUNDLE_CORE_RUNTIME.equals(modelName) || IPDEBuildConstants.BUNDLE_DS.equals(modelName)) {
				autoValue = "true"; //$NON-NLS-1$
				item.setText(2, autoValue);
			} else {
				autoValue = index == -1 ? "" : value.substring(index + 1); //$NON-NLS-1$
				item.setText(2, autoValue);
			}
			levelColumnCache.put(model, levelValue);
			autoColumnCache.put(model, autoValue);
		}
	}

	protected void resetText(IPluginModelBase model) {
		String levelText = ""; //$NON-NLS-1$
		String autoText = ""; //$NON-NLS-1$

		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (fPluginTreeViewer.getChecked(model)) {
			levelText = levelColumnCache.get(model);
			levelText = levelText == null || levelText.length() == 0 ? "default" : levelText; //$NON-NLS-1$
			autoText = autoColumnCache.get(model);
			autoText = autoText == null || autoText.length() == 0 ? "default" : autoText; //$NON-NLS-1$

			// Replace run levels and auto start values for certain important system bundles
			String systemValue = BundleLauncherHelper.resolveSystemRunLevelText(model);
			levelText = systemValue != null ? systemValue : levelText;

			systemValue = BundleLauncherHelper.resolveSystemAutoText(model);
			autoText = systemValue != null ? systemValue : autoText;

			// Recache the values in case they changed.  I believe the code to only recache
			// if they actually changed takes more time than just setting the value.
			levelColumnCache.put(model, levelText);
			autoColumnCache.put(model, autoText);
		}

		// Set values in UI (although I'm not sure why we don't use the label provider here)
		if (widget instanceof TreeItem) {
			((TreeItem) widget).setText(1, levelText);
		}

		if (widget instanceof TreeItem) {
			((TreeItem) widget).setText(2, autoText);
		}
	}

	protected void handleGroupStateChanged(Object group, boolean checked) {
		if (!fPluginFilteredTree.getPatternFilter().select(fPluginTreeViewer, null, group)) {
			return;
		}

		fPluginTreeViewer.setChecked(group, checked);

		if (group instanceof NamedElement) {
			NamedElement namedElement = (NamedElement) group;
			TreeItem item = (TreeItem) fPluginTreeViewer.testFindItem(namedElement);
			if (item != null) {
				TreeItem[] children = item.getItems();
				if (children == null) {
					return;
				}
				for (TreeItem childItem : children) {
					Object child = childItem.getData();
					if (child instanceof IPluginModelBase) {
						resetText((IPluginModelBase) child);
					}
				}
			}
		}
	}

	protected void toggleGroups(boolean select) {
		handleGroupStateChanged(fWorkspacePlugins, select);
		handleGroupStateChanged(fExternalPlugins, select);

		countSelectedModels();
	}

	protected void handleFilterButton() {
		fPluginTreeViewer.refresh();
		fPluginTreeViewer.expandAll();
	}

	private void handleWorkingSets() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(getShell(), true);
		if (dialog.open() == Window.OK) {
			String[] ids = getPluginIDs(dialog.getSelection());
			for (String id : ids) {
				IPluginModelBase model = PluginRegistry.findModel(id);
				if (model != null) {
					if (!fPluginTreeViewer.getChecked(model)) {
						setChecked(model, true);
					}
				}
			}
			countSelectedModels();
		}
	}

	protected void setChecked(IPluginModelBase model, boolean checked) {
		fPluginTreeViewer.setChecked(model, checked);
		resetText(model);
	}

	private String[] getPluginIDs(IWorkingSet[] workingSets) {
		HashSet<String> set = new HashSet<>();
		for (IWorkingSet workingSet : workingSets) {
			IAdaptable[] elements = workingSet.getElements();
			for (IAdaptable element2 : elements) {
				Object element = element2;
				if (element instanceof PersistablePluginObject) {
					set.add(((PersistablePluginObject) element).getPluginID());
				} else {
					if (element instanceof IJavaProject) {
						element = ((IJavaProject) element).getProject();
					}
					if (element instanceof IProject) {
						IPluginModelBase model = PluginRegistry.findModel((IProject) element);
						if (model != null) {
							set.add(model.getPluginBase().getId());
						}
					}
				}
			}
		}
		return set.toArray(new String[set.size()]);
	}

	/**
	 * Initializes the contents of this block from the given config.  The table's input
	 * will only be initialized if the boolean parameter is set to true.
	 *
	 * @param config launch configuration to init from or <code>null</code>
	 * @param enableTable whether to set the input on the table
	 * @throws CoreException
	 */
	public void initializeFrom(ILaunchConfiguration config, boolean enableTable) throws CoreException {
		levelColumnCache = new HashMap<>();
		autoColumnCache = new HashMap<>();
		fPluginFilteredTree.getPatternFilter().setPattern(null);
		fIncludeOptionalButton.setSelection(config.getAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true));
		fAddWorkspaceButton.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true));
		fAutoValidate.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, true));
		if (!enableTable) {
			fPluginTreeViewer.setInput(null);
		} else if (fPluginTreeViewer.getInput() == null) {
			fPluginTreeViewer.setUseHashlookup(true);
			fPluginTreeViewer.setInput(PDEPlugin.getDefault());
			fPluginTreeViewer.reveal(fWorkspacePlugins);
		}
		fFilterButton.setSelection(config.getAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false));
	}

	/**
	 * Looks at the currently checked plugins and finds any plug-ins required by them.  The required plug-ins are
	 * then also checked in the tree
	 */
	protected void addRequiredPlugins() {
		Object[] checked = fPluginTreeViewer.getCheckedLeafElements();
		Set<IPluginModelBase> toCheck = Arrays.stream(checked).filter(IPluginModelBase.class::isInstance)
				.map(IPluginModelBase.class::cast).collect(Collectors.toSet());

		Set<String> additionalIds = DependencyManager.getDependencies(toCheck.toArray(),
				fIncludeOptionalButton.getSelection(), null);

		additionalIds.stream().map(id -> PluginRegistry.findEntry(id))
				.filter(Objects::nonNull).map(entry -> entry.getModel())
				.forEach(model -> toCheck.add(model));

		checked = toCheck.toArray();
		setCheckedElements(checked);

		countSelectedModels();
	}

	protected IPluginModelBase findPlugin(String id) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getModel();
			if (fPluginTreeViewer.getChecked(model)) {
				return model;
			}

			IPluginModelBase[] models = entry.getWorkspaceModels();
			for (IPluginModelBase pluginModel : models) {
				if (fPluginTreeViewer.getChecked(pluginModel)) {
					return pluginModel;
				}
			}

			models = entry.getExternalModels();
			for (IPluginModelBase pluginModel : models) {
				if (fPluginTreeViewer.getChecked(pluginModel)) {
					return pluginModel;
				}
			}
			return null;
		}
		return null;
	}

	protected void setCheckedElements(Object[] checked) {
		fPluginTreeViewer.setCheckedElements(checked);
		updateGroup(fWorkspacePlugins);
		updateGroup(fExternalPlugins);
	}

	private void updateGroup(Object group) {
		Widget item = fPluginTreeViewer.testFindItem(group);
		if (item instanceof TreeItem) {
			TreeItem[] items = ((TreeItem) item).getItems();
			for (TreeItem childItem : items) {
				if (childItem.getChecked() == (childItem.getText(1).length() == 0)) {
					Object model = childItem.getData();
					if (model instanceof IPluginModelBase) {
						resetText((IPluginModelBase) model);
					}
				}
			}
		}
	}

	private void countSelectedModels() {
		fNumWorkspaceChecked = countChecked(getWorkspaceModels());
		fNumExternalChecked = countChecked(getExternalModels());
	}

	private int countChecked(Object[] elements) {
		return (int) Arrays.stream(elements).filter(fPluginTreeViewer::isCheckedLeafElement).count();
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, fIncludeOptionalButton.getSelection());
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, fAddWorkspaceButton.getSelection());
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, fAutoValidate.getSelection());
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, fFilterButton.getSelection());
		savePluginState(config);
		updateCounter();
	}

	protected abstract void savePluginState(ILaunchConfigurationWorkingCopy config);

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, true);
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
	}

	public void enableViewer(boolean enable) {
		fPluginFilteredTree.setEnabled(enable);
		fAddRequiredButton.setEnabled(enable);
		fDefaultsButton.setEnabled(enable);
		fWorkingSetButton.setEnabled(enable);
		fSelectAllButton.setEnabled(enable);
		fDeselectButton.setEnabled(enable);
		fIncludeOptionalButton.setEnabled(enable);
		fAddWorkspaceButton.setEnabled(enable);
		fCounter.setEnabled(enable);
		fFilterButton.setEnabled(enable);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		fIsDisposed = true;
	}

	protected boolean isEnabled() {
		return fPluginTreeViewer.getTree().isEnabled();
	}

	protected void handleRestoreDefaults() {
		TreeSet<String> wtable = new TreeSet<>();

		for (int i = 0; i < getWorkspaceModels().length; i++) {
			IPluginModelBase model = getWorkspaceModels()[i];
			fPluginTreeViewer.setChecked(model, true);
			String id = model.getPluginBase().getId();
			if (id != null) {
				wtable.add(model.getPluginBase().getId());
			}
		}

		IPluginModelBase[] externalModels = getExternalModels();
		for (IPluginModelBase model : externalModels) {
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (!masked && model.isEnabled()) {
				fPluginTreeViewer.setChecked(model, true);
			}
		}
		countSelectedModels();

		Object[] selected = fPluginTreeViewer.getCheckedElements();
		for (Object selectedElement : selected) {
			if (selectedElement instanceof IPluginModelBase) {
				resetText((IPluginModelBase) selectedElement);
			}
		}
	}

	protected Shell getShell() {
		// use Shell of launcher window.  If launcher window is disposed (not sure how it could happen), use workbenchwindow.  Bug 168198
		try {
			Control c = fTab.getControl();
			if (!c.isDisposed()) {
				return c.getShell();
			}
		} catch (SWTException e) {
		}
		return PDEPlugin.getActiveWorkbenchShell();
	}

	public void handleValidate() {
		if (fOperation == null) {
			fOperation = createValidationOperation();
		}
		try {
			fOperation.run(new NullProgressMonitor());

			if (fDialog == null) {
				if (fOperation.hasErrors()) {
					fDialog = new PluginStatusDialog(getShell(), SWT.MODELESS | SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
					fDialog.setInput(fOperation.getInput());
					fDialog.open();
					fDialog = null;
				} else if (fOperation.isEmpty()) {
					if (fTab instanceof PluginsTab) {
						MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation,PDEUIMessages.AbstractLauncherToolbar_noSelection_plugins);
					}else if (fTab instanceof BundlesTab) {
						MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation,PDEUIMessages.AbstractLauncherToolbar_noSelection_bundles);
					}else{
						MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
					}

				} else {
					MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, PDEUIMessages.AbstractLauncherToolbar_noProblems);
				}
			} else {
				if (fOperation.getInput().size() > 0) {
					fDialog.refresh(fOperation.getInput());
				} else {
					Map<String, IStatus> input = new HashMap<>(1);
					input.put(PDEUIMessages.AbstractLauncherToolbar_noProblems, Status.OK_STATUS);
					fDialog.refresh(input);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	protected void setVisible(boolean visible) {
		if (!visible) {
			if (fDialog != null) {
				fDialog.close();
				fDialog = null;
			}
		}
	}

	protected abstract LaunchValidationOperation createValidationOperation();

	/**
	 * called before the TreeView is refreshed. This allows any subclasses to cache
	 * any information in the view and redisplay after the refresh.  This is used by the
	 * OSGiBundleBlock to cache the values of the default launch and auto launch columns
	 * in the table tree.
	 *
	 * @param treeView The tree view that will be refreshed.
	 */
	protected void refreshTreeView(CheckboxTreeViewer treeView) {
		// Remove any selection
		if (treeView.getTree().getItemCount() > 0) {
			treeView.getTree().setSelection(treeView.getTree().getItem(0));
		} else {
			treeView.setSelection(new StructuredSelection(StructuredSelection.EMPTY));
		}

		// Reset any editors on the tree viewer
		if (levelColumnEditor != null && levelColumnEditor.getEditor() != null && !levelColumnEditor.getEditor().isDisposed()) {
			levelColumnEditor.getEditor().dispose();
		}

		if (autoColumnEditor != null && autoColumnEditor.getEditor() != null && !autoColumnEditor.getEditor().isDisposed()) {
			autoColumnEditor.getEditor().dispose();
		}
	}

	protected void resetGroup(NamedElement group) {
		Widget widget = fPluginTreeViewer.testFindItem(group);
		if (widget instanceof TreeItem) {
			TreeItem[] items = ((TreeItem) widget).getItems();
			for (int i = 0; i < items.length; i++) {
				if (!items[i].getChecked()) {
					Object model = items[i].getData();
					if (model instanceof IPluginModelBase) {
						resetText((IPluginModelBase) model);
					}
				}
			}
		}
	}

	protected final void initializePluginsState(Map<IPluginModelBase, String> selectedPlugins) {
		for (Entry<IPluginModelBase, String> entry : selectedPlugins.entrySet()) {
			IPluginModelBase model = entry.getKey();
			setText(model, entry.getValue());
		}

		fPluginTreeViewer.setCheckedElements(selectedPlugins.keySet().toArray());
		countSelectedModels();
		resetGroup(fWorkspacePlugins);
		resetGroup(fExternalPlugins);

		handleFilterButton(); // Once the page is initialized, apply any filtering.
		updateCounter();
	}

}
