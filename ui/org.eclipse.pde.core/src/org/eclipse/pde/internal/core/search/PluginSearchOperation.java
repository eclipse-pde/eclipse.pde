package org.eclipse.pde.internal.core.search;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.util.StringMatcher;


public class PluginSearchOperation {
	protected PluginSearchInput input;
	private IPluginSearchResultCollector collector;
	private StringMatcher stringMatcher;
	private String taskName;
	
	public PluginSearchOperation(
		PluginSearchInput input,
		IPluginSearchResultCollector collector) {
		this.input = input;
		this.collector = collector;
		collector.setOperation(this);
		this.stringMatcher =new StringMatcher(input.getSearchString(),!input.isCaseSensitive(),false);
	}
	
	public void execute(IProgressMonitor monitor) {
		IPluginModelBase[] entries = input.getSearchScope().getMatchingModels();
		collector.searchStarted();
		collector.setProgressMonitor(monitor);
		monitor.beginTask("", entries.length);

		try {
			for (int i = 0; i < entries.length; i++) {
				IPluginModelBase candidate = entries[i];
				visit(candidate);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
			collector.done();
		}
	}
	
	private void visit(IPluginModelBase model) {
		ArrayList matches = findMatch(model);
		for (int i = 0; i < matches.size(); i++) {
			collector.accept((IPluginObject)matches.get(i));
		}
	}
	
	private ArrayList findMatch(IPluginModelBase model) {
		ArrayList result = new ArrayList();
		int searchLimit = input.getSearchLimit();
		switch (input.getSearchElement()) {
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
			default :
				;
		}
		return result;
	}
	
	private void findFragmentDeclaration(
		IPluginModelBase model,
		ArrayList result) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IFragment
			&& stringMatcher.match(pluginBase.getId())) {
			result.add(pluginBase); }
	}
				
	private void findPluginDeclaration(IPluginModelBase model, ArrayList result) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IPlugin && stringMatcher.match(pluginBase.getId()))
			result.add(pluginBase);
	}
	
	private void findPluginReferences(
		IPluginModelBase model,
		ArrayList result) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IFragment) {
			if (stringMatcher.match(((IFragment) pluginBase).getPluginId()))
				result.add(pluginBase);
		}
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (stringMatcher.match(imports[i].getId()))
				result.add(imports[i]);
		}
	}

	private void findExtensionPointDeclarations(
		IPluginModelBase model,
		ArrayList result) {
		IPluginExtensionPoint[] extensionPoints =
			model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < extensionPoints.length; i++) {
			if (stringMatcher
				.match(
					model.getPluginBase().getId()
						+ "."
						+ extensionPoints[i].getId()))
				result.add(extensionPoints[i]);
		}
	}
	
	private void findExtensionPointReferences(IPluginModelBase model, ArrayList result) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (stringMatcher.match(extensions[i].getPoint()))
				result.add(extensions[i]);
		}
	}
		
}
