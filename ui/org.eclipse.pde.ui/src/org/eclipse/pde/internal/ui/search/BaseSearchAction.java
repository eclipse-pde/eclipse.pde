package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.*;
import org.eclipse.search.ui.*;


public abstract class BaseSearchAction extends Action {

	public BaseSearchAction(String text) {
		setText(text);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQuery(createSearchQuery());
	}
	
	protected abstract ISearchQuery createSearchQuery();

}
