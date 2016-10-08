/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.core.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.util.PatternConstructor;

public class PluginSearchOperation {
	protected PluginSearchInput fInput;
	private ISearchResultCollector fCollector;
	private Pattern fPattern;

	public PluginSearchOperation(PluginSearchInput input, ISearchResultCollector collector) {
		this.fInput = input;
		this.fCollector = collector;
		this.fPattern = PatternConstructor.createPattern(input.getSearchString(), input.isCaseSensitive());
	}

	public void execute(IProgressMonitor monitor) {
		IPluginModelBase[] plugins = fInput.getSearchScope().getMatchingModels();
		IFeatureModel[] features = fInput.getSearchScope().getMatchingFeatureModels();
		SubMonitor subMonitor = SubMonitor.convert(monitor, plugins.length + features.length);

		for (IPluginModelBase candidate : plugins) {
			visit(candidate);
			subMonitor.step(1);
		}

		for (IFeatureModel candidate : features) {
			visit(candidate);
			subMonitor.step(1);
		}
	}

	private void visit(IPluginModelBase model) {
		ArrayList<IIdentifiable> matches = findMatch(model);
		for (int i = 0; i < matches.size(); i++) {
			fCollector.accept(matches.get(i));
		}
	}

	private void visit(final IFeatureModel model) {
		final List<IIdentifiable> matches = findMatch(model);
		for (int i = 0; i < matches.size(); i++) {
			fCollector.accept(matches.get(i));
		}
	}

	private ArrayList<IIdentifiable> findMatch(IPluginModelBase model) {
		ArrayList<IIdentifiable> result = new ArrayList<>();
		int searchLimit = fInput.getSearchLimit();
		switch (fInput.getSearchElement()) {
			case PluginSearchInput.ELEMENT_PLUGIN :
				if (searchLimit != PluginSearchInput.LIMIT_REFERENCES)
					findPluginDeclaration(model, result);
				if (searchLimit != PluginSearchInput.LIMIT_DECLARATIONS)
					findPluginReferences(model, result);
				break;
			case PluginSearchInput.ELEMENT_FRAGMENT :
				findFragmentDeclaration(model, result);
				break;
			case PluginSearchInput.ELEMENT_EXTENSION_POINT :
				if (searchLimit != PluginSearchInput.LIMIT_REFERENCES)
					findExtensionPointDeclarations(model, result);
				if (searchLimit != PluginSearchInput.LIMIT_DECLARATIONS)
					findExtensionPointReferences(model, result);
				break;
		}
		return result;
	}

	private List<IIdentifiable> findMatch(final IFeatureModel model) {
		final List<IIdentifiable> result = new ArrayList<>();
		int searchLimit = fInput.getSearchLimit();
		switch (fInput.getSearchElement()) {
		case PluginSearchInput.ELEMENT_PLUGIN:
			if (searchLimit != PluginSearchInput.LIMIT_DECLARATIONS) {
				findPluginReferences(model, result);
			}
			break;
		}
		return result;
	}

	private void findFragmentDeclaration(IPluginModelBase model, ArrayList<IIdentifiable> result) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IFragment && fPattern.matcher(pluginBase.getId()).matches()) {
			result.add(pluginBase);
		}
	}

	private void findPluginDeclaration(IPluginModelBase model, ArrayList<IIdentifiable> result) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IPlugin && fPattern.matcher(pluginBase.getId()).matches())
			result.add(pluginBase);
	}

	private void findPluginReferences(IPluginModelBase model, ArrayList<IIdentifiable> result) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IFragment) {
			if (fPattern.matcher(((IFragment) pluginBase).getPluginId()).matches())
				result.add(pluginBase);
		}
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (fPattern.matcher(imports[i].getId()).matches())
				result.add(imports[i]);
		}
	}

	/**
	 * Search feature if any of its included plugins match the pattern search.
	 *
	 * @param model
	 *            of feature
	 * @param result
	 *            will contain references to plugins included in feature
	 *            matching the pattern
	 */
	private void findPluginReferences(final IFeatureModel model, final List<IIdentifiable> result) {
		final IFeaturePlugin[] includedPlugins = model.getFeature().getPlugins();
		for (IFeaturePlugin plugin : includedPlugins) {
			if (fPattern.matcher(plugin.getId()).matches()) {
				result.add(plugin);
			}
		}
	}

	private void findExtensionPointDeclarations(IPluginModelBase model, ArrayList<IIdentifiable> result) {
		IPluginExtensionPoint[] extensionPoints = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < extensionPoints.length; i++) {
			if (fPattern.matcher(extensionPoints[i].getFullId()).matches())
				result.add(extensionPoints[i]);
		}
	}

	private void findExtensionPointReferences(IPluginModelBase model, ArrayList<IIdentifiable> result) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (fPattern.matcher(extensions[i].getPoint()).matches())
				result.add(extensions[i]);
		}
	}

}
