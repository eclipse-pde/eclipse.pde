package $packageName$;

public interface Dictionary {
	
    /**
     * Returns the language of the dictionary
     *
     * @return the language of the dictionary
     */
    public String getLanguage();
	
    /**
     * Check for the existence of a word in the dictionary
     * 
     * @param word the word to be checked.
     * @return true if the word is in the dictionary
     */
    public boolean check(String word);


}
