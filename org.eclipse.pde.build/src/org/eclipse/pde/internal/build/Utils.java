package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * General utility class.
 */
public final class Utils {

/**
 * Convert a list of tokens into an array. The list separator has to be specified.
 */
public static String[] getArrayFromString(String list, String separator) {
	if (list == null || list.trim().equals(""))
		return new String[0];
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return (String[]) result.toArray(new String[result.size()]);
}

/**
 * convert a list of comma-separated tokens into an array
 */
public static String[] getArrayFromString(String list) {
	return getArrayFromString(list, ",");
}

}
