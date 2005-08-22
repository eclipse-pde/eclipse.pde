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
	private FormEntry fPluginMinVersionEntry;
	private FormEntry fPluginMaxVersionEntry;
	private ComboPart fPluginMinVersionBound;
	private ComboPart fPluginMaxVersionBound;
	private ComboPart fMatchCombo;

	public FragmentGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_fdesc; 
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
				PDEUIMessages.GeneralInfoSection_pluginId,  
				PDEUIMessages.GeneralInfoSection_browse, // 
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
			fPluginMinVersionEntry.setValue(plugin.getVersion());
		}
	}

	private void createPluginVersionEntry(Composite client,
			FormToolkit toolkit, IActionBars actionBars) {
		if (isBundle()) {
			createBundlePluginVersionEntry(client, toolkit, actionBars);
		} else {
			createNonBundlePluginVersionEntry(client, toolkit, actionBars);
		}
		
	}
	
	private void createBundlePluginVersionEntry(Composite client,
			FormToolkit toolkit, IActionBars actionBars) {

		String[] items = new String[] {
				PDEUIMessages.DependencyPropertiesDialog_comboInclusive,
				PDEUIMessages.DependencyPropertiesDialog_comboExclusive };
		fPluginMinVersionEntry = new FormEntry(client, toolkit,
				PDEUIMessages.GeneralInfoSection_hostMinVersionRange, 0, 1); 
		fPluginMinVersionEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(getVersion());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry entry) {
				setMaxFieldsEnabled();
				super.textDirty(entry);
			}
		});
		fPluginMinVersionBound = new ComboPart();
		fPluginMinVersionBound.createControl(client, toolkit, SWT.READ_ONLY);
		fPluginMinVersionBound.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL));
		fPluginMinVersionBound.setItems(items);
		fPluginMinVersionBound.getControl().setEnabled(isEditable());
		fPluginMinVersionBound.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(getVersion());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		
		fPluginMaxVersionEntry = new FormEntry(client, toolkit,
				PDEUIMessages.GeneralInfoSection_hostMaxVersionRange, 0, 1); 
		fPluginMaxVersionEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(getVersion());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fPluginMaxVersionBound = new ComboPart();
		fPluginMaxVersionBound.createControl(client, toolkit, SWT.READ_ONLY);
		fPluginMaxVersionBound.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL));
		fPluginMaxVersionBound.setItems(items);
		fPluginMaxVersionBound.getControl().setEnabled(isEditable());
		fPluginMaxVersionBound.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(getVersion());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}			
		});
	}

	private void createNonBundlePluginVersionEntry(Composite client,
			FormToolkit toolkit, IActionBars actionBars) {
		fPluginMinVersionEntry = new FormEntry(
				client,
				toolkit,
				PDEUIMessages.GeneralInfoSection_pluginVersion, null, false); 
		fPluginMinVersionEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fPluginMinVersionEntry.setEditable(isEditable());
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
		fPluginMinVersionEntry.commit();
		fPluginMaxVersionEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fPluginIdEntry.cancelEdit();
		fPluginMinVersionEntry.cancelEdit();
		fPluginMaxVersionEntry.cancelEdit();
		super.cancelEdit();
	}
	
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IFragment fragment = (IFragment) model.getPluginBase();
		fPluginIdEntry.setValue(fragment.getPluginId(), true);
		if (isBundle()) {
			refreshVersion();
		} else {
			fPluginMinVersionEntry.setValue(fragment.getPluginVersion(), true);
		}
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

	private void setMaxFieldsEnabled() {
		boolean enabled = fPluginMinVersionEntry.getText().getText().trim().length() != 0;
		fPluginMaxVersionEntry.getText().setEnabled(enabled);
		fPluginMaxVersionBound.getControl().setEnabled(enabled);
	}
	
	private String getVersion() {
		if (isBundle()) {
			if (!fPluginMinVersionEntry.getValue().equals(fPluginMaxVersionEntry.getValue()) &&
					fPluginMaxVersionEntry.getText().getEnabled()) {
				if (fPluginMaxVersionEntry.getValue().length() == 0)
					return fPluginMinVersionEntry.getValue();
				String version;
				if (fPluginMinVersionBound.getSelectionIndex() == 0)
					version = "["; //$NON-NLS-1$
				else
					version = "("; //$NON-NLS-1$
				version += fPluginMinVersionEntry.getValue() + "," + fPluginMaxVersionEntry.getValue(); //$NON-NLS-1$
				if (fPluginMaxVersionBound.getSelectionIndex() == 0)
					version += "]"; //$NON-NLS-1$
				else
					version += ")"; //$NON-NLS-1$
				return version;
			}
		}
		return fPluginMinVersionEntry.getValue();
	}
	
	private void refreshVersion() {	
		String version = getAttribute(Constants.FRAGMENT_HOST, Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (version == null) {
			setVersionFields("", true, "", false); //$NON-NLS-1$ //$NON-NLS-2$
			setMaxFieldsEnabled();
			return;
		}
		version = version.trim();
		int comInd = version.indexOf(","); //$NON-NLS-1$
		int lastPos = version.length() - 1;
		char first = version.charAt(0);
		char last = version.charAt(lastPos);
		if (comInd == -1) {
			setVersionFields(version, true, "", false); //$NON-NLS-1$
		} else if ((first == '[' || first == '(') && 
				(last == ']' || last == ')')) {
			version = version.substring(1, lastPos);
			setVersionFields(
					version.substring(0, comInd - 1),
					first == '[',
					version.substring(comInd),
					last == ']');
		}
		setMaxFieldsEnabled();
	}
	
	private void setVersionFields(String minVersion, boolean minInclusive, String maxVersion, boolean maxInclusive) {
		fPluginMinVersionEntry.setValue(minVersion, true);
		fPluginMinVersionBound.select(minInclusive ? 0 : 1);
		fPluginMaxVersionEntry.setValue(maxVersion, true);
		fPluginMaxVersionBound.select(maxInclusive ? 0 : 1);
	}
}
