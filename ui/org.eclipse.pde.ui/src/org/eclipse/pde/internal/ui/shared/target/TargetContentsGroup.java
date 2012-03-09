/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import com.ibm.icu.text.MessageFormat;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.BundleException;

/**
 * UI Part that displays all of the bundle contents of a target.  The bundles can be
 * excluded by unchecking them.  There are a variety of options to change the tree's
 * format.
 * 
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see TargetBundle
 */
public class TargetContentsGroup {

	private CachedCheckboxTreeViewer fTree;
	private MenuManager fMenuManager;
	private Button fSelectButton;
	private Button fDeselectButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Button fSelectRequiredButton;
	private Label fModeLabel;
	private Button fPluginModeButton;
	private Button fFeaureModeButton;
	private Label fShowLabel;
	private Button fShowSourceButton;
	private Button fShowPluginsButton;
	private Label fCountLabel;
	private Label fGroupLabel;
	private Combo fGroupCombo;
	private ComboPart fGroupComboPart;

	private ViewerFilter fSourceFilter;
	private ViewerFilter fPluginFilter;

	private TargetDefinition fTargetDefinition;
	/**
	 * Maps file paths to a list of bundles that reside in that location, use {@link #getFileBundleMapping()} rather than accessing the field directly
	 */
	private Map fFileBundleMapping;

	/**
	 * Set of TargetBundles that are being used to display error statuses for missing plug-ins/features, possibly <code>null</code>
	 */
	private Set fMissing;

	private static final NameVersionDescriptor OTHER_CATEGORY = new NameVersionDescriptor(Messages.TargetContentsGroup_OtherPluginsParent, null);

	/**
	 * Cached list of all bundles, used to quickly obtain bundle counts.
	 */
	private List fAllBundles = new ArrayList();

