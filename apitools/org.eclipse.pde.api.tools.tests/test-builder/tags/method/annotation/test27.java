package a.b.c;

/**
 * Test unsupported @noreference tag on methods in outer / inner annotations
 */
public @interface test27 {
	@interface inner {
		/**
		 * @nooverride
		 * @noimplement
		 * @noinstantiate
		 * @noextend
		 * @noreference
		 * @return
		 */
		public String m1() default "one";
		
		@interface inner2 {
			/**
			 * @nooverride
			 * @noimplement
			 * @noinstantiate
			 * @noextend
			 * @noreference
			 * @return
			 */
			public String m1() default "one";
		}
	}
}

@interface outer {
	/**
	 * @nooverride
	 * @noimplement
	 * @noinstantiate
	 * @noextend
	 * @noreference
	 * @return
	 */
	public String m1() default "one";
}