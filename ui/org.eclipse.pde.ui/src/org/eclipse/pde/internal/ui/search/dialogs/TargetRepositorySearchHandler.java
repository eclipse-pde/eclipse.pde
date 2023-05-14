/*******************************************************************************
* Copyright (c) 2010, 2017 EclipseSource and others.
*
* This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM - Further improvements
******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler that pops up an IU selection dialog, the result of which is added to the active
 *  target.
 *
 *  @since 3.6
 */
public class TargetRepositorySearchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// create the query for packages, features and bundles
		IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("properties[$0] == true || providedCapabilities.exists(p | p.namespace == 'osgi.bundle')", new Object[] {MetadataFactory.InstallableUnitDescription.PROP_TYPE_GROUP}); //$NON-NLS-1$
		//IQuery query = QueryUtil.createIUAnyQuery();

		FilteredIUSelectionDialog dialog = new FilteredIUSelectionDialog(window.getShell(), query);
		String pattern = event.getParameter("org.eclipse.pde.ui.searchTargetRepositories.term"); //$NON-NLS-1$
		if (pattern != null)
			dialog.setInitialPattern(pattern);
		int status = dialog.open();
		if (status == Window.OK) {
			Object[] result = dialog.getResult();
			if (result != null) {
				Set<Object> set = new HashSet<>();
				for (Object resultObject : result) {
					if (resultObject instanceof IUPackage)
						set.add(((IUPackage) resultObject).getIU());
					else if (resultObject instanceof IInstallableUnit)
						set.add(resultObject);
				}
				IInstallableUnit[] units = set.toArray(new IInstallableUnit[set.size()]);
				try {
					installIntoActiveTarget(units, null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
		return null;
	}

	/**
	 * Creates a new IUBundleContainer, adds it to the active target, then reloads the active target.
	 *
	 * @param units new installable units to include in the container
	 * @param repositories list of repositories the container can use as a context or <code>null</code> to use all available repos
	 */
	private static void installIntoActiveTarget(IInstallableUnit[] units, URI[] repositories) throws CoreException {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition definition = service.getWorkspaceTargetDefinition();
		// Force the target into slicer mode as all requirements may not be available
		int flags = IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS | IUBundleContainer.INCLUDE_SOURCE;
		IUBundleContainer container = (IUBundleContainer) service.newIULocation(units, repositories, flags);
		ITargetLocation[] oldContainers = definition.getTargetLocations();
		if (oldContainers == null) {
			definition.setTargetLocations(new ITargetLocation[] {container});
		} else {
			ITargetLocation[] newContainers = new ITargetLocation[oldContainers.length + 1];
			System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
			newContainers[newContainers.length - 1] = container;
			definition.setTargetLocations(newContainers);
			service.saveTargetDefinition(definition);
			LoadTargetDefinitionJob.load(definition);
		}
	}
}
