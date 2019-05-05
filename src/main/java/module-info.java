/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 2018-12-13.
 *
 * @author Thomas Couchoud
 * @since 2018-12-13
 */
open module FTPFetcher {
	requires java.scripting;
	requires fr.mrcraftcod.utils.config;
	requires org.slf4j;
	requires jsch;
	requires jdeferred.core;
	requires jcommander;
	requires org.json;
	requires org.apache.commons.io;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires progressbar;
}