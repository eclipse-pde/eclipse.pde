/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.OSGiFrameworkManager;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class OSGiFrameworkPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
	
	class FrameworkLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_BUNDLE);
		}
		
		public String getText(Object element) {
			if (element instanceof IConfigurationElement) {
				String name = ((IConfigurationElement)element).getAttribute(OSGiFrameworkManager.ATT_NAME);
				String id = ((IConfigurationElement)element).getAttribute(OSGiFrameworkManager.ATT_ID); 
				return fDefaultFramework.equals(id) ? name + " " + PDEUIMessages.OSGiFrameworkPreferencePage_default : name; //$NON-NLS-1$
			}
			return super.getText(element);
		}
	}

	private TableViewer fTableViewer;
	private Button fSetDefaultButton;
	private String fDefaultFramework;

	public OSGiFrameworkPreferencePage() {	
		setDefaultFramework();
	}
	
	private void setDefaultFramework() {
		IPreferenceStore store  = PDEPlugin.getDefault().getPreferenceStore();
		fDefaultFramework = store.getString(IPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK);
	}
	
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		Link text = new Link(container, SWT.WRAP);
		text.setText(PDEUIMessages.OSGiFrameworkPreferencePage_installed); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		text.setLayoutData(gd);
		text.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(OSGiFrameworkManager.POINT_ID);
				if (point != null) {
					new ShowDescriptionAction(point, true).run();	
				} else {
					Display.getDefault().beep();
				}
			}
		});

		fTableViewer = new TableViewer(container, SWT.BORDER);
		fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setLabelProvider(new FrameworkLabelProvider());
		fTableViewer.setInput(PDEPlugin.getDefault().getOSGiFrameworkManager().getSortedFrameworks());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ssel = (IStructuredSelection)event.getSelection();
				String id = ((IConfigurationElement)ssel.getFirstElement()).getAttribute("id"); //$NON-NLS-1$
				fSetDefaultButton.setEnabled(ssel.size() == 1 && !fDefaultFramework.equals(id));
			}
		});
	
		Composite buttonContainer = new Composite(container, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fSetDefaultButton = new Button(buttonContainer, SWT.PUSH);
		fSetDefaultButton.setText(PDEUIMessages.OSGiFrameworkPreferencePage_setAs); 
		fSetDefaultButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fSetDefaultButton);
		fSetDefaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection)fTableViewer.getSelection();
				IConfigurationElement element = (IConfigurationElement)ssel.getFirstElement();
				fDefaultFramework = element.getAttribute(OSGiFrameworkManager.ATT_ID);
				fTableViewer.refresh();
				fSetDefaultButton.setEnabled(false);
			}
		});
		fSetDefaultButton.setEnabled(false);	
		Dialog.applyDialogFont(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.OSGI_PREFERENCE_PAGE);
		return container;
	}
	
	public boolean performOk() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setValue(IPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, fDefaultFramework);
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	protected void performDefaults() {
		setDefaultFramework();
		fTableViewer.refresh();
	}

	public void init(IWorkbench workbench) {
	}	
	
}
