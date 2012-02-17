/*******************************************************************************
 *  Copyright (c) 2011, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sascha Becher <s.becher@qualitype.de> - bug 360894
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginParent;
import org.eclipse.pde.internal.core.search.ISearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchInput;

/**
 * Search operation for finding extension elements within a plugin using the {@link ExtensionsPatternFilter}.
 * 
 * @author Sascha Becher
 */
public class ExtensionElementSearchOperation {

	protected PluginSearchInput fInput;
	private ISearchResultCollector fCollector;

	public ExtensionElementSearchOperation(PluginSearchInput input, ISearchResultCollector collector) {
		this.fInput = input;
		this.fCollector = collector;
	}

	public void execute(IProgressMonitor monitor) {
		IPluginModelBase[] entries = fInput.getSearchScope().getMatchingModels();
		monitor.beginTask("", entries.length); //$NON-NLS-1$

		try {
			for (int i = 0; i < entries.length; i++) {
				IPluginModelBase candidate = entries[i];
				visit(candidate);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	private void visit(IPluginModelBase model) {
		ArrayList matches = findMatch(model);
		for (int i = 0; i < matches.size(); i++) {
			fCollector.accept(matches.get(i));
		}
	}

	private ArrayList findMatch(IPluginModelBase model) {
		ArrayList result = new ArrayList();
		int searchLimit = fInput.getSearchLimit();
		if (fInput.getSearchElement() == PluginSearchInput.ELEMENT_PLUGIN) {
			if (searchLimit != PluginSearchInput.LIMIT_REFERENCES)
				findPluginDeclaration(model, result);
			if (searchLimit != PluginSearchInput.LIMIT_DECLARATIONS)
				findPluginReferences(model, result);
		}
		return result;
	}

	private void findPluginDeclaration(IPluginModelBase model, ArrayList result) {
		IPluginBase pluginBase = model.getPluginBase();
		ExtensionsPatternFilter filter = new ExtensionsPatternFilter();
		filter.setPattern(fInput.getSearchString());
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			PluginExtension pluginExtension = (PluginExtension) extensions[i];
			boolean foundAny = traversePluginElements(pluginExtension, filter);
			if (foundAny && pluginBase instanceof IPlugin) {
				result.add(pluginBase);
				return;
			}
		}
	}

	private boolean traversePluginElements(PluginParent pluginParent, ExtensionsPatternFilter filter) {
		IPluginObject[] pluginObjects = pluginParent.getChildren();
		if (pluginObjects != null) {
			for (int i = 0; i < pluginObjects.length; i++) {
				boolean foundAny = traversePluginElements((PluginParent) pluginObjects[i], filter);
				if (foundAny) {
					return true;
				}
			}
		}
		return filter.isLeafMatch(null, pluginParent);
	}

	private void findPluginReferences(IPluginModelBase model, ArrayList result) {
		IPluginBase pluginBase = model.getPluginBase();
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			findPluginDeclaration(imports[i].getPluginModel(), result);
		}
	}

}