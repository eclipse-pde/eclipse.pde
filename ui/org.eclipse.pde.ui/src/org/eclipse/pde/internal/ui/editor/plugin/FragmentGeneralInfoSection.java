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
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class FragmentGeneralInfoSection extends GeneralInfoSection {

	private FormEntry fPluginIdEntry;
	private FormEntry fPluginVersionEntry;
	private ComboPart fMatchCombo;

	public FragmentGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_fdesc; //$NON-NLS-1$
	}
	
	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createPluginIdEntry(parent, toolkit, actionBars);
		createPluginVersionEntry(parent, toolkit, actionBars);
		if (!isBundle())
			createMatchCombo(parent, toolkit, actionBars);
	}
	
	private void createPluginIdEntry(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		fPluginIdEntry = new FormEntry(
				parent,
				toolkit,
				PDEUIMessages.GeneralInfoSection_pluginId,  //$NON-NLS-1$
				PDEUIMessages.GeneralInfoSection_browse, //$NON-NLS-1$ 
				isEditable());
		fPluginIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginId(fPluginIdEntry.getValue());
				} catch (CoreException e1) {
					PDEPlugin.logException(e1);
				}
			}
			public void linkActivated(HyperlinkEvent e) {
				String plugin = fPluginIdEntry.getValue();				
				if (PDECore.getDefault().getModelManager().findPluginModel(
						plugin) == null) {
					createFragmentPlugin();
				}
				ManifestEditor.openPluginEditor(fPluginIdEntry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleOpenDialog();
			}
			private void createFragmentPlugin() {
				NewPluginProjectWizard wizard = new NewPluginProjectWizard();
				WizardDialog dialog = new WizardDialog(PDEPlugin
						.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 500);
				if (dialog.open() == WizardDialog.OK) {
					String plugin = wizard.getPluginId();
					try {
						((IFragment) getPluginBase()).setPluginId(plugin);
						fPluginIdEntry.setValue(plugin, false);
					} catch (CoreException ce) {
						PDEPlugin.logException(ce);
					}
				}
			}
		});
		fPluginIdEntry.setEditable(isEditable());		
	}

	protected void handleOpenDialog() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getSection().getShell(), false, false);
		dialog.create();
		if (dialog.open() == PluginSelectionDialog.OK) {
			IPluginModel model = (IPluginModel) dialog.getFirstResult();
			IPlugin plugin = model.getPlugin();
			fPluginIdEntry.setValue(plugin.getId());
			fPluginVersionEntry.setValue(plugin.getVersion());
		}
	}

	private void createPluginVersionEntry(Composite client,
			FormToolkit toolkit, IActionBars actionBars) {
		String labelText;
		if(isBundle())
			labelText= PDEUIMessages.GeneralInfoSection_hostVersionRange;
		else
			labelText= PDEUIMessages.GeneralInfoSection_pluginVersion;
		fPluginVersionEntry = new FormEntry(
				client,
				toolkit,
				labelText, null, false); 
		fPluginVersionEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fPluginVersionEntry.setEditable(isEditable());
	}
	
	private void createMatchCombo(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		Label matchLabel = toolkit.createLabel(client, PDEUIMessages.ManifestEditor_PluginSpecSection_versionMatch);
		matchLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		TableWrapData td = new TableWrapData();
		td.valign = TableWrapData.MIDDLE;
		matchLabel.setLayoutData(td);
		
		fMatchCombo = new ComboPart();
		fMatchCombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new TableWrapData(TableWrapData.FILL);
		td.colspan = 2;
		td.valign = TableWrapData.MIDDLE;
		fMatchCombo.getControl().setLayoutData(td);
		
		String[] items = new String[]{"", //$NON-NLS-1$
				PDEUIMessages.ManifestEditor_MatchSection_equivalent,
				PDEUIMessages.ManifestEditor_MatchSection_compatible,
				PDEUIMessages.ManifestEditor_MatchSection_perfect,
				PDEUIMessages.ManifestEditor_MatchSection_greater};
		fMatchCombo.setItems(items);
		fMatchCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int match = fMatchCombo.getSelectionIndex();
				try {
					((IFragment) getPluginBase()).setRule(match);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fMatchCombo.getControl().setEnabled(isEditable());
	}
	
	public void commit(boolean onSave) {
		fPluginIdEntry.commit();
		fPluginVersionEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fPluginIdEntry.cancelEdit();
		fPluginVersionEntry.cancelEdit();
		super.cancelEdit();
	}
	
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IFragment fragment = (IFragment) model.getPluginBase();
		fPluginIdEntry.setValue(fragment.getPluginId(), true);
		String hostVersion;
		if (isBundle())
			hostVersion = getAttribute(Constants.FRAGMENT_HOST, Constants.BUNDLE_VERSION_ATTRIBUTE);
		else
			hostVersion = fragment.getPluginVersion();
		fPluginVersionEntry.setValue(hostVersion, true);
		if (fMatchCombo != null)
			fMatchCombo.select(fragment.getRule());
		super.refresh();
	}
	
	protected String getAttribute(String header, String attribute) {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		String value = bundle.getHeader(header);
		if (value == null)
			return null;
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(header, value);
			if (elements.length > 0)
				return elements[0].getAttribute(attribute);
		} catch (BundleException e) {
		}
		return null;
	}

	
}
