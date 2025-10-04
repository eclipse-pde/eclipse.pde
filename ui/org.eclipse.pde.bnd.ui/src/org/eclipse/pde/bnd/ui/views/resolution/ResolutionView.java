/*******************************************************************************
 * Copyright (c) 2015, 2025 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Christoph Rueger <chrisrueger@gmail.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.views.resolution;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.bnd.ui.FilterPanelPart;
import org.eclipse.pde.bnd.ui.HelpButtons;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.pde.bnd.ui.internal.PartAdapter;
import org.eclipse.pde.bnd.ui.model.repo.ResourceProvider;
import org.eclipse.pde.bnd.ui.model.resolution.BndBuilderCapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.BndFileCapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.CapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.JarFileCapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.ManifestCapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.ResourceCapReqLoader;
import org.eclipse.pde.bnd.ui.views.ViewEventTopics;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.unmodifiable.Sets;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class ResolutionView extends ViewPart implements ISelectionListener, IResourceChangeListener {

	public static final String PLUGIN_ID = "bndtools.core";

	private final List<EE> ees = Arrays.asList(EE.values());
	private Display display = null;

	private Tree reqsTree = null;
	private Table capsTable = null;

	private TreeViewer reqsViewer;
	private TableViewer capsViewer;

	private Label reqsLabel;
	private Label capsLabel;

	private ViewerFilter hideSelfImportsFilter;
	private ViewerFilter hideOptionalRequirements;
	private ViewerFilter filterShowCapsProblems;

	private boolean inputLocked = false;
	private boolean outOfDate = false;
	Set<CapReqLoader> loaders;
	private Job analysisJob;
	private int currentEE = 4;

	private final Set<String> filteredCapabilityNamespaces;
	private Set<Capability> duplicateCapabilitiesWithDifferentHashes = new HashSet<>();

	private final FilterPanelPart reqsFilterPart = new FilterPanelPart(Resources.getScheduler());
	private final FilterPanelPart capsFilterPart = new FilterPanelPart(Resources.getScheduler());

	private static final String SEARCHSTRING_HINT = "Enter search string (Space to separate terms; '*' for partial matches)";

	private CapReqMapContentProvider reqsContentProvider;
	private CapReqMapContentProvider capsContentProvider;

	private final IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

	public ResolutionView() {
		filteredCapabilityNamespaces = Sets.of(IdentityNamespace.IDENTITY_NAMESPACE, HostNamespace.HOST_NAMESPACE);
		loaders = Collections.emptySet();
	}

	private final IPartListener partAdapter = new PartAdapter() {
		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part == ResolutionView.this) {
				if (outOfDate) {
					executeAnalysis();
				}
			} else if (part instanceof IEditorPart editor) {
				CapReqLoader editorLoader = Adapters.adapt(editor, CapReqLoader.class);
				if (editorLoader != null) {
					System.out.println("Got loader from editor!");
					setLoaders(Collections.singleton(editorLoader));
					return;
				}
				System.out.println("Editor is: " + editor);
				IEditorInput editorInput = editor.getEditorInput();
				System.out.println("Editor input is: " + editorInput);
				IFile file = ResourceUtil.getFile(editorInput);

				if (file != null) {
					IPath location = file.getLocation();

					if (location != null) {
						CapReqLoader loader = getLoaderForFile(location.toFile());

						if (loader != null) {
							setLoaders(Collections.singleton(loader));
						}
					}
				}
			} else {
				System.out.println("Activated: " + part);
			}
		}
	};

	private boolean setLoaders(Set<CapReqLoader> newLoaders) {
		System.out.println("Set loaders: " + newLoaders);
		Set<CapReqLoader> oldLoaders = loaders;
		boolean swap = !oldLoaders.equals(newLoaders);
		if (swap) {
			loaders = newLoaders;
		}
		for (CapReqLoader l : swap ? oldLoaders : newLoaders) {
			IO.close(l);
		}
		if (swap) {
			if (getSite().getPage().isPartVisible(ResolutionView.this)) {
				executeAnalysis();
			} else {
				outOfDate = true;
			}
		}
		return swap;
	}

	private CapReqLoader getLoaderForFile(File file) {
		if (Strings.endsWithIgnoreCase(file.getName(), ".bnd")) {
			return new BndFileCapReqLoader(file);
		} else if (Strings.endsWithIgnoreCase(file.getName(), ".jar")) {
			return new JarFileCapReqLoader(file);
		} else if (Strings.endsWithIgnoreCase(file.getName(), ".mf")) {
			return new ManifestCapReqLoader(file);
		}
		return null;
	}

	@Override
	public void createPartControl(Composite parent) {
		this.display = parent.getDisplay();

		SashForm splitPanel = new SashForm(parent, SWT.HORIZONTAL);
		splitPanel.setLayout(new FillLayout());

		Composite reqsPanel = new Composite(splitPanel, SWT.NONE);
		reqsPanel.setBackground(parent.getBackground());

		GridLayout reqsLayout = new GridLayout(1, false);
		reqsLayout.marginWidth = 0;
		reqsLayout.marginHeight = 0;
		reqsLayout.verticalSpacing = 2;
		reqsPanel.setLayout(reqsLayout);
		reqsFilterPart.createControl(reqsPanel, 5, 5);
		reqsFilterPart.setHint(SEARCHSTRING_HINT);
		reqsFilterPart.addPropertyChangeListener(event -> {
			String filter = (String) event.getNewValue();
			updateReqsFilter(filter);
		});

		reqsLabel = new Label(reqsPanel, SWT.NONE);
		reqsLabel.setText("Requirements:");
		setContentDescription("Select on one or more resources to see their requirements and capabilities.");
		reqsTree = new Tree(reqsPanel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		reqsTree.setHeaderVisible(false);
		reqsTree.setLinesVisible(false);
		reqsTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		reqsViewer = new TreeViewer(reqsTree);
		ColumnViewerToolTipSupport.enableFor(reqsViewer);
		reqsViewer.setLabelProvider(new RequirementWrapperLabelProvider(true));
		reqsContentProvider = new CapReqMapContentProvider();
		reqsViewer.setContentProvider(reqsContentProvider);
		reqsViewer.addDoubleClickListener(event -> handleReqsViewerDoubleClickEvent(event));

		reqsViewer.getControl()
				.addKeyListener(createCopyToClipboardAdapter(reqsViewer,
						(IStructuredSelection selection, StringBuilder clipboardContent) -> reqsCopyToClipboard(
								selection, (RequirementWrapperLabelProvider) reqsViewer.getLabelProvider(),
								clipboardContent)));

		Composite capsPanel = new Composite(splitPanel, SWT.NONE);
		capsPanel.setBackground(parent.getBackground());

		GridLayout capsLayout = new GridLayout(1, false);
		capsLayout.marginWidth = 0;
		capsLayout.marginHeight = 0;
		capsLayout.verticalSpacing = 2;
		capsPanel.setLayout(capsLayout);
		capsFilterPart.createControl(capsPanel, 5, 5);
		capsFilterPart.setHint(SEARCHSTRING_HINT);
		capsFilterPart.addPropertyChangeListener(event -> {
			String filter = (String) event.getNewValue();
			updateCapsFilter(filter);
		});
		capsLabel = new Label(capsPanel, SWT.NONE);
		capsLabel.setText("Capabilities:");
		capsTable = new Table(capsPanel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		capsTable.setHeaderVisible(false);
		capsTable.setLinesVisible(false);
		capsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		capsViewer = new TableViewer(capsTable);
		ColumnViewerToolTipSupport.enableFor(capsViewer);
		capsViewer.setLabelProvider(new CapabilityLabelProvider(true));
		capsContentProvider = new CapReqMapContentProvider();
		capsViewer.setContentProvider(capsContentProvider);
		capsViewer.setFilters(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {
				return !filteredCapabilityNamespaces.contains(((Capability) element).getNamespace());
			}
		});

		filterShowCapsProblems = new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof Capability cap) {
					return duplicateCapabilitiesWithDifferentHashes.contains((cap));
				}
				return false;
			}
		};

		capsViewer.addDoubleClickListener(event -> handleCapsViewerDoubleClickEvent(event));

		capsViewer.getTable()
				.addKeyListener(createCopyToClipboardAdapter(capsViewer,
						(IStructuredSelection selection1, StringBuilder clipboardContent1) -> capsCopyToClipboard(
								selection1, (CapabilityLabelProvider) capsViewer.getLabelProvider(),
								clipboardContent1)));

		hideSelfImportsFilter = new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof RequirementWrapper rw) {
					boolean resolved = rw.resolved | rw.java;
					return !resolved;
				}
				return true;
			}
		};

		hideOptionalRequirements = new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				Requirement requirement = Adapters.adapt(element, Requirement.class);
				if (requirement != null) {
					String resolution = requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
					return !Constants.RESOLUTION_OPTIONAL.equals(resolution);
				}
				return true;
			}
		};

		reqsViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY,
				new Transfer[] { LocalSelectionTransfer.getTransfer() }, new LocalTransferDragListener(reqsViewer));

		capsViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY,
				new Transfer[] { LocalSelectionTransfer.getTransfer() }, new LocalTransferDragListener(capsViewer));

		reqsViewer.addOpenListener(this::openEditor);

		fillActionBars();

		getSite().getPage().addPostSelectionListener(this);
		getSite().getPage().addPartListener(partAdapter);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

		// Current selection & part
		IWorkbenchPart activePart = getSite().getPage().getActivePart();
		ISelection activeSelection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		selectionChanged(activePart, activeSelection);
	}

	private void openEditor(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object item = iter.next();
			if (item instanceof Clazz clazz) {
				String className = clazz.getFQN();
				IType type = null;
				if (!loaders.isEmpty()) {
					IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
					for (CapReqLoader loader : loaders) {
						if (loader instanceof BndBuilderCapReqLoader) {
							File loaderFile = ((BndBuilderCapReqLoader) loader).getFile();
							IFile[] wsfiles = wsroot.findFilesForLocationURI(loaderFile.toURI());
							for (IFile wsfile : wsfiles) {
								IJavaProject javaProject = JavaCore.create(wsfile.getProject());
								try {
									type = javaProject.findType(className);
									if (type != null) {
										break;
									}
								} catch (JavaModelException e1) {
									ErrorDialog.openError(getSite().getShell(), "Error", "",
											new Status(IStatus.ERROR, PLUGIN_ID, 0,
													MessageFormat.format("Error opening Java class '{0}'.", className),
													e1));
								}
							}
						}

					}
				}
				try {
					if (type != null) {
						JavaUI.openInEditor(type, true, true);
					}
				} catch (PartInitException e2) {
					ErrorDialog.openError(getSite().getShell(), "Error", "", new Status(IStatus.ERROR, PLUGIN_ID, 0,
							MessageFormat.format("Error opening Java editor for class '{0}'.", className), e2));
				} catch (JavaModelException e3) {
					ErrorDialog.openError(getSite().getShell(), "Error", "", new Status(IStatus.ERROR, PLUGIN_ID, 0,
							MessageFormat.format("Error opening Java class '{0}'.", className), e3));
				}
			}
		}
	}

	void fillActionBars() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

		// Reqs Buttons
		toolBarManager.add(createToggleHideSelfImportsButton());
		toolBarManager.add(createToggleHideOptionalReqsFilterButton());
		toolBarManager.add(new Separator());

		// Caps Buttons
		toolBarManager.add(createShowProblemCapsAction());
		toolBarManager.add(new Separator());

		// Other Buttons
		toolBarManager.add(createToggleLockInputButton());
		doEEActionMenu(toolBarManager);
		toolBarManager.add(HelpButtons.HELP_BTN_RESOLUTION_VIEW);

	}

	private void doEEActionMenu(IToolBarManager toolBarManager) {
		MenuManager menuManager = new MenuManager("Java", "resolutionview.java.menu");

		Action showMenuAction = new Action("Java") {
			@Override
			public void runWithEvent(Event event) {
				Menu menu = menuManager.createContextMenu(getViewSite().getShell());
				MenuItem[] items = menu.getItems();
				if (items != null && items.length == ees.size()) {
					menu.setDefaultItem(items[currentEE]);
				}
				Point location = getViewSite().getShell().getDisplay().getCursorLocation();
				menu.setLocation(location.x, location.y);
				menu.setVisible(true);
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return Resources.getImageDescriptor("jcu_obj");
			}
		};
		for (int n = 0; n < ees.size(); n++) {
			int nn = n;
			EE ee = ees.get(n);
			if (ee.getRelease() == 9) {
				currentEE = n;
			}
			String name = getEEName(ee);
			Action action = new Action(name) {
				int index = nn;

				@Override
				public void run() {
					setEE(index);
					showMenuAction.setToolTipText(getEEName(ees.get(currentEE)));
				}
			};
			menuManager.add(action);
		}
		showMenuAction.setToolTipText(getEEName(ees.get(currentEE)));

		toolBarManager.add(showMenuAction);
	}

	private String getEEName(EE ee) {
		return ee == EE.UNKNOWN ? "unknown" : ee.getEEName();
	}

	protected void setEE(int ee) {
		currentEE = ee;
		executeAnalysis();
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getSite().getPage().removePartListener(partAdapter);
		setLoaders(Collections.<CapReqLoader>emptySet());
		duplicateCapabilitiesWithDifferentHashes.clear();
		super.dispose();
	}

	private void setInput(Set<CapReqLoader> sourceLoaders, Map<String, Collection<Capability>> capabilities,
			Map<String, Collection<RequirementWrapper>> requirements) {
		setLoaders(sourceLoaders);
		sourceLoaders = loaders;
		if (reqsTree != null && !reqsTree.isDisposed() && capsTable != null && !capsTable.isDisposed()) {
			reqsViewer.setInput(requirements);
			capsViewer.setInput(capabilities);

			String label;
			if (!sourceLoaders.isEmpty()) {
				StringBuilder builder = new StringBuilder();
				String delim = "";
				boolean shortLabel = sourceLoaders.size() > 1;
				for (CapReqLoader l : sourceLoaders) {
					builder.append(delim);
					builder.append(shortLabel ? l.getShortLabel() : l.getLongLabel());
					delim = ", ";
				}
				label = builder.toString();
			} else {
				label = "<no input>";
			}
			setContentDescription(label);

			updateReqsLabel();

			List<Capability> caps = capabilities.values().stream().flatMap(Collection::stream).toList();

			duplicateCapabilitiesWithDifferentHashes = new HashSet<Capability>(
					ResourceUtils.detectDuplicateCapabilitiesWithDifferentHashes("osgi.wiring.package", caps));

			updateCapsLabel();

		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection == null || !(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return;
		}
		setLoaders(getLoadersFromSelection((IStructuredSelection) selection));
	}

	private void updateReqsLabel() {
		reqsLabel.setText("Requirements: " + reqsViewer.getTree().getItemCount());
		reqsLabel.getParent().layout();
	}

	private void updateCapsLabel() {
		String problemAddition = "";
		if (!duplicateCapabilitiesWithDifferentHashes.isEmpty()) {

			int problemCount = 0;
			TableItem[] items = capsViewer.getTable().getItems();
			for (int i = 0; i < items.length; i++) {
				TableItem tableItem = items[i];
				Object data = tableItem.getData();
				if (data instanceof Capability cap) {
					if (duplicateCapabilitiesWithDifferentHashes.contains(cap)) {
						problemCount++;
					}
				}
			}

			if (problemCount > 0) {
				problemAddition = " Problems: " + problemCount;
			}
		}
		capsLabel.setText("Capabilities: " + capsViewer.getTable().getItemCount() + problemAddition);

		capsLabel.getParent().layout();
	}

	private Set<CapReqLoader> getLoadersFromSelection(IStructuredSelection structSel) {
		Set<CapReqLoader> result = new LinkedHashSet<>();
		Iterator<?> iter = structSel.iterator();
		while (iter.hasNext()) {

			Object element = iter.next();
			if (element instanceof Repository repo) {
				ResourceUtils.getAllResources(repo).stream().filter(r -> {
					try {
						return ResourceUtils.getContentCapabilities(r) != null;
					} catch (Exception e) {
						return false;
					}
				}).map(ResourceCapReqLoader::new).forEach(result::add);
			} else {
				CapReqLoader loader = findLoaderForElement(element);
				if (loader != null) {
					result.add(loader);
				}
			}
		}

		return result;
	}

	protected CapReqLoader findLoaderForElement(Object element) {
		CapReqLoader loader = Adapters.adapt(element, CapReqLoader.class);
		if (loader != null) {
			return loader;
		}
		ResourceProvider resourceProvider = Adapters.adapt(element, ResourceProvider.class);
		if (resourceProvider != null) {
			return new ResourceCapReqLoader(resourceProvider.getResource());
		}
		Resource resource = Adapters.adapt(element, Resource.class);
		if (resource != null) {
			return new ResourceCapReqLoader(resource);
		}
		File file = Adapters.adapt(element, File.class);
		if (file != null) {
			return getLoaderForFile(file);
		}
		IResource eclipseResource = Adapters.adapt(element, IResource.class);
		if (eclipseResource != null) {
			IPath location = eclipseResource.getLocation();
			if (location != null) {
				return getLoaderForFile(location.toFile());
			}
		}
		return null;
	}

	void executeAnalysis() {
		if (inputLocked) {
			return;
		}

		outOfDate = false;
		synchronized (this) {
			Job oldJob = analysisJob;
			if (oldJob != null && oldJob.getState() != Job.NONE) {
				oldJob.cancel();
			}

			if (!loaders.isEmpty()) {
				final AnalyseBundleResolutionJob job = new AnalyseBundleResolutionJob("importExportAnalysis", loaders,
						ees.get(currentEE), msg -> {
							if (display != null && !display.isDisposed()) {
								display.execute(() -> setContentDescription(msg));
							}
						});
				job.setSystem(true);

				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void aboutToRun(IJobChangeEvent event) {
					}

					@Override
					public void done(IJobChangeEvent event) {
						IStatus result = job.getResult();
						if (result != null && result.isOK()) {
							if (display != null && !display.isDisposed()) {
								display.asyncExec(
										() -> setInput(loaders, job.getCapabilities(), job.getRequirements()));
							}
						}
					}
				});

				analysisJob = job;
				analysisJob.schedule(500);
			} else {
				analysisJob = null;
			}
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (!loaders.isEmpty()) {
			IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
			for (CapReqLoader loader : loaders) {
				if (loader instanceof BndBuilderCapReqLoader) {
					File file = ((BndBuilderCapReqLoader) loader).getFile();
					IFile[] wsfiles = wsroot.findFilesForLocationURI(file.toURI());
					for (IFile wsfile : wsfiles) {
						if (event.getDelta().findMember(wsfile.getFullPath()) != null) {
							executeAnalysis();
							break;
						}
					}
				}
			}
		}
	}

	static class LocalTransferDragListener implements DragSourceListener {

		private final Viewer viewer;

		public LocalTransferDragListener(Viewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void dragStart(DragSourceEvent event) {
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
			if (transfer.isSupportedType(event.dataType)) {
				transfer.setSelection(viewer.getSelection());
			}
		}

		@Override
		public void dragFinished(DragSourceEvent event) {
		}
	}

	private void handleReqsViewerDoubleClickEvent(DoubleClickEvent event) {
		if (!event.getSelection().isEmpty()) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			final Object element = selection.getFirstElement();

			if (element instanceof RequirementWrapper rw) {

				// Open AdvanvedSearch of RepositoriesView
				Requirement req = rw.requirement;
				eventBroker.post(ViewEventTopics.REPOSITORIESVIEW_OPEN_ADVANCED_SEARCH.topic(), req);
			}

		}
	}

	private void handleCapsViewerDoubleClickEvent(DoubleClickEvent event) {
		if (!event.getSelection().isEmpty()) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			final Object element = selection.getFirstElement();

			if (element instanceof Capability cap) {

				// convert the capability to a requirement (without
				// bundle-attributes and bnd.hashes) for better results in the
				// advanced search e.g.
				// (&(osgi.wiring.package=my.package.foo)(version>=1.7.23))
				Requirement req = CapReqBuilder.createRequirementFromCapability(cap, (name) -> {
					if (name.equals("bundle-symbolic-name") || name.equals("bundle-version")
							|| name.equals("bnd.hashes")) {
						return false;
					}

					return true;
				}).buildSyntheticRequirement();
				// Open AdvanvedSearch of RepositoriesView
				eventBroker.post(ViewEventTopics.REPOSITORIESVIEW_OPEN_ADVANCED_SEARCH.topic(), req);
			}

		}
	}

	/**
	 * Generic copy to clipboard handling via Ctrl+C or MacOS: Cmd+C
	 *
	 * @param viewer                    the viewer
	 * @param clipboardContentExtractor handler to extract content from the selected
	 *                                  items.
	 * @return a KeyAdapter copying content of the selected items to clipboard
	 */
	private KeyAdapter createCopyToClipboardAdapter(StructuredViewer viewer,
			BiConsumer<IStructuredSelection, StringBuilder> clipboardContentExtractor) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				// Check if Ctrl+C or MacOS: Cmd+C was pressed
				if ((e.stateMask & SWT.MOD1) == SWT.MOD1 && e.keyCode == 'c') {
					IStructuredSelection selection = viewer.getStructuredSelection();
					StringBuilder clipboardString = new StringBuilder();

					clipboardContentExtractor.accept(selection, clipboardString);

					if (clipboardString.length() > 0) {
						Clipboard clipboard = new Clipboard(Display.getCurrent());
						TextTransfer textTransfer = TextTransfer.getInstance();
						clipboard.setContents(new Object[] { clipboardString.toString() },
								new Transfer[] { textTransfer });
						clipboard.dispose();
					}
				}
			}

		};
	}

	private void reqsCopyToClipboard(IStructuredSelection selection, RequirementWrapperLabelProvider lp,
			StringBuilder clipboardContent) {

		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object element = iterator.next();

			if (element instanceof RequirementWrapper reqWrapper) {

				clipboardContent.append(lp.getToolTipText(reqWrapper));

				if (iterator.hasNext()) {
					clipboardContent.append(System.lineSeparator());
				}

			} else {
				clipboardContent.append(element.toString());

				if (iterator.hasNext()) {
					clipboardContent.append(System.lineSeparator());
				}

			}
		}
	}

	private void capsCopyToClipboard(IStructuredSelection selection, CapabilityLabelProvider lp,
			StringBuilder clipboardContent) {

		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object element = iterator.next();

			if (element instanceof Capability cap) {

				clipboardContent.append(lp.getToolTipText(cap));

				if (iterator.hasNext()) {
					clipboardContent.append(System.lineSeparator());
				}

			} else {
				clipboardContent.append(element.toString());
			}

			if (iterator.hasNext()) {
				clipboardContent.append(System.lineSeparator());
			}
		}
	}

	private void updateReqsFilter(String filterString) {
		reqsContentProvider.setFilter(filterString);
		reqsViewer.refresh();
		updateReqsLabel();
		if (filterString != null) {
			reqsViewer.expandToLevel(1);
		}
	}

	private void updateCapsFilter(String filterString) {
		capsContentProvider.setFilter(filterString);
		capsViewer.refresh();
		updateCapsLabel();
	}

	private IAction createShowProblemCapsAction() {
		String tooltipTextUnlocked = "Click to detect capabilities containing packages that have the same name but differ in the contained classes.";

		IAction toggleShowProblemCaps = new Action("showProblemCaps", IAction.AS_CHECK_BOX) {
			@Override
			public void runWithEvent(Event event) {
				if (isChecked()) {
					capsViewer.addFilter(filterShowCapsProblems);
					this.setToolTipText(
							"Showing capabilities containing packages that have the same name but differ in the contained classes.");
				} else {
					capsViewer.removeFilter(filterShowCapsProblems);
					this.setToolTipText(tooltipTextUnlocked);
				}

				updateCapsLabel();
			}
		};
		toggleShowProblemCaps.setChecked(false);
		toggleShowProblemCaps.setImageDescriptor(Resources.getImageDescriptor("/icons/warning_obj.svg"));
		toggleShowProblemCaps.setToolTipText(tooltipTextUnlocked);
		return toggleShowProblemCaps;
	}

	private IAction createToggleLockInputButton() {
		String toolTipTextUnchecked = "Lock to current selection";

		IAction toggleLockInput = new Action("lockInput", IAction.AS_CHECK_BOX) {
			@Override
			public void runWithEvent(Event event) {
				inputLocked = isChecked();
				if (!inputLocked) {
					this.setToolTipText(toolTipTextUnchecked);
					executeAnalysis();
				} else {
					this.setToolTipText("Current selection is locked");
				}
			}
		};
		toggleLockInput.setChecked(false);
		toggleLockInput.setImageDescriptor(Resources.getImageDescriptor("lockedstate.svg"));
		toggleLockInput.setToolTipText(toolTipTextUnchecked);
		return toggleLockInput;
	}

	private IAction createToggleHideSelfImportsButton() {
		String toolTipTextShowAll = "Showing all requirements.";
		String toolTipTextHideSelfImports = "Hiding resolved (including self-imported) requirements.\n\n"
				+ "Requirements that are resolved (exported and imported) within the set of selected bundles are hidden. Click to show all requirements.";

		IAction toggleShowSelfImports = new Action("showSelfImports", IAction.AS_CHECK_BOX) {
			@Override
			public void runWithEvent(Event event) {
				if (isChecked()) {
					reqsViewer.addFilter(hideSelfImportsFilter);
					this.setToolTipText(toolTipTextHideSelfImports);
				} else {
					reqsViewer.removeFilter(hideSelfImportsFilter);
					this.setToolTipText(toolTipTextShowAll);
				}
				updateReqsLabel();
			}
		};
		toggleShowSelfImports.setChecked(false);
		toggleShowSelfImports.setImageDescriptor(Resources.getImageDescriptor("/icons/bundle-importer-exporter.svg"));
		toggleShowSelfImports.setToolTipText(toolTipTextShowAll);
		return toggleShowSelfImports;
	}

	private IAction createToggleHideOptionalReqsFilterButton() {
		String toggleShowShowUnresolvedReqsFilterUnchecked = "Optional requirements are included. Click to hide optional requirements.";

		final IAction toggleShowShowUnresolvedReqsFilter = new Action("hideOptionalReqs", IAction.AS_CHECK_BOX) {
			@Override
			public void runWithEvent(Event event) {
				if (isChecked()) {
					reqsViewer.addFilter(hideOptionalRequirements);
					this.setToolTipText("Optional requirements are now hidden");
				} else {
					reqsViewer.removeFilter(hideOptionalRequirements);
					this.setToolTipText(toggleShowShowUnresolvedReqsFilterUnchecked);
				}
				updateReqsLabel();
			}
		};
		toggleShowShowUnresolvedReqsFilter.setChecked(false);
		toggleShowShowUnresolvedReqsFilter
				.setImageDescriptor(Resources.getImageDescriptor("/icons/excludeMode_filter.svg"));
		toggleShowShowUnresolvedReqsFilter.setToolTipText(toggleShowShowUnresolvedReqsFilterUnchecked);
		return toggleShowShowUnresolvedReqsFilter;
	}
}
