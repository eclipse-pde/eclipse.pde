package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.search.ui.text.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;


public class PluginSearchResultPage extends AbstractSearchResultPage {

	class SearchLabelProvider extends LabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object object) {
			if (object instanceof IPluginBase)
				return ((IPluginBase)object).getId();
			
			if (object instanceof IPluginImport) {
				IPluginImport dep = (IPluginImport)object;
				return dep.getId() 
					+ " - " //$NON-NLS-1$
					+ dep.getPluginBase().getId();
			} 
			
			if (object instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension)object;
				return extension.getPoint() + " - " + extension.getPluginBase().getId();
			}
			
			if (object instanceof IPluginExtensionPoint)
				return ((IPluginExtensionPoint)object).getFullId();

			return PDEPlugin.getDefault().getLabelProvider().getText(object);
		}
	}
	
	public PluginSearchResultPage() {
		super();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		mgr.add(new Separator());
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(getViewer().getSelection()));
		actionGroup.fillContextMenu(mgr);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.search.AbstractSearchResultPage#createLabelProvider()
	 */
	protected ILabelProvider createLabelProvider() {
		return new SearchLabelProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.search.AbstractSearchResultPage#createViewerSorter()
	 */
	protected ViewerSorter createViewerSorter() {
		return new ViewerSorter();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#showMatch(org.eclipse.search.ui.text.Match, int, int, boolean)
	 */
	protected void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		ManifestEditorOpener.open(match, activate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

}
