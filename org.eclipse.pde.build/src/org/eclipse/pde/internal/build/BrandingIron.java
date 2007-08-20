/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import org.eclipse.pde.internal.swt.tools.IconExe;

/**
 *
 */
public class BrandingIron implements IXMLConstants {
	private static final String MARKER_NAME = "%EXECUTABLE_NAME%"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = "%BUNDLE_NAME%"; //$NON-NLS-1$
	private static final String ICON_NAME = "%ICON_NAME%"; //$NON-NLS-1$
	private static final String MARKER_KEY = "<key>CFBundleExecutable</key>"; //$NON-NLS-1$
	private static final String BUNDLE_KEY = "<key>CFBundleName</key>"; //$NON-NLS-1$
	private static final String ICON_KEY = "<key>CFBundleIconFile</key>"; //$NON-NLS-1$
	private static final String STRING_START = "<string>"; //$NON-NLS-1$
	private static final String STRING_END = "</string>"; //$NON-NLS-1$
	private static final String XDOC_ICON = "-Xdock:icon=../Resources/Eclipse.icns"; //$NON-NLS-1$

	private String[] icons = null;
	private String root;
	private String name;
	private String os = "win32"; //$NON-NLS-1$
	private boolean brandIcons = true;

	public void setName(String value) {
		name = value;
	}

	public void setIcons(String value) {
		icons = value.split(",\\s*"); //$NON-NLS-1$
		if (icons[0].startsWith("${")) { //$NON-NLS-1$
			if (icons.length > 1) {
				String[] temp = new String[icons.length - 1];
				System.arraycopy(icons, 1, temp, 0, temp.length);
				icons = temp;
			} else {
				icons = null;
			}
		}
	}

	public void setRoot(String value) {
		root = value;
	}

	public void brand() throws Exception {
		// if the name property is not set it will be ${launcher.name} so just bail.
		if (name.startsWith("${")) //$NON-NLS-1$
			return;

		if (icons == null || icons[0].startsWith("${")) //$NON-NLS-1$
			brandIcons = false;

		// if the root does not exists (happens in some packaging cases) or 
		// there is already a file with target name and we don't need to update its icons, don't do anything
		String testName = os.equals("win32") ? name + ".exe" : name; //$NON-NLS-1$ //$NON-NLS-2$
		if (!new File(root).exists() || (!brandIcons && new File(root, testName).exists()))
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
		if (brandIcons)
			copy(new File(icons[0]), new File(root, "icon.xpm")); //$NON-NLS-1$
	}

	private void brandSolaris() throws Exception {
		renameLauncher();
		if (brandIcons == false)
			return;

		for (int i = 0; i < icons.length; i++) {
			String icon = icons[i];
			if (icon.endsWith(".l.pm")) //$NON-NLS-1$
				copy(new File(icon), new File(root, name + ".l.pm")); //$NON-NLS-1$
			if (icon.endsWith(".m.pm")) //$NON-NLS-1$
				copy(new File(icon), new File(root, name + ".m.pm")); //$NON-NLS-1$
			if (icon.endsWith(".s.pm")) //$NON-NLS-1$
				copy(new File(icon), new File(root, name + ".s.pm")); //$NON-NLS-1$
			if (icon.endsWith(".t.pm")) //$NON-NLS-1$
				copy(new File(icon), new File(root, name + ".t.pm")); //$NON-NLS-1$
		}
	}

	private void brandMac() throws Exception {
		//Initially the files are in: <root>/Eclipse.app/ 
		//and they must appear in <root>/MyAppName.app/
		//Because java does not support the rename of a folder, files are copied.

		//Initialize the target folders
		String target = root + '/' + name + ".app/Contents"; //$NON-NLS-1$
		new File(target).mkdirs();
		new File(target + "/MacOS").mkdirs(); //$NON-NLS-1$
		new File(target + "/Resources").mkdirs(); //$NON-NLS-1$

		String initialRoot = root + "/Launcher.app/Contents"; //$NON-NLS-1$
		if (!new File(initialRoot).exists())
			initialRoot = root + "/Eclipse.app/Contents"; //$NON-NLS-1$
		copyMacLauncher(initialRoot, target);
		String iconName = ""; //$NON-NLS-1$
		File splashApp = new File(initialRoot, "Resources/Splash.app"); //$NON-NLS-1$
		if (brandIcons) {
			File icon = new File(icons[0]);
			iconName = icon.getName();
			copy(icon, new File(target + "/Resources/" + icon.getName())); //$NON-NLS-1$
			new File(initialRoot + "/Resources/Eclipse.icns").delete(); //$NON-NLS-1$
			if (!splashApp.exists())
				new File(initialRoot + "/Resources/").delete(); //$NON-NLS-1$
		}
		copyMacIni(initialRoot, target, iconName);
		modifyInfoPListFile(initialRoot, target, iconName);
		if (splashApp.exists()) {
			brandMacSplash(initialRoot, target, iconName);
		}

		File rootFolder = new File(initialRoot);
		rootFolder.delete();
		if (rootFolder.exists()) {
			//if the rootFolder still exists, its because there were other files that need to be moved over
			moveContents(rootFolder, new File(target));
		}
		rootFolder.getParentFile().delete();
	}

