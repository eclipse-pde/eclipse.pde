/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Peter Friese <peter.friese@gentleware.com> - bug 191769, 199431
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - bug 61185
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.LazyStartHeader;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeFieldAssistDisposer;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.Constants;

public class PluginGeneralInfoSection extends GeneralInfoSection {

	private FormEntry fClassEntry;
	private Button fLazyStart;
	private TypeFieldAssistDisposer fTypeFieldAssistDisposer;
	private boolean fBlockListener = false;

	public PluginGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}

	@Override
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_desc;
	}

	@Override
	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createClassEntry(parent, toolkit, actionBars);
		FormEditor formEditor = getPage().getEditor();
		if (isBundle() && (formEditor instanceof ManifestEditor)) {
			createLazyStart(parent, toolkit, actionBars);
			createSingleton(parent, toolkit, actionBars, PDEUIMessages.PluginGeneralInfoSection_singleton);
		}
	}

	@Override
	protected void addListeners() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.addModelChangedListener(this);
		}
		super.addListeners();
	}

	@Override
	protected void removeListeners() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.removeModelChangedListener(this);
		}
		super.removeListeners();
	}

	private void createLazyStart(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		fLazyStart = toolkit.createButton(parent, PDEUIMessages.PluginGeneralInfoSection_lazyStart, SWT.CHECK);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		fLazyStart.setLayoutData(td);
		fLazyStart.setEnabled(isEditable());
		fLazyStart.addSelectionListener(widgetSelectedAdapter(e -> {
			LazyStartHeader[] headers = getLazyStartHeaders();
			// must block the refresh otherwise we have problems with multiple activation headers.
			fBlockListener = true;
			for (LazyStartHeader header : headers)
				header.setLazyStart(fLazyStart.getSelection());
			if (headers.length == 0)
				getBundle().setHeader(getLazyStartHeaderName(), getLazyStateHeaderValue(fLazyStart.getSelection()));
			fBlockListener = false;
		}));
	}

	private void createClassEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		boolean isEditable = isEditable();
		fClassEntry = new FormEntry(client, toolkit, PDEUIMessages.GeneralInfoSection_class, PDEUIMessages.GeneralInfoSection_browse, //
				isEditable());
		fClassEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				try {
					((IPlugin) getPluginBase()).setClassName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String value = fClassEntry.getValue();
				IProject project = getPage().getPDEEditor().getCommonProject();
				value = PDEJavaHelperUI.createClass(value, project, createJavaAttributeValue(), false);
				if (value != null)
					fClassEntry.setValue(value);
			}

			@Override
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(entry.getValue());
			}
		});
		fClassEntry.setEditable(isEditable);

		if (isEditable) {
			fTypeFieldAssistDisposer = PDEJavaHelperUI.addTypeFieldAssistToText(fClassEntry.getText(), getProject(), IJavaSearchConstants.CLASS);
		}
	}

	private void doOpenSelectionDialog(String className) {
		IResource resource = getPluginBase().getModel().getUnderlyingResource();
		String type = PDEJavaHelperUI.selectType(resource, IJavaElementSearchConstants.CONSIDER_CLASSES, className, null);
		if (type != null)
			fClassEntry.setValue(type);
	}

	private JavaAttributeValue createJavaAttributeValue() {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		return new JavaAttributeValue(project, model, null, fClassEntry.getValue());
	}

	@Override
	public void cancelEdit() {
		fClassEntry.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public void commit(boolean onSave) {
		fClassEntry.commit();
		super.commit(onSave);
	}

	@Override
	public void refresh() {
		if (fBlockListener)
			return;
		// if we are refactoring, the Manifest moves before the editor closes.  This could cause the model to be null on a refresh()
		if (getPage().getModel() instanceof IPluginModelBase model) {
			IPlugin plugin = (IPlugin) model.getPluginBase();
			// Only update this field if it already has not been modified
			// This will prevent the cursor from being set to position 0 after
			// accepting a field assist proposal using \r
			if (fClassEntry.isDirty() == false) {
				fClassEntry.setValue(plugin.getClassName(), true);
			}
			if (fLazyStart != null) {
				IManifestHeader header = getLazyStartHeader();
				fLazyStart.setSelection(header instanceof LazyStartHeader && ((LazyStartHeader) header).isLazyStart());
			}
			super.refresh();
		}
	}

	private LazyStartHeader[] getLazyStartHeaders() {
		IBundle bundle = getBundle();
		ArrayList<IManifestHeader> headers = new ArrayList<>(3);
		if (bundle instanceof Bundle) {
			String[] keys = new String[] {ICoreConstants.ECLIPSE_LAZYSTART, ICoreConstants.ECLIPSE_AUTOSTART, Constants.BUNDLE_ACTIVATIONPOLICY};
			for (String key : keys) {
				IManifestHeader header = bundle.getManifestHeader(key);
				if (header != null)
					headers.add(header);
			}
		}
		return headers.toArray(new LazyStartHeader[headers.size()]);
	}

	// get the LazyStartHeader the same way the runtime would resolve them
	private IManifestHeader getLazyStartHeader() {
		IBundle bundle = getBundle();
		IManifestHeader header = null;
		if (bundle instanceof Bundle) {
			double targetVersion = TargetPlatformHelper.getTargetVersion();
			if (targetVersion >= 3.3)
				header = bundle.getManifestHeader(Constants.BUNDLE_ACTIVATIONPOLICY);
			if (header == null && targetVersion >= 3.2)
				header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_LAZYSTART);
			if (header == null)
				header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_AUTOSTART);
		}
		return header;
	}

	private String getLazyStartHeaderName() {
		if (BundlePluginBase.getBundleManifestVersion(getBundle()) >= 2) {
			if (TargetPlatformHelper.getTargetVersion() >= 3.4)
				return Constants.BUNDLE_ACTIVATIONPOLICY;
			else if (TargetPlatformHelper.getTargetVersion() >= 3.2)
				return ICoreConstants.ECLIPSE_LAZYSTART;
		}
		return ICoreConstants.ECLIPSE_AUTOSTART;
	}

	private String getLazyStateHeaderValue(boolean lazyStart) {
		if (BundlePluginBase.getBundleManifestVersion(getBundle()) >= 2 && TargetPlatformHelper.getTargetVersion() >= 3.4)
			return lazyStart ? Constants.ACTIVATION_LAZY : null;
		return Boolean.toString(lazyStart);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fTypeFieldAssistDisposer != null) {
			fTypeFieldAssistDisposer.dispose();
		}
	}

}
