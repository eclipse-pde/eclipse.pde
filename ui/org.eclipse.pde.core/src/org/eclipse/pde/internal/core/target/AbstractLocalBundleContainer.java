package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * TODO Doc and verify need for abstract class
 */
public abstract class AbstractLocalBundleContainer implements IBundleContainer {

	/**
	 * The Java VM Arguments specified by this bundle container 
	 */
	private String[] fVMArgs;

	/**
	 * Returns a path in the local file system to the root of the bundle container.
	 * 
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	public abstract String getLocation(boolean resolve) throws CoreException;

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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getVMArguments()
	 */
	public String[] getVMArguments() {
		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$

		if (fVMArgs == null) {
			try {
				FrameworkAdmin fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
				if (fwAdmin == null) {
					Bundle fwAdminBundle = Platform.getBundle(FWK_ADMIN_EQ);
					fwAdminBundle.start();
					fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
				}
				Manipulator manipulator = fwAdmin.getManipulator();
				ConfigData configData = new ConfigData(null, null, null, null);

				String home = getLocation(true);
				manipulator.getLauncherData().setLauncher(new File(home, "eclipse")); //$NON-NLS-1$
				File installDirectory = new File(home);
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					installDirectory = new File(installDirectory, "Eclipse.app/Contents/MacOS"); //$NON-NLS-1$
				manipulator.getLauncherData().setLauncherConfigLocation(new File(installDirectory, "eclipse.ini")); //$NON-NLS-1$
				manipulator.getLauncherData().setHome(new File(home));

				manipulator.setConfigData(configData);
				manipulator.load();
				fVMArgs = manipulator.getLauncherData().getJvmArgs();
			} catch (BundleException e) {
				PDECore.log(e);
			} catch (CoreException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			}

		}
		if (fVMArgs == null || fVMArgs.length == 0) {
			return null;
		}
		return fVMArgs;
	}

}
