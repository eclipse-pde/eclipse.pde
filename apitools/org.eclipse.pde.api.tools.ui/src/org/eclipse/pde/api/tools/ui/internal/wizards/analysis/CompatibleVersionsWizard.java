/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.api.tools.internal.builder.ApiUseAnalyzer.CompatibilityResult;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPluginImages;

/**
 * Wizard to determine compatible version range of required bundles.
 * 
 * @since 1.0
 */
public class CompatibleVersionsWizard extends Wizard {
	
	/**
	 * Plug-in to analyze
	 */
	private IPluginModelBase fPlugin;
	
	/**
	 * Results from analysis
	 */
	private List fResults = new ArrayList();
	
	/**
	 * Constructs a new wizard to analyze the given plug-in.
	 * 
	 * @param plugin plug-in to analyze
	 */
	public CompatibleVersionsWizard(IPluginModelBase plugin) {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.CompatibleVersionsWizard_0);
		fPlugin = plugin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new SelectBundlesPage());
		addPage(new CompatibilityResultsPage());
	}

	/**
	 * Returns the plug-in being analyzed.
	 * 
	 * @return the plug-in being analyzed
	 */
	IPluginModelBase getPlugin() {
		return fPlugin;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		return getContainer().getCurrentPage() instanceof CompatibilityResultsPage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return true;
	}
	
	/**
	 * Sets the results from the analysis.
	 * 
	 * @param results
	 */
	void setResults(CompatibilityResult[] results) {
		fResults.clear();
		if (results != null) {
			for (int i = 0; i < results.length; i++) {
				fResults.add(results[i]);
			}
		}
	}
	
	/**
	 * Returns the results from the analysis or <code>null</code> if none.
	 * 
	 * @return results or <code>null</code> if none.
	 */
	List getResults() {
		return fResults;
	}

}
