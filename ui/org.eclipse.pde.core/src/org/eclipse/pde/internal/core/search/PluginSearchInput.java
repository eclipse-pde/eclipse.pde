package org.eclipse.pde.internal.core.search;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchInput {
	public static final int ELEMENT_PLUGIN = 1;
	public static final int ELEMENT_FRAGMENT = 2;
	public static final int ELEMENT_EXTENSION_POINT = 3;
	
	public static final int LIMIT_DECLARATIONS = 1;
	public static final int LIMIT_REFERENCES = 2;
	public static final int LIMIT_ALL = 3;
		
	private String searchString = null;
	private boolean caseSensitive = false;
	private int searchElement = 0;
	private int searchLimit = 0;
	private PluginSearchScope searchScope;
	
	public String getSearchString() {
		return searchString;
	}
	
	public boolean isCaseSensitive() {
		return caseSensitive;
	}
	
	public void setCaseSensitive(boolean value) {
		caseSensitive = value;
	}
	
	public void setSearchString(String name) {
		searchString = name;
	}
	
	public int getSearchElement() {
		return searchElement;
	}
	
	public void setSearchElement(int element) {
		searchElement = element;
	}
	
	public int getSearchLimit() {
		return searchLimit;
	}
	
	public void setSearchLimit(int limit) {
		searchLimit = limit;
	}
	
	public PluginSearchScope getSearchScope() {
		return searchScope;
	}
	
	public void setSearchScope(PluginSearchScope scope) {
		searchScope = scope;
	}
	
}
