/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;


public class SplashSection extends PDESection {

	private FormEntry fPluginEntry;
	private ColorSelector fColorSelector;
	private boolean fBlockNotification;
	
	private Button fAddBarButton;
	// spinners controlling the progress bar geometry
	private Spinner[] fBarSpinners = new Spinner[4];
	// all swt controls under the progress bar checkbox
	private Control[] fBarControls = new Control[8];
	
	private Button fAddMessageButton;
	// spinners controlling the progress message geometry
	private Spinner[] fMessageSpinners =  new Spinner[4];
	// all swt controls under the progress message checkbox
	private Control[] fMessageControls = new Control[10];
	
	public SplashSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.SplashSection_title); 
		section.setDescription(PDEUIMessages.SplashSection_desc); 

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 10;
		layout.marginHeight = 5;
		client.setLayout(layout);
		
		Label label = toolkit.createLabel(client, PDEUIMessages.SplashSection_label, SWT.WRAP); 
		GridData gd = new GridData();
		gd.horizontalSpan = 10;
		label.setLayoutData(gd);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fPluginEntry = new FormEntry(client, toolkit, PDEUIMessages.SplashSection_plugin, PDEUIMessages.SplashSection_browse, false); // 
		fPluginEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getSplashInfo().setLocation(entry.getValue(), false);
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});
		fPluginEntry.setEditable(isEditable());
		
		createProgressBarConfig(client, toolkit);
		createProgressMessageConfig(client, toolkit);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		section.setLayoutData(gd);
	}
	
	private void createProgressBarConfig(Composite parent, FormToolkit toolkit) {
		fAddBarButton = createButton(parent, toolkit, PDEUIMessages.SplashSection_progressBar);
		fAddBarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enable = fAddBarButton.getSelection();
				getSplashInfo().addProgressBar(enable, false);
				for (int i = 0; i < fBarControls.length; i++)
					fBarControls[i].setEnabled(enable);
			}
		});
		
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fBarControls[0] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_progressX);
		fBarControls[1] = fBarSpinners[0] = createSpinner(parent, toolkit);
		fBarControls[2] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_progressY);
		fBarControls[3] = fBarSpinners[1] = createSpinner(parent, toolkit);
		fBarControls[4] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_progressWidth);
		fBarControls[5] = fBarSpinners[2] = createSpinner(parent, toolkit);
		fBarControls[6] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_progressHeight);
		fBarControls[7] = fBarSpinners[3] = createSpinner(parent, toolkit);
		
		for (int i = 0; i < fBarSpinners.length; i++) {
			fBarSpinners[i].addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					applySpinners(true);
				}
			});
		}
		
		Composite filler = toolkit.createComposite(parent);
		filler.setLayout(new GridLayout());
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		filler.setLayoutData(gd);
	}
	
	private void createProgressMessageConfig(Composite parent, FormToolkit toolkit) {
		fAddMessageButton = createButton(parent, toolkit, PDEUIMessages.SplashSection_progressMessage);
		fAddMessageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enable = fAddMessageButton.getSelection();
				getSplashInfo().addProgressMessage(enable, false);
				for (int i = 0; i < fMessageControls.length; i++)
					fMessageControls[i].setEnabled(enable);
			}
		});
		
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fMessageControls[0] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_messageX);
		fMessageControls[1] = fMessageSpinners[0] = createSpinner(parent, toolkit);
		fMessageControls[2] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_messageY);
		fMessageControls[3] = fMessageSpinners[1] = createSpinner(parent, toolkit);
		
		fMessageControls[4] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_messageWidth);
		fMessageControls[5] = fMessageSpinners[2] = createSpinner(parent, toolkit);
		fMessageControls[6] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_messageHeight);
		fMessageControls[7] = fMessageSpinners[3] = createSpinner(parent, toolkit);

		fMessageControls[8] = createLabel(parent, toolkit, foreground, PDEUIMessages.SplashSection_messageColor);
		fColorSelector = new ColorSelector(parent);
		fColorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (!event.getNewValue().equals(event.getOldValue()))
					applyColor();
			}
		});
		toolkit.adapt(fColorSelector.getButton(), true, true);
		fMessageControls[9] = fColorSelector.getButton();

		for (int i = 0; i < fMessageSpinners.length; i++) {
			fMessageSpinners[i].addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					applySpinners(false);
				}
			});
		}
	}
	
	private void applyColor() {
		if (fBlockNotification)
			return;
		RGB rgb = fColorSelector.getColorValue();
		getSplashInfo().setForegroundColor(rgbToHEX(rgb), false);
	}
	
	private void applySpinners(boolean bar) {
		if (fBlockNotification)
			return;
		Spinner[] spinners = bar ? fBarSpinners : fMessageSpinners;
		int[] geo = new int[] {
				spinners[0].getSelection(),
				spinners[1].getSelection(),
				spinners[2].getSelection(),
				spinners[3].getSelection()
		};
		if (bar)
			getSplashInfo().setProgressGeometry(geo, false);
		else
			getSplashInfo().setMessageGeometry(geo, false);
	}
	
	private Label createLabel(Composite parent, FormToolkit toolkit, Color color, String labelName) {
		Label label = toolkit.createLabel(parent, labelName);
		label.setForeground(color);
		GridData gd = new GridData();
		gd.horizontalIndent = 10;
		label.setLayoutData(gd);
		return label;
	}
	
	private Button createButton(Composite parent, FormToolkit toolkit, String label) {
		Button button = toolkit.createButton(parent, label, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 10;
		button.setLayoutData(gd);
		return button;
	}
	
	private Spinner createSpinner(Composite parent, FormToolkit toolkit) {
		Spinner spinner = new Spinner(parent, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(9999);
		toolkit.adapt(spinner, false, false);
		return spinner;
	}
	
	public void refresh() {
		ISplashInfo info = getSplashInfo();
		fBlockNotification = true;
		
		fPluginEntry.setValue(info.getLocation(), true);
		fColorSelector.setColorValue(hexToRGB(info.getForegroundColor()));
		
		int[] pgeo = info.getProgressGeometry();
		boolean addProgress = pgeo != null;
		info.addProgressBar(addProgress, fBlockNotification);
		if (addProgress)
			for (int i = 0; i < pgeo.length; i++)
				fBarSpinners[i].setSelection(pgeo[i]);
		
		fAddBarButton.setSelection(addProgress);
		for (int i = 0; i < fBarControls.length; i++)
			fBarControls[i].setEnabled(addProgress);
		
		int[] mgeo = info.getMessageGeometry();
		boolean addMessage = mgeo != null;
		info.addProgressMessage(addMessage, fBlockNotification);
		if (addMessage)
			for (int i = 0; i < mgeo.length; i++)
				fMessageSpinners[i].setSelection(mgeo[i]);
		fColorSelector.setColorValue(
				addMessage ?
						hexToRGB(info.getForegroundColor()) :
						new RGB(0,0,0));
		
		fAddMessageButton.setSelection(addMessage);
		for (int i = 0; i < fMessageControls.length; i++)
			fMessageControls[i].setEnabled(addMessage);
		
		fBlockNotification = false;
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fPluginEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fPluginEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private ISplashInfo getSplashInfo() {
		ISplashInfo info = getProduct().getSplashInfo();
		if (info == null) {
			info = getModel().getFactory().createSplashInfo();
			getProduct().setSplashInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	private void handleBrowse() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());
		dialog.setElements(PDECore.getDefault().getModelManager().getAllPlugins());
		dialog.setMultipleSelection(false);
		dialog.setTitle(PDEUIMessages.SplashSection_selection); 
		dialog.setMessage(PDEUIMessages.SplashSection_message); 
		if (dialog.open() == Window.OK) {
			IPluginModelBase model = (IPluginModelBase)dialog.getFirstResult();
			fPluginEntry.setValue(model.getPluginBase().getId());
		}
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	private String rgbToHEX(RGB rgb) {
		if (rgb == null)
			return null;
		return rgbToHex(rgb.red) + rgbToHex(rgb.green) + rgbToHex(rgb.blue);
	}
	
	private String rgbToHex(int value) {
		value = Math.max(0, value);
		value = Math.min(value, 255);
		String hex = Integer.toHexString(value).toUpperCase();
		if (hex.length() == 1)
			hex = '0' + hex;
		return hex;
	}
	
	private RGB hexToRGB(String hexValue) {
		if (hexValue == null || hexValue.length() < 6)
			return new RGB(0,0,0);
		return new RGB(
				Integer.parseInt(hexValue.substring(0,2),16),
				Integer.parseInt(hexValue.substring(2,4),16),
				Integer.parseInt(hexValue.substring(4,6),16)
			);
	}
}
