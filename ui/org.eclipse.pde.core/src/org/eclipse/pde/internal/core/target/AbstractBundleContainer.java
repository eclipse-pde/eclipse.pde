/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Manumitting Technologies Inc - bug 437726: wrong error messages opening target definition
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Common function for bundle containers.
 *
 * @since 3.5
 */
public abstract class AbstractBundleContainer extends PlatformObject implements ITargetLocation {

	/**
	 * Resolved bundles or <code>null</code> if unresolved
	 */
	protected TargetBundle[] fBundles;

	/**
	 * List of target features contained in this bundle container or <code>null</code> if unresolved
	 */
	protected TargetFeature[] fFeatures;

	/**
	 * Status generated when this container was resolved, possibly <code>null</code>
	 */
	protected IStatus fResolutionStatus;

	/**
	 * The Java VM Arguments specified by this bundle container
	 */
	private String[] fVMArgs;

	static private HashMap<AbstractBundleContainer, String[]> hash = new HashMap<>();

	/**
	 * Resolves any string substitution variables in the given text returning
	 * the result.
	 *
	 * @param text text to resolve
	 * @return result of the resolution
	 * @throws CoreException if unable to resolve
	 */
	protected String resolveVariables(String text) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(text);
	}

	@Override
	public final boolean isResolved() {
		return fResolutionStatus != null && fResolutionStatus.getSeverity() != IStatus.CANCEL;
	}

	@Override
	public final IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
		int resolveBundlesWork = getResolveBundlesWork();
		int resolveFeaturesWork = getResolveFeaturesWork();

		SubMonitor subMonitor = SubMonitor.convert(monitor, resolveBundlesWork + resolveFeaturesWork);
		try {
			fResolutionStatus = Status.OK_STATUS;
			fBundles = resolveBundles(definition, subMonitor.split(resolveBundlesWork));
			fFeatures = resolveFeatures(definition, subMonitor.split(resolveFeaturesWork));
			if (subMonitor.isCanceled()) {
				fBundles = null;
				fResolutionStatus = Status.CANCEL_STATUS;
			}
		} catch (CoreException e) {
			fBundles = new TargetBundle[0];
			fFeatures = new TargetFeature[0];
			fResolutionStatus = e.getStatus();
		} finally {
			subMonitor.done();
			if (monitor != null) {
				monitor.done();
			}
		}
		return fResolutionStatus;
	}

  /**
   * Can be overridden in subclasses to redistribute the work between {@link #resolveBundles(ITargetDefinition, IProgressMonitor)}
   * and {@link #resolveFeatures(ITargetDefinition, IProgressMonitor)}.
   *
   * @return the value 100, making {@link #resolveFeatures(ITargetDefinition, IProgressMonitor)} consume two thirds of
   *  the overall work being done in {@link #resolve(ITargetDefinition, IProgressMonitor)}.
   * @see #getResolveFeaturesWork()
   */
  protected int getResolveBundlesWork() {
    return 100;
  }

  /**
   * Can be overridden in subclasses to redistribute the work between {@link #resolveBundles(ITargetDefinition, IProgressMonitor)}
   * and {@link #resolveFeatures(ITargetDefinition, IProgressMonitor)}.
   *
   * @return the value 50, making {@link #resolveFeatures(ITargetDefinition, IProgressMonitor)} consume one third of
   *  the overall work being done in {@link #resolve(ITargetDefinition, IProgressMonitor)}.
   * @see #getResolveBundlesWork()
   */
  protected int getResolveFeaturesWork() {
    return 50;
  }

	@Override
	public IStatus getStatus() {
		if (!isResolved()) {
			return null;
		}
		return fResolutionStatus;
	}

	@Override
	public final TargetBundle[] getBundles() {
		if (isResolved()) {
			return fBundles;
		}
		return null;
	}

	@Override
	public TargetFeature[] getFeatures() {
		if (isResolved()) {
			return fFeatures;
		}
		return null;
	}

	/**
	 * Resolves all source and executable bundles in this container
	 * <p>
	 * Subclasses must implement this method.
	 * </p><p>
	 * <code>beginTask()</code> and <code>done()</code> will be called on the given monitor by the caller.
	 * </p>
	 * @param definition target context
	 * @param monitor progress monitor
	 * @return all source and executable bundles in this container
	 * @throws CoreException if an error occurs
	 */
	protected abstract TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException;

	/**
	 * Collects all of the features in this container.  May return an empty array if {@link #resolveBundles(ITargetDefinition, IProgressMonitor)}
	 * has not been called previously.
	 * <p>
	 * Subclasses must implement this method.
	 * </p><p>
	 * <code>beginTask()</code> and <code>done()</code> will be called on the given monitor by the caller.
	 * </p>
	 * @param definition target context
	 * @param monitor progress monitor
	 * @return all features in this container
	 * @throws CoreException if an error occurs
	 */
	protected abstract TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns a string that identifies the type of bundle container.  This type is persisted to xml
	 * so that the correct bundle container is created when deserializing the xml.  This type is also
	 * used to alter how the containers are presented to the user in the UI.
	 *
	 * @return string identifier for the type of bundle container.
	 */
	@Override
	public abstract String getType();

	/**
	 * Returns a path in the local file system to the root of the bundle container.
	 * <p>
	 * TODO: Ideally we won't need this method. Currently the PDE target platform preferences are
	 * based on a home location and additional locations, so we need the information.
	 * </p>
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	@Override
	public abstract String getLocation(boolean resolve) throws CoreException;

	/**
	 * Sets the resolution status to null.  This container will be considered unresolved.
	 */
	protected void clearResolutionStatus() {
		fResolutionStatus = null;
	}

	@Override
	public String[] getVMArguments() {
		for (Entry<AbstractBundleContainer, String[]> entry : hash.entrySet()) {
			if (entry.getKey().equals(this)) {
				return entry.getValue();
			}
		}
		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$
		if (fVMArgs == null) {
			try {
				FrameworkAdmin fwAdmin = PDECore.getDefault().acquireService(FrameworkAdmin.class);
				if (fwAdmin == null) {
					Bundle fwAdminBundle = Platform.getBundle(FWK_ADMIN_EQ);
					if (fwAdminBundle != null) {
						fwAdminBundle.start();
						fwAdmin = PDECore.getDefault().acquireService(FrameworkAdmin.class);
					}
				}
				if (fwAdmin != null) {
					Manipulator manipulator = fwAdmin.getManipulator();
					ConfigData configData = new ConfigData(null, null, null, null);

					String home = getLocation(true);
					manipulator.getLauncherData().setLauncher(new File(home, "eclipse")); //$NON-NLS-1$
					File installDirectory = new File(home);
//					if (Platform.getOS().equals(Platform.OS_MACOSX))
//						installDirectory = new File(installDirectory, "Eclipse.app/Contents/MacOS"); //$NON-NLS-1$
					manipulator.getLauncherData().setLauncherConfigLocation(new File(installDirectory, "eclipse.ini")); //$NON-NLS-1$
					manipulator.getLauncherData().setHome(new File(home));

					manipulator.setConfigData(configData);
					manipulator.load();
					fVMArgs = manipulator.getLauncherData().getJvmArgs();
				}
			} catch (BundleException e) {
				PDECore.log(e);
			} catch (CoreException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			}

		}
		if (fVMArgs == null || fVMArgs.length == 0) {
			hash.put(this, null);
			return null;
		}
		hash.put(this, fVMArgs);
		return fVMArgs;
	}

	/**
	 * Associate this bundle container with the given target.  This allows for the container and
	 * the target to share configuration information etc.
	 *
	 * @param target the target to which this container is being added.
	 */
	protected void associateWithTarget(ITargetDefinition target) {
		// Do nothing by default
	}

	@Override
	public String serialize() {
		// The default implementation returns null as most containers do not use the new UI
		return null;
	}

}
