/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com>  - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *******************************************************************************/
package bndtools.wizards.project;

public enum ProjectTemplateParam {

	PROJECT_NAME("projectName"),
	VERSION("version"),
	SRC_DIR("srcDir"),
	BIN_DIR("binDir"),
	TEST_SRC_DIR("testSrcDir"),
	TEST_BIN_DIR("testBinDir"),
	BASE_PACKAGE_NAME("basePackageName"),
	BASE_PACKAGE_DIR("basePackageDir"),
	TARGET_DIR("targetDir"),
	VERSION_OUTPUTMASK("outputmask"),
	JAVA_LEVEL("javaLevel");

	private final String string;

	private ProjectTemplateParam(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}

	public static String[] valueStrings() {
		ProjectTemplateParam[] vals = values();
		String[] strings = new String[vals.length];
		for (int i = 0; i < vals.length; i++) {
			strings[i] = vals[i].getString();
		}
		return strings;
	}

}
