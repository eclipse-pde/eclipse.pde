package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.model.IProcess;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LaunchesListener implements ILaunchesListener {
	private ArrayList managedLaunches;

	public LaunchesListener() {
		managedLaunches = new ArrayList();
	}
	
	public void manage(ILaunch launch) {
		//if (managedLaunches)
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchesRemoved(ILaunch[] launches) {
		update(launches);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchesAdded(ILaunch[] launches) {
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchesChanged(ILaunch[] launches) {
		checkForRestart(launches);
		update(launches);
	}

	private void update(ILaunch[] launches) {
		for (int i = 0; i < launches.length; i++) {
			ILaunch launch = launches[i];
			if (managedLaunches.contains(launch)) {
				if (launch.isTerminated()) {
					managedLaunches.remove(launch);
				}
			}
		}
		if (managedLaunches.size() == 0) {
			hookListener(false);
		}
	}

	private void hookListener(boolean add) {
		ILaunchManager launchManager =
			DebugPlugin.getDefault().getLaunchManager();
		if (add)
			launchManager.addLaunchListener(this);
		else
			launchManager.removeLaunchListener(this);
	}

	private void checkForRestart(ILaunch[] launches) {
		for (int i = 0; i < launches.length; i++) {
			ILaunch launch = launches[i];
			checkForRestart(launch);
		}
	}

	private void checkForRestart(ILaunch launch) {
		if (launch.isTerminated()) {
			IProcess[] processes = launch.getProcesses();
			for (int i = 0; i < processes.length; i++) {
				IProcess process = processes[i];
				try {
					if (process.getExitValue() == 23) {
						doRestart(launch);
						return;
					}
				} catch (DebugException e) {
				}
			}
		}
	}

	private void doRestart(ILaunch launch) {
	}
}
