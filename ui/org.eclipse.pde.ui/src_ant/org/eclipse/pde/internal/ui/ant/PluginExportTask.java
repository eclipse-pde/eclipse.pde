package org.eclipse.pde.internal.ui.ant;

import java.util.*;

import org.eclipse.core.runtime.jobs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

/**
 * @author melhem
 *
 */
public class PluginExportTask extends BaseExportTask {
	protected IPluginModelBase[] fModels = new IPluginModelBase[0];

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.ant.BaseExportTask#getExportJob()
	 */
	protected Job getExportJob() {
		return new PluginExportJob(fExportType, fExportSource,
				fDestination, fZipFilename, fModels);
	}
	
	public void setPlugins(String plugins) {
		StringTokenizer tok = new StringTokenizer(plugins, ",");
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ArrayList models = new ArrayList();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IPluginModelBase model = manager.findPlugin(id, null, IMatchRules.NONE);
			if (model != null && model.getUnderlyingResource() != null)
				models.add(model);
		}
		fModels = (IPluginModelBase[])models.toArray(new IPluginModelBase[models.size()]);
	}
	

}
