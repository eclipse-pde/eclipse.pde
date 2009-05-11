/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.BundleException;

/**
 * UI Part that displays all of the bundle contents of a target.  The bundles can be
 * excluded by unchecking them.  There are a variety of options to change the tree's
 * format.
 * 
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see IResolvedBundle
 */
public class TargetContentsGroup extends FilteredTree {

	private CheckboxTreeViewer fTree;
	private Button fSelectButton;
	private Button fDeselectButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Button fSelectRequiredButton;
	private Label fShowLabel;
	private Button fShowSourceButton;
	private Button fShowPluginsButton;
	private Label fCountLabel;
	private Label fGroupLabel;
	private Combo fGroupCombo;
	private ComboPart fGroupComboPart;

	private ViewerFilter fSourceFilter;
	private ViewerFilter fPluginFilter;

	/*
	 * TODO This could likely be done better with fewer datastructures by using a 
	 * similar structure to FilteredCheckboxTree.  Instead of storing resolved bundles
	 * store a special object which remembers it's check state.
	 */
	private List fAllBundles;
	private Set fAllChecked;
	private Map fContainerBundles;
	private Map fContainerChecked;
	private Map fFileBundles;
	private Map fFileChecked;

	private ITargetDefinition fTargetDefinition;

	private FormToolkit fToolkit;

	private int fGrouping;
	private static final int GROUP_BY_NONE = 0;
	private static final int GROUP_BY_FILE_LOC = 1;
	private static final int GROUP_BY_CONTAINER = 2;
	private ListenerList fChangeListeners = new ListenerList();

	public TargetContentsGroup(Composite parent) {
		super(parent, SWT.NONE, null, true);
		PatternFilter filter = new PatternFilter();
		filter.setIncludeLeadingWildcard(true);
		super.init(SWT.NONE, filter);
	}

