package com.liquidsys.coco.localconfig;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Provide convenient means to get at local configuration - stuff that
 * we do not want in LDAP.  Use one of these methods:
 * 
 *      String v = LC.get("my_config_param");
 * 
 * or 
 * 
 *      String v = LC.my_config_param.value();
 * 
 * The latter ofcourse is better.
 */
public class LC {
   
    public static String get(String key) {
        String value = null;
        try {
            value = LocalConfig.getInstance().get(key);
        } catch (ConfigException ce) {
            /*
             * We swallow the exception here because we know that
             * LocalConfig does a verify and the exception can never
             * occur here - we do not let that any sets on the
             * mLocalConfig field in this class - so no one can beat
             * the verification done in the constructor.
             */
            Logging.warn("LC.get(" + key + ") caused exception, returning empty string", ce);
            value = "";
        }
        if (value == null) {
            Logging.warn("LC.get(" + key + ") was null, returning empty string");
            value = "";
        }
        return value;
    }

    public static String[] getAllKeys() {
    	return LocalConfig.getInstance().allKeys();
    }

    static void init() {
        // This method is there to guarantee static initializer of this
        // class is run.
    }

    public static final KnownKey liquid_home;
    public static final KnownKey liquid_java_home;
    public static final KnownKey liquid_log_directory;
    public static final KnownKey liquid_index_directory;
    public static final KnownKey liquid_store_directory;
    public static final KnownKey liquid_db_directory;
    public static final KnownKey liquid_mysql_user;
    public static final KnownKey liquid_mysql_password;
    public static final KnownKey liquid_ldap_userdn;
    public static final KnownKey liquid_ldap_password;
    public static final KnownKey liquid_server_hostname;
    
    public static final KnownKey stats_img_folder;
    
    public static final KnownKey ldap_host;
    public static final KnownKey ldap_port;
    public static final KnownKey ldap_root_password;
    
    public static final KnownKey mysql_directory;
    public static final KnownKey mysql_data_directory;
    public static final KnownKey mysql_socket;
    public static final KnownKey mysql_pidfile;
    public static final KnownKey mysql_mycnf;
    public static final KnownKey mysql_bind_address;
    public static final KnownKey mysql_port;
    public static final KnownKey mysql_max_connections;
    public static final KnownKey mysql_memory_percent;
    public static final KnownKey mysql_innodb_log_buffer_size;
    public static final KnownKey mysql_innodb_thread_concurrency;
    public static final KnownKey mysql_root_password;
    public static final KnownKey mysql_table_cache;
    
    public static final KnownKey tomcat_directory;
    public static final KnownKey tomcat_java_heap_memory_percent;
    public static final KnownKey tomcat_java_options;
    public static final KnownKey tomcat_java_home;
    public static final KnownKey tomcat_pidfile;

    public static final KnownKey spell_hostname;
    public static final KnownKey spell_port;
    public static final KnownKey spell_retry_interval_millis;
    
    public static final KnownKey ssl_allow_untrusted_certs;

