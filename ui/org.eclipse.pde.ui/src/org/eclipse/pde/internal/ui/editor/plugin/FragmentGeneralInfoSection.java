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
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
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

public class FragmentGeneralInfoSection extends GeneralInfoSection {

	public static final String KEY_MATCH = "ManifestEditor.PluginSpecSection.versionMatch"; //$NON-NLS-1$
	public static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect"; //$NON-NLS-1$
	public static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent"; //$NON-NLS-1$
	public static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible"; //$NON-NLS-1$
	public static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater"; //$NON-NLS-1$

	private FormEntry fPluginIdEntry;
	private FormEntry fPluginVersionEntry;
	private ComboPart fMatchCombo;

	public FragmentGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEPlugin.getResourceString("ManifestEditor.PluginSpecSection.fdesc"); //$NON-NLS-1$
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
				PDEPlugin.getResourceString("GeneralInfoSection.pluginId"),  //$NON-NLS-1$
				PDEPlugin.getResourceString("GeneralInfoSection.browse"), //$NON-NLS-1$ 
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
				ManifestEditor.openPluginEditor(fPluginIdEntry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleOpenDialog();
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
		String key = isBundle() ? "GeneralInfoSection.hostVersionRange" : "GeneralInfoSection.pluginVersion"; //$NON-NLS-1$ //$NON-NLS-2$
		fPluginVersionEntry = new FormEntry(
				client,
				toolkit,
				PDEPlugin.getResourceString(key), null, false); 
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
		Label matchLabel = toolkit.createLabel(client, PDEPlugin.getResourceString(KEY_MATCH));
		matchLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fMatchCombo = new ComboPart();
		fMatchCombo.createControl(client, toolkit, SWT.READ_ONLY);
		TableWrapData td = new TableWrapData(TableWrapData.FILL);
		td.colspan = 2;
		fMatchCombo.getControl().setLayoutData(td);
		
		String[] items = new String[]{"", //$NON-NLS-1$
				PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
				PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
				PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
				PDEPlugin.getResourceString(KEY_MATCH_GREATER)};
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
		fPluginVersionEntry.setValue(fragment.getPluginVersion(), true);
		if (fMatchCombo != null)
			fMatchCombo.select(fragment.getRule());
		super.refresh();
	}
	
}
