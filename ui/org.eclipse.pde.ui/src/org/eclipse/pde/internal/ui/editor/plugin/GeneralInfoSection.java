/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public abstract class GeneralInfoSection extends PDESection {
	private static String PLATFORM_FILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$

	private FormEntry fIdEntry;
	private FormEntry fVersionEntry;
	private FormEntry fNameEntry;
	private FormEntry fProviderEntry;
	private FormEntry fPlatformFilterEntry;

	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ManifestEditor_PluginSpecSection_title); 
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		section.setDescription(getSectionDescription()); 
		
		Composite client = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 3;
		client.setLayout(layout);
		section.setClient(client);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		createIDEntry(client, toolkit, actionBars);
		createVersionEntry(client, toolkit, actionBars);
		createNameEntry(client, toolkit, actionBars);
		createProviderEntry(client, toolkit, actionBars);		
		if (isBundle() && ((ManifestEditor)getPage().getEditor()).isEquinox())
			createPlatformFilterEntry(client, toolkit, actionBars);
		createSpecificControls(client, toolkit, actionBars);
		toolkit.paintBordersFor(client);
		
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
	}
	
	protected abstract String getSectionDescription();
	
	protected abstract void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars);
	
	protected  IPluginBase getPluginBase() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}
	
	protected boolean isBundle() {
		return getBundleContext() != null;
	}
	
	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}
	
	protected IBundle getBundle() {
		BundleInputContext context = getBundleContext();
		if (context != null) {
			IBundleModel model = (IBundleModel)context.getModel();
			return model.getBundle();
		}
		return null;
	}
	
	private void createIDEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIdEntry = new FormEntry(client, toolkit, PDEUIMessages.GeneralInfoSection_id, null, false); 
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setId(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fIdEntry.setEditable(isEditable());
	}
	
	private void createVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fVersionEntry = new FormEntry(client, toolkit, PDEUIMessages.GeneralInfoSection_version, null, false); 
		fVersionEntry.setFormEntryListener(new FormEntryAdapter(this,actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setVersion(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fVersionEntry.setEditable(isEditable());
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit,IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.GeneralInfoSection_name, null, false); 
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fNameEntry.setEditable(isEditable());
	}
	
	private void createProviderEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fProviderEntry = new FormEntry(client, toolkit, PDEUIMessages.GeneralInfoSection_provider, null, false);
		fProviderEntry.setFormEntryListener(new FormEntryAdapter(this,actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setProviderName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fProviderEntry.setEditable(isEditable());
	}
	
	private void createPlatformFilterEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fPlatformFilterEntry = new FormEntry(client, toolkit, PDEUIMessages.GeneralInfoSection_platformFilter, null, false);
		fPlatformFilterEntry.setFormEntryListener(new FormEntryAdapter(this,actionBars) {
			public void textValueChanged(FormEntry entry) {
				getBundle().setHeader(PLATFORM_FILTER, fPlatformFilterEntry.getValue());
			}
		});
		fPlatformFilterEntry.setEditable(isEditable());
	}

	
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fVersionEntry.commit();
		fNameEntry.commit();
		fProviderEntry.commit();
		if (fPlatformFilterEntry != null)
			fPlatformFilterEntry.commit();
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IPluginBase) {
				String property = e.getChangedProperty();
				if (property != null && property.equals(getPage().getPDEEditor().getTitleProperty()))
					getPage().getPDEEditor().updateTitle();
			}
		}
	}

	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getContextManager().getAggregateModel();
		IPluginBase pluginBase = model.getPluginBase();
		fIdEntry.setValue(pluginBase.getId(), true);
		fNameEntry.setValue(pluginBase.getName(), true);
		fVersionEntry.setValue(pluginBase.getVersion(), true);
		fProviderEntry.setValue(pluginBase.getProviderName(), true);
		if (fPlatformFilterEntry != null) {
			IBundle bundle = getBundle();
			if (bundle != null)
				fPlatformFilterEntry.setValue(bundle.getHeader(PLATFORM_FILTER), true);
		}
		getPage().getPDEEditor().updateTitle();
		super.refresh();
	}
	
	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fVersionEntry.cancelEdit();
		fProviderEntry.cancelEdit();
		if (fPlatformFilterEntry != null) 
			fPlatformFilterEntry.cancelEdit();
		super.cancelEdit();
	}
	
	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return (d.getFocusControl() instanceof Text);
	}
	
}