    static {
        final String LQMYCNF_CAVEAT = "This value is stored here for use by lqmycnf program.  Changing this setting does not immediately reflect in MySQL server.  You will have to, with abundant precaution, re-generate my.cnf and restart MySQL server for the change to take effect.";
        final String FS = File.separator;
        String hostname = "lookup.failed";
        try {
            InetAddress address = InetAddress.getLocalHost();
            String host = address.getCanonicalHostName();
            if (Character.isDigit(host.charAt(0)))
                host = address.getHostName();
            hostname = host.toLowerCase();
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
        }

        liquid_home = new KnownKey("liquid_home");
        liquid_home.setDefault(FS + "opt" + FS + "liquid");
        liquid_home.setForceToEdit(true);
        liquid_home.setDoc
            ("Liquid installation root and home directory of `liquid'" + 
             " UNIX user. You can not relocate install root - do" +
             " not change this setting.");
        
        liquid_java_home = new KnownKey("liquid_java_home");
        liquid_java_home.setDefault("${liquid_home}" + FS + "java");
        liquid_java_home.setDoc
            ("Path to a JDK/J2SDK.");
        
        liquid_log_directory = new KnownKey("liquid_log_directory");
        liquid_log_directory.setDefault("${liquid_home}" + FS + "log");
        liquid_log_directory.setDoc
            ("Directory where log files are written.");

        liquid_index_directory = new KnownKey("liquid_index_directory");
        liquid_index_directory.setDefault("${liquid_home}" + FS + "index");
        liquid_index_directory.setDoc
            ("Directory for mailbox index files.");

        liquid_store_directory = new KnownKey("liquid_store_directory");
        liquid_store_directory.setDefault("${liquid_home}" + FS + "store");
        liquid_store_directory.setDoc
            ("Directory for mailbox messages.");

        liquid_db_directory = new KnownKey("liquid_db_directory");
        liquid_db_directory.setDefault("${liquid_home}" + FS + "db");
        liquid_db_directory.setDoc
            ("Directory for database files.");

        liquid_mysql_user = new KnownKey("liquid_mysql_user");
        liquid_mysql_user.setDefault("liquid");
        liquid_mysql_user.setDoc
            ("MySQL username to use to create/access liquid databases" +
             " and tables. This is the value you would supply to" +
             " the '-u' option of 'mysql' command line program.");

        liquid_mysql_password = new KnownKey("liquid_mysql_password");
        liquid_mysql_password.setDefault("liquid");
        liquid_mysql_password.setForceToEdit(true);
        liquid_mysql_password.setDoc
            ("Password for " + liquid_mysql_user.key() + ". Stored in" +
             " local config for use by the store application to" +
             " authenticate.  If you want to change this password," +
             " please use the lqmypasswd program which will change the" +
             " password in both MySQL and in local config.");

        liquid_ldap_userdn = new KnownKey("liquid_ldap_userdn");
        liquid_ldap_userdn.setDefault("uid=liquid,cn=admins,cn=liquid");
        liquid_ldap_userdn.setDoc
            ("LDAP dn used to authenticate the store application with LDAP.");
        
        liquid_ldap_password = new KnownKey("liquid_ldap_password");
        liquid_ldap_password.setDefault("liquid");
        liquid_ldap_password.setForceToEdit(true);
        liquid_ldap_password.setDoc
            ("Password for " + liquid_ldap_userdn.key() + ". Stored in" +
             " local config for use by the store application to" +
             " authenticate.  If you want to change this password," +
             " please use the lqldappasswd program which will  change the" +
             " password in both LDAP and in local config.");

        liquid_server_hostname = new KnownKey("liquid_server_hostname");
        liquid_server_hostname.setDefault(hostname);
        liquid_server_hostname.setDoc
            ("The provisioned name of this server. There should exist" +
             " a corresponding `server' entry in LDAP - consult" +
             " documentation for CreateServer command of the lqprov program.");

        stats_img_folder = new KnownKey("stats_img_folder");
        stats_img_folder.setDefault("${liquid_home}" + FS + "liquidmon" + FS + "rrdtool" + FS + "work");
        stats_img_folder.setDoc
            ("Directory for storing generated statistics images.");

        ldap_host = new KnownKey("ldap_host");
        ldap_host.setDefault("localhost");
        ldap_host.setDoc("LDAP host to use.");

        ldap_port = new KnownKey("ldap_port");
        ldap_port.setDefault("389");
        ldap_port.setDoc("LDAP port to use.");

        ldap_root_password = new KnownKey("ldap_root_password");
        ldap_root_password.setDefault("liquid");
        ldap_root_password.setForceToEdit(true);
        ldap_root_password.setDoc
            ("Password for LDAP slapd.conf rootdn.  As a convenience," +
             " during LDAP initialization a random password is" +
             " generated, saved in local config and in slapd.conf.  If you" +
             " want to change this password, please use the lqldappasswd" +
             " program which will change the password in both slapd.conf" +
             " and in local config.");

        mysql_directory = new KnownKey("mysql_directory");
        mysql_directory.setDefault("${liquid_home}" + FS + "mysql");
        mysql_directory.setDoc
            ("Location of MySQL installation.");

        mysql_data_directory = new KnownKey("mysql_data_directory");
        mysql_data_directory.setDefault("${liquid_db_directory}" + FS + "data");
        mysql_data_directory.setDoc
            ("Directory in which MySQL data should reside.");

        mysql_socket = new KnownKey("mysql_socket");
        mysql_socket.setDefault("${liquid_db_directory}" + FS + "mysql.sock");
        mysql_socket.setDoc
            ("Path to MySQL socket for use by MySQL command line tools.");

        mysql_pidfile = new KnownKey("mysql_pidfile");
        mysql_pidfile.setDefault("${liquid_db_directory}" + FS + "mysql.pid");
        mysql_pidfile.setDoc
            ("File in which MySQL process id is stored.");

        mysql_mycnf = new KnownKey("mysql_mycnf");
        mysql_mycnf.setDefault("${liquid_home}" + FS + "conf" + FS + "my.cnf");
        mysql_mycnf.setDoc
            ("Path to my.cnf, the MySQL config file.");

        mysql_bind_address = new KnownKey("mysql_bind_address");
        mysql_bind_address.setDefault("localhost");
        mysql_bind_address.setDoc
            ("Interface on this host to which MySQL will bind.");

        mysql_port = new KnownKey("mysql_port");
        mysql_port.setDefault("3306");
        mysql_port.setDoc
            ("Port number on which MySQL server should listen.");

        mysql_max_connections = new KnownKey("mysql_max_connections");
        mysql_max_connections.setDefault("200");
        mysql_max_connections.setDoc
            ("Maximum number of client connections that mysql server" +
             " should allow. " + LQMYCNF_CAVEAT);

        mysql_memory_percent = new KnownKey("mysql_memory_percent");
        mysql_memory_percent.setDefault("40");
        mysql_memory_percent.setDoc
            ("Percentage of system memory that mysql should use. TODO:" +
             " change docs, and make sure lqmycnf handles " +
             " innodb_log_buffer_size delicately as change in that " +
             " influencnes changes to the log file sizes. " + LQMYCNF_CAVEAT);

        mysql_innodb_log_buffer_size = new KnownKey("mysql_innodb_log_buffer_size");
        mysql_innodb_log_buffer_size.setDefault("8M");
        mysql_innodb_log_buffer_size.setDoc
            ("Consult MySQL documentation for innodb_log_buffer_size. " +
             LQMYCNF_CAVEAT);

        mysql_innodb_thread_concurrency = new KnownKey("mysql_innodb_thread_concurrency");
        mysql_innodb_thread_concurrency.setDefault("200");
        mysql_innodb_thread_concurrency.setDoc
            ("Consult MySQL documentation for innodb_thread_concurrency. " +
             LQMYCNF_CAVEAT);

        mysql_root_password = new KnownKey("mysql_root_password");
        mysql_root_password.setDefault("liquid");
        mysql_root_password.setForceToEdit(true);
        mysql_root_password.setDoc
            ("Password for MySQL's built-in `root' user, not to be" +
             " confused with the UNIX root login.  As a convenience," +
             " during database initialization a random password is" +
             " generated, saved in local config and in MySQL.  If you" +
             " want to change this password, please use the lqmypasswd" +
             " program which will change the password in both MySQL" +
             " and in local config.");

        mysql_table_cache = new KnownKey("mysql_table_cache");
        mysql_table_cache.setDefault("500");
        mysql_table_cache.setDoc
            ("Consult MySQL documentation for table_cache. " + LQMYCNF_CAVEAT);

        tomcat_directory = new KnownKey("tomcat_directory");
        tomcat_directory.setDefault("${liquid_home}" + FS + "tomcat");
        tomcat_directory.setDoc("Location of tomcat installation.");

        tomcat_java_heap_memory_percent = new KnownKey("tomcat_java_heap_memory_percent");
        tomcat_java_heap_memory_percent.setDefault("30");
        tomcat_java_heap_memory_percent.setDoc
            ("Percentage of system memory that will be used as the" +
             " maximum Java heap size (-Xmx) of the JVM running Tomcat.");
        
        tomcat_java_options = new KnownKey("tomcat_java_options");
        tomcat_java_options.setDefault("-client -XX:NewRatio=2");
        tomcat_java_options.setDoc
            ("JVM options to use when launching Tomcat.");
        
        tomcat_java_home = new KnownKey("tomcat_java_home");
        tomcat_java_home.setDefault("${liquid_java_home}");
        tomcat_java_home.setDoc
            ("Path to JDK/JRE to use for running Tomcat.");
        
        tomcat_pidfile = new KnownKey("tomcat_pidfile");
        tomcat_pidfile.setDefault("${liquid_log_directory}" + FS + "tomcat.pid");
        tomcat_pidfile.setDoc
            ("File in which process id of Tomcat JVM is stored.");

        ssl_allow_untrusted_certs = new KnownKey("ssl_allow_untrusted_certs");
        ssl_allow_untrusted_certs.setDefault("false");
        ssl_allow_untrusted_certs.setDoc
            ("If true, allow self-signed SSL certificates.");
        
        spell_hostname = new KnownKey("spell_hostname");
        spell_hostname.setDefault("");
        spell_hostname.setDoc("Spell server hostname");
        
        spell_port = new KnownKey("spell_port");
        spell_port.setDefault("80");
        spell_port.setDoc("Spell server port");
        
        spell_retry_interval_millis = new KnownKey("spell_retry_interval_millis");
        spell_retry_interval_millis.setDefault("60000");
        spell_retry_interval_millis.setDoc(
            "Number of milliseconds to wait before reconnecting to the " +
            "spell server after a connection fails");
    }

}
