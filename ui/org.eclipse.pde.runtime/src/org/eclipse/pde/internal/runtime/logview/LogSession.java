package org.eclipse.pde.internal.runtime.logview;

import java.io.*;

import org.eclipse.core.boot.BootLoader;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class LogSession {
	private String sessionData;

	/**
	 * Constructor for LogSession.
	 */
	public LogSession() {
	}

	public String getSessionData() {
		return sessionData;
	}

	void setSessionData(String data) {
		this.sessionData = data;
	}

	void createSessionData() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter, true);
		// Write out certain values found in System.getProperties()
		try {
			String key = "java.fullversion"; //$NON-NLS-1$
			String value = System.getProperty(key);
			if (value == null) {
				key = "java.version"; //$NON-NLS-1$
				value = System.getProperty(key);
				writer.println(key + "=" + value); //$NON-NLS-1$
				key = "java.vendor"; //$NON-NLS-1$
				value = System.getProperty(key);
				writer.println(key + "=" + value); //$NON-NLS-1$
			} else {
				writer.println(key + "=" + value); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// If we're not allowed to get the values of these properties
			// then just skip over them.
		}

		// The Bootloader has some information that we might be interested in.
		writer.print("BootLoader constants: OS=" + BootLoader.getOS()); //$NON-NLS-1$
		writer.print(", ARCH=" + BootLoader.getOSArch()); //$NON-NLS-1$
		writer.print(", WS=" + BootLoader.getWS()); //$NON-NLS-1$
		writer.println(", NL=" + BootLoader.getNL()); //$NON-NLS-1$

		// Add the command-line arguments used to envoke the platform.
		String[] args = BootLoader.getCommandLineArgs();
		if (args != null && args.length > 0) {
			writer.print("Command-line arguments:"); //$NON-NLS-1$
			for (int i = 0; i < args.length; i++) {
				writer.print(" " + args[i]); //$NON-NLS-1$
			}
			writer.println();
		}
		sessionData = swriter.toString();
		try {
			swriter.close();
			writer.close();
		} catch (IOException e) {
		}
	}
}