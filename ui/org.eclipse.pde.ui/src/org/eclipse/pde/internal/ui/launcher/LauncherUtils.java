package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class LauncherUtils {
	
	public static IVMInstall[] getAllVMInstances() {
		ArrayList res = new ArrayList();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstall[] installs = types[i].getVMInstalls();
			for (int k = 0; k < installs.length; k++) {
				res.add(installs[k]);
			}
		}
		return (IVMInstall[]) res.toArray(new IVMInstall[res.size()]);
	}
	
	public static String[] getVMInstallNames() {
		IVMInstall[] installs = getAllVMInstances();
		String[] names = new String[installs.length];
		for (int i = 0; i < installs.length; i++) {
			names[i] = installs[i].getName();
		}
		return names;
	}
	
	public static String getDefaultVMInstallName() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null)
			return install.getName();
		return null;
	}

	public static String getDefaultProgramArguments() {
		String os = TargetPlatform.getOS();
		String ws = TargetPlatform.getWS();
		String arch = TargetPlatform.getOSArch();
		String nl = TargetPlatform.getNL();
		return "-os " + os + " -ws " + ws + " -arch " + arch + " -nl " + nl;
	}
	
	public static String getDefaultWorkspace() {
		IPath path =
			PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
		return path.append("runtime-workspace").toOSString();
	}
	
	public static TreeSet parseDeselectedWSIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet deselected = new TreeSet();
		String ids = config.getAttribute(ILauncherSettings.WSPROJECT, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens())
				deselected.add(tok.nextToken());
		}
		return deselected;
	}
	
	public static TreeSet parseSelectedExtIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet selected = new TreeSet();
		String ids = config.getAttribute(ILauncherSettings.EXTPLUGINS, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int loc = token.lastIndexOf(',');
				if (loc == -1) {
					selected.add(token);
				} else if (token.charAt(loc + 1) == 't') {
					selected.add(token.substring(0, loc));
				}
			}
		}
		return selected;
	}

}