	private int fGrouping;
	private static final int GROUP_BY_NONE = 0;
	private static final int GROUP_BY_FILE_LOC = 1;
	private static final int GROUP_BY_CONTAINER = 2;
	private ListenerList fChangeListeners = new ListenerList();

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @return generated instance of the table part
	 */
	public static TargetContentsGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetContentsGroup contentTable = new TargetContentsGroup();
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @return generated instance of the table part
	 */
	public static TargetContentsGroup createInDialog(Composite parent) {
		TargetContentsGroup contentTable = new TargetContentsGroup();
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Use {@link #createInDialog(Composite)} or {@link #createInDialog(Composite)}
	 */
	protected TargetContentsGroup() {
	}

	/**
	 * Adds a listener to the set of listeners that will be notified when the bundle containers
	 * are modified.  This method has no effect if the listener has already been added. 
	 * 
	 * @param listener target changed listener to add
	 */
	public void addTargetChangedListener(ITargetChangedListener listener) {
		fChangeListeners.add(listener);
	}

	/**
	 * Informs the target content listeners that check state has changed
	 */
	public void contentChanged() {
		Object[] listeners = fChangeListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((ITargetChangedListener) listeners[i]).contentsChanged(fTargetDefinition, this, false, false);
		}
	}

	/**
	 * Disposes the contents of this group
	 */
	public void dispose() {
		if (fMenuManager != null) {
			fMenuManager.dispose();
		}
	}

	/**
	 * Creates the contents of this group, using the given toolkit where appropriate so that the controls
	 * have the form editor look and feel.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create controls with
	 */
	protected void createFormContents(Composite parent, FormToolkit toolkit) {
		fGrouping = GROUP_BY_NONE;

		Composite comp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		comp.setFont(parent.getFont());

		createTree(comp, toolkit);
		createButtons(comp, toolkit);

		fCountLabel = toolkit.createLabel(comp, ""); //$NON-NLS-1$
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fCountLabel.setLayoutData(data);

		updateButtons();
		initializeFilters();
	}

	/**
	 * Creates the contents of this group in the normal dialog style
	 * 
	 * @param parent parent composite
	 */
	protected void createDialogContents(Composite parent) {
		fGrouping = GROUP_BY_NONE;

		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		createTree(comp, null);
		createButtons(comp, null);

		fCountLabel = SWTFactory.createLabel(comp, "", 2); //$NON-NLS-1$

		updateButtons();
		initializeFilters();
	}

	/**
	 * Creates the tree in this group
	 * 
	 * @param parent parent composite
	 * @param style toolkit for form style or <code>null</code> for dialog style
	 */
	private TreeViewer createTree(Composite parent, FormToolkit toolkit) {
		FilteredCheckboxTree tree = new FilteredCheckboxTree(parent, toolkit);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.getPatternFilter().setIncludeLeadingWildcard(true);
		tree.getFilterControl().setFont(parent.getFont());

		fTree = tree.getCheckboxTreeViewer();
		((GridData) fTree.getControl().getLayoutData()).heightHint = 300;
		fTree.getControl().setFont(parent.getFont());
		fTree.setUseHashlookup(true);
		fTree.setContentProvider(new TreeContentProvider());
		fTree.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object first = selection.getFirstElement();
				fTree.setChecked(first, !fTree.getChecked(first));
				saveIncludedBundleState();
				contentChanged();
				updateButtons();
				fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
			}
		});
		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				saveIncludedBundleState();
				contentChanged();
				updateButtons();
				fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
			}
		});
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTree.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (fFeaureModeButton.getSelection()) {
					if (e1 == OTHER_CATEGORY) {
						return 1;
					}
					if (e2 == OTHER_CATEGORY) {
						return -1;
					}
				}
				if (e1 instanceof TargetBundle && !(e2 instanceof TargetBundle)) {
					return -1;
				}
				if (e2 instanceof TargetBundle && !(e1 instanceof TargetBundle)) {
					return 1;
				}
				if (e1 instanceof TargetBundle && e2 instanceof TargetBundle) {
					IStatus status1 = ((TargetBundle) e1).getStatus();
					IStatus status2 = ((TargetBundle) e2).getStatus();
					if (!status1.isOK() && status2.isOK()) {
						return -1;
					}
					if (status1.isOK() && !status2.isOK()) {
						return 1;
					}
				}
				return super.compare(viewer, e1, e2);
			}

		});

		fMenuManager = new MenuManager();
		fMenuManager.add(new Action(Messages.TargetContentsGroup_collapseAll, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL)) {
			public void run() {
				fTree.collapseAll();
			}
		});
		Menu contextMenu = fMenuManager.createContextMenu(tree);
		tree.setMenu(contextMenu);

		return fTree;
	}

	/**
	 * Creates the buttons in this group inside a new composite
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to give form style or <code>null</code> for dialog style
	 */
	private void createButtons(Composite parent, FormToolkit toolkit) {
		if (toolkit != null) {
			Composite buttonComp = toolkit.createComposite(parent);
			GridLayout layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			buttonComp.setLayout(layout);
			buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

			fSelectButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_0, SWT.PUSH);
			fSelectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_1, SWT.PUSH);
			fDeselectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label emptySpace = new Label(buttonComp, SWT.NONE);
			GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectAllButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_2, SWT.PUSH);
			fSelectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectAllButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_3, SWT.PUSH);
			fDeselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			emptySpace = new Label(buttonComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectRequiredButton = toolkit.createButton(buttonComp, Messages.TargetContentsGroup_4, SWT.PUSH);
			fSelectRequiredButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Composite filterComp = toolkit.createComposite(buttonComp);
			layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			filterComp.setLayout(layout);
			filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

			fModeLabel = toolkit.createLabel(filterComp, Messages.TargetContentsGroup_ManageUsing);

			fPluginModeButton = toolkit.createButton(filterComp, Messages.TargetContentsGroup_PluginMode, SWT.RADIO);
			fPluginModeButton.setSelection(true);
			fFeaureModeButton = toolkit.createButton(filterComp, Messages.TargetContentsGroup_FeatureMode, SWT.RADIO);
			fFeaureModeButton.setSelection(true);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fShowLabel = toolkit.createLabel(filterComp, Messages.BundleContainerTable_9);

			fShowPluginsButton = toolkit.createButton(filterComp, Messages.BundleContainerTable_14, SWT.CHECK);
			fShowPluginsButton.setSelection(true);
			fShowSourceButton = toolkit.createButton(filterComp, Messages.BundleContainerTable_15, SWT.CHECK);
			fShowSourceButton.setSelection(true);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fGroupLabel = toolkit.createLabel(filterComp, Messages.TargetContentsGroup_0);

			fGroupComboPart = new ComboPart();
			fGroupComboPart.createControl(filterComp, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent = 10;
			fGroupComboPart.getControl().setLayoutData(gd);
			fGroupComboPart.setItems(new String[] {Messages.TargetContentsGroup_1, Messages.TargetContentsGroup_2, Messages.TargetContentsGroup_3});
			fGroupComboPart.setVisibleItemCount(30);
			fGroupComboPart.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleGroupChange();
				}
			});
			fGroupComboPart.select(0);

		} else {
			Composite buttonComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL, 0, 0);
			fSelectButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_0, null);
			fDeselectButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_1, null);

			Label emptySpace = new Label(buttonComp, SWT.NONE);
			GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectAllButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_2, null);
			fDeselectAllButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_3, null);

			emptySpace = new Label(buttonComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectRequiredButton = SWTFactory.createPushButton(buttonComp, Messages.TargetContentsGroup_4, null);

			Composite filterComp = SWTFactory.createComposite(buttonComp, 1, 1, SWT.NONE, 0, 0);
			filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

			fModeLabel = SWTFactory.createLabel(filterComp, Messages.TargetContentsGroup_ManageUsing, 1);

			fPluginModeButton = SWTFactory.createRadioButton(filterComp, Messages.TargetContentsGroup_PluginMode);
			fFeaureModeButton = SWTFactory.createRadioButton(filterComp, Messages.TargetContentsGroup_FeatureMode);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fShowLabel = SWTFactory.createLabel(filterComp, Messages.BundleContainerTable_9, 1);

			fShowPluginsButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_14, null, true, 1);
			fShowSourceButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_15, null, true, 1);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fGroupLabel = SWTFactory.createLabel(filterComp, Messages.TargetContentsGroup_0, 1);
			fGroupCombo = SWTFactory.createCombo(filterComp, SWT.READ_ONLY, 1, new String[] {Messages.TargetContentsGroup_1, Messages.TargetContentsGroup_2, Messages.TargetContentsGroup_3});
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent = 10;
			fGroupCombo.setLayoutData(gd);
			fGroupCombo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleGroupChange();
				}
			});
			fGroupCombo.select(0);
		}

		fSelectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					for (int i = 0; i < selected.length; i++) {
						fTree.setChecked(selected[i], true);
					}
					saveIncludedBundleState();
					contentChanged();
					updateButtons();
					fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
				}
			}
		});

		fDeselectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					for (int i = 0; i < selected.length; i++) {
						fTree.setChecked(selected[i], false);
					}
					saveIncludedBundleState();
					contentChanged();
					updateButtons();
					fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
				}
			}
		});

		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTree.setAllChecked(true);
				saveIncludedBundleState();
				contentChanged();
				updateButtons();
				fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
			}
		});

		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTree.setAllChecked(false);
				saveIncludedBundleState();
				contentChanged();
				updateButtons();
				fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
			}
		});

		fSelectRequiredButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] allChecked = fTree.getCheckedLeafElements();
				Collection required = null;
				if (fFeaureModeButton.getSelection()) {
					required = getRequiredFeatures(fTargetDefinition.getAllFeatures(), allChecked);
				} else {
					required = getRequiredPlugins(fAllBundles, allChecked);
				}
				for (Iterator iterator = required.iterator(); iterator.hasNext();) {
					fTree.setChecked(iterator.next(), true);
				}
				saveIncludedBundleState();
				contentChanged();
				updateButtons();
				fTree.update(fTargetDefinition.getTargetLocations(), new String[] {IBasicPropertyConstants.P_TEXT});
			}
		});

		fPluginModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Moving from feature based filtering to plug-in based, need to update storage
				fTargetDefinition.setUIMode(TargetDefinition.MODE_PLUGIN);
				contentChanged();
				fTargetDefinition.setIncluded(null);

				fGroupLabel.setEnabled(true);
				if (fGroupCombo != null) {
					fGroupCombo.setEnabled(true);
				} else {
					fGroupComboPart.getControl().setEnabled(true);
				}

				fTree.getControl().setRedraw(false);
				fTree.refresh(false);
				fTree.expandAll();
				updateCheckState();
				updateButtons();
				fTree.getControl().setRedraw(true);
			}
		});
		fPluginModeButton.setSelection(true);
		GridData gd = new GridData();
		gd.horizontalIndent = 10;
		fPluginModeButton.setLayoutData(gd);

		fFeaureModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Moving from plug-in based filtering to feature based, need to update storage
				fTargetDefinition.setUIMode(TargetDefinition.MODE_FEATURE);
				contentChanged();
				fTargetDefinition.setIncluded(null);

				fGroupLabel.setEnabled(false);
				if (fGroupCombo != null) {
					fGroupCombo.setEnabled(false);
				} else {
					fGroupComboPart.getControl().setEnabled(false);
				}

				fTree.getControl().setRedraw(false);
				fTree.refresh(false);
				fTree.expandAll();
				updateCheckState();
				updateButtons();
				fTree.getControl().setRedraw(true);
			}
		});
		fFeaureModeButton.setSelection(false);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fFeaureModeButton.setLayoutData(gd);

		fShowPluginsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowPluginsButton.getSelection()) {
					fTree.addFilter(fPluginFilter);
				} else {
					fTree.removeFilter(fPluginFilter);
					fTree.expandAll();
					updateCheckState();
				}
				updateButtons();
			}
		});
		fShowPluginsButton.setSelection(true);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fShowPluginsButton.setLayoutData(gd);

		fShowSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowSourceButton.getSelection()) {
					fTree.addFilter(fSourceFilter);
				} else {
					fTree.removeFilter(fSourceFilter);
					fTree.expandAll();
					updateCheckState();
				}
				updateButtons();
			}
		});
		fShowSourceButton.setSelection(true);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fShowSourceButton.setLayoutData(gd);

	}

	private void initializeFilters() {
		fSourceFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof TargetBundle) {
					if (((TargetBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
		fPluginFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof TargetBundle) {
					if (!((TargetBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
	}

	/**
	 * Returns the file path where the given resolved bundle can be found.
	 * Used to group bundles by file path in the tree.
	 * 
	 * @param bundle bundle to lookup parent path for
	 * @return path of parent directory, if unknown it will be a path object containing "Unknown"
	 */
	private IPath getParentPath(TargetBundle bundle) {
		URI location = bundle.getBundleInfo().getLocation();
		if (location == null) {
			return new Path(Messages.TargetContentsGroup_8);
		}
		IPath path = new Path(URIUtil.toUnencodedString(location));
		path = path.removeLastSegments(1);
		return path;
	}

	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws IOException if unable to parse
	 */
	protected Map loadManifest(File bundleLocation) throws IOException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		try {
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
			if (manifestStream == null) {
				return null;
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
		} catch (BundleException e) {
			PDEPlugin.log(e);
		} finally {
			try {
				if (manifestStream != null) {
					manifestStream.close();
				}
			} catch (IOException e) {
				PDEPlugin.log(e);
			}
			try {
				if (jarFile != null) {
					jarFile.close();
				}
			} catch (IOException e) {
				PDEPlugin.log(e);
			}
		}
		return null;
	}

	/**
	 * Uses the target state to determine all bundles required by the
	 * currently checked bundles and returns them so they can be checked in the tree.
	 * 
	 * @param allBundles list of all bundles to search requirements in
	 * @param checkedBundles list of bundles to get requirements for
	 * @return list of resolved bundles from the collection to be checked
	 */
	private List/*<TargetBundle>*/getRequiredPlugins(final Collection/*<TargetBundle>*/allBundles, final Object[] checkedBundles) {
		final Set dependencies = new HashSet();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(Messages.TargetContentsGroup_5, 150);

					// Get all the bundle locations
					List allLocations = new ArrayList(allBundles.size());
					for (Iterator iterator = allBundles.iterator(); iterator.hasNext();) {
						TargetBundle current = (TargetBundle) iterator.next();
						try {
							// Some bundles, such as those with errors, may not have locations
							URI location = current.getBundleInfo().getLocation();
							if (location != null) {
								allLocations.add(new File(location).toURL());
							}
						} catch (MalformedURLException e) {
							PDEPlugin.log(e);
							monitor.setCanceled(true);
							return;
						}
					}
					if (monitor.isCanceled()) {
						return;
					}
					monitor.worked(20);

					// Create a PDE State containing all of the target bundles					
					PDEState state = new PDEState((URL[]) allLocations.toArray(new URL[allLocations.size()]), true, new SubProgressMonitor(monitor, 50));
					if (monitor.isCanceled()) {
						return;
					}

					// Figure out which of the models have been checked
					IPluginModelBase[] models = state.getTargetModels();
					List checkedModels = new ArrayList(checkedBundles.length);
					for (int i = 0; i < checkedBundles.length; i++) {
						if (checkedBundles[i] instanceof TargetBundle) {
							BundleInfo bundle = ((TargetBundle) checkedBundles[i]).getBundleInfo();
							for (int j = 0; j < models.length; j++) {
								if (models[j].getBundleDescription().getSymbolicName().equals(bundle.getSymbolicName()) && models[j].getBundleDescription().getVersion().toString().equals(bundle.getVersion())) {
									checkedModels.add(models[j]);
									break;
								}
							}
						}
					}
					monitor.worked(20);
					if (monitor.isCanceled()) {
						return;
					}

					// Get implicit dependencies as a list of strings
					// This is wasteful since the dependency calculation puts them back into BundleInfos
					NameVersionDescriptor[] implicitDependencies = fTargetDefinition.getImplicitDependencies();
					List implicitIDs = new ArrayList();
					if (implicitDependencies != null) {
						for (int i = 0; i < implicitDependencies.length; i++) {
							implicitIDs.add(implicitDependencies[i].getId());
						}
					}
					monitor.worked(10);

					// Get all dependency bundles
					// exclude "org.eclipse.ui.workbench.compatibility" - it is only needed for pre-3.0 bundles
					dependencies.addAll(DependencyManager.getDependencies(checkedModels.toArray(), (String[]) implicitIDs.toArray(new String[implicitIDs.size()]), state.getState(), new String[] {"org.eclipse.ui.workbench.compatibility"})); //$NON-NLS-1$
					monitor.worked(50);

				} finally {
					monitor.done();
				}
			}
		};
		try {
			// Calculate the dependencies
			new ProgressMonitorDialog(fTree.getControl().getShell()).run(true, true, op);

			// We want to check the dependents, the source of the dependents, and the source of the originally checked
			Set checkedNames = new HashSet(checkedBundles.length);
			for (int i = 0; i < checkedBundles.length; i++) {
				if (checkedBundles[i] instanceof TargetBundle) {
					checkedNames.add(((TargetBundle) checkedBundles[i]).getBundleInfo().getSymbolicName());
				}
			}

			List toCheck = new ArrayList();
			for (Iterator iterator = fAllBundles.iterator(); iterator.hasNext();) {
				TargetBundle bundle = (TargetBundle) iterator.next();
				if (bundle.isSourceBundle()) {
					String name = bundle.getSourceTarget().getSymbolicName();
					if (name != null && (dependencies.contains(name) || checkedNames.contains(name))) {
						toCheck.add(bundle);
					}
				} else if (dependencies.contains(bundle.getBundleInfo().getSymbolicName())) {
					toCheck.add(bundle);
				}
			}
			return toCheck;
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
		}

		return new ArrayList(0);
	}

	/**
	 * Uses the feature model to determine the set of features required by the
	 * given list of checked features
	 * 
	 * @param allFeatures list of all features to search requirements in
	 * @param checkedFeatures list of features to get requirements for
	 * @return set of features to be checked
	 */
	private Set getRequiredFeatures(final TargetFeature[] allFeatures, final Object[] checkedFeatures) {
		Set required = new HashSet();
		for (int j = 0; j < checkedFeatures.length; j++) {
			if (checkedFeatures[j] instanceof TargetFeature) {
				getFeatureDependencies((TargetFeature) checkedFeatures[j], allFeatures, required);
			}
		}
		return required;
	}

	/**
	 * Recursively gets the ID of required features of this feature and adds them to the required features list
	 * @param model target feature to get requirements of
	 * @param requiredFeatures collector for the required target features {@link TargetFeature}
	 */
	private void getFeatureDependencies(TargetFeature feature, TargetFeature[] allFeatures, Set/*<TargetFeature>*/requiredFeatures) {
		NameVersionDescriptor[] dependents = feature.getDependentFeatures();
		for (int i = 0; i < dependents.length; i++) {
			for (int j = 0; j < allFeatures.length; j++) {
				if (allFeatures[j].getId().equals(dependents[i].getId())) {
					if (!requiredFeatures.contains(allFeatures[j])) {
						requiredFeatures.add(allFeatures[j]);
						getFeatureDependencies(allFeatures[j], allFeatures, requiredFeatures);
					}
					break;
				}
			}
		}
	}

	private void handleGroupChange() {
		int index;
		if (fGroupCombo != null) {
			index = fGroupCombo.getSelectionIndex();
		} else {
			index = fGroupComboPart.getSelectionIndex();
		}
		if (index != fGrouping) {
			// Refresh tree
			fGrouping = index;
			fTree.getControl().setRedraw(false);
			fTree.refresh(false);
			fTree.expandAll();
			updateCheckState();
			updateButtons();
			fTree.getControl().setRedraw(true);
		}
	}

	private void updateButtons() {
		if (fTargetDefinition != null && !fTree.getSelection().isEmpty()) {
			Object[] selection = ((IStructuredSelection) fTree.getSelection()).toArray();
			boolean hasResolveBundle = false;
			boolean hasParent = false;
			boolean allSelected = true;
			boolean noneSelected = true;
			for (int i = 0; i < selection.length; i++) {
				if (!hasResolveBundle || !hasParent) {
					if (selection[i] instanceof TargetBundle) {
						hasResolveBundle = true;
					} else {
						hasParent = true;
					}
				}
				boolean checked = fTree.getChecked(selection[i]);
				if (checked) {
					noneSelected = false;
				} else {
					allSelected = false;
				}
			}
			// Selection is available if not everything is already selected and not both a parent and child item are selected
			fSelectButton.setEnabled(!allSelected && !(hasResolveBundle && hasParent));
			fDeselectButton.setEnabled(!noneSelected && !(hasResolveBundle && hasParent));
		} else {
			fSelectButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
		}

		int total = fAllBundles.size();
		if (fFeaureModeButton.getSelection()) {
			if (fTargetDefinition == null) {
				total = 0;
			} else {
				total = fTargetDefinition.getAllFeatures().length;
				total += fTargetDefinition.getOtherBundles().length;
			}
		}
		if (fMissing != null) {
			total += fMissing.size();
		}

		fSelectAllButton.setEnabled(fTargetDefinition != null && fTree.getCheckedLeafCount() != total);
		fDeselectAllButton.setEnabled(fTargetDefinition != null && fTree.getCheckedLeafCount() != 0);
		fSelectRequiredButton.setEnabled(fTargetDefinition != null && fTree.getCheckedLeafCount() > 0 && fTree.getCheckedLeafCount() != total);

		if (fTargetDefinition != null) {
			fCountLabel.setText(MessageFormat.format(Messages.TargetContentsGroup_9, new String[] {Integer.toString(fTree.getCheckedLeafCount()), Integer.toString(total)}));
		} else {
			fCountLabel.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * Set the container to display in the tree or <code>null</code> to disable the tree 
	 * @param input bundle container or <code>null</code>
	 */
	public void setInput(ITargetDefinition input) {
		// Update the cached data
		fFileBundleMapping = null;
		fAllBundles.clear();

		if (fMissing != null) {
			fMissing.clear();
			fMissing = null;
		}

		if (input instanceof TargetDefinition) {
			fTargetDefinition = (TargetDefinition) input;
		} else {
			setEnabled(false);
			return;
		}

		if (!input.isResolved()) {
			fTree.setInput(Messages.TargetContentsGroup_10);
			setEnabled(false);
			return;
		}

		TargetBundle[] allResolvedBundles = input.getAllBundles();
		if (allResolvedBundles == null || allResolvedBundles.length == 0) {
			fTree.setInput(Messages.TargetContentsGroup_11);
			setEnabled(false);
			return;
		}

		for (int i = 0; i < allResolvedBundles.length; i++) {
			// We only display bundles that have symbolic names
			if (allResolvedBundles[i].getBundleInfo().getSymbolicName() != null) {
				fAllBundles.add(allResolvedBundles[i]);
			}
		}

		boolean isFeatureMode = fTargetDefinition.getUIMode() == TargetDefinition.MODE_FEATURE;
		fFeaureModeButton.setSelection(isFeatureMode);
		fPluginModeButton.setSelection(!isFeatureMode);
		fGroupLabel.setEnabled(!isFeatureMode);

		fTree.getControl().setRedraw(false);
		fTree.setInput(fTargetDefinition);
		fTree.expandAll();
		updateCheckState();
		updateButtons();
		setEnabled(true);
		fTree.getControl().setRedraw(true);
	}

	private void updateCheckState() {
		List result = new ArrayList();
		// Checked error statuses
		if (fMissing != null) {
			result.addAll(fMissing);
		}
		if (fFeaureModeButton.getSelection()) {
			// Checked features and plugins
			result.addAll(fTargetDefinition.getFeaturesAndBundles());
		} else {
			// Bundles with errors are already included from fMissing, do not add twice
			TargetBundle[] bundles = fTargetDefinition.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				if (bundles[i].getStatus().isOK()) {
					result.add(bundles[i]);
				}
			}
		}
		fTree.setCheckedElements(result.toArray());
	}

	/**
	 * This method clears any current target information and puts "Resolve Cancelled" into the
	 * tree.  Setting the input to null results in "Resolving..." to be put into the table which 
	 * may not be accurate.
	 */
	public void setCancelled() {
		fTargetDefinition = null;
		fTree.setInput(Messages.TargetContentsGroup_resolveCancelled);
		setEnabled(false);
	}

	/**
	 * @return a map connecting IPath to the resolved bundles in that path
	 */
	private Map getFileBundleMapping() {
		if (fFileBundleMapping != null) {
			return fFileBundleMapping;
		}

		// Map the bundles into their file locations
		fFileBundleMapping = new HashMap();
		for (Iterator iterator = fAllBundles.iterator(); iterator.hasNext();) {
			TargetBundle currentBundle = (TargetBundle) iterator.next();
			IPath parentPath = getParentPath(currentBundle);
			List bundles = (List) fFileBundleMapping.get(parentPath);
			if (bundles == null) {
				bundles = new ArrayList();
				bundles.add(currentBundle);
				fFileBundleMapping.put(parentPath, bundles);
			} else {
				bundles.add(currentBundle);
			}
		}
		return fFileBundleMapping;
	}

	private Object[] getBundleChildren(Object parent) {
		Object[] result = null;
		if (parent == null) {
			result = fAllBundles.toArray();
		} else if (fFeaureModeButton.getSelection() && parent == OTHER_CATEGORY) {
			result = fTargetDefinition.getOtherBundles();
		} else if (fGrouping == GROUP_BY_CONTAINER && parent instanceof ITargetLocation) {
			ITargetLocation container = (ITargetLocation) parent;
			result = container.getBundles();
		} else if (fGrouping == GROUP_BY_FILE_LOC && parent instanceof IPath) {
			List bundles = (List) getFileBundleMapping().get(parent);
			if (bundles != null && bundles.size() > 0) {
				result = bundles.toArray();
			}
		}
		if (result == null) {
			return new Object[0];
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		fTree.getControl().setEnabled(enabled);
		if (enabled) {
			updateButtons();
		} else {
			fSelectButton.setEnabled(false);
			fSelectAllButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
			fDeselectAllButton.setEnabled(false);
			fSelectRequiredButton.setEnabled(false);
			fCountLabel.setText(""); //$NON-NLS-1$
		}
		fModeLabel.setEnabled(enabled);
		fPluginModeButton.setEnabled(enabled);
		fFeaureModeButton.setEnabled(enabled);
		fShowLabel.setEnabled(enabled);
		fShowPluginsButton.setEnabled(enabled);
		fShowSourceButton.setEnabled(enabled);
		boolean isPluginMode = !fFeaureModeButton.getSelection();
		fGroupLabel.setEnabled(enabled && isPluginMode);
		if (fGroupCombo != null) {
			fGroupCombo.setEnabled(enabled && isPluginMode);
		} else {
			fGroupComboPart.setEnabled(enabled && isPluginMode);
		}
	}

	public void saveIncludedBundleState() {
		if (fFeaureModeButton.getSelection()) {
			// Create a list of checked bundle infos
			List included = new ArrayList();
			int missingCount = 0;
			Object[] checked = fTree.getCheckedLeafElements();
			for (int i = 0; i < checked.length; i++) {
				if (checked[i] instanceof TargetFeature) {
					included.add(new NameVersionDescriptor(((TargetFeature) checked[i]).getId(), null, NameVersionDescriptor.TYPE_FEATURE));
				}
				if (checked[i] instanceof TargetBundle) {
					// Missing features are included as TargetBundles, save them as features instead
					if (((TargetBundle) checked[i]).getStatus().getCode() == TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST) {
						included.add(new NameVersionDescriptor(((TargetBundle) checked[i]).getBundleInfo().getSymbolicName(), null, NameVersionDescriptor.TYPE_PLUGIN));
						missingCount++;
					} else if (((TargetBundle) checked[i]).getStatus().getCode() == TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST) {
						included.add(new NameVersionDescriptor(((TargetBundle) checked[i]).getBundleInfo().getSymbolicName(), null, NameVersionDescriptor.TYPE_FEATURE));
						missingCount++;
					} else {
						included.add(new NameVersionDescriptor(((TargetBundle) checked[i]).getBundleInfo().getSymbolicName(), null));
					}
				}
			}

			if (included.size() == 0) {
				fTargetDefinition.setIncluded(new NameVersionDescriptor[0]);
			} else if (included.size() == 0 || included.size() - missingCount == fTargetDefinition.getAllFeatures().length + fTargetDefinition.getOtherBundles().length) {
				fTargetDefinition.setIncluded(null);
			} else {
				fTargetDefinition.setIncluded((NameVersionDescriptor[]) included.toArray(new NameVersionDescriptor[included.size()]));
			}
		} else {
			// Figure out if there are multiple bundles sharing the same id
			Set multi = new HashSet(); // BSNs of bundles with multiple versions available
			Set all = new HashSet();
			for (Iterator iterator = fAllBundles.iterator(); iterator.hasNext();) {
				TargetBundle rb = (TargetBundle) iterator.next();
				if (!all.add(rb.getBundleInfo().getSymbolicName())) {
					multi.add(rb.getBundleInfo().getSymbolicName());
				}
			}

			// Create a list of checked bundle infos
			List included = new ArrayList();
			Object[] checked = fTree.getCheckedLeafElements();
			for (int i = 0; i < checked.length; i++) {
				if (checked[i] instanceof TargetBundle) {
					// Create the bundle info object, if the bundle has no symbolic name don't save it
					String bsn = ((TargetBundle) checked[i]).getBundleInfo().getSymbolicName();
					if (bsn != null) {
						NameVersionDescriptor info = null;
						if (multi.contains(bsn)) {
							// include version info
							info = new NameVersionDescriptor(bsn, ((TargetBundle) checked[i]).getBundleInfo().getVersion());
						} else {
							// don't store version info
							info = new NameVersionDescriptor(bsn, null);
						}
						included.add(info);
					}
				}
			}

			if (included.size() == 0) {
				fTargetDefinition.setIncluded(new NameVersionDescriptor[0]);
			} else if (included.size() == fAllBundles.size() + fMissing.size()) {
				fTargetDefinition.setIncluded(null);
			} else {
				fTargetDefinition.setIncluded((NameVersionDescriptor[]) included.toArray(new NameVersionDescriptor[included.size()]));
			}
		}
	}

	/**
	 * Content provider for the content tree.  Allows for different groupings to be used.
	 *
	 */
	class TreeContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parentElement) {
			return getBundleChildren(parentElement);
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (fFeaureModeButton.getSelection() && element == OTHER_CATEGORY) {
				return true;
			}
			if (fGrouping == GROUP_BY_NONE || element instanceof TargetBundle) {
				return false;
			}
			if (element instanceof ITargetLocation || element instanceof IPath) {
				return getBundleChildren(element).length > 0;
			}
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {
				List result = new ArrayList();

				// Check if there are any errors for missing features/bundles to display
				if (fMissing == null || fMissing.isEmpty()) {
					fMissing = new HashSet(); // A set is used to remove copies of problem bundles
					TargetBundle[] bundles = fTargetDefinition.getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (!bundles[i].getStatus().isOK()) {
							// We only display error bundles that have symbolic names
							if (bundles[i].getBundleInfo().getSymbolicName() != null) {
								fMissing.add(bundles[i]);
							}
						}
					}
					result.addAll(fMissing);
				} else {
					// As missing bundles are unchecked, we want to keep them in the table, only if locations change does fMissing become null
					result.addAll(fMissing);
				}

				if (fFeaureModeButton.getSelection()) {
					TargetFeature[] features = fTargetDefinition.getAllFeatures();
					result.addAll(Arrays.asList(features));

					// Check if we need the other category
					if (fTargetDefinition.getOtherBundles().length > 0) {
						result.add(OTHER_CATEGORY);
					}
				} else if (fGrouping == GROUP_BY_CONTAINER) {
					result.addAll(Arrays.asList(fTargetDefinition.getTargetLocations()));
				} else if (fGrouping == GROUP_BY_NONE) {
					// Missing bundles are already handled by adding to fMissing, avoid adding twice
					TargetBundle[] allBundles = fTargetDefinition.getAllBundles();
					for (int i = 0; i < allBundles.length; i++) {
						if (allBundles[i].getStatus().isOK()) {
							// Assume that if the bundle is OK, it has a symbolic name
							result.add(allBundles[i]);
						}
					}
				} else {
					result.addAll(Arrays.asList(getFileBundleMapping().keySet().toArray()));
				}

				return result.toArray();
			}
			return new Object[] {inputElement};
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

}
