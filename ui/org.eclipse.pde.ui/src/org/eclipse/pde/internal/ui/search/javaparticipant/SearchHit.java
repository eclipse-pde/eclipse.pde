package org.eclipse.pde.internal.ui.search.javaparticipant;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class SearchHit {
	private String fValue;
	private IPluginObject fPlugObj;
	private SearchHitAdapter fSearchHitAdapter;
	private boolean fIsType;
	
	public SearchHit(IPluginObject object, String value, boolean isType) {
		fValue = value;
		fPlugObj = object;
		fIsType = isType;
		fSearchHitAdapter = new SearchHitAdapter();
	}
	public Object getHitElement() {
		return fPlugObj;
	}
	public String getValue() {
		return fValue;
	}
	public IResource getResource() {
		return fPlugObj.getModel().getUnderlyingResource();
	}
	
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return fSearchHitAdapter;
		return null;
	}

	public boolean isTypeHit() {
		return fIsType;
	}
}