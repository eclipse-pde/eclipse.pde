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
package org.eclipse.pde.internal.ui.editor.build;

import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.preferences.PDEPreferencesUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;

public class BuildExecutionEnvironmentSection extends PDESection {

	private ComboPart fCombo;

	public BuildExecutionEnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.BuildExecutionEnvironmentSection_title);
		section.setDescription(PDEUIMessages.BuildExecutionEnvironmentSection_desc);
		
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = toolkit.createLabel(client, PDEUIMessages.BuildExecutionEnvironmentSection_label, SWT.NONE);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fCombo = new ComboPart();
		fCombo.createControl(client, toolkit, SWT.READ_ONLY);
		fCombo.setItems(getExecutionEnvironments());
		fCombo.add("", 0); //$NON-NLS-1$
		fCombo.setText(""); //$NON-NLS-1$
		fCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					IBuildEntry entry = getBuildEntry();
					String text = fCombo.getSelection();
					if (text.length() == 0) {
						if (entry != null)
							getBuildModel().getBuild().remove(entry);
					} else {
						if (entry != null) {
							String[] tokens = entry.getTokens();
							if (tokens.length > 0)
								entry.renameToken(tokens[0], text);
							else
								entry.addToken(text);
						} else {
							entry = getBuildModel().getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JRE_COMPILATION_PROFILE);
							entry.addToken(text);
							getBuildModel().getBuild().add(entry);
						}
					}
				} catch (CoreException e1) {
				}
			}
		});
		
		Hyperlink link = toolkit.createHyperlink(client, PDEUIMessages.BuildExecutionEnvironmentSection_configure, SWT.NONE);
		link.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		link.addHyperlinkListener(new IHyperlinkListener() {
			public void linkEntered(HyperlinkEvent e) {
			}
			public void linkExited(HyperlinkEvent e) {
			}
			public void linkActivated(HyperlinkEvent e) {
				PDEPreferencesUtil.showPreferencePage(new String[] {"org.eclipse.jdt.debug.ui.jreProfiles"}); //$NON-NLS-1$
			}
		});
		
		toolkit.paintBordersFor(client);

		section.setClient(client);
		IBuildModel model = getBuildModel();
		if (model != null)
			model.addModelChangedListener(this);
	}
	
	private String[] getExecutionEnvironments() {
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		TreeSet result = new TreeSet();
		for (int i = 0; i < envs.length; i++) 
			result.add(envs[i].getId());
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (IBuildPropertiesConstants.PROPERTY_JRE_COMPILATION_PROFILE.equals(e.getChangedProperty())) {
			refresh();
		}
	}
	
	public void dispose() {
		IBuildModel model = getBuildModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	public void refresh() {
		String env = getBuildExecutionEnvironment();
		fCombo.setText(fCombo.indexOf(env) != -1 ? env : ""); //$NON-NLS-1$
		super.refresh();
	}
	
	private String getBuildExecutionEnvironment() {
		IBuildEntry entry = getBuildEntry();
		if (entry != null) {
			String[] tokens = entry.getTokens();
			if (tokens.length > 0)
				return tokens[0];
		}	
		return ""; //$NON-NLS-1$
	}
	
	private IBuildEntry getBuildEntry() {
		IBuildModel model = getBuildModel();
		if (model != null) {
			IBuild build = model.getBuild();
			if (build != null) {
				return build.getEntry(IBuildPropertiesConstants.PROPERTY_JRE_COMPILATION_PROFILE);
			}
		}
		return null;
	}
	
	private IBuildModel getBuildModel() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		InputContext context = manager.findContext(BuildInputContext.CONTEXT_ID);
		return (context == null) ? null : (IBuildModel) context.getModel();
	}
	

}