	/**
	 * Brand the splash.app Info.plist and  link or copy the mac launcher.
	 * It is assumed that the mac launcher has been branded already.
	 * @param initialRoot
	 * @param target
	 * @param iconName
	 */
	private void brandMacSplash(String initialRoot, String target, String iconName) {
		String splashContents = "/Resources/Splash.app/Contents"; //$NON-NLS-1$
		modifyInfoPListFile(initialRoot + splashContents, target + splashContents, iconName);

		//link the MacOS launcher for the splash app
		int result = -1;
		String osName = System.getProperty("os.name"); //$NON-NLS-1$
		if (osName != null && !osName.startsWith("Windows")) { //$NON-NLS-1$
			try {
				String[] command = new String[] {"ln", "-sf", "../../../MacOS/" + name, "MacOS/" + name}; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				File linkDir = new File(target, splashContents);
				Process proc = Runtime.getRuntime().exec(command, null, linkDir);
				result = proc.waitFor();
			} catch (IOException e) {
				// ignore
			} catch (InterruptedException e) {
				// ignore
			}
		}

		if (result != 0) {
			//ln failed, or we are on windows, just copy the executable instead
			File macOSDir = new File(target, "MacOS"); //$NON-NLS-1$
			File splashMacOSDir = new File(target, splashContents + "/MacOS"); //$NON-NLS-1$
			splashMacOSDir.mkdirs();
			try {
				File targetFile = new File(splashMacOSDir, name);
				copy(new File(macOSDir, name), targetFile);
				try {
					Runtime.getRuntime().exec(new String[] {"chmod", "755", targetFile.getAbsolutePath()}); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (IOException e) {
					// ignore
				}
			} catch (IOException e) {
				System.out.println("Could not copy macosx splash launcher"); //$NON-NLS-1$
			}
		}
	}

	private void moveContents(File source, File target) {
		if (!source.exists())
			return;

		try {
			if (source.getCanonicalFile().equals(target.getCanonicalFile()))
				return;
		} catch (IOException e) {
			System.out.println("Could not copy macosx resources."); //$NON-NLS-1$
			return;
		}

		target.getParentFile().mkdirs();
		if (source.isDirectory()) {
			target.mkdirs();
			File[] contents = source.listFiles();
			for (int i = 0; i < contents.length; i++) {
				File dest = new File(target, contents[i].getName());
				if (contents[i].isFile())
					contents[i].renameTo(dest);
				else
					moveContents(contents[i], dest);
			}
			source.delete();
		} else {
			source.renameTo(target);
		}
	}

	private void brandWindows() throws Exception {
		File templateLauncher = new File(root, "launcher.exe"); //$NON-NLS-1$
		if (!templateLauncher.exists())
			templateLauncher = new File(root, "eclipse.exe"); //$NON-NLS-1$
		if (brandIcons) {
			String[] args = new String[icons.length + 1];
			args[0] = templateLauncher.getAbsolutePath();
			System.arraycopy(icons, 0, args, 1, icons.length);
			IconExe.main(args);
		}
		templateLauncher.renameTo(new File(root, name + ".exe")); //$NON-NLS-1$
	}

	private void renameLauncher() {
		if (!new File(root, "launcher").renameTo(new File(root, name))) //$NON-NLS-1$
			new File(root, "eclipse").renameTo(new File(root, name)); //$NON-NLS-1$
	}

	private void copyMacLauncher(String initialRoot, String target) {
		String targetLauncher = target + "/MacOS/"; //$NON-NLS-1$
		File launcher = new File(initialRoot + "/MacOS/launcher"); //$NON-NLS-1$
		File eclipseLauncher = new File(initialRoot + "/MacOS/eclipse"); //$NON-NLS-1$
		if (!launcher.exists()) {
			launcher = eclipseLauncher;
		} else if (eclipseLauncher.exists()) {
			//we may actually have both if exporting from the mac
			eclipseLauncher.delete();
		}
		File targetFile = new File(targetLauncher, name);
		try {
			if (targetFile.getCanonicalFile().equals(launcher.getCanonicalFile())) {
				try {
					//Force the executable bit on the exe because it has been lost when copying the file
					Runtime.getRuntime().exec(new String[] {"chmod", "755", targetFile.getAbsolutePath()}); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (IOException e) {
					//ignore
				}
				return;
			}
			copy(launcher, targetFile);
		} catch (IOException e) {
			System.out.println("Could not copy macosx launcher"); //$NON-NLS-1$
			return;
		}
		try {
			//Force the executable bit on the exe because it has been lost when copying the file
			Runtime.getRuntime().exec(new String[] {"chmod", "755", targetFile.getAbsolutePath()}); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			//ignore
		}
		launcher.delete();
		launcher.getParentFile().delete();
	}

	private void copyMacIni(String initialRoot, String target, String iconName) {
		File brandedIni = new File(initialRoot, "/MacOS/" + name + ".ini"); //$NON-NLS-1$ //$NON-NLS-2$

		File ini = new File(initialRoot, "/MacOS/eclipse.ini"); //$NON-NLS-1$
		if (!ini.exists() && !brandedIni.exists())
			return;

		if (brandedIni.exists() && ini.exists()) {
			//take the one that is already branded
			ini.delete();
			ini = brandedIni;
		}

		StringBuffer buffer;
		try {
			buffer = readFile(ini);
			ini.delete();
		} catch (IOException e) {
			System.out.println("Impossible to brand ini file"); //$NON-NLS-1$
			return;
		}

		if (iconName.length() > 0) {
			int xdoc = scan(buffer, 0, XDOC_ICON);
			if (xdoc != -1) {
				String icns = XDOC_ICON.replaceFirst("Eclipse.icns", iconName); //$NON-NLS-1$
				buffer.replace(xdoc, xdoc + XDOC_ICON.length(), icns);
			}
		}

		try {
			File targetFile = new File(target, "/MacOS/" + name + ".ini"); //$NON-NLS-1$//$NON-NLS-2$
			transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(targetFile));
		} catch (FileNotFoundException e) {
			System.out.println("Impossible to brand ini file"); //$NON-NLS-1$
			return;
		} catch (IOException e) {
			System.out.println("Impossible to brand ini file"); //$NON-NLS-1$
			return;
		}
	}

	private void modifyInfoPListFile(String initialRoot, String targetRoot, String iconName) {
		File infoPList = new File(initialRoot, "Info.plist"); //$NON-NLS-1$
		StringBuffer buffer;
		try {
			buffer = readFile(infoPList);
		} catch (IOException e) {
			System.out.println("Impossible to brand info.plist file"); //$NON-NLS-1$
			return;
		}
		int exePos = scan(buffer, 0, MARKER_NAME);
		if (exePos != -1)
			buffer.replace(exePos, exePos + MARKER_NAME.length(), name);
		else {
			exePos = scan(buffer, 0, MARKER_KEY);
			if (exePos != -1) {
				int start = scan(buffer, exePos + MARKER_KEY.length(), STRING_START);
				int end = scan(buffer, start + STRING_START.length(), STRING_END);
				if (start > -1 && end > start) {
					buffer.replace(start + STRING_START.length(), end, name);
				}
			}
		}

		int bundlePos = scan(buffer, 0, BUNDLE_NAME);
		if (bundlePos != -1)
			buffer.replace(bundlePos, bundlePos + BUNDLE_NAME.length(), name);
		else {
			exePos = scan(buffer, 0, BUNDLE_KEY);
			if (exePos != -1) {
				int start = scan(buffer, exePos + BUNDLE_KEY.length(), STRING_START);
				int end = scan(buffer, start + STRING_START.length(), STRING_END);
				if (start > -1 && end > start) {
					buffer.replace(start + STRING_START.length(), end, name);
				}
			}
		}

		int iconPos = scan(buffer, 0, ICON_NAME);
		if (iconPos != -1)
			buffer.replace(iconPos, iconPos + ICON_NAME.length(), iconName);
		else {
			exePos = scan(buffer, 0, ICON_KEY);
			if (exePos != -1) {
				int start = scan(buffer, exePos + ICON_KEY.length(), STRING_START);
				int end = scan(buffer, start + STRING_START.length(), STRING_END);
				if (start > -1 && end > start) {
					buffer.replace(start + STRING_START.length(), end, iconName);
				}
			}
		}

		File target = null;
		try {
			target = new File(targetRoot, "Info.plist"); //$NON-NLS-1$
			target.getParentFile().mkdirs();
			transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(target));
		} catch (FileNotFoundException e) {
			System.out.println("Impossible to brand info.plist file"); //$NON-NLS-1$
			return;
		} catch (IOException e) {
			System.out.println("Impossible to brand info.plist file"); //$NON-NLS-1$
			return;
		}
		try {
			if (!infoPList.getCanonicalFile().equals(target.getCanonicalFile()))
				infoPList.delete();
		} catch (IOException e) {
			//ignore
		}
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
			in = new BufferedInputStream(new FileInputStream(source));
			out = new BufferedOutputStream(new FileOutputStream(destination));
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

	private int scan(StringBuffer buf, int start, String targetName) {
		return scan(buf, start, new String[] {targetName});
	}

	private int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (int j = 0; j < targets.length; j++) {
				if (i < buf.length() - targets[j].length()) {
					String match = buf.substring(i, i + targets[j].length());
					if (targets[j].equalsIgnoreCase(match))
						return i;
				}
			}
		}
		return -1;
	}

	private StringBuffer readFile(File targetName) throws IOException {
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(targetName)));
		StringBuffer result = new StringBuffer();
		char[] buf = new char[4096];
		int count;
		try {
			count = reader.read(buf, 0, buf.length);
			while (count != -1) {
				result.append(buf, 0, count);
				count = reader.read(buf, 0, buf.length);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore exceptions here
			}
		}
		return result;
	}

	private void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public void setOS(String value) {
		os = value;
	}
}
