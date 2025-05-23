/*******************************************************************************
 * Copyright (c) 2010, 2024 bndtools project and others.
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
 *     David Savage <davemssavage@gmail.com>  - ongoing enhancements
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aqute.biz> - ongoing enhancements
 *     gcollins <gareth.o.collins@gmail.com>
 *     Carter Smithhart <carter.smithhart@gmail.com>
 *     Gregory Amerson <gregory.amerson@liferay.com>
 *     Marc Schlegel <marc.schlegel@gmx.de
 *     Sean Bright <sean@malleable.com>
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Luke Winkenbach <luke.winkenbach@schneider-electric.com>
 *     wodencafe <wodencafe@gmail.com>
 *     Juergen Albert <j.albert@data-in-motion.biz>
 *     Raymond Augé <raymond.auge@liferay.com>
 *     Christoph Rueger <chrisrueger@gmail.com>
 *     Christoph Läubrich - Adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.views.repository;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.bnd.ui.Central;
import org.eclipse.pde.bnd.ui.FilterPanelPart;
import org.eclipse.pde.bnd.ui.HelpButtons;
import org.eclipse.pde.bnd.ui.HierarchicalLabel;
import org.eclipse.pde.bnd.ui.HierarchicalMenu;
import org.eclipse.pde.bnd.ui.RepositoryUtils;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.pde.bnd.ui.SWTUtil;
import org.eclipse.pde.bnd.ui.SelectionDragAdapter;
import org.eclipse.pde.bnd.ui.Workspaces;
import org.eclipse.pde.bnd.ui.dnd.GAVIPageListener;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundle;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundleVersion;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryEntry;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryTreeLabelProvider;
import org.eclipse.pde.bnd.ui.model.repo.SearchableRepositoryTreeContentProvider;
import org.eclipse.pde.bnd.ui.plugins.RepositoriesViewRefresher;
import org.eclipse.pde.bnd.ui.preferences.BndPreferences;
import org.eclipse.pde.bnd.ui.preferences.WorkspaceOfflineChangeAdapter;
import org.eclipse.pde.bnd.ui.views.ViewEventTopics;
import org.eclipse.pde.bnd.ui.wizards.AddFilesToRepositoryWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.osgi.resource.Requirement;
import org.osgi.service.event.Event;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.resource.FilterParser.PackageExpression;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.clipboard.Clipboard;

public class RepositoriesView extends ViewPart implements RepositoriesViewRefresher.RefreshModel {

	private static final String						DROP_TARGET					= "dropTarget";

	private final FilterPanelPart filterPart = new FilterPanelPart(Resources.getScheduler());
	private SearchableRepositoryTreeContentProvider	contentProvider;
	private TreeViewer								viewer;
	private Control									filterPanel;
	private GAVIPageListener						dndgaviPageListener;

	private Action									collapseAllAction;
	private Action									refreshAction;
	private Action									addBundlesAction;
	private Action									advancedSearchAction;
	private Action									downloadAction;
	private String									advancedSearchState;
	private Action									offlineAction;
	private final IEventBroker						eventBroker					= PlatformUI.getWorkbench()
		.getService(IEventBroker.class);


	private final WorkspaceOfflineChangeAdapter workspaceOfflineListener = new WorkspaceOfflineChangeAdapter() {
		@Override
		public void workspaceOfflineChanged(boolean offline) {
			updateOfflineAction(offline);
			if (!offline) {
				// Fire a fake selection event so that repo plugins can do what they would do if
				// they were already online
				viewer.setSelection(viewer.getSelection(), false);
			}
		}
	};

	private BndPreferences preferences;

	private Workspace workspace;

	private final IObservableValue<String> workspaceName = new WritableValue<>();
	private final IObservableValue<String> workspaceDescription = new WritableValue<>();

	private Object[]                               lastExpandedElements;
    private TreePath[]                              lastExpandedPaths;

	@Override
	public void createPartControl(final Composite parent) {
		// CREATE CONTROLS

		final StackLayout stackLayout = new StackLayout();
		parent.setLayout(stackLayout);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Composite defaultParent = toolkit.createComposite(parent, SWT.NONE);
		FillLayout fill = new FillLayout();
		fill.marginHeight = 5;
		fill.marginWidth = 5;
		defaultParent.setLayout(fill);
		stackLayout.topControl = defaultParent;
		parent.layout();

		final Composite mainPanel = new Composite(parent, SWT.NONE);
		filterPanel = filterPart.createControl(mainPanel, 5, 5);
		Tree tree = new Tree(mainPanel, SWT.FULL_SELECTION | SWT.MULTI);
		filterPanel.setBackground(tree.getBackground());

		viewer = new TreeViewer(tree);
		dndgaviPageListener = new GAVIPageListener();

		PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow()
			.getPartService()
			.addPartListener(dndgaviPageListener);

		contentProvider = new SearchableRepositoryTreeContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				super.inputChanged(viewer, oldInput, newInput);

				if (newInput != null) {
					stackLayout.topControl = mainPanel;

					advancedSearchAction.setEnabled(true);
					refreshAction.setEnabled(true);
					collapseAllAction.setEnabled(true);
					setPrefrences(getPreferences());
					parent.layout();
				}
			}
		};
		viewer.setContentProvider(contentProvider);
		ColumnViewerToolTipSupport.enableFor(viewer);

		viewer.setLabelProvider(new RepositoryTreeLabelProvider(false));
		getViewSite().setSelectionProvider(viewer);
		RepositoriesViewRefresher.addViewer(viewer, RepositoriesView.this);

		// LISTENERS
		filterPart.addPropertyChangeListener(event -> {
			String filter = (String) event.getNewValue();
			updatedFilter(filter);
		});
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (target == null) {
					return false;
				}

				if (canDrop(target, transferType)) {
					return true;
				}

				boolean valid = false;
				if (target instanceof RepositoryPlugin) {
					if (((RepositoryPlugin) target).canWrite()) {

						if (URLTransfer.getInstance()
							.isSupportedType(transferType)) {
							return true;
						}

						if (LocalSelectionTransfer.getTransfer()
							.isSupportedType(transferType)) {
							ISelection selection = LocalSelectionTransfer.getTransfer()
								.getSelection();
							if (selection instanceof IStructuredSelection) {
								for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter
									.hasNext();) {
									Object element = iter.next();
									if (element instanceof RepositoryBundle
										|| element instanceof RepositoryBundleVersion) {
										valid = true;
										break;
									}
									if (element instanceof IFile) {
										valid = true;
										break;
									}
									if (element instanceof IAdaptable) {
										IFile file = ((IAdaptable) element).getAdapter(IFile.class);
										if (file != null) {
											valid = true;
											break;
										}
									}
								}
							}
						} else {
							valid = true;
						}
					}
				}
				return valid;
			}

			@Override
			public void dragEnter(DropTargetEvent event) {
				super.dragEnter(event);
				event.detail = DND.DROP_COPY;
			}

			@Override
			public boolean performDrop(Object data) {
				if (RepositoriesView.this.performDrop(getCurrentTarget(), getCurrentEvent().currentDataType, data)) {
					viewer.refresh(getCurrentTarget(), true);
					return true;
				}

				boolean copied = false;
				if (URLTransfer.getInstance()
					.isSupportedType(getCurrentEvent().currentDataType)) {
					try {
						URL url = new URL((String) URLTransfer.getInstance()
							.nativeToJava(getCurrentEvent().currentDataType));

						File tmp = File.createTempFile("dwnl", ".jar");
						try (HttpClient client = new HttpClient()) {
							Files.copy(client.connect(url), tmp.toPath());
						}

						if (isJarFile(tmp)) {
							copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), new File[] {
								tmp
							});
						} else {
							tmp.delete();
							MessageDialog.openWarning(null, "Unrecognized Artifact",
								"The dropped URL is not recognized as a remote JAR file: " + url.toString());
						}
					} catch (Exception e) {
						return false;
					}
				} else if (data instanceof String[] paths) {
					File[] files = new File[paths.length];
					for (int i = 0; i < paths.length; i++) {
						files[i] = new File(paths[i]);
					}
					copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
				} else if (data instanceof IResource[] resources) {
					File[] files = new File[resources.length];
					for (int i = 0; i < resources.length; i++) {
						files[i] = resources[i].getLocation()
							.toFile();
					}
					copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
				} else if (data instanceof IStructuredSelection) {
					File[] files = convertSelectionToFiles((IStructuredSelection) data);
					if (files != null && files.length > 0) {
						copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
					}
				}
				return copied;
			}
		};
		dropAdapter.setFeedbackEnabled(false);
		dropAdapter.setExpandEnabled(false);

		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			URLTransfer.getInstance(), FileTransfer.getInstance(), ResourceTransfer.getInstance(),
			LocalSelectionTransfer.getTransfer()
		}, dropAdapter);
		viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer()
		}, new SelectionDragAdapter(viewer));

		viewer.addSelectionChangedListener(event -> {
			boolean writableRepoSelected = false;
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			Object element = selection.getFirstElement();
			if (element instanceof RepositoryPlugin repo) {
				writableRepoSelected = repo.canWrite();
			}
			addBundlesAction.setEnabled(writableRepoSelected);
		});

		viewer.addDoubleClickListener(event -> {
			if (!event.getSelection()
				.isEmpty()) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				final Object element = selection.getFirstElement();
				if (element instanceof IAdaptable) {
					final URI uri = ((IAdaptable) element).getAdapter(URI.class);
					if (uri == null && element instanceof final RepositoryEntry entry) {
						boolean download = MessageDialog.openQuestion(getSite().getShell(), "Repositories",
							"This repository entry is unable to be opened because it has not been downloaded. Download and open it now?");
						if (download) {
							Job downloadJob = new Job("Downloading repository entry " + entry.getBsn()) {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									final File repoFile = entry.getFile(true);
									if (repoFile != null && repoFile.exists()) {
										getSite().getShell()
											.getDisplay()
											.asyncExec(() -> openURI(repoFile.toURI()));
									}
									return Status.OK_STATUS;
								}
							};
							downloadJob.setUser(true);
							downloadJob.schedule();
						}
					} else if (uri != null) {
						openURI(uri);
					}
				} else if (element instanceof RepositoryPlugin) {
					viewer.setExpandedState(element, !viewer.getExpandedState(element));
				}

			}
		});

		createContextMenu();

		// LAYOUT
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		mainPanel.setLayout(layout);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// Toolbar
		createActions();
		fillToolBar(getViewSite().getActionBars()
			.getToolBarManager());
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

		// Event subscription
		eventBroker.subscribe(ViewEventTopics.REPOSITORIESVIEW_OPEN_ADVANCED_SEARCH.topic(),
			event -> handleOpenAdvancedSearch(event));
		ISelectionService selectionService = getSite().getWorkbenchWindow().getSelectionService();

		selectionService.addSelectionListener(new ISelectionListener() {

			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (getSite().getPart() == part) {
					return;
				}
				updateSelection(selection);
			}
		});
		updateSelection(selectionService.getSelection());
	}

	private void updateSelection(ISelection selection) {
		IProject project = getProject(selection);
		Workspace ws = Workspaces.getWorkspace(project).or(() -> Workspaces.getGlobalWorkspace()).orElse(null);
		if (this.workspace != ws) {
			this.workspace = ws;
			RepositoriesViewRefresher.refreshViewer(viewer, this);
			workspaceName.setValue(Workspaces.getName(ws));
			workspaceDescription.setValue(Workspaces.getDescription(ws));
		}
		BndPreferences pref = Adapters.adapt(project, BndPreferences.class);
		setPrefrences(pref);
	}

	protected IProject getProject(ISelection selection) {
		if (selection instanceof IStructuredSelection structured) {
			Object firstElement = structured.getFirstElement();
			IProject project = Adapters.adapt(firstElement, IProject.class);
			if (project != null) {
				return project;
			}
			IResource resource = Adapters.adapt(firstElement, IResource.class);
			if (resource != null) {
				return resource.getProject();
			}
			return null;
		}
		if (selection instanceof ITextSelection) {
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (editor != null) {
				IEditorInput editorInput = editor.getEditorInput();
				if (editorInput instanceof IFileEditorInput fileInput) {
					return fileInput.getFile().getProject();
				}
			}
		}
		return null;
	}

	private void setPrefrences(BndPreferences preferences) {
		if (Objects.equals(this.preferences, preferences)) {
			return;
		}
		if (this.preferences!=null) {
			this.preferences.removePropertyChangeListener(workspaceOfflineListener);
		}
		this.preferences = preferences;
		if (preferences == null) {
			offlineAction.setChecked(false);
			offlineAction.setToolTipText("Go Offline");
			offlineAction.setImageDescriptor(Resources.getImageDescriptor("connect.png"));
			offlineAction.setEnabled(false);
			return;
		}
		preferences.addPropertyChangeListener(workspaceOfflineListener);
		updateOfflineAction(preferences.isWorkspaceOffline());
	}

	protected void updateOfflineAction(boolean offline) {
		if (offline) {
			offlineAction.setChecked(true);
			offlineAction.setToolTipText("Go Online");
			offlineAction.setImageDescriptor(Resources.getImageDescriptor("disconnect.png"));
		} else {
			offlineAction.setChecked(false);
			offlineAction.setToolTipText("Go Offline");
			offlineAction.setImageDescriptor(Resources.getImageDescriptor("connect.png"));
		}
		offlineAction.setEnabled(true);
	}

	protected void openURI(URI uri) {
		IWorkbenchPage page = getSite().getPage();
		try {
			IFileStore fileStore = EFS.getLocalFileSystem()
				.getStore(uri);
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (PartInitException e) {
			ILog.get().error("Error opening editor for " + uri, e);
		}
	}

	@Override
	public void setFocus() {
		filterPart.setFocus();
	}

	private static File[] convertSelectionToFiles(ISelection selection) {
		if (!(selection instanceof IStructuredSelection structSel)) {
			return new File[0];
		}

		List<File> files = new ArrayList<>(structSel.size());

		for (Iterator<?> iter = structSel.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof IFile) {
				files.add(((IFile) element).getLocation()
					.toFile());
			} else if (element instanceof IAdaptable adaptable) {
				IFile ifile = adaptable.getAdapter(IFile.class);
				if (ifile != null) {
					files.add(ifile.getLocation()
						.toFile());
				} else {
					File file = adaptable.getAdapter(File.class);
					if (file != null) {
						files.add(file);
					}
				}
			}
		}

		return files.toArray(new File[0]);
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow()
			.getPartService()
			.removePartListener(dndgaviPageListener);
		RepositoriesViewRefresher.removeViewer(viewer);
		BndPreferences preferences = getPreferences();
		if (preferences != null) {
			preferences.removePropertyChangeListener(workspaceOfflineListener);
		}
		super.dispose();
	}

	boolean addFilesToRepository(RepositoryPlugin repo, File[] files) {
		AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(getWorkspace(), repo, files);
		WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
		dialog.open();
		viewer.refresh(repo);
		return true;
	}

	private void updatedFilter(String filterString) {
	    viewer.getTree()
        .setRedraw(false);

        try {
            if (filterString == null || filterString.isEmpty()) {
                // Restore previous state when clearing filter
                contentProvider.setFilter(null);
                viewer.refresh(); // Required to clear filter
                restoreExpansionState();
                viewer.refresh();
            } else {
                // Save state before applying new filter
                saveExpansionState();
                contentProvider.setFilter(filterString);
                viewer.refresh();
                viewer.expandToLevel(2);
            }

        } finally {
            viewer.getTree()
                .setRedraw(true);
        }
	}

	void createActions() {
		collapseAllAction = new Action() {
			@Override
			public void run() {
				viewer.collapseAll();
			}
		};
		collapseAllAction.setEnabled(false);
		collapseAllAction.setText("Collapse All");
		collapseAllAction.setToolTipText("Collapse All");
		collapseAllAction.setImageDescriptor(Resources.getImageDescriptor("collapseall.png"));

		refreshAction = new Action() {
			@Override
			public void run() {
				new WorkspaceJob("Refresh repositories") {

					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						if (monitor == null) {
							monitor = new NullProgressMonitor();
						}

						monitor.subTask("Refresh all repositories");

						try {
							refreshAction.setEnabled(false);
							Central.refreshPlugins(getWorkspace());
						} catch (Exception e) {
							Throwable t = Exceptions.unrollCause(e, InvocationTargetException.class);

							ILog.get().error("Unexpected error in refreshing plugns", t);

							return Status.error("Failed to refresh plugins", t);
						} finally {
							refreshAction.setEnabled(true);
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		};
		refreshAction.setEnabled(false);
		refreshAction.setText("Refresh");
		refreshAction.setToolTipText("Refresh Repositories Tree");
		refreshAction.setImageDescriptor(Resources.getImageDescriptor("arrow_refresh.png"));

		addBundlesAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				Object element = selection.getFirstElement();
				if (element != null && element instanceof RepositoryPlugin repo) {
					if (repo.canWrite()) {
						AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(getWorkspace(), repo,
								new File[0]);
						WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
						dialog.open();

						viewer.refresh(repo);
					}
				}
			}
		};
		addBundlesAction.setEnabled(false);
		addBundlesAction.setText("Add");
		addBundlesAction.setToolTipText("Add Bundles to Repository");
		addBundlesAction.setImageDescriptor(Resources.getImageDescriptor("add_obj.png"));

		advancedSearchAction = new Action("Advanced Search", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				if (advancedSearchAction.isChecked()) {
					AdvancedSearchDialog dialog = new AdvancedSearchDialog(getSite().getShell());
					if (advancedSearchState != null) {
						try {
							XMLMemento memento = XMLMemento.createReadRoot(new StringReader(advancedSearchState));
							dialog.restoreState(memento);
						} catch (Exception e) {
							ILog.get().error("Failed to load dialog state", e);
						}
					}

					if (Window.OK == dialog.open()) {
						Requirement req = dialog.getRequirement();
						contentProvider.setRequirementFilter(req);
						SWTUtil.recurseEnable(false, filterPanel);
						viewer.refresh();
						viewer.expandToLevel(2);
					} else {
						advancedSearchAction.setChecked(false);
					}

					try {
						XMLMemento memento = XMLMemento.createWriteRoot("search");
						dialog.saveState(memento);

						CharArrayWriter writer = new CharArrayWriter();
						memento.save(writer);
						advancedSearchState = writer.toString();
					} catch (Exception e) {
						ILog.get().error("Failed to save dialog state", e);
					}
				} else {
					contentProvider.setRequirementFilter(null);
					SWTUtil.recurseEnable(true, filterPanel);
					viewer.refresh();
				}
			}
		};
		advancedSearchAction.setEnabled(false);
		advancedSearchAction.setText("Advanced Search");
		advancedSearchAction.setToolTipText("Toggle Advanced Search");
		advancedSearchAction.setImageDescriptor(Resources.getImageDescriptor("search.png"));

		downloadAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

				// The set of Repos included in the selection; they will be
				// completely downloaded.
				Set<RemoteRepositoryPlugin> repos = Collections.newSetFromMap(new IdentityHashMap<>());
				repos.addAll(selectionByType(selection, RemoteRepositoryPlugin.class));

				// The set of Bundles included in the selection.
				Set<RepositoryBundle> bundles = Collections.newSetFromMap(new IdentityHashMap<>());
				for (RepositoryBundle bundle : selectionByType(selection, RepositoryBundle.class)) {
					// filter out bundles that come from already-selected repos.
					if (!repos.contains(bundle.getRepo())) {
						bundles.add(bundle);
					}
				}

				// The set of Bundle Versions included in the selection
				Set<RepositoryBundleVersion> bundleVersions = Collections.newSetFromMap(new IdentityHashMap<>());
				for (RepositoryBundleVersion bundleVersion : selectionByType(selection,
					RepositoryBundleVersion.class)) {
					// filter out bundles that come from already-selected repos.
					if (!repos.contains(bundleVersion.getRepo())) {
						bundleVersions.add(bundleVersion);
					}
				}

				RepoDownloadJob downloadJob = new RepoDownloadJob(repos, bundles, bundleVersions);
				downloadJob.schedule();
			}

			private <T> List<T> selectionByType(IStructuredSelection selection, Class<T> type) {
				List<T> result = new ArrayList<>(selection.size());
				@SuppressWarnings("unchecked")
				Iterator<Object> iterator = selection.iterator();
				while (iterator.hasNext()) {
					Object item = iterator.next();
					if (type.isInstance(item)) {
						@SuppressWarnings("unchecked")
						T cast = (T) item;
						result.add(cast);
					}
				}
				return result;
			}
		};
		downloadAction.setEnabled(false);
		downloadAction.setText("Download Repository Content");
		downloadAction.setImageDescriptor(Resources.getImageDescriptor("download.png"));

		offlineAction = new Action("Online/Offline Mode", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				BndPreferences preferences = getPreferences();
				if (preferences != null) {
					preferences.setWorkspaceOffline(offlineAction.isChecked());
				}
			}
		};
		offlineAction.setEnabled(false);
		offlineAction.setToolTipText("Go Offline");
		offlineAction.setImageDescriptor(Resources.getImageDescriptor("connect.png"));

		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();

			boolean enable = false;
			@SuppressWarnings("unchecked")
			List<Object> list = selection.toList();
			for (Object item : list) {
				if (item instanceof RemoteRepositoryPlugin) {
					enable = true;
					break;
				} else if (item instanceof RepositoryEntry) {
					if (!((RepositoryEntry) item).isLocal()) {
						enable = true;
						break;
					}
				}
			}
			downloadAction.setEnabled(enable);
		});
	}

	void createContextMenu() {
		MenuManager mgr = new MenuManager();
		Menu menu = mgr.createContextMenu(viewer.getControl());
		viewer.getControl()
			.setMenu(menu);
		mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(mgr, viewer);

		mgr.addMenuListener(manager -> {
			try {
				manager.removeAll();
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (!selection.isEmpty()) {
					final Object firstElement = selection.getFirstElement();
					if (firstElement instanceof final Actionable act) {

						final RepositoryPlugin rp = getRepositoryPlugin(firstElement);

						//
						// Use the Actionable interface to fill the menu
						// Should extend this to allow other menu entries
						// from the view, but currently there are none
						//
						// use HierarchicalMenu to build up a menue with SubMenu
						// entries
						HierarchicalMenu hmenu = new HierarchicalMenu();
						addCopyToClipboardSubMenueEntries(act, rp, hmenu);

						// add the other actions
						Map<String, Runnable> actions = act.actions();

						if (actions != null) {

							for (final Entry<String, Runnable> e1 : actions.entrySet()) {

								String label = e1.getKey();

								hmenu.add(new HierarchicalLabel<Action>(label.replace("&", "&&"), l -> {

									return createAction(l.getLeaf(), l.getDescription(), l.isEnabled(), l.isChecked(),
										rp, e1.getValue());
								}));

							}

						}

						// build the final menue
						hmenu.build(manager);

					}
				}
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			}
		});
	}

	private void fillToolBar(IToolBarManager toolBar) {
		toolBar.add(new ControlContribution("label") {

			@Override
			protected Control createControl(Composite parent) {
				final Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new FillLayout());
				// TODO maybe make this a combo so the user can choose other namespaces eg:
				// - active selection
				// - any of the ones registered at OSGi
				CLabel label = new CLabel(composite, SWT.CENTER);

				IChangeListener labelListener = e -> {
					String text = label.getText();
					String newText = getLabelText();
					if (Objects.equals(text, newText)) {
						return;
					}
					label.setText(newText);
					label.setToolTipText(workspaceDescription.getValue());
					toolBar.update(true);
				};
				IChangeListener tooltipListener = e -> {
					String text = label.getToolTipText();
					String newText = workspaceDescription.getValue();
					if (Objects.equals(text, newText)) {
						return;
					}
					label.setToolTipText(newText);
				};
				label.addDisposeListener(e -> {
					workspaceDescription.removeChangeListener(tooltipListener);
					workspaceName.removeChangeListener(labelListener);
				});
				workspaceName.addChangeListener(labelListener);
				workspaceDescription.addChangeListener(tooltipListener);
				label.setText(getLabelText());
				label.setToolTipText(workspaceDescription.getValue());
				return composite;
			}

			private String getLabelText() {
				return Objects.requireNonNullElse(workspaceName.getValue(), "");
			}
		});
		toolBar.add(advancedSearchAction);
		toolBar.add(downloadAction);
		toolBar.add(new Separator());
		toolBar.add(refreshAction);
		toolBar.add(collapseAllAction);
		toolBar.add(addBundlesAction);
		toolBar.add(new Separator());
		toolBar.add(offlineAction);
		toolBar.add(new Separator());
		toolBar.add(HelpButtons.HELP_BTN_REPOSITORIES);
		toolBar.add(new Separator());
	}

	/**
	 * Handle the drop on targets that understand drops.
	 *
	 * @param target The current target
	 * @param data The transfer data
	 * @return true if the data is acceptable, otherwise false
	 */
	boolean canDrop(Object target, TransferData data) {
		try {
			Class<?> type = toJavaType(data);
			if (type != null) {
				target.getClass()
					.getMethod(DROP_TARGET, type);
				return true;
			}
		} catch (Exception e) {
			// Ignore
		}
		return false;
	}

	/**
	 * Try a drop on the target. A drop is allowed if the target implements a
	 * {@code dropTarget} method that returns a boolean.
	 *
	 * @param target the target being dropped upon
	 * @param data the data
	 * @param dropped
	 * @return true if dropped and processed, false if not
	 */
	boolean performDrop(Object target, TransferData data, Object dropped) {
		try {
			Object java = toJava(data);
			if (java == null) {
				java = toJava(dropped);
				if (java == null) {
					return false;
				}
			}

			try {
				Method m = target.getClass()
					.getMethod(DROP_TARGET, java.getClass());
				Boolean invoke = (Boolean) m.invoke(target, java);
				if (!invoke) {
					return false;
				}
			} catch (NoSuchMethodException e) {
				return false;
			}

			RepositoryPlugin repositoryPlugin = getRepositoryPlugin(target);
			if (repositoryPlugin != null && repositoryPlugin instanceof Refreshable) {
				Central.refreshPlugin(getWorkspace(), (Refreshable) repositoryPlugin);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private Object toJava(Object dropped) {
		if (dropped instanceof IStructuredSelection selection) {
			if (!selection.isEmpty()) {
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof IResource resource) {
					IPath path = resource.getRawLocation();
					if (path != null) {
						File file = path.toFile();
						if (file != null) {
							return file;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return the class of the dropped object
	 *
	 * <pre>
	 *    URLTransfer             URI
	 *    FileTransfer            File[]
	 *    TextTransfer            String
	 *    ImageTransfer           ImageData
	 * </pre>
	 *
	 * @param data the dropped object
	 * @return the class of the dropped object, or null when it's unknown
	 * @throws Exception upon error
	 */
	Class<?> toJavaType(TransferData data) throws Exception {
		if (URLTransfer.getInstance()
			.isSupportedType(data)) {
			return URI.class;
		}
		if (FileTransfer.getInstance()
			.isSupportedType(data)) {
			return File[].class;
		}

		if (TextTransfer.getInstance()
			.isSupportedType(data)) {
			return String.class;
		}

		if (ResourceTransfer.getInstance()
			.isSupportedType(data)) {
			return String.class;
		}

		if (LocalSelectionTransfer.getTransfer()
			.isSupportedType(data)) {
			ISelection selection = LocalSelectionTransfer.getTransfer()
				.getSelection();
			if (selection instanceof IStructuredSelection) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				if (firstElement instanceof IFile) {
					return File.class;
				}
			}
			return null;
		}

		// if (ImageTransfer.getInstance().isSupportedType(data))
		// return Image.class;
		return null;
	}

	/**
	 * Return a native data type that represents the dropped object
	 *
	 * <pre>
	 *    URLTransfer             URI
	 *    FileTransfer            File[]
	 *    TextTransfer            String
	 *    ImageTransfer           ImageData
	 * </pre>
	 *
	 * @param data the dropped object
	 * @return a native data type that represents the dropped object, or null
	 *         when the data type is unknown
	 * @throws Exception upon error
	 */
	Object toJava(TransferData data) throws Exception {
		LocalSelectionTransfer local = LocalSelectionTransfer.getTransfer();
		if (local.isSupportedType(data)) {
			ISelection selection = LocalSelectionTransfer.getTransfer()
				.getSelection();
			if (selection instanceof IStructuredSelection) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				if (firstElement instanceof IFile f) {
					return f.getLocationURI();
				}
			}
		}
		if (URLTransfer.getInstance()
			.isSupportedType(data)) {
			Object nativeUrl = URLTransfer.getInstance().nativeToJava(data);
			if (nativeUrl instanceof String s) {
				return new URI(s);
			}
		} else if (FileTransfer.getInstance()
			.isSupportedType(data)) {
			Object nativeFiles = FileTransfer.getInstance()
					.nativeToJava(data);
			if (nativeFiles instanceof String[] str) {
				return Arrays.stream(str).map(File::new).toArray(File[]::new);
			}
		} else if (TextTransfer.getInstance()
			.isSupportedType(data)) {
			return TextTransfer.getInstance()
				.nativeToJava(data);
		}
		// Need to write the transfer code since the ImageTransfer turns it into
		// something very Eclipsy
		// else if (ImageTransfer.getInstance().isSupportedType(data))
		// return ImageTransfer.getInstance().nativeToJava(data);

		return null;
	}

	private RepositoryPlugin getRepositoryPlugin(Object element) {
		if (element instanceof RepositoryPlugin) {
			return (RepositoryPlugin) element;
		} else if (element instanceof RepositoryBundle) {
			return ((RepositoryBundle) element).getRepo();
		} else if (element instanceof RepositoryBundleVersion) {
			return ((RepositoryBundleVersion) element).getParentBundle()
				.getRepo();
		}

		return null;
	}

	@Override
	public List<RepositoryPlugin> getRepositories() {
		return RepositoryUtils.listRepositories(getWorkspace(), true);
	}

	private static boolean isJarFile(File candidate) {
		try (JarFile jar = new JarFile(candidate)) {
			return jar.getManifest() != null;
		} catch (IOException ex) {
			return false;
		}
	}

	private Action createAction(String label, String description, boolean enabled, boolean checked, RepositoryPlugin rp,
		Runnable r) {

		Action a = new Action(label) {
			@Override
			public void run() {
				Job backgroundJob = new Job("Repository Action '" + getText() + "'") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							r.run();
							if (rp != null && rp instanceof Refreshable) {
								Central.refreshPlugin(getWorkspace(), (Refreshable) rp, true);
							}
						} catch (final Exception e) {
							ILog.get().error("Error executing: " + getName(), e);
						}
						monitor.done();
						return Status.OK_STATUS;
					}
				};

				backgroundJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (event.getResult()
							.isOK()) {
							viewer.getTree()
								.getDisplay()
								.asyncExec(() -> viewer.refresh());
						}
					}
				});

				backgroundJob.setUser(true);
				backgroundJob.setPriority(Job.SHORT);
				backgroundJob.schedule();
			}
		};
		a.setEnabled(enabled);
		if (description != null) {
			a.setDescription(description);
		}
		a.setChecked(checked);

		return a;
	}

	private void addCopyToClipboardSubMenueEntries(Actionable act, final RepositoryPlugin rp, HierarchicalMenu hmenu) {

		final Registry registry = getWorkspace();
		if (registry == null) {
			return;
		}

		final Clipboard clipboard = registry.getPlugin(Clipboard.class);

		if (clipboard == null) {
			return;
		}

		if (act instanceof RepositoryBundleVersion rbr) {
			hmenu.add(createContextMenueBsn(rp, clipboard, rbr));
			hmenu.add(createContextMenueCopyInfoRepoBundleVersion(act, rp, clipboard, rbr));
		}

		if (act instanceof RepositoryBundle rb) {
			hmenu.add(createContextMenueCopyInfoRepoBundle(act, rp, clipboard, rb));
		}

		if ((act instanceof Repository) || (act instanceof RepositoryPlugin)) {
			hmenu.add(createContextMenueCopyInfoRepo(act, rp, clipboard));
			hmenu.add(createContextMenueCopyBundlesWithSelfImports(act, rp, clipboard));
		}

	}


	private HierarchicalLabel<Action> createContextMenueCopyBundlesWithSelfImports(Actionable act, final RepositoryPlugin rp,
        final Clipboard clipboard) {
        return new HierarchicalLabel<Action>("Copy to clipboard :: Bundles with substitution packages (self-imports)",
            (label) -> createAction(label.getLeaf(),
                "Add list of bundles containing packages which are imported and exported in their Manifest.", true,
                false, rp, () -> {

                	final StringBuilder sb = new StringBuilder(
    						"Shows list of bundles in the repository '" + rp.getName()
    							+ "' containing substitution packages / self-imports (i.e. same package imported and exported) in their Manifest. \n"
    							+ "Note: a missing version range can cause wiring / resolution problems.\n"
    							+ "See https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#i3238802 "
    							+ "and https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#framework.module-import.export.same.package "
    							+ "for more information."
    							+ "\n\n");

                    for (RepositoryBundleVersion rpv : contentProvider.allRepoBundleVersions(rp)) {
                        org.osgi.resource.Resource r = rpv.getResource();
                        Collection<PackageExpression> selfImports = ResourceUtils
                            .getSubstitutionPackages(r);

                        if (!selfImports.isEmpty()) {
                            long numWithoutRange = selfImports.stream()
                                .filter(pckExp -> pckExp.getRangeExpression() == null)
                                .count();

                            // Main package information
                            sb.append(r.toString())
                                .append("\n");
                            sb.append("    Substitution packages: ")
                                .append(selfImports.size());

                            // Additional information about packages without
                            // version range
                            if (numWithoutRange > 0) {
                                sb.append("    (")
                                    .append(numWithoutRange)
                                    .append(" without version range)");
                            }
                            sb.append("\n");

                            // List of substitution packages
                            sb.append("    [\n");
                            for (PackageExpression pckExp : selfImports) {
                                sb.append("        ")
                                    .append(pckExp.toString())
                                    .append(",\n");
                            }
                            // Remove the last comma and newline
                            if (!selfImports.isEmpty()) {
                                sb.setLength(sb.length() - 2);
                            }
                            sb.append("\n    ]\n\n");
                        }

                    }

                    if (sb.isEmpty()) {
                        clipboard.copy("-Empty-");
                    } else {
                        clipboard.copy(sb.toString());
                    }

            }));
    }

	private HierarchicalLabel<Action> createContextMenueCopyInfoRepo(Actionable act, final RepositoryPlugin rp,
		final Clipboard clipboard) {
		return new HierarchicalLabel<Action>("Copy to clipboard :: Copy info", (label) -> createAction(label.getLeaf(),
			"Add general info about this entry to clipboard.", true, false, rp, () -> {

				final StringBuilder info = new StringBuilder();

				// append the tooltip content
				try {

					String tooltipContent = act.tooltip();

					if (tooltipContent != null && !tooltipContent.isBlank()) {
						info.append(tooltipContent);
						clipboard.copy(info.toString());
					}
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}

			}));
	}

	private HierarchicalLabel<Action> createContextMenueCopyInfoRepoBundle(Actionable act, final RepositoryPlugin rp,
		final Clipboard clipboard, RepositoryBundle rb) {
		return new HierarchicalLabel<Action>("Copy to clipboard :: Copy info", (label) -> createAction(label.getLeaf(),
			"Add general info about this entry to clipboard.", true, false, rp, () -> {

				final StringBuilder info = new StringBuilder();

				// append the tooltip content
				try {

					String tooltipContent = act.tooltip(rb.getBsn());

					if (tooltipContent != null && !tooltipContent.isBlank()) {
						info.append(tooltipContent);
						clipboard.copy(info.toString());
					}
			else {
						// bundle does not seem to have a tooltip
						// let's just add general bundle info
						info.append(rb.toString());
						clipboard.copy(info.toString());
					}

				} catch (Exception e) {
					throw Exceptions.duck(e);
				}

			}));
	}

	private HierarchicalLabel<Action> createContextMenueCopyInfoRepoBundleVersion(Actionable act,
		final RepositoryPlugin rp,
		final Clipboard clipboard, RepositoryBundleVersion rbr) {

		return new HierarchicalLabel<Action>("Copy to clipboard :: Copy info", (label) -> createAction(label.getLeaf(),
			"Add general info about this entry to clipboard.", true, false, rp, () -> {

				final StringBuilder info = new StringBuilder();

				// append the tooltip content +
				// RepositoryBundleVersion.toString() general info
				try {
					String tooltipContent = act.tooltip(rbr.getBsn(), rbr.getVersion()
						.toString());

					if (tooltipContent != null && !tooltipContent.isBlank()) {
						info.append(tooltipContent);
					}
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}

				if (!info.isEmpty()) {
					info.append('\n');
				}

				info.append(rbr.toString());

				clipboard.copy(info.toString());

			}));
	}

	private HierarchicalLabel<Action> createContextMenueBsn(final RepositoryPlugin rp, final Clipboard clipboard,
		RepositoryBundleVersion rbr) {

		return new HierarchicalLabel<Action>("Copy to clipboard :: Copy bsn+version",
			(label) -> createAction(label.getLeaf(), "Copy bsn;version=version to clipboard.", true, false, rp, () -> {

				String rev = rbr.getBsn() + ";version=" + rbr.getVersion()
					.toString();
				clipboard.copy(rev);

			}));
	}

	private void handleOpenAdvancedSearch(Event event) {

		if (event == null) {
			return;
		}

		// Handle the event, open the dialog
		if (event.getProperty(IEventBroker.DATA) instanceof Requirement req) {

			// fill and open advanced search
			advancedSearchState = AdvancedSearchDialog.toNamespaceSearchPanelMemento(req)
				.toString();
			advancedSearchAction.setChecked(true);
			advancedSearchAction.run();

		}
	}

	private void saveExpansionState() {
        lastExpandedElements = viewer.getExpandedElements();
        lastExpandedPaths = viewer.getExpandedTreePaths();
    }

    private void restoreExpansionState() {
        if (lastExpandedElements != null) {
            viewer.setExpandedElements(lastExpandedElements);
        }
        if (lastExpandedPaths != null) {
            viewer.setExpandedTreePaths(lastExpandedPaths);
        }
    }


	@Override
	public Workspace getWorkspace() {
		return this.workspace;
	}

	private BndPreferences getPreferences() {
		return this.preferences;
	}

}
