package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.custom.BusyIndicator;

public class RegistryBrowser extends ViewPart implements ISelectionListener {
	public static final String KEY_REFRESH_LABEL = "RegistryView.refresh.label";
	public static final String KEY_REFRESH_TOOLTIP = "RegistryView.refresh.tooltip";
	private TreeViewer treeViewer;
	private Action propertiesAction;
	private Action refreshAction;

public RegistryBrowser() {
	super();
	propertiesAction = new PropertiesAction(this);
	refreshAction = new Action("refresh") {
		public void run() {
			BusyIndicator.showWhile(treeViewer.getTree().getDisplay(), new Runnable() {
				public void run() {
					treeViewer.refresh();
				}
			});
		}
	};
	refreshAction.setText(PDERuntimePlugin.getResourceString(KEY_REFRESH_LABEL));
	refreshAction.setToolTipText(PDERuntimePlugin.getResourceString(KEY_REFRESH_TOOLTIP));
	refreshAction.setImageDescriptor(PDERuntimePluginImages.DESC_REFRESH);
	refreshAction.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_DISABLED);
	refreshAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_HOVER);
}
public void createPartControl(Composite parent) {
	Tree tree = new Tree(parent, SWT.NONE);

	treeViewer = new TreeViewer(tree);
	treeViewer.setContentProvider(new RegistryBrowserContentProvider());
	treeViewer.setLabelProvider(new RegistryBrowserLabelProvider());
	treeViewer.setUseHashlookup(true);

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.setRemoveAllWhenShown(true);
	popupMenuManager.addMenuListener(listener);
	Menu menu=popupMenuManager.createContextMenu(tree);
	tree.setMenu(menu);

	IViewSite site = getViewSite();
	site.getActionBars().getToolBarManager().add(propertiesAction);
	site.getActionBars().getToolBarManager().add(refreshAction);
	treeViewer.setInput(Platform.getPluginRegistry());
	site.setSelectionProvider(treeViewer);
}
public void fillContextMenu(IMenuManager manager) {
	manager.add(propertiesAction);
	manager.add(refreshAction);
}
public org.eclipse.jface.viewers.TreeViewer getTreeViewer() {
	return treeViewer;
}
private void initializeImages() {}
public void selectionChanged(IWorkbenchPart part, ISelection selection) {
}
public void setFocus() {}
}
