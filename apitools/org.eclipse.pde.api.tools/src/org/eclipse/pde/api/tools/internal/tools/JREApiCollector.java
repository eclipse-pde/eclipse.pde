/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.ClassReader;

public class JREApiCollector {
	private static boolean DEBUG = false;
	private static final String PROFILE = "-profile"; //$NON-NLS-1$
	private static final String JRE_LOCATION = "-jre"; //$NON-NLS-1$
	private static final String OUTPUT = "-output"; //$NON-NLS-1$
	private static final String PACKAGE_PROPERTY = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	static {
		DEBUG = DEBUG || System.getProperty("DEBUG") != null; //$NON-NLS-1$
	}
	String jreLocation;
	String jreProfile;
	String output;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JREApiCollector apiCollector = new JREApiCollector();
		apiCollector.configure(args);
		apiCollector.run();
	}

	public void configure(String[] args) {
		// command line processing
		/*
		 * Recognized options:
		 * -jre
		 * -output
		 * -profile
		 */
		final int OPTION_DEFAULT = 0;
		final int OPTION_PROFILE  = 1;
		final int OPTION_JRE_LOCATION = 2;
		final int OPTION_OUTPUT = 3;
		int mode = OPTION_DEFAULT;
		for (int i = 0, max = args.length; i < max; i++) {
			String currentArg = args[i];
			switch (mode) {
				case OPTION_DEFAULT:
					if (PROFILE.equals(currentArg)) {
						mode = OPTION_PROFILE;
						continue;
					}
					if (JRE_LOCATION.equals(currentArg)) {
						mode = OPTION_JRE_LOCATION;
						continue;
					}
					if (OUTPUT.equals(currentArg)) {
						mode = OPTION_OUTPUT;
						continue;
					}
					System.err.println("Unknown option : " + currentArg); //$NON-NLS-1$
					break;
				case OPTION_PROFILE:
					if (this.jreProfile != null) {
						throw new IllegalArgumentException("Cannot set the jre profile value more than once"); //$NON-NLS-1$
					}
					this.jreProfile = currentArg;
					mode = OPTION_DEFAULT;
					break;
				case OPTION_OUTPUT:
					if (this.output != null) {
						throw new IllegalArgumentException("Cannot set the output value more than once"); //$NON-NLS-1$
					}
					this.output = currentArg;
					mode = OPTION_DEFAULT;
					break;
				case OPTION_JRE_LOCATION :
					if (this.jreLocation != null) {
						throw new IllegalArgumentException("Cannot set the jre location value more than once"); //$NON-NLS-1$
					}
					this.jreLocation = currentArg;
					mode = OPTION_DEFAULT;
					break;
			}
		}
		if (this.jreProfile == null || this.jreLocation == null || this.output == null) {
			printUsage();
			throw new IllegalArgumentException("Missing arguments"); //$NON-NLS-1$
		}
	}

	private void printUsage() {
		System.out.println("Usage: JREApiCollector -profile <path> -jre <path to jre home directory -output <path to output file>"); //$NON-NLS-1$
	}

	private void run() {
		File profileFile = new File(this.jreProfile);
		if (!profileFile.exists()) {
			throw new IllegalArgumentException(NLS.bind(Messages.noProfile, this.jreProfile));
		}
		File outputFile = new File(this.output);
		if (outputFile.exists()) {
			if (!outputFile.delete()) {
				throw new IllegalArgumentException(NLS.bind(Messages.cannotDeleteOutputFile, this.output));
			}
		} else {
			File parent = outputFile.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) {
				throw new IllegalArgumentException(NLS.bind(Messages.cannotCreateOutputFileParent, parent.getAbsolutePath()));
			}
		}
		Properties properties = new Properties();
		try {
			FileInputStream stream = new FileInputStream(profileFile);
			properties.load(stream);
			stream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String property = properties.getProperty(PACKAGE_PROPERTY);
		if (property == null) {
			throw new IllegalStateException(NLS.bind(Messages.cannotCreateOutputFileParent, PACKAGE_PROPERTY));
		}
		if (DEBUG) System.out.println(property);
		Set packagesNames = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(property, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			packagesNames.add(token.trim());
		}
		File jreHome = new File(this.jreLocation);
		if (!jreHome.exists()) {
			throw new IllegalArgumentException(NLS.bind(Messages.noJRELocation, this.jreLocation));
		}
		File[] files = Util.getAllFiles(jreHome, new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory() || Util.isArchive(pathname.getName());
			}
		});
		if (files != null && files.length != 0) {
			// directly process all entries
			Set collector = new HashSet();
			for (int i = 0, max = files.length; i < max; i++) {
				collectClassFiles(files[i], packagesNames, collector);
			}
			List list = new ArrayList();
			list.addAll(collector);
			Collections.sort(list);
			FileWriter fileWriter = null;
			PrintWriter writer = null;
			try {
				fileWriter = new FileWriter(this.output);
				writer = new PrintWriter(fileWriter);
				for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
					writer.println(iterator.next());
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
			
			Set set = new HashSet();
			set.addAll(collector);
			if (DEBUG) {
				System.out.println(NLS.bind(Messages.numberOfElements, Integer.toString(collector.size())));
				System.out.println(NLS.bind(Messages.numberOfUniqueElements, Integer.toString(set.size())));
			}
		}
	}

	private void collectClassFiles(File file, Set packagesNames, Set collector) {
		if (DEBUG) System.out.println(file.getAbsolutePath());
		ZipInputStream inputStream = null;
		try {
			inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
			processArchiveEntry(inputStream, packagesNames, collector);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void processArchiveEntry(ZipInputStream inputStream, Set packageNames, Set collector) throws IOException {
		ZipEntry zipEntry = inputStream.getNextEntry();
		while (zipEntry != null) {
			String name = zipEntry.getName();
			String packageName = getPackageName(name);
			if (Util.isClassFile(name) && includesPackage(packageNames, packageName)) {
				extractApis(inputStream, collector);
			}
			inputStream.closeEntry();
			zipEntry = inputStream.getNextEntry();
		}
	}

	private boolean includesPackage(Set packageNames, String packageName) {
		return packageNames.contains(packageName) || packageName.startsWith("java."); //$NON-NLS-1$
	}

	private void extractApis(InputStream inputStream, Set collector) throws IOException {
		byte[] contents = Util.getInputStreamAsByteArray(inputStream, -1);
		ClassReader classReader = new ClassReader(contents);
		ApiExtractorAdapter visitor = new ApiExtractorAdapter(collector);
		classReader.accept(visitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
	}

	private String getPackageName(String name) {
		int index = name.lastIndexOf('/');
		if (index != -1) {
			return name.substring(0, index).replace('/', '.');
		}
		return Util.EMPTY_STRING;
	}
}
