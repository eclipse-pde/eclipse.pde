/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import org.apache.tools.ant.BuildException;
import org.eclipse.swt.tools.internal.IconExe;

/**
 *
 */
public class BrandingIron {

	private String[] icons;
	private String root;
	private String name;
	private String os = "win32";

	public void setName(String value) {
		name = value;
	}

	public void setIcons(String value) {
		icons = value.split(",");
	}

	public void setRoot(String value) {
		root = value;
	}

	public void execute() throws BuildException {
		try {
			brand();
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	public void brand(String launcher, String[] images, String[] platform) throws Exception {
		root = launcher;
		icons = images;
		os = platform[1];
		brand();
	}

	public void brand() throws Exception {
		// if the name property is not set it will be ${launcher.name} so just bail.
		if (name.equals("${launcher.name}"))
			return;
		if ("win32".equals(os)) //$NON-NLS-1$
			brandWindows();
		if ("linux".equals(os)) //$NON-NLS-1$
			brandLinux();
		if ("solaris".equals(os)) //$NON-NLS-1$
			brandSolaris();
		if ("macosx".equals(os)) //$NON-NLS-1$
			brandMac();
		if ("aix".equals(os)) //$NON-NLS-1$
			brandAIX();
		if ("hpux".equals(os)) //$NON-NLS-1$
			brandHPUX();
	}

	private void brandAIX() {
		renameLauncher();
	}

	private void brandHPUX() {
		renameLauncher();
	}

	private void brandLinux() throws Exception {
		renameLauncher();
		if (icons.length > 0)
			copy(new File(icons[0]), new File(root, "icon.xpm"));
	}

	private void brandSolaris() throws Exception {
		renameLauncher();
		for (int i = 0; i < icons.length; i++) {
			String icon = icons[i];
			if (icon.endsWith(".l.pm")) //$NON-NLS-1$
				copy(new File(icon), new File(root, name + ".l.pm")); //$NON-NLS-1$
			if (icon.endsWith(".m.pm")) //$NON-NLS-1$
				copy(new File(icon), new File(root, name + ".m.pm"));
			if (icon.endsWith(".s.pm"))
				copy(new File(icon), new File(root, name + ".s.pm"));
			if (icon.endsWith(".t.pm"))
				copy(new File(icon), new File(root, name + ".t.pm"));
		}
	}

	private void brandMac() throws Exception {
		renameLauncher();
	}

	private void brandWindows() throws Exception {
		File templateLauncher = new File(root, "eclipse.exe");
		String[] args = new String[icons.length + 1];
		args[0] = templateLauncher.getAbsolutePath();
		System.arraycopy(icons, 0, args, 1, icons.length);
		IconExe.main(args);
		templateLauncher.renameTo(new File(root, name + ".exe"));
	}

	private void renameLauncher() {
		new File(root, "eclipse").renameTo(new File(root, name));
	}

	/**
	 * Transfers all available bytes from the given input stream to the given output stream. 
	 * Regardless of failure, this method closes both streams.
	 * @throws IOException 
	 */
	public void copy(File source, File destination) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(destination);
			final byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				bytesRead = in.read(buffer);
				if (bytesRead == -1)
					break;
				out.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				if (in != null)
					in.close();
			} finally {
				if (out != null)
					out.close();
			}
		}
	}

}
