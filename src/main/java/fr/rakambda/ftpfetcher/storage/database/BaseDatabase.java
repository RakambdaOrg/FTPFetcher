package fr.rakambda.ftpfetcher.storage.database;

import com.zaxxer.hikari.HikariDataSource;
import fr.rakambda.ftpfetcher.storage.IStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
@Log4j2
public abstract class BaseDatabase implements IStorage, IDatabase{
	private final HikariDataSource dataSource;
	
	@Override
	public void close(){
		dataSource.close();
	}
	
	protected int execute(@NonNull String... statements) throws SQLException{
		try(var conn = getConnection()){
			conn.setAutoCommit(false);
			for(var sql : statements){
				try(var statement = conn.createStatement()){
					statement.execute(sql);
					return statement.getUpdateCount();
				}
				catch(SQLException e){
					conn.rollback();
					throw e;
				}
			}
			conn.commit();
		}
		return -1;
	}
	
	@NonNull
	@Override
	public Connection getConnection() throws SQLException{
		return dataSource.getConnection();
	}
}
