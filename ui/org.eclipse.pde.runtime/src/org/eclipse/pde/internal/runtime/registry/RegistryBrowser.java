/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.properties.*;
public class RegistryBrowser extends ViewPart {// implements ISelectionListener
											   // {
	public static final String KEY_REFRESH_LABEL = "RegistryView.refresh.label";
	public static final String KEY_REFRESH_TOOLTIP = "RegistryView.refresh.tooltip";
	public static final String SEARCH_BY = "RegistryBrowser.menu.searchBy";
	public static final String LIST_BY = "RegistryBrowser.menu.listBy";
	private TreeViewer treeViewer;
	private Action refreshAction;
	private DrillDownAdapter drillDownAdapter;
	//attributes view
	private SashForm fSashForm;
	private ISelectionChangedListener fDetailSelectionChangedListener;
	private Label fPropertyLabel;
	private Label fPropertyImage;
	private PropertySheetPage fPropertySheet;
	public RegistryBrowser() {
		super();
		refreshAction = new Action("refresh") {
			public void run() {
				BusyIndicator.showWhile(treeViewer.getTree().getDisplay(),
						new Runnable() {
							public void run() {
								((RegistryBrowserContentProvider) treeViewer.getContentProvider()).setViewerPlugins(null);
								treeViewer.refresh();
								updateAttributesView(null);
							}
						});
			}
		};
		refreshAction.setText(PDERuntimePlugin
				.getResourceString(KEY_REFRESH_LABEL));
		refreshAction.setToolTipText(PDERuntimePlugin
				.getResourceString(KEY_REFRESH_TOOLTIP));
		refreshAction.setImageDescriptor(PDERuntimePluginImages.DESC_REFRESH);
		refreshAction
				.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_DISABLED);
		refreshAction
				.setHoverImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_HOVER);
	}
	public void createPartControl(Composite parent) {
		// create the sash form that will contain the tree viewer & text viewer
		fSashForm = new SashForm(parent, SWT.HORIZONTAL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = layout.marginWidth = 0;
		fSashForm.setLayout(layout);
		fSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		fSashForm.setBackground(fSashForm.getDisplay().getSystemColor(
				SWT.COLOR_WHITE));
		setSashForm(fSashForm);
		createTreeViewer();
		createAttributesViewer();
	}
	public void createTreeViewer() {
		// place all controls into sash form
		Composite composite = new Composite(getSashForm(), SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
		// end
		Tree tree = new Tree(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		tree.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(gd);
		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new RegistryBrowserContentProvider());
		treeViewer.setLabelProvider(new RegistryBrowserLabelProvider());
		treeViewer.setUseHashlookup(true);
		treeViewer.setSorter(new ViewerSorter() {
		});
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.setRemoveAllWhenShown(true);
		popupMenuManager.addMenuListener(listener);
		Menu menu = popupMenuManager.createContextMenu(tree);
		tree.setMenu(menu);
		drillDownAdapter = new DrillDownAdapter(treeViewer);
		IViewSite site = getViewSite();
		IToolBarManager mng = site.getActionBars().getToolBarManager();
		drillDownAdapter.addNavigationActions(mng);
		mng.add(new Separator());
		//	mng.add(propertiesAction);
		mng.add(refreshAction);
		treeViewer.setInput(new PluginObjectAdapter(Platform
				.getPluginRegistry()));
		site.setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((IStructuredSelection) event.getSelection())
						.getFirstElement();
				updateAttributesView(selection);
			}
		});
		WorkbenchHelp.setHelp(treeViewer.getControl(),
				IHelpContextIds.REGISTRY_VIEW);
	}
	public void fillContextMenu(IMenuManager manager) {
		MenuManager listMenu = new MenuManager(PDERuntimePlugin.getResourceString(LIST_BY));
		manager.add(listMenu);
		new ListingMenu(listMenu, getViewSite().getWorkbenchWindow(), false,
				treeViewer, refreshAction, !drillDownAdapter.canGoHome());
		
		MenuManager searchMenu = new MenuManager(PDERuntimePlugin.getResourceString(SEARCH_BY));
		manager.add(searchMenu);
		new RegistrySearchMenu(searchMenu, getViewSite().getWorkbenchWindow(),
				false, treeViewer, refreshAction,
				!drillDownAdapter.canGoHome());
		
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		//	manager.add(propertiesAction);
	}
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	public void setFocus() {
	}
	/* add attributes viewer */
	protected void createAttributesViewer() {
		Composite composite = new Composite(getSashForm(), SWT.FLAT);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		composite.setLayout(layout);
		fPropertyImage = new Label(composite, SWT.NONE);
		GridData gd = new GridData(GridData.FILL);
		gd.widthHint = 20;
		fPropertyImage.setLayoutData(gd);
		fPropertyLabel = new Label(composite, SWT.NULL);
		fPropertyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createPropertySheet(composite);
	}
	protected SashForm getSashForm() {
		return fSashForm;
	}
	private void setSashForm(SashForm sashForm) {
		fSashForm = sashForm;
	}
	protected void createPropertySheet(Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);
		fPropertySheet = new PropertySheetPage();
		fPropertySheet.createControl(composite);
		gd = new GridData(GridData.FILL_BOTH);
		fPropertySheet.getControl().setLayoutData(gd);
		fPropertySheet.makeContributions(new MenuManager(),
				new ToolBarManager(), null);
	}
	public void updateAttributesView(Object selection) {
		fPropertyImage.setImage(((RegistryBrowserLabelProvider) treeViewer
				.getLabelProvider()).getImage(selection));
		fPropertyLabel.setText(((RegistryBrowserLabelProvider) treeViewer
				.getLabelProvider()).getText(selection));
		if (selection != null)
			fPropertySheet.selectionChanged(null, new StructuredSelection(
					selection));
		else
			fPropertySheet.selectionChanged(null, new StructuredSelection(new Object()));
	}
}
