open module fr.raksrinana.ftpfetcher {
	requires java.sql;
	requires fr.raksrinana.utils.base;
	requires fr.raksrinana.utils.config;
	requires info.picocli;
	requires org.apache.commons.io;
	requires org.apache.commons.collections4;
	requires progressbar;
	requires org.slf4j;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires org.apache.logging.log4j.slf4j;
	requires java.scripting;
	requires com.hierynomus.sshj;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires static lombok;
	requires static org.jetbrains.annotations;
}