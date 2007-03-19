/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public abstract class AbstractLauncherToolbar {
	
	private Image fValidateImage;
	private Image fFilterImage;
	private Image fDisabledFilterImage;
	private ToolItem fFilterItem;
	private ToolItem fValidateItem;
	
	private MenuItem fAutoValidateItem;
	private AbstractLauncherTab fTab;
	protected ILaunchConfiguration fLaunchConfiguration;
	private LaunchValidationOperation fOperation;
	private PluginStatusDialog fDialog;

	public AbstractLauncherToolbar(AbstractLauncherTab tab) { 
		fTab = tab;
		fValidateImage = PDEPluginImages.DESC_VALIDATE_TOOL.createImage();	
		fFilterImage = PDEPluginImages.DESC_FILTER.createImage();
		fDisabledFilterImage = PDEPluginImages.DESC_FILTER_DISABLED.createImage();
	}
	
	public void createContents(Composite parent) {
		final ToolBar bar = new ToolBar(parent, SWT.FLAT);
		bar.setBackground(parent.getBackground());
		bar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING|GridData.HORIZONTAL_ALIGN_END));

		new ToolItem(bar, SWT.SEPARATOR);
		//createFilterItem(bar);
		createValidateItem(bar);
		new ToolItem(bar, SWT.SEPARATOR);
	}
	
	protected void createFilterItem(final ToolBar bar) {
		fFilterItem = new ToolItem(bar, SWT.DROP_DOWN);
		fFilterItem.setImage(fFilterImage);
		fFilterItem.setDisabledImage(fDisabledFilterImage);
		fFilterItem.setToolTipText(PDEUIMessages.PluginsTabToolBar_filter_options);

		final Menu menu = new Menu (PDEPlugin.getActiveWorkbenchShell(), SWT.POP_UP);
		MenuItem item = new MenuItem (menu, SWT.CHECK);
		item.setText(NLS.bind(PDEUIMessages.PluginsTabToolBar_filter_disabled, fTab.getName().toLowerCase(Locale.ENGLISH)));
		item.setSelection(true);
		
		fFilterItem.addListener (SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle rect = fFilterItem.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					pt = bar.toDisplay(pt);
					menu.setLocation(pt.x, pt.y);
					menu.setVisible(true);
				}
			}
		});		
	}

	private void createValidateItem(final ToolBar bar) {
		fValidateItem = new ToolItem(bar, SWT.DROP_DOWN);
		fValidateItem.setImage(fValidateImage);
		fValidateItem.setToolTipText(NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName()));
		fValidateItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == 0)
					handleValidate();
			}
		});
		
		final Menu menu = new Menu (PDEPlugin.getActiveWorkbenchShell(), SWT.POP_UP);
		fAutoValidateItem = new MenuItem (menu, SWT.CHECK);
		fAutoValidateItem.setText(PDEUIMessages.PluginsTabToolBar_auto_validate);
		fAutoValidateItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTab.updateLaunchConfigurationDialog();
			}
		});

		fValidateItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle rect = fValidateItem.getBounds();
					Point pt = new Point (rect.x, rect.y + rect.height);
					pt = bar.toDisplay(pt);
					menu.setLocation(pt.x, pt.y);
					menu.setVisible(true);
				}
			}
		});		
	}

	public void dispose() {
		fValidateImage.dispose();
		fFilterImage.dispose();
		fDisabledFilterImage.dispose();
	}

	public void enableFiltering(boolean enable) {
		//fFilterItem.setEnabled(enable);
	}

	public void initializeFrom(ILaunchConfiguration configuration, boolean custom) {
		try {
			fLaunchConfiguration = configuration;
			fAutoValidateItem.setSelection(configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false));
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		//fFilterItem.setEnabled(custom);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, fAutoValidateItem.getSelection());
	}
	
	protected abstract LaunchValidationOperation createValidationOperation();
	
	public void handleValidate() {
		if (fOperation == null)
			fOperation = createValidationOperation();
		try {
			fOperation.run(new NullProgressMonitor());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		if (fDialog == null) {
			if (fOperation.hasErrors()) {
				fDialog = new PluginStatusDialog(fTab.getControl().getShell(), SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
				fDialog.setInput(fOperation.getInput());
				if (fDialog.open() == IDialogConstants.OK_ID)
					fDialog = null;
			} else if (fOperation.isEmpty()) {
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), 
											PDEUIMessages.PluginStatusDialog_pluginValidation, 
											NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
			} else {
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), 
						PDEUIMessages.PluginStatusDialog_pluginValidation, 
						PDEUIMessages.AbstractLauncherToolbar_noProblems);
			}
		} else {
			fDialog.refresh(fOperation.getInput());
		}
	}

}
