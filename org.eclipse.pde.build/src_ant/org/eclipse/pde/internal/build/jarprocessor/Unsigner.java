/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.jarprocessor;
import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.*;

/**
 * This class removes the signature files from a jar and clean up the manifest.   
 */
public class Unsigner {
	private static final String META_INF = "META-INF"; //$NON-NLS-1$
	private static final String DSA_EXT = ".DSA"; //$NON-NLS-1$
	private static final String RSA_EXT = ".RSA"; //$NON-NLS-1$
	private static final String SF_EXT = ".SF"; //$NON-NLS-1$
	private static final String META_INF_PREFIX = META_INF + '/';
	
	private String[] signers;
	private File jarFile;
	private boolean keepManifestContent = false;
	
	private boolean isSigned(File file) {
		ZipFile jar = null;
		try {
			jar = new ZipFile(file);

			if (signers != null) {
				for (int i = 0; i < signers.length; i++) {
					if (jar.getEntry((META_INF_PREFIX + signers[i] + SF_EXT).toUpperCase()) != null)
						return true;
				}
			} else {
				Enumeration entries = jar.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();
					if(entryName.endsWith(SF_EXT) && entryName.startsWith(META_INF))
						return true;
				}
			}
			return false;
		} catch (ZipException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			if (jar != null)
				try {
					jar.close();
				} catch (IOException e) {
					//Ignore
				}
		}
	}
	
	public void execute() {
		processJar(jarFile);
	}
	
	private void processJar(File inputFile) {
		if (!isSigned(inputFile))
			return;

		try {
			ZipInputStream input = new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
			File outputFile = File.createTempFile("removing.", ".signature", inputFile.getParentFile()); //$NON-NLS-1$ //$NON-NLS-2$
			ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

			ZipEntry inputZe = input.getNextEntry();
			byte[] b = new byte[8192];
			while (inputZe != null) {
				byte remove = shouldRemove(inputZe);
				if (remove == 2) {
					inputZe = input.getNextEntry();
					continue;
				}

				//copy the file or modify the manifest.mf
				if (remove == 1) {
					output.putNextEntry(new ZipEntry(inputZe.getName()));
					Manifest m = new Manifest();
					m.read(input);
					m.getEntries().clear(); //This is probably not subtle enough
					m.write(output);
				} else {
					output.putNextEntry(inputZe);
					while (input.available() != 0) {
						int read = input.read(b);
						if (read != -1)
							output.write(b, 0, read);
					}
				}
				output.closeEntry();
				input.closeEntry();

				inputZe = input.getNextEntry();
			}
			output.close();
			input.close();
			inputFile.delete();
			outputFile.renameTo(inputFile);
		} catch (FileNotFoundException e) {
			//this can't happen we have checked before
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private byte shouldRemove(ZipEntry entry) {
		String entryName = entry.getName();
		if(keepManifestContent == false && entryName.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
			return 1;
		}
		if (signers != null) {
			for (int i = 0; i < signers.length; i++) {
				if (entryName.equalsIgnoreCase(META_INF_PREFIX + signers[i] + SF_EXT) || entryName.equalsIgnoreCase(META_INF_PREFIX + signers[i] + RSA_EXT) || entryName.equalsIgnoreCase(META_INF_PREFIX + signers[i] + DSA_EXT))
					return 2;
			}
		} else {
			if (entryName.startsWith(META_INF) && (entryName.endsWith(SF_EXT) || entryName.endsWith(RSA_EXT) || entryName.endsWith(DSA_EXT)))
				return 2;
		}
		return 0;
	}
	
	public void setRemoveSigners(String[] fileName) {
		signers = fileName;
	}
	
	public void setJar(File file) {
		jarFile = file;
	}
	
	public void setKeepManifestEntries(boolean keep) {
		keepManifestContent = keep;
	}
}
