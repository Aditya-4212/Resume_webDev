package com.latexeditor.util;

import jakarta.servlet.ServletContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * DatabaseUtil
 *
 * Provides a pooled JDBC Connection via Tomcat's JNDI DataSource.
 *
 * Setup in context.xml:
 * ─────────────────────
 * <Resource name="jdbc/latexeditor"
 *           auth="Container"
 *           type="javax.sql.DataSource"
 *           driverClassName="com.mysql.cj.jdbc.Driver"
 *           url="jdbc:mysql://localhost:3306/latexeditor?useSSL=false&amp;serverTimezone=UTC"
 *           username="root"
 *           password="your_password"
 *           maxTotal="20"
 *           maxIdle="10"
 *           maxWaitMillis="-1"/>
 *
 * web.xml resource-ref:
 * ─────────────────────
 * <resource-ref>
 *   <res-ref-name>jdbc/latexeditor</res-ref-name>
 *   <res-type>javax.sql.DataSource</res-type>
 *   <res-auth>Container</res-auth>
 * </resource-ref>
 *
 * Required JAR: mysql-connector-j-8.x.x.jar in WEB-INF/lib/
 */
public class DatabaseUtil {

    private static final Logger log = Logger.getLogger(DatabaseUtil.class.getName());
    private static final String JNDI_NAME = "java:comp/env/jdbc/latexeditor";

    private static DataSource dataSource;

    /** Returns a pooled Connection. Caller must close() it after use. */
    public static Connection getConnection() throws Exception {
        if (dataSource == null) {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(JNDI_NAME);
            log.info("DataSource acquired from JNDI: " + JNDI_NAME);
        }
        return dataSource.getConnection();
    }
}
