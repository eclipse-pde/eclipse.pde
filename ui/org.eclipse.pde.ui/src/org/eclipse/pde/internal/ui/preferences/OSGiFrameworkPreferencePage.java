/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Anyware Technologies - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Provides the preference page for managing the default OSGi framework to use.
 *
 * @since 3.3
 */
public class OSGiFrameworkPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IPluginExtensionPoint fPluginExtensionPoint = null;
	/**
	 * Label provider for the table viewer. Annotates the default framework with bold text
	 */
	class FrameworkLabelProvider extends LabelProvider implements IFontProvider {
		private Font font = null;

		@Override
		public Image getImage(Object element) {
			return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_BUNDLE);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IConfigurationElement) {
				String name = ((IConfigurationElement) element).getAttribute(OSGiFrameworkManager.ATT_NAME);
				String id = ((IConfigurationElement) element).getAttribute(OSGiFrameworkManager.ATT_ID);
				return fDefaultFramework.equals(id) ? name + " " + PDEUIMessages.OSGiFrameworkPreferencePage_default : name; //$NON-NLS-1$
			}
			return super.getText(element);
		}

		@Override
		public Font getFont(Object element) {
			if (element instanceof IConfigurationElement) {
				String id = ((IConfigurationElement) element).getAttribute(OSGiFrameworkManager.ATT_ID);
				if (fDefaultFramework.equals(id)) {
					if (this.font == null) {
						Font dialogFont = JFaceResources.getDialogFont();
						FontData[] fontData = dialogFont.getFontData();
						for (FontData data : fontData) {
							data.setStyle(SWT.BOLD);
						}
						Display display = getControl().getShell().getDisplay();
						this.font = new Font(display, fontData);
					}
					return this.font;
				}
			}
			return null;
		}

		@Override
		public void dispose() {
			if (this.font != null) {
				this.font.dispose();
			}
			super.dispose();
		}
	}

	private CheckboxTableViewer fTableViewer;
	private String fDefaultFramework;

	/**
	 * Constructor
	 */
	public OSGiFrameworkPreferencePage() {
		setDefaultFramework();
	}

	/**
	 * Restores the default framework setting from the PDE preferences
	 */
	private void setDefaultFramework() {
		PDEPreferencesManager preferenceManager = PDELaunchingPlugin.getDefault().getPreferenceManager();
		fDefaultFramework = preferenceManager.getString(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);

		Link text = new Link(comp, SWT.WRAP);

		if (PDECore.getDefault().areModelsInitialized())
			fPluginExtensionPoint = PDECore.getDefault().getExtensionsRegistry()
					.findExtensionPoint(OSGiFrameworkManager.POINT_ID);
		text.setText((fPluginExtensionPoint != null && SchemaRegistry.getSchemaURL(fPluginExtensionPoint) != null)
				? PDEUIMessages.OSGiFrameworkPreferencePage_installed
				: PDEUIMessages.OSGiFrameworkPreferencePage_installed_nolink);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		text.setLayoutData(gd);
		text.addSelectionListener(widgetSelectedAdapter(e -> new ShowDescriptionAction(fPluginExtensionPoint, true).run()));

		fTableViewer = new CheckboxTableViewer(new Table(comp, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION));
		fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		fTableViewer.setLabelProvider(new FrameworkLabelProvider());
		fTableViewer.setInput(PDELaunchingPlugin.getDefault().getOSGiFrameworkManager().getSortedFrameworks());
		fTableViewer.addCheckStateListener(event -> {
			IConfigurationElement element = (IConfigurationElement) event.getElement();
			fTableViewer.setCheckedElements(new Object[] {element});
			fDefaultFramework = element.getAttribute(OSGiFrameworkManager.ATT_ID);
			fTableViewer.refresh();
		});
		if (fDefaultFramework != null) {
			IConfigurationElement element = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager().getFramework(fDefaultFramework);
			if (element != null) {
				fTableViewer.setCheckedElements(new Object[] {element});
			}
		}
		Dialog.applyDialogFont(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.OSGI_PREFERENCE_PAGE);
		return comp;
	}

	@Override
	public boolean performOk() {
		IEclipsePreferences instancePrefs = InstanceScope.INSTANCE.getNode(IPDEConstants.PLUGIN_ID);
		IEclipsePreferences defaultPrefs = DefaultScope.INSTANCE.getNode(IPDEConstants.PLUGIN_ID);
		if (defaultPrefs.get(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, "").equals(fDefaultFramework)) { //$NON-NLS-1$
			instancePrefs.remove(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK);
		} else {
			instancePrefs.put(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, fDefaultFramework);
		}
		try {
			instancePrefs.flush();
		} catch (BackingStoreException e) {
			PDEPlugin.log(e);
		}

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		setDefaultFramework();
		fTableViewer.refresh();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
