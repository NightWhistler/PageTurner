package net.nightwhistler.pageturner.library;


public interface LibraryService {
	
	public void storeBook( String fileName, String authorFirstName, String authorLastName, String title, 
			 byte[] coverImage );
	
	public QueryResult<LibraryBook> findAllByLastRead();
	
	public QueryResult<LibraryBook> findAllByLastAdded();
	
	public QueryResult<LibraryBook> findAllByTitle();
	
	public QueryResult<LibraryBook> findAllByAuthor();
	
	public boolean hasBook( String fileName );
	
	public void close();
}
