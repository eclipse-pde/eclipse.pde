/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404 and bug 207064
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import java.util.List;
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
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.launcher.FilteredCheckboxTree.FilterableCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.launcher.FilteredCheckboxTree.PreRefreshNotifier;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
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
	protected CheckboxTreeViewer fPluginTreeViewer;
	protected NamedElement fWorkspacePlugins;
	protected NamedElement fExternalPlugins;
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

	private boolean viewerEnabled = true;

	private HashMap levelColumnCache = new HashMap();
	private HashMap autoColumnCache = new HashMap();
	private TreeEditor levelColumnEditor = null;
	private TreeEditor autoColumnEditor = null;
	private boolean fIsDisposed = false;

	private PluginStatusDialog fDialog;

	class PluginModelNameBuffer {
		private List nameList;

		PluginModelNameBuffer() {
			super();
			nameList = new ArrayList();
		}

		void add(IPluginModelBase model) {
			nameList.add(getPluginName(model));
		}

		private String getPluginName(IPluginModelBase model) {
			String startLevel = null;
			String autoStart = null;
			if (fPluginTreeViewer.getChecked(model)) {
				startLevel = levelColumnCache.get(model) != null ? levelColumnCache.get(model).toString() : null;
				autoStart = autoColumnCache.get(model) != null ? autoColumnCache.get(model).toString() : null;
			}
			return BundleLauncherHelper.writeBundleEntry(model, startLevel, autoStart);
		}

		public String toString() {
			Collections.sort(nameList);
			StringBuffer result = new StringBuffer();
			for (Iterator iterator = nameList.iterator(); iterator.hasNext();) {
				String name = (String) iterator.next();
				if (result.length() > 0)
					result.append(',');
				result.append(name);
			}

			if (result.length() == 0)
				return null;

			return result.toString();
		}
	}

	/**
	 * Label provider for the tree.
	 */
	class OSGiLabelProvider extends PDELabelProvider {

		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}

		public String getColumnText(Object obj, int index) {
			boolean isChecked = fPluginTreeViewer.getChecked(obj);
			switch (index) {
				case 0 :
					return super.getColumnText(obj, index);
				case 1 :
					if (isChecked && levelColumnCache != null && levelColumnCache.containsKey(obj))
						return (String) levelColumnCache.get(obj);
					return ""; //$NON-NLS-1$
				case 2 :
					if (isChecked && autoColumnCache != null && autoColumnCache.containsKey(obj))
						return (String) autoColumnCache.get(obj);
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
			if (!fIsDisposed)
				fTab.updateLaunchConfigurationDialog();
		}
	}

	class PluginContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IPluginModelBase);
		}

		public Object[] getChildren(Object parent) {
			if (parent == fExternalPlugins)
				return getExternalModels();
			if (parent == fWorkspacePlugins)
				return getWorkspaceModels();
			return new Object[0];
		}

		public Object getParent(Object child) {
			if (child instanceof IPluginModelBase) {
				IResource resource = ((IPluginModelBase) child).getUnderlyingResource();
				return resource == null ? fExternalPlugins : fWorkspacePlugins;
			}
			return null;
		}

		public Object[] getElements(Object input) {
			ArrayList list = new ArrayList();
			if (getWorkspaceModels().length > 0)
				list.add(fWorkspacePlugins);
			if (getExternalModels().length > 0)
				list.add(fExternalPlugins);
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
			PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
			String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
			if (saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
				fExternalModels = new IPluginModelBase[0];
				return fExternalModels;
			}

			IPluginModelBase[] models = PluginRegistry.getExternalModels();
			if (saved.equals(ICoreConstants.VALUE_SAVED_ALL)) {
				fExternalModels = models;
				return fExternalModels;
			}

			ArrayList list = new ArrayList(models.length);
			for (int i = 0; i < models.length; i++) {
				if (models[i].isEnabled()) {
					list.add(models[i]);
				}
			}
			fExternalModels = (IPluginModelBase[]) list.toArray(new IPluginModelBase[list.size()]);
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
			IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
			ArrayList list = new ArrayList(models.length);
			for (int i = 0; i < models.length; i++) {
				if (models[i].getBundleDescription() != null) {
					list.add(models[i]);
				}
			}
			fWorkspaceModels = (IPluginModelBase[]) list.toArray(new IPluginModelBase[list.size()]);
		}
		return fWorkspaceModels;
	}

	protected void updateCounter() {
		if (fCounter != null) {
			int checked = fNumExternalChecked + fNumWorkspaceChecked;
			int total = getWorkspaceModels().length + getExternalModels().length;
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	public void createControl(Composite parent, int span, int indent) {
		createPluginViewer(parent, span - 1, indent);
		createButtonContainer(parent, fPluginFilteredTree.getTreeLocationOffset());
		fIncludeOptionalButton = createButton(parent, span, indent, NLS.bind(PDEUIMessages.AdvancedLauncherTab_includeOptional, fTab.getName().toLowerCase(Locale.ENGLISH)));
		fAddWorkspaceButton = createButton(parent, span, indent, NLS.bind(PDEUIMessages.AdvancedLauncherTab_addNew, fTab.getName().toLowerCase(Locale.ENGLISH)));

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gd);

		fAutoValidate = createButton(parent, span - 1, indent, NLS.bind(PDEUIMessages.PluginsTabToolBar_auto_validate, fTab.getName().replaceAll("&", "").toLowerCase(Locale.ENGLISH))); //$NON-NLS-1$ //$NON-NLS-2$

		fValidateButton = new Button(parent, SWT.PUSH);
		fValidateButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fValidateButton.setText(NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName().replaceAll("&", ""))); //$NON-NLS-1$ //$NON-NLS-2$
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
		fPluginFilteredTree = new FilteredCheckboxTree(composite, getTreeViewerStyle(), patternFilter);
		fPluginTreeViewer = (CheckboxTreeViewer) fPluginFilteredTree.getViewer();
		((FilterableCheckboxTreeViewer) fPluginTreeViewer).addPreRefreshNotifier(new PreRefreshNotifier() {

			boolean previousFilter = false;

			public void preRefresh(FilterableCheckboxTreeViewer viewer, boolean filtered) {
				refreshTreeView(fPluginTreeViewer);
				if (previousFilter != filtered) {
					if (viewerEnabled) {
						// Only update these buttons if the viewer is actually enabled
						fWorkingSetButton.setEnabled(!filtered);
						fAddRequiredButton.setEnabled(!filtered);
						fDefaultsButton.setEnabled(!filtered);
					}

					fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
					fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < getWorkspaceModels().length);
					fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
					fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < getExternalModels().length);
				}
				previousFilter = filtered;
			}
		});

		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				// Since a check on the root of a CheckBoxTreeViewer selects all its children 
				// (hidden or not), we need to ensure that all items are shown
				// if this happens.  Since it not clear what the best behaviour is here
				// this just "un-selects" the filter button.

				if (!event.getChecked())
					return; // just return if the check state goes to false
				// It is not clear if this is the best approach, but it 
				// is hard to tell without user feedback.  
				TreeItem[] items = fPluginTreeViewer.getTree().getItems();
				for (int i = 0; i < items.length; i++) {
					if (event.getElement() == items[i].getData()) {
						// If the even happens on the root of the tree
						fFilterButton.setSelection(false);
						handleFilterButton();
						return;
					}
				}
			}

		});
		fPluginTreeViewer.setContentProvider(new PluginContentProvider());
		fPluginTreeViewer.setLabelProvider(getLabelProvider());
		fPluginTreeViewer.setAutoExpandLevel(2);
		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModelBase) {
					handleCheckStateChanged(event);
				} else {
					handleGroupStateChanged(element, event.getChecked());
				}
				fTab.updateLaunchConfigurationDialog();
			}
		});
		fPluginTreeViewer.setComparator(new ListUtil.PluginComparator() {
			public int category(Object obj) {
				if (obj == fWorkspacePlugins)
					return -1;
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
		fPluginTreeViewer.addFilter(new SourcePluginFilter());

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
		if (Util.isMac())
			levelColumnEditor.minimumHeight = 27;

		autoColumnEditor = new TreeEditor(tree);
		autoColumnEditor.horizontalAlignment = SWT.CENTER;
		autoColumnEditor.grabHorizontal = true;
		autoColumnEditor.minimumWidth = 60;

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Clean up any previous editor control
				Control oldEditor = levelColumnEditor.getEditor();
				if (oldEditor != null && !oldEditor.isDisposed())
					oldEditor.dispose();

				oldEditor = autoColumnEditor.getEditor();
				if (oldEditor != null && !oldEditor.isDisposed())
					oldEditor.dispose();

				// Identify the selected row
				final TreeItem item = (TreeItem) e.item;
				if (item != null && !isEditable(item))
					return;

				if (item != null && !isFragment(item)) { // only display editing controls if we're not a fragment
					final Spinner spinner = new Spinner(tree, SWT.BORDER);
					spinner.setMinimum(0);
					String level = item.getText(1);
					int defaultLevel = level.length() == 0 || "default".equals(level) ? 0 : Integer.parseInt(level); //$NON-NLS-1$
					spinner.setSelection(defaultLevel);
					spinner.addModifyListener(new ModifyListener() {
						public void modifyText(ModifyEvent e) {
							if (item.getChecked()) {
								int selection = spinner.getSelection();
								item.setText(1, selection == 0 ? "default" //$NON-NLS-1$
										: Integer.toString(selection));
								levelColumnCache.put(item.getData(), item.getText(1));
								fTab.updateLaunchConfigurationDialog();
							}
						}
					});
					levelColumnEditor.setEditor(spinner, item, 1);

					final CCombo combo = new CCombo(tree, SWT.BORDER | SWT.READ_ONLY);
					combo.setItems(new String[] {"default", Boolean.toString(true), Boolean.toString(false)}); //$NON-NLS-1$
					combo.setText(item.getText(2));
					combo.pack();
					combo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							if (item.getChecked()) {
								item.setText(2, combo.getText());
								autoColumnCache.put(item.getData(), item.getText(2));
								fTab.updateLaunchConfigurationDialog();
							}
						}
					});
					autoColumnEditor.setEditor(combo, item, 2);
				}
			}
		});
	}

	private boolean isEditable(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) obj;
			String systemBundleId = PDECore.getDefault().getModelManager().getSystemBundleId();
			if (!(systemBundleId.equals(model.getPluginBase().getId())))
				return fPluginTreeViewer.getChecked(model);
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
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (fFilterButton.getSelection()) {
				return fPluginTreeViewer.getChecked(element);
			}
			return true;
		}
	}

	private void createButtonContainer(Composite parent, int vOffset) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = vOffset;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll, SWT.PUSH);
		fDeselectButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll, SWT.PUSH);
		fWorkingSetButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_workingSet, SWT.PUSH);
		fAddRequiredButton = createButton(composite, NLS.bind(PDEUIMessages.AdvancedLauncherTab_subset, fTab.getName()), SWT.PUSH);
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
		IPluginModelBase model = (IPluginModelBase) event.getElement();
		if (model.getUnderlyingResource() == null) {
			if (event.getChecked()) {
				fNumExternalChecked += 1;
			} else {
				fNumExternalChecked -= 1;
			}
		} else {
			if (event.getChecked()) {
				fNumWorkspaceChecked += 1;
			} else {
				fNumWorkspaceChecked -= 1;
			}
		}
		adjustGroupState();
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
			levelText = (String) levelColumnCache.get(model);
			levelText = levelText == null || levelText.length() == 0 ? "default" : levelText; //$NON-NLS-1$
			autoText = (String) autoColumnCache.get(model);
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
		fPluginTreeViewer.setSubtreeChecked(group, checked);
		fPluginTreeViewer.setGrayed(group, false);

		Object[] checkedChildren = ((FilteredCheckboxTree.FilterableCheckboxTreeViewer) fPluginTreeViewer).getCheckedChildren(group);
		int numberOfChildren = 0;
		if (checkedChildren != null)
			numberOfChildren = checkedChildren.length;

		if (group == fWorkspacePlugins)
			fNumWorkspaceChecked = numberOfChildren;
		else if (group == fExternalPlugins)
			fNumExternalChecked = numberOfChildren;

		if (group instanceof NamedElement) {
			NamedElement namedElement = (NamedElement) group;
			TreeItem item = (TreeItem) fPluginTreeViewer.testFindItem(namedElement);
			if (item != null) {
				TreeItem[] children = item.getItems();
				if (children == null)
					return;
				for (int i = 0; i < children.length; i++) {
					TreeItem childItem = children[i];
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
			for (int i = 0; i < ids.length; i++) {
				IPluginModelBase model = PluginRegistry.findModel(ids[i]);
				if (model != null) {
					if (!fPluginTreeViewer.getChecked(model)) {
						setChecked(model, true);
						if (model.getUnderlyingResource() == null)
							fNumExternalChecked += 1;
						else
							fNumWorkspaceChecked += 1;
					}
				}
			}
			adjustGroupState();
		}
	}

	protected void setChecked(IPluginModelBase model, boolean checked) {
		fPluginTreeViewer.setChecked(model, checked);
		resetText(model);
	}

	private String[] getPluginIDs(IWorkingSet[] workingSets) {
		HashSet set = new HashSet();
		for (int i = 0; i < workingSets.length; i++) {
			IAdaptable[] elements = workingSets[i].getElements();
			for (int j = 0; j < elements.length; j++) {
				Object element = elements[j];
				if (element instanceof PersistablePluginObject) {
					set.add(((PersistablePluginObject) element).getPluginID());
				} else {
					if (element instanceof IJavaProject)
						element = ((IJavaProject) element).getProject();
					if (element instanceof IProject) {
						IPluginModelBase model = PluginRegistry.findModel((IProject) element);
						if (model != null)
							set.add(model.getPluginBase().getId());
					}
				}
			}
		}
		return (String[]) set.toArray(new String[set.size()]);
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
		levelColumnCache = new HashMap();
		autoColumnCache = new HashMap();
		fPluginFilteredTree.resetFilter();
		fIncludeOptionalButton.setSelection(config.getAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true));
		fAddWorkspaceButton.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true));
		fAutoValidate.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false));
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
		Object[] checked = fPluginTreeViewer.getCheckedElements();
		ArrayList toCheck = new ArrayList(checked.length);
		for (int i = 0; i < checked.length; i++)
			if (checked[i] instanceof IPluginModelBase)
				toCheck.add(checked[i]);

		// exclude "org.eclipse.ui.workbench.compatibility" - it is only needed for pre-3.0 bundles
		Set additionalIds = DependencyManager.getDependencies(checked, fIncludeOptionalButton.getSelection(), new String[] {"org.eclipse.ui.workbench.compatibility"}); //$NON-NLS-1$

		Iterator it = additionalIds.iterator();
		while (it.hasNext()) {
			String id = (String) it.next();
			if (findPlugin(id) == null) {
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					IPluginModelBase model = entry.getModel();
					if (model != null)
						toCheck.add(model);
				}
			}
		}

		checked = toCheck.toArray();
		setCheckedElements(checked);
		fNumExternalChecked = 0;
		fNumWorkspaceChecked = 0;
		for (int i = 0; i < checked.length; i++) {
			if (((IPluginModelBase) checked[i]).getUnderlyingResource() != null)
				fNumWorkspaceChecked += 1;
			else
				fNumExternalChecked += 1;
		}
		adjustGroupState();
	}

	protected IPluginModelBase findPlugin(String id) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getModel();
			if (fPluginTreeViewer.getChecked(model))
				return model;

			IPluginModelBase[] models = entry.getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				if (fPluginTreeViewer.getChecked(models[i]))
					return models[i];
			}

			models = entry.getExternalModels();
			for (int i = 0; i < models.length; i++) {
				if (fPluginTreeViewer.getChecked(models[i]))
					return models[i];
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
			for (int i = 0; i < items.length; i++) {
				TreeItem child = items[i];
				if (child.getChecked() == (child.getText(1).length() == 0)) {
					Object model = items[i].getData();
					if (model instanceof IPluginModelBase) {
						resetText((IPluginModelBase) model);
					}
				}
			}
		}
	}

	protected void adjustGroupState() {
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < getExternalModels().length);
		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < getWorkspaceModels().length);
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
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
	}

	public void enableViewer(boolean enable) {
		viewerEnabled = enable;
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
		TreeSet wtable = new TreeSet();
		fNumWorkspaceChecked = 0;
		fNumExternalChecked = 0;

		for (int i = 0; i < getWorkspaceModels().length; i++) {
			IPluginModelBase model = getWorkspaceModels()[i];
			fNumWorkspaceChecked += 1;
			String id = model.getPluginBase().getId();
			if (id != null)
				wtable.add(model.getPluginBase().getId());
		}
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, true);

		fNumExternalChecked = 0;
		IPluginModelBase[] externalModels = getExternalModels();
		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase model = externalModels[i];
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (!masked && model.isEnabled()) {
				fPluginTreeViewer.setChecked(model, true);
				fNumExternalChecked += 1;
			}
		}
		adjustGroupState();

		Object[] selected = fPluginTreeViewer.getCheckedElements();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] instanceof IPluginModelBase) {
				resetText((IPluginModelBase) selected[i]);
			}
		}
	}

	protected Shell getShell() {
		// use Shell of launcher window.  If launcher window is disposed (not sure how it could happen), use workbenchwindow.  Bug 168198
		try {
			Control c = fTab.getControl();
			if (!c.isDisposed())
				return c.getShell();
		} catch (SWTException e) {
		}
		return PDEPlugin.getActiveWorkbenchShell();
	}

	public void handleValidate() {
		if (fOperation == null)
			fOperation = createValidationOperation();
		try {
			fOperation.run(new NullProgressMonitor());

			if (fDialog == null) {
				if (fOperation.hasErrors()) {
					fDialog = new PluginStatusDialog(getShell(), SWT.MODELESS | SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
					fDialog.setInput(fOperation.getInput());
					fDialog.open();
					fDialog = null;
				} else if (fOperation.isEmpty()) {
					MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
				} else {
					MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, PDEUIMessages.AbstractLauncherToolbar_noProblems);
				}
			} else {
				if (fOperation.getInput().size() > 0)
					fDialog.refresh(fOperation.getInput());
				else {
					Map input = new HashMap(1);
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

}
