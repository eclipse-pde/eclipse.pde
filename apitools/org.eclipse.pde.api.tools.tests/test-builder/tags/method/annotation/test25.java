

/**
 * Test unsupported @noreference tag on methods in outer / inner annotations
 */
public @interface test25 {
	@interface inner {
		/**
		 * @noreference
		 * @return
		 */
		public int m1();
		
		/**
		 * @noreference
		 * @return
		 */
		public abstract char m2();
		@interface inner2 {
			/**
			 * @noreference
			 * @return
			 */
			public int m1();
			
			/**
			 * @noreference
			 * @return
			 */
			public abstract char m2();
		}
	}
}

@interface outer {
	/**
	 * @noreference
	 * @return
	 */
	public int m1();
	
	/**
	 * @noreference
	 * @return
	 */
	public abstract char m2();
}
