/*
 * Created on Nov 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * @author dejan
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class WaitCommand extends MacroCommand {
	public static final String TYPE = "wait";
	private static final WidgetIdentifier nullIdentifier = new WidgetIdentifier(new Path(""), new Path(""));
	
	private static class JobListener extends JobChangeAdapter {
		private int counter=0;
		private MessageDialog dialog;
		private IProgressMonitor monitor;
		private Thread t;
		
		public JobListener(IProgressMonitor monitor, Thread t, int number) {
			this.counter = number;
			this.monitor = monitor;
			this.t = t;
		}
		private synchronized void change(int increment) {
			this.counter += increment;
			if (counter==0) { 
				monitor.subTask("");
				synchronized (t) {
					t.interrupt();
				}
			}
		}
		public void running(IJobChangeEvent event) {
			change(1);
		}
		public void done(IJobChangeEvent event) {
			change(-1);
		}
	}

	public WaitCommand() {
		super(nullIdentifier);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.MacroCommand#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.MacroCommand#processEvent(org.eclipse.swt.widgets.Event)
	 */
	public void processEvent(Event e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<command type=\"");
		writer.print(getType());
		writer.print("\"");
		writer.println("/>");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.IPlayable#playback(org.eclipse.swt.widgets.Composite)
	 */
	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		if (parent.isDisposed())
			return false;
		IJobManager jobManager = Platform.getJobManager();
		int nrunning = getNumberOfRunningJobs(jobManager);
		if (nrunning==0) return true;
		String message = "Waiting for the background jobs...";
		JobListener listener = new JobListener(monitor, Thread.currentThread(), nrunning);
		jobManager.addJobChangeListener(listener);
		monitor.subTask(message);
		try {
			Thread.sleep(30000);
		}
		catch (InterruptedException e) {
		}
		jobManager.removeJobChangeListener(listener);
		return true;
	}
	private int getNumberOfRunningJobs(IJobManager manager) {
		int count = 0;
		Job[] jobs = manager.find(null);
		for (int i=0; i<jobs.length; i++) {
			if (jobs[i].getState()==Job.RUNNING)
				count++;
		}
		return count;
	}
}