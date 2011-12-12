package net.nightwhistler.pageturner.library;


public interface LibraryService {
	
	public void storeBook( String fileName, String authorFirstName, String authorLastName, String title, 
			 byte[] coverImage );
	
	public QueryResult<LibraryBook> findAllByLastRead();
	
	
	public void close();
}
