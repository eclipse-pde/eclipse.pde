package org.eclipse.pde.internal.core.search;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchScope {

	public static final int SCOPE_WORKSPACE = 0;
	public static final int SCOPE_SELECTION = 1;
	public static final int SCOPE_WORKING_SETS = 2;
	
	public static final int EXTERNAL_SCOPE_NONE = 0;
	public static final int EXTERNAL_SCOPE_ENABLED = 1;
	public static final int EXTERNAL_SCOPE_ALL = 2;
	
	private String description;
	private int workspaceScope;
	private int externalScope;
	private IFile[] selectedItems;
	
	/**
	 * Create a scope object with the provided arguments.
	 * @param workspaceScope  one of SCOPE_WORKSPACE, SCOPE_SELECTION,
	 * SCOPE_WORKING_SETS
	 * @param externalScope  one of EXTERNAL_SCOPE_NONE, EXTERNAL_SCOPE_ENABLED,
	 * EXTERNAL_SCOPE_ALL
	 * @param workingSets  goes with SCOPE_WORKING_SETS, otherwise null	 * @param description  an NL string that describes the entire scope	 */
	public PluginSearchScope(
		int workspaceScope,
		int externalScope,
		IFile[] selectedItems,
		String description) {
			this.workspaceScope = workspaceScope;
			this.externalScope = externalScope;
			this.selectedItems = selectedItems;
			this.description = description;
	}
	
	
	/**
	 * Creates a default scope object that will return all the entries in the
	 * PluginSearchScope.  It is equivalent to workspace scope being set to
	 * 'Workspace' and external scope being set to 'Only Enabled'
	 */
	public PluginSearchScope() {
		this(SCOPE_WORKSPACE, EXTERNAL_SCOPE_ENABLED, null, "");
	}
	
	public String getDescription() {
		return description;
	}
	
	private void addExternalModels(ArrayList result) {
		if (externalScope != EXTERNAL_SCOPE_NONE) {
			IPluginModelBase[] extModels =
				PDECore.getDefault().getExternalModelManager().getModels();
			for (int i = 0; i < extModels.length; i++) {
				if (externalScope == EXTERNAL_SCOPE_ENABLED
					&& !extModels[i].isEnabled())
					continue;
				result.add(extModels[i]);
			}
		}
	}
	
	private void addWorkspaceModels(ArrayList result) {
		if (workspaceScope != SCOPE_WORKSPACE) {
			PluginModelManager modelManager =
				PDECore.getDefault().getModelManager();
			for (int i = 0; i < selectedItems.length; i++) {
				IFile file = selectedItems[i];
				ModelEntry entry = modelManager.findEntry(file.getProject());
				if (entry != null) {
					result.add(entry.getActiveModel());
				}
			}
		} else {
			IPluginModelBase[] wModels =
				PDECore.getDefault().getWorkspaceModelManager().getAllModels();
			for (int i = 0; i < wModels.length; i++) {
				result.add(wModels[i]);
			}
		}
	}
	
	public IPluginModelBase[] getMatchingModels() {
		ArrayList result = new ArrayList();
		addWorkspaceModels(result);
		addExternalModels(result);
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
}