	public TargetContentsGroup(Composite parent, FormToolkit toolkit) {
		// Hack to setup the toolkit before creating the controls
		super(parent, SWT.NONE, null, true);
		fToolkit = toolkit;
		PatternFilter filter = new PatternFilter();
		filter.setIncludeLeadingWildcard(true);
		super.init(SWT.NONE, filter);
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredTree#init(int, org.eclipse.ui.dialogs.PatternFilter)
	 */
	protected void init(int treeStyle, PatternFilter filter) {
		// Overridden to do nothing to avoid creating the controls when we don't have a form toolkit
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredTree#createTreeControl(org.eclipse.swt.widgets.Composite, int)
	 */
	protected Control createTreeControl(Composite parent, int style) {
		fGrouping = GROUP_BY_NONE;
		Composite treeComp = null;
		if (fToolkit != null) {
			treeComp = fToolkit.createComposite(parent);
			GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = layout.marginHeight = 0;
			treeComp.setLayout(layout);
			treeComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		} else {
			treeComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);
		}
		super.createTreeControl(treeComp, style);
		((GridData) fTree.getControl().getLayoutData()).heightHint = 300;
		createButtons(treeComp);

		if (fToolkit != null) {
			fCountLabel = fToolkit.createLabel(treeComp, ""); //$NON-NLS-1$
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			fCountLabel.setLayoutData(data);
		} else {
			fCountLabel = SWTFactory.createLabel(treeComp, "", 2); //$NON-NLS-1$
		}

		updateButtons();
		initializeFilters();
		return treeComp;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredTree#doCreateRefreshJob()
	 */
	protected WorkbenchJob doCreateRefreshJob() {
		WorkbenchJob job = super.doCreateRefreshJob();
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				// Don't update the tree if no filtering has been done yet
				if (event.getResult().getSeverity() != IStatus.CANCEL && fAllBundles != null) {
					fTree.expandAll();
					updateCheckState();
				}
			}
		});
		return job;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredTree#doCreateTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		Tree tree = null;
		if (fToolkit != null) {
			tree = fToolkit.createTree(parent, SWT.CHECK | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		} else {
			tree = new Tree(parent, style | SWT.CHECK | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		}

		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTree = new CheckboxTreeViewer(tree);
		fTree.setUseHashlookup(true);
		fTree.setContentProvider(new TreeContentProvider());
		fTree.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object first = selection.getFirstElement();
				handleCheck(new Object[] {selection.getFirstElement()}, !fTree.getChecked(first));
			}
		});
		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				handleCheck(new Object[] {event.getElement()}, fTree.getChecked(event.getElement()));
			}
		});
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTree.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof IResolvedBundle && e2 instanceof IResolvedBundle) {
					IStatus status1 = ((IResolvedBundle) e1).getStatus();
					IStatus status2 = ((IResolvedBundle) e2).getStatus();
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
		return fTree;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredTree#doCreateFilterText(org.eclipse.swt.widgets.Composite)
	 */
	protected Text doCreateFilterText(Composite parent) {
		// Overridden so the text gets create using the toolkit if we have one
		Text parentText = super.doCreateFilterText(parent);
		if (fToolkit != null) {
			int style = parentText.getStyle();
			parentText.dispose();
			return fToolkit.createText(parent, null, style);
		}
		return parentText;
	}

	private void createButtons(Composite parent) {
		if (fToolkit != null) {
			Composite buttonComp = fToolkit.createComposite(parent);
			GridLayout layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			buttonComp.setLayout(layout);
			buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

			fSelectButton = fToolkit.createButton(buttonComp, Messages.IncludedBundlesTree_0, SWT.PUSH);
			fSelectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectButton = fToolkit.createButton(buttonComp, Messages.IncludedBundlesTree_1, SWT.PUSH);
			fDeselectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label emptySpace = new Label(buttonComp, SWT.NONE);
			GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectAllButton = fToolkit.createButton(buttonComp, Messages.IncludedBundlesTree_2, SWT.PUSH);
			fSelectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectAllButton = fToolkit.createButton(buttonComp, Messages.IncludedBundlesTree_3, SWT.PUSH);
			fDeselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			emptySpace = new Label(buttonComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectRequiredButton = fToolkit.createButton(buttonComp, Messages.TargetContentsGroup_4, SWT.PUSH);
			fSelectRequiredButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Composite filterComp = fToolkit.createComposite(buttonComp);
			layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			filterComp.setLayout(layout);
			filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

			fShowLabel = fToolkit.createLabel(filterComp, Messages.BundleContainerTable_9);

			fShowPluginsButton = fToolkit.createButton(filterComp, Messages.BundleContainerTable_14, SWT.CHECK);
			fShowPluginsButton.setSelection(true);
			fShowSourceButton = fToolkit.createButton(filterComp, Messages.BundleContainerTable_15, SWT.CHECK);
			fShowSourceButton.setSelection(true);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fGroupLabel = fToolkit.createLabel(filterComp, Messages.TargetContentsGroup_0);

			fGroupComboPart = new ComboPart();
			fGroupComboPart.createControl(filterComp, fToolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
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
					handleCheck(selected, true);
				}
			}
		});

		fDeselectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					handleCheck(selected, false);
				}
			}
		});

		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] elements = ((ITreeContentProvider) fTree.getContentProvider()).getElements(fTree.getInput());
				handleCheck(elements, true);
			}
		});

		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] elements = ((ITreeContentProvider) fTree.getContentProvider()).getElements(fTree.getInput());
				handleCheck(elements, false);
			}
		});

		fSelectRequiredButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleCheck(getRequiredElements(fAllBundles, fAllChecked), true);
			}
		});

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
		GridData gd = new GridData();
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
				if (element instanceof IResolvedBundle) {
					if (((IResolvedBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
		fPluginFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResolvedBundle) {
					if (!((IResolvedBundle) element).isSourceBundle()) {
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
	private IPath getParentPath(IResolvedBundle bundle) {
		URI location = bundle.getBundleInfo().getLocation();
		if (location == null) {
			return new Path(Messages.TargetContentsGroup_8);
		}
		IPath path = new Path(URIUtil.toUnencodedString(location));
		path = path.removeLastSegments(1);
		return path;
	}

	/**
	 * 
	 * TODO SHOULD BE EQUIVALENT METHOD ELSEWHERE IN PDE
	 * 
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
	 * @return list of plug-ins required by the currently checked plug-ins
	 */
	private Object[] getRequiredElements(final Collection allBundles, final Collection checkedBundles) {
		final Set dependencies = new HashSet();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(Messages.TargetContentsGroup_5, 150);

					// Get all the bundle locations
					List allLocations = new ArrayList(allBundles.size());
					for (Iterator iterator = allBundles.iterator(); iterator.hasNext();) {
						IResolvedBundle current = (IResolvedBundle) iterator.next();
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
					List checkedModels = new ArrayList(checkedBundles.size());
					for (Iterator iterator = checkedBundles.iterator(); iterator.hasNext();) {
						BundleInfo bundle = ((IResolvedBundle) iterator.next()).getBundleInfo();
						for (int j = 0; j < models.length; j++) {
							if (models[j].getBundleDescription().getSymbolicName().equals(bundle.getSymbolicName()) && models[j].getBundleDescription().getVersion().toString().equals(bundle.getVersion())) {
								checkedModels.add(models[j]);
								break;
							}
						}
					}
					monitor.worked(20);
					if (monitor.isCanceled()) {
						return;
					}

					// Get implicit dependencies as a list of strings
					// This is wasteful since the dependency calculation puts them back into BundleInfos
					BundleInfo[] implicitDependencies = fTargetDefinition.getImplicitDependencies();
					List implicitIDs = new ArrayList();
					if (implicitDependencies != null) {
						for (int i = 0; i < implicitDependencies.length; i++) {
							implicitIDs.add(implicitDependencies[i].getSymbolicName());
						}
					}
					monitor.worked(10);

					// Get all dependency bundles
					dependencies.addAll(DependencyManager.getDependencies(checkedModels.toArray(), (String[]) implicitIDs.toArray(new String[implicitIDs.size()]), state.getState()));
					monitor.worked(50);

				} finally {
					monitor.done();
				}
			}
		};
		try {
			// Calculate the dependencies
			new ProgressMonitorDialog(getShell()).run(true, true, op);

			// We want to check the dependents, the source of the dependents, and the source of the originally checked
			Set checkedNames = new HashSet(checkedBundles.size());
			for (Iterator iterator = checkedBundles.iterator(); iterator.hasNext();) {
				IResolvedBundle current = (IResolvedBundle) iterator.next();
				checkedNames.add(current.getBundleInfo().getSymbolicName());
			}

			List toCheck = new ArrayList();
			for (Iterator iterator = fAllBundles.iterator(); iterator.hasNext();) {
				IResolvedBundle bundle = (IResolvedBundle) iterator.next();
				if (bundle.isSourceBundle()) {
					String name = bundle.getSourceTarget().getSymbolicName();
					if (name != null && (dependencies.contains(name) || checkedNames.contains(name))) {
						toCheck.add(bundle);
					}
				} else if (dependencies.contains(bundle.getBundleInfo().getSymbolicName())) {
					toCheck.add(bundle);
				}
			}
			return toCheck.toArray();
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
		}

		return new Object[0];
	}

	/**
	 * Sets the check state of the given elements to the given state.  Updates
	 * all the datastructures that store the current check state and updates any
	 * parent items in the tree.
	 * 
	 * @param changedElements list of changed elements
	 * @param checkState new check state for the elements
	 */
	private void handleCheck(Object[] changedElements, boolean checkState) {
		if (changedElements.length > 0) {
			if (changedElements[0] instanceof IResolvedBundle) {
				Set changedContainers = new HashSet();
				Set changedFiles = new HashSet();
				for (int i = 0; i < changedElements.length; i++) {
					Object parent = ((IResolvedBundle) changedElements[i]).getParentContainer();
					changedContainers.add(parent);
					Set containerChecked = ((Set) fContainerChecked.get(parent));

					parent = getParentPath((IResolvedBundle) changedElements[i]);
					changedFiles.add(parent);
					Set fileChecked = ((Set) fFileChecked.get(parent));

					if (checkState) {
						fAllChecked.add(changedElements[i]);
						containerChecked.add(changedElements[i]);
						fileChecked.add(changedElements[i]);
					} else {
						fAllChecked.remove(changedElements[i]);
						containerChecked.remove(changedElements[i]);
						fileChecked.remove(changedElements[i]);
					}
					fTree.setChecked(changedElements[i], checkState);
				}
				if (fGrouping != GROUP_BY_NONE) {
					Iterator iterator = fGrouping == GROUP_BY_CONTAINER ? changedContainers.iterator() : changedFiles.iterator();
					while (iterator.hasNext()) {
						Object parent = iterator.next();
						if (getChecked(parent).size() == 0) {
							fTree.setGrayChecked(parent, false);
						} else if (getChecked(parent).size() == getBundleChildren(parent).size()) {
							fTree.setGrayed(parent, false);
							fTree.setChecked(parent, true);
						} else {
							fTree.setGrayChecked(parent, true);
						}
					}
				}
				saveIncludedBundleState(changedContainers.toArray());
			} else {
				Set totalChanged = new HashSet();
				for (int i = 0; i < changedElements.length; i++) {
					fTree.setGrayed(changedElements[i], false);
					fTree.setChecked(changedElements[i], checkState);
					fTree.setSubtreeChecked(changedElements[i], checkState);

					Set checked;
					List all;
					if (fGrouping == GROUP_BY_CONTAINER) {
						checked = (Set) fContainerChecked.get(changedElements[i]);
						all = (List) fContainerBundles.get(changedElements[i]);
					} else {
						checked = (Set) fFileChecked.get(changedElements[i]);
						all = (List) fFileBundles.get(changedElements[i]);
					}
					if (checkState) {
						checked.addAll(all);
					} else {
						checked.removeAll(all);
					}
					totalChanged.addAll(all);
				}
				// Update the maps that we are not currently displaying
				Iterator iterator = fGrouping == GROUP_BY_CONTAINER ? fFileChecked.values().iterator() : fContainerChecked.values().iterator();
				while (iterator.hasNext()) {
					Set current = (Set) iterator.next();
					if (checkState) {
						current.addAll(totalChanged);
					} else {
						current.removeAll(totalChanged);
					}
				}
				if (checkState) {
					fAllChecked.addAll(totalChanged);
				} else {
					fAllChecked.removeAll(totalChanged);
				}
				if (fGrouping == GROUP_BY_CONTAINER) {
					saveIncludedBundleState(changedElements);
				} else {
					// Easier to just save everything than loop through every bundle that changed and find its parent
					saveIncludedBundleState(fTargetDefinition.getBundleContainers());
				}
			}
			contentChanged();
			updateButtons();
			// Update the parent container labels with the new count
			if (fGrouping == GROUP_BY_CONTAINER) {
				fTree.update(fContainerBundles.keySet().toArray(), new String[] {IBasicPropertyConstants.P_TEXT});
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
			fGrouping = index;
			fTree.getControl().setRedraw(false);
			fTree.refresh(false);
			fTree.expandAll();
			updateCheckState();
			updateButtons();
			fTree.getControl().setRedraw(true);
		}
	}

	private void updateCheckState() {
		for (Iterator iterator = fAllChecked.iterator(); iterator.hasNext();) {
			fTree.setChecked(iterator.next(), true);
		}
		if (fGrouping != GROUP_BY_NONE) {
			Map bundleMap = null;
			Map checkedMap = null;
			if (fGrouping == GROUP_BY_CONTAINER) {
				bundleMap = fContainerBundles;
				checkedMap = fContainerChecked;
			} else if (fGrouping == GROUP_BY_FILE_LOC) {
				bundleMap = fFileBundles;
				checkedMap = fFileChecked;
			}
			for (Iterator iterator = bundleMap.keySet().iterator(); iterator.hasNext();) {
				Object currentParent = iterator.next();
				Set checked = (Set) checkedMap.get(currentParent);
				if (checked.size() == 0) {
					fTree.setGrayed(currentParent, false);
					fTree.setChecked(currentParent, false);
				} else if (checked.size() == ((List) bundleMap.get(currentParent)).size()) {
					fTree.setGrayed(currentParent, false);
					fTree.setChecked(currentParent, true);
				} else {
					fTree.setGrayChecked(currentParent, true);
				}
			}
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
					if (selection[i] instanceof IResolvedBundle) {
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
			// Selection is available is not everything is already selected and not both a parent and child item are selected
			fSelectButton.setEnabled(!allSelected && !(hasResolveBundle && hasParent));
			fDeselectButton.setEnabled(!noneSelected && !(hasResolveBundle && hasParent));
		} else {
			fSelectButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
		}

		fSelectAllButton.setEnabled(fTargetDefinition != null && fAllChecked.size() != fAllBundles.size());
		fDeselectAllButton.setEnabled(fTargetDefinition != null && fAllChecked.size() != 0);
		fSelectRequiredButton.setEnabled(fTargetDefinition != null && fAllChecked.size() > 0 && fAllChecked.size() != fAllBundles.size());

		if (fTargetDefinition != null) {
			fCountLabel.setText(MessageFormat.format(Messages.TargetContentsGroup_9, new String[] {Integer.toString(fAllChecked.size()), Integer.toString(fAllBundles.size())}));
		} else {
			fCountLabel.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * Set the container to display in the tree or <code>null</code> to disable the tree 
	 * @param input bundle container or <code>null</code>
	 */
	public void setInput(ITargetDefinition input) {
		fTargetDefinition = input;

		if (input == null || !input.isResolved()) {
			fTree.setInput(Messages.TargetContentsGroup_10);
			setEnabled(false);
			return;
		}

		IResolvedBundle[] allResolvedBundles = input.getAllBundles();
		if (allResolvedBundles == null || allResolvedBundles.length == 0) {
			fTree.setInput(Messages.TargetContentsGroup_11);
			setEnabled(false);
			return;
		}

		fTree.setInput(Messages.TargetContentsGroup_12);
		setEnabled(false);
		Job initJob = new InitalizeJob();
		initJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				Job refreshJob = new UIJob(Messages.TargetContentsGroup_13) {
					public IStatus runInUIThread(IProgressMonitor monitor) {
						fTree.setInput(fTargetDefinition);
						fTree.expandAll();
						updateCheckState();
						updateButtons();
						setEnabled(true);
						return Status.OK_STATUS;
					}
				};
				refreshJob.setSystem(true);
				refreshJob.schedule();
			}
		});
		initJob.schedule();
	}

	private class InitalizeJob extends Job {

		public InitalizeJob() {
			super(Messages.TargetContentsGroup_13);
			setSystem(true);
		}

		protected IStatus run(IProgressMonitor monitor) {
			fAllBundles = new ArrayList();
			fAllChecked = new HashSet();
			fContainerBundles = new HashMap();
			fContainerChecked = new HashMap();
			IBundleContainer[] containers = fTargetDefinition.getBundleContainers();
			// Iterate through each container, adding bundles to the map and list
			for (int i = 0; i < containers.length; i++) {
				Object[] containerBundlesArray = containers[i].getAllBundles();
				List containerBundles = new ArrayList(containerBundlesArray.length);
				for (int j = 0; j < containerBundlesArray.length; j++) {
					containerBundles.add(containerBundlesArray[j]);
				}
				fAllBundles.addAll(containerBundles);
				fContainerBundles.put(containers[i], containerBundles);

				// Determine which of the bundles are checked (included)
				if (containers[i].getIncludedBundles() == null) {
					// Everything is included
					Set checked = new HashSet();
					checked.addAll(containerBundles);
					fContainerChecked.put(containers[i], checked);
					fAllChecked.addAll(checked);
				} else {
					// Mark the included bundles as checked
					List includedBundles = Arrays.asList(containers[i].getBundles());
					// If an included bundle has errors it must be explicitly added to the bundle list as getAllBundles does not return it.
					for (Iterator iterator = includedBundles.iterator(); iterator.hasNext();) {
						IResolvedBundle currentIncluded = (IResolvedBundle) iterator.next();
						if (!currentIncluded.getStatus().isOK()) {
							((List) fContainerBundles.get(containers[i])).add(currentIncluded);
							fAllBundles.add(currentIncluded);
						}
					}
					Set checked = new HashSet();
					checked.addAll(includedBundles);
					fContainerChecked.put(containers[i], checked);
					fAllChecked.addAll(checked);
				}
			}

			// Map the bundles into their file locations
			fFileBundles = new HashMap();
			fFileChecked = new HashMap();
			for (Iterator iterator = fAllBundles.iterator(); iterator.hasNext();) {
				IResolvedBundle currentBundle = (IResolvedBundle) iterator.next();
				IPath parentPath = getParentPath(currentBundle);
				List bundles = (List) fFileBundles.get(parentPath);
				if (bundles == null) {
					bundles = new ArrayList();
					bundles.add(currentBundle);
					fFileBundles.put(parentPath, bundles);
					// Some paths may have nothing checked, but we still need a set stored in the map
					fFileChecked.put(parentPath, new HashSet());
				} else {
					bundles.add(currentBundle);
				}
				// Determine whether the current bundle is checked
				if (fAllChecked.contains(currentBundle)) {
					Set checked = (Set) fFileChecked.get(parentPath);
					if (checked == null) {
						checked = new HashSet();
						checked.add(currentBundle);
						fFileChecked.put(parentPath, checked);
					} else {
						checked.add(currentBundle);
					}
				}
			}

			return Status.OK_STATUS;
		}
	}

	private Set getChecked(Object parent) {
		Set result = null;
		if (parent == null) {
			result = fAllChecked;
		} else if (fGrouping == GROUP_BY_CONTAINER) {
			result = (Set) fContainerChecked.get(parent);
		} else if (fGrouping == GROUP_BY_FILE_LOC) {
			result = (Set) fFileChecked.get(parent);
		}
		if (result == null) {
			return new HashSet(0);
		}
		return result;
	}

	private List getBundleChildren(Object parent) {
		List result = null;
		if (parent == null) {
			result = fAllBundles;
		} else if (fGrouping == GROUP_BY_CONTAINER) {
			result = (List) fContainerBundles.get(parent);
		} else if (fGrouping == GROUP_BY_FILE_LOC) {
			result = (List) fFileBundles.get(parent);
		}
		if (result == null) {
			return new ArrayList(0);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
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
		fShowLabel.setEnabled(enabled);
		fShowPluginsButton.setEnabled(enabled);
		fShowSourceButton.setEnabled(enabled);
		fGroupLabel.setEnabled(enabled);
		if (fGroupCombo != null) {
			fGroupCombo.setEnabled(enabled);
		} else {
			fGroupComboPart.setEnabled(enabled);
		}
		super.setEnabled(enabled);
	}

	public void saveIncludedBundleState(Object[] changeContainers) {
		for (int i = 0; i < changeContainers.length; i++) {
			if (changeContainers[i] instanceof IBundleContainer) {
				Set checked = (Set) fContainerChecked.get(changeContainers[i]);
				if (checked.size() == ((Collection) fContainerBundles.get(changeContainers[i])).size()) {
					((IBundleContainer) changeContainers[i]).setIncludedBundles(null);
				} else {
					List included = new ArrayList(checked.size());
					for (Iterator iterator = checked.iterator(); iterator.hasNext();) {
						IResolvedBundle currentBundle = (IResolvedBundle) iterator.next();
						included.add(new BundleInfo(currentBundle.getBundleInfo().getSymbolicName(), null, null, BundleInfo.NO_BUNDLEID, false));
					}
					((IBundleContainer) changeContainers[i]).setIncludedBundles((BundleInfo[]) included.toArray(new BundleInfo[included.size()]));
				}
			}
		}
	}

	/**
	 * Content provider for the content tree.  Allows for different groupings to be used.
	 *
	 */
	class TreeContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parentElement) {
			return getBundleChildren(parentElement).toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (fGrouping == GROUP_BY_NONE || element instanceof IResolvedBundle) {
				return false;
			}
			if (element instanceof IBundleContainer || element instanceof IPath) {
				return getBundleChildren(element).size() > 0;
			}
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {
				if (fGrouping == GROUP_BY_NONE) {
					return fAllBundles.toArray();
				} else if (fGrouping == GROUP_BY_CONTAINER) {
					return fContainerBundles.keySet().toArray();
				} else {
					return fFileBundles.keySet().toArray();
				}
			}
			return new Object[] {inputElement};
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

}
