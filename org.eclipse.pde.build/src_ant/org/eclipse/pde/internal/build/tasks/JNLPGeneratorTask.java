/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     G&H Softwareentwicklung GmbH - internationalization implementation (bug 150933)
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.util.Locale;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class JNLPGeneratorTask extends Task {

	private String feature;
	private String jnlp = null;
	private String codebase = null;
	private String j2se;
	private Locale locale = Locale.getDefault();
	private boolean generateOfflineAllowed = true;
	private String configs = null;

	/**
	 * The URL location of a feature.xml file.  This can be either a jar: URL to the feature.xml,
	 * or a file: url to the feature.
	 * @param value
	 */
	public void setFeature(String value) {
		feature = value;
	}

	/**
	 * The location the output jnlp file.  The value may be null (or simply not set).  If it is
	 * null then the output file will be placed beside the feature dir or jar (as appropriate)
	 * and its name will be derived from the feature id and version.  If this is set
	 * it must be a file path.  If it ends in a "/" or "\" then the output file is places in this
	 * directory with the file name derived as described.  Finally, if the value is a file name,
	 * the output is placed in that file.
	 * @param value the location to write the output or null if the default
	 * computed location is to be used.
	 */
	public void setJNLP(String value) {
		jnlp = value;
	}

	/**
	 * The codebase of the output.  This shoudl be a URL that will be used as the
	 * root of all relative URLs in the output.  Typically this is the root of the application
	 * deployment website.
	 * @param value the URL root of for the application content.
	 */
	public void setCodebase(String value) {
		codebase = value;
	}

	/**
	 * The j2se spec of the output
	 * @param value the JNLP j2se spec to use in the output.
	 */
	public void setJ2SE(String value) {
		j2se = value;
	}

	/**
	 * The locale in which the jnlp file generated should be translated into.
	 *The translation values are read from the feature_<locale>.properties file.
	 * @param nlsString - the locale in which to generate the jnlp files.
	 * */
	public void setLocale(String nlsString) {
		String[] strings = nlsString.split("_"); //$NON-NLS-1$
		if (nlsString.charAt(0) == '$')
			return;

		if (strings != null) {
			switch (strings.length) {
				case 1 :
					locale = new Locale(strings[0]);
					break;
				case 2 :
					locale = new Locale(strings[0], strings[1]);
					break;
				case 3 :
					locale = new Locale(strings[0], strings[1], strings[2]);
					break;
			}
		}
	}

	public void execute() throws BuildException {
		JNLPGenerator generator = new JNLPGenerator(feature, jnlp, codebase, j2se, locale, generateOfflineAllowed, configs);
		generator.process();
	}

	public void setGenerateOfflineAllowed(String generateOfflineAllowed) {
		if (generateOfflineAllowed.equalsIgnoreCase("false")) //$NON-NLS-1$
			this.generateOfflineAllowed = false;
		if (generateOfflineAllowed.equalsIgnoreCase("true")) //$NON-NLS-1$
			this.generateOfflineAllowed = false;
	}

	public void setConfigInfo(String configs) {
		this.configs = configs;
	}

}
