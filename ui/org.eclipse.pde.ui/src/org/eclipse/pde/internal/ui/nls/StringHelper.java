package org.eclipse.pde.internal.ui.nls;

public class StringHelper {
	
	protected static String preparePropertiesString(String s, char[] newLine) {
		if (s != null) {
			int length = s.length();
			StringBuffer sb = new StringBuffer(length);
			for (int i = 0; i < length; i++) {
				char c = s.charAt(i);
				int nlLength = newLine.length;
				if (i + nlLength < length) {
					boolean notNewLine = false;
					for (int j = 0; j < nlLength; j++) {
						if (s.charAt(i + j) != newLine[j])
							notNewLine = true;
					}
					if (!notNewLine)
						sb.append("\\"); //$NON-NLS-1$
				}
				sb.append(c);
			}
			return sb.toString();
		}
		return null;
	}
	
	protected static String unwindEscapeChars(String s) {
		if (s != null) {
			int length = s.length();
			StringBuffer sb = new StringBuffer(length);
			for (int i = 0; i < length; i++) {
				char c = s.charAt(i);
				sb.append(getUnwoundString(c));
			}
			return sb.toString();
		}
		return null;
	}

	protected static String getUnwoundString(char c) {
		switch (c) {
			case '\b' :
				return "\\b";//$NON-NLS-1$
			case '\t' :
				return "\\t";//$NON-NLS-1$
			case '\n' :
				return "\\n";//$NON-NLS-1$
			case '\f' :
				return "\\f";//$NON-NLS-1$	
			case '\r' :
				return "\\r";//$NON-NLS-1$
			case '\\' :
				return "\\\\";//$NON-NLS-1$
		}
		return String.valueOf(c);
	}

	protected static String windEscapeChars(String s) {
		if (s == null)
			return null;

		char aChar;
		int len= s.length();
		StringBuffer outBuffer= new StringBuffer(len);

		for (int x= 0; x < len;) {
			aChar= s.charAt(x++);
			if (aChar == '\\') {
				aChar= s.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value= 0;
					for (int i= 0; i < 4; i++) {
						aChar= s.charAt(x++);
						switch (aChar) {
							case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
								value= (value << 4) + aChar - '0';
								break;
							case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
								value= (value << 4) + 10 + aChar - 'a';
								break;
							case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
								value= (value << 4) + 10 + aChar - 'A';
								break;
							default:
								throw new IllegalArgumentException("Malformed \\uxxxx encoding."); //$NON-NLS-1$
						}
					}
					outBuffer.append((char) value);
				} else {
					if (aChar == 't') {
						outBuffer.append('\t');
					} else {
						if (aChar == 'r') {
							outBuffer.append('\r');
						} else {
							if (aChar == 'n') {
								outBuffer.append('\n');
							} else {
								if (aChar == 'f') {
									outBuffer.append('\f');
								} else {
									outBuffer.append(aChar);
								}
							}
						}
					}
				}
			} else
				outBuffer.append(aChar);
		}
		return outBuffer.toString();
	}
	
    protected static boolean isValidLocalization(String name) {
        if (name.length() <= 0) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c)
                    && (c < '0' || '9' < c) && c != '_' && c != '-') {
                return false;
            }
        }
        return true;
    }
}
