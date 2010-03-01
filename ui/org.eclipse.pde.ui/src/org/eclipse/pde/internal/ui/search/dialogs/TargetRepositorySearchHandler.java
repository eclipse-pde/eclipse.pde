/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.commands.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.InstallIUIntoTarget;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class TargetRepositorySearchHandler extends AbstractHandler implements IHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// create the query for packages, features and bundles
		IQuery query = QueryUtil.createMatchQuery("properties[$0] == true || providedCapabilities.exists(p | p.namespace == 'osgi.bundle')", new Object[] {QueryUtil.PROP_TYPE_GROUP}); //$NON-NLS-1$
		//IQuery query = QueryUtil.createIUAnyQuery();

		FilteredIUSelectionDialog dialog = new FilteredIUSelectionDialog(window.getShell(), query);
		int status = dialog.open();
		if (status == Window.OK) {
			Object[] result = dialog.getResult();
			if (result != null) {
				Set set = new HashSet();
				for (int i = 0; i < result.length; i++) {
					if (result[i] instanceof IUPackage)
						set.add(((IUPackage) result[i]).getIU());
					else if (result[i] instanceof IInstallableUnit)
						set.add(result[i]);
				}
				IInstallableUnit[] units = (IInstallableUnit[]) set.toArray(new IInstallableUnit[set.size()]);
				ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
				try {
					ITargetHandle currentTarget = service.getWorkspaceTargetHandle();
					ITargetDefinition definition = currentTarget.getTargetDefinition();
					InstallIUIntoTarget.install(definition, units, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
