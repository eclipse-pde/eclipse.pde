package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.runtime.*;


public class PropertiesAction extends Action {
	public static final String KEY_LABEL = "RegistryView.properties.label";
	public static final String KEY_TOOLTIP = "RegistryView.properties.tooltip";
	private RegistryBrowser browser;

public PropertiesAction(RegistryBrowser browser) {
	this.browser = browser;
	setText(PDERuntimePlugin.getResourceString(KEY_LABEL));
	setToolTipText(PDERuntimePlugin.getResourceString(KEY_TOOLTIP));
	setImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES);
	setDisabledImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES_DISABLED);
	setHoverImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES_HOVER);
}
public void run() {
	try {
		String viewId = IPageLayout.ID_PROP_SHEET;
		IWorkbenchPage workbenchPage = PDERuntimePlugin.getActivePage();
		PropertySheet propertySheet = (PropertySheet)workbenchPage.showView(viewId);
		workbenchPage.activate(browser);
		TreeViewer viewer = browser.getTreeViewer();
		viewer.getControl().setFocus();
		propertySheet.selectionChanged(browser, viewer.getSelection());
	} catch (PartInitException e) {
		System.out.println(e);
		Display.getCurrent().beep();
	}
}
}
