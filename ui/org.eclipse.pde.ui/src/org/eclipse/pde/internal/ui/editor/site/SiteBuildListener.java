package org.eclipse.pde.internal.ui.editor.site;

import java.io.*;

import org.apache.tools.ant.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteBuildListener implements BuildListener {
	private PrintWriter writer;

	/**
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		try {
			File staticFile = FeatureBuildOperation.getDefault().getLogFile();
			//File file = FeatureBuildOperation.getDefault().getLogFile();
			File file = new File("d:\\buildlog.txt");
			FileOutputStream fos = new FileOutputStream(file);
			writer = new PrintWriter(fos, true);
		} catch (IOException e) {
		}

		//System.out.println("Build started.");
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#buildFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void buildFinished(BuildEvent event) {
		if (writer!=null) {
			writer.println("Build finished.");
			writer.close();
		}
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
		if (writer!=null) writer.println("Target started: "+event.getTarget().getName());
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void targetFinished(BuildEvent event) {
		if (writer!=null) writer.println("Target finished: "+event.getTarget().getName());
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
		if (writer!=null) {
			Task task = event.getTask();
			String description = task.getDescription();
			if (description==null) description = task.getTaskName();
			writer.println("Task started: "+description);
		}
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void taskFinished(BuildEvent event) {
		if (writer!=null) {
			Task task = event.getTask();
			String description = task.getDescription();
			if (description==null) description = task.getTaskName();
			writer.println("Task finished: "+description);
		}
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#messageLogged(org.apache.tools.ant.BuildEvent)
	 */
	public void messageLogged(BuildEvent event) {
		int priority = event.getPriority();
		if (priority==Project.MSG_ERR || priority==Project.MSG_WARN) {
			if (writer!=null) writer.println("Message logged: "+event.getMessage());
		}
	}
}
