open module fr.raksrinana.ftpfetcher {
	requires java.sql;
	requires fr.raksrinana.utils.base;
	requires jcommander;
	requires org.apache.commons.io;
	requires progressbar;
	requires static lombok;
	requires org.slf4j;
	requires ch.qos.logback.classic;
	requires jsch;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.zaxxer.hikari;
}