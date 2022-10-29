package fr.rakambda.ftpfetcher.storage.database;

import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabase extends AutoCloseable{
	void initDatabase() throws SQLException;
	
	@Override
	void close();
	
	@NotNull
	Connection getConnection() throws SQLException;
}
