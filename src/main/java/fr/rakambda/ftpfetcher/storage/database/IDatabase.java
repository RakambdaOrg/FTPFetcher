package fr.rakambda.ftpfetcher.storage.database;

import org.jspecify.annotations.NonNull;
import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabase extends AutoCloseable{
	void initDatabase() throws SQLException;
	
	@Override
	void close();
	
	@NonNull
	Connection getConnection() throws SQLException;
}
