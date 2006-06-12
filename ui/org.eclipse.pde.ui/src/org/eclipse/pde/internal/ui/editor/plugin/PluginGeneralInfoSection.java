/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatform;
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
import org.eclipse.pde.internal.ui.editor.contentassist.TypeCompletionListener;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeCompletionProcessor;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.contentassist.ContentAssistHandler;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class PluginGeneralInfoSection extends GeneralInfoSection {

	private FormEntry fClassEntry;
	private Button fLazyStart;

	public PluginGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_desc; 
	}
	
	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createClassEntry(parent, toolkit, actionBars);		
		if (isBundle()) {
			createLazyStart(parent, toolkit, actionBars);
		}
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.addModelChangedListener(this);
		}
	}
	
	public void dispose() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.removeModelChangedListener(this);
		}
		super.dispose();
	}
	
	private void createLazyStart(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		fLazyStart = toolkit.createButton(parent, PDEUIMessages.PluginGeneralInfoSection_lazyStart, SWT.CHECK);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		fLazyStart.setLayoutData(td);
		fLazyStart.setEnabled(isEditable());
		fLazyStart.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IManifestHeader header = getLazyStartHeader();
				if (header instanceof LazyStartHeader)
					((LazyStartHeader)header).setLazyStart(fLazyStart.getSelection());
				else
					getBundle().setHeader(getLazyStartHeaderName(), 
							Boolean.toString(fLazyStart.getSelection()));
			}
		});
	}
	
	private void createClassEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		boolean isEditable = isEditable();
		fClassEntry = new FormEntry(
							client,
							toolkit,
							PDEUIMessages.GeneralInfoSection_class,  
							PDEUIMessages.GeneralInfoSection_browse, // 
							isEditable());
		fClassEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IPlugin) getPluginBase()).setClassName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void linkActivated(HyperlinkEvent e) {
				String value = fClassEntry.getValue();
				IProject project = getPage().getPDEEditor().getCommonProject();
				value = PDEJavaHelper.createClass(value, project, createJavaAttributeValue(), false);
				if (value != null)
					fClassEntry.setValue(value);
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog();
			}
		});
		fClassEntry.setEditable(isEditable);
		
		if (isEditable) {
			TypeCompletionProcessor processor = new TypeCompletionProcessor(
					getProject(),
					IJavaSearchConstants.CLASS
					);
			SubjectControlContentAssistant contentAssistant = new SubjectControlContentAssistant();
			contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
			contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			contentAssistant.setProposalSelectorBackground(new Color(client.getDisplay(), 255, 255, 255));
			ContentAssistHandler.createHandlerForText(fClassEntry.getText(), contentAssistant);
			contentAssistant.addCompletionListener(new TypeCompletionListener());
		}
	}
	
	private void doOpenSelectionDialog() {
		IResource resource = getPluginBase().getModel().getUnderlyingResource();
		String type = PDEJavaHelper.selectType(resource, IJavaElementSearchConstants.CONSIDER_CLASSES);
		if (type != null)
			fClassEntry.setValue(type);
	}

	private JavaAttributeValue createJavaAttributeValue() {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		return new JavaAttributeValue(project, model, null, fClassEntry.getValue());
	}
	
	public void cancelEdit() {
		fClassEntry.cancelEdit();
		super.cancelEdit();
	}
	
	public void commit(boolean onSave) {
		fClassEntry.commit();
		super.commit(onSave);
	}
	
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPlugin plugin = (IPlugin)model.getPluginBase();
		fClassEntry.setValue(plugin.getClassName(), true);
		if (fLazyStart != null) {
			IManifestHeader header = getLazyStartHeader();
			fLazyStart.setSelection(header instanceof LazyStartHeader 
					&& ((LazyStartHeader)header).isLazyStart());
		}
		super.refresh();
	}
	
	private IManifestHeader getLazyStartHeader() {
		IBundle bundle = getBundle();
		if (bundle instanceof Bundle) {
			IManifestHeader header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_LAZYSTART);
			if (header == null)
				header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_AUTOSTART);
			return header;
		}
		return null;
	}
	
	private String getLazyStartHeaderName() {
		if (TargetPlatform.getTargetVersion() >= 3.2
				&& BundlePluginBase.getBundleManifestVersion(getBundle()) >= 2)
			return ICoreConstants.ECLIPSE_LAZYSTART;
		return ICoreConstants.ECLIPSE_AUTOSTART;
	}

}
