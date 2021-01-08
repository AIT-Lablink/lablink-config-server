//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * This class implements the Lablink Configuration Server.
 */
public class Config extends Thread {

  /** The db path. */
  private static String dbPath;

  /** The port. */
  private int port;

  /** The Constant USAGE. */
  private static final String USAGE =
      "-a [run|add|update|delete|blank] [-d <path>] [-p <port>] -k <key> -v <value> | -h ]";

  /** The Constant LOG. */
  static final Logger LOG = LogManager.getLogger("LablinkConfig"); // LogManager.getRootLogger();

  /** The Constant DEFAULT_DB. */
  private static final String DEFAULT_DB = "C:/workbench/config_dbs/configurations.db";

  /** The Constant DEFAULT_CONFIG_COL. */
  private static final String DEFAULT_CONFIG_COL = "config";

  /** The Constant DEFAULT_PORT. */
  private static final int DEFAULT_PORT = 10101;

  /** The Constant SQL_SIMPLE. */
  private static final String SQL_SIMPLE_GET = "SELECT config, IFNULL(invoke, 'RAW') as token "
      + "FROM LablinkConfigSimple WHERE identifier = ? LIMIT 1";

  /** The Constant SQL_SIMPLE_VIEW. */
  private static final String SQL_SIMPLE_VIEW =
      "SELECT identifier, config, description, IFNULL(invoke, 'null') as token"
          + " FROM LablinkConfigSimple WHERE identifier = ? LIMIT 1";

  /** The Constant SQL_SIMPLE_VIEW_ALL. */
  private static final String SQL_SIMPLE_VIEW_ALL =
      "SELECT identifier, description, IFNULL(invoke, 'null') as token"
          + " FROM LablinkConfigSimple ORDER BY 1";

  static final String LOGO_PATH = Config.class.getResource("/logo.png").getPath();
  static final String HTML_PAGE_TITLE = "AIT Lablink Simulation Configuration Viewer";

  /** The web server. */
  private static HttpServer webServer;

  /** The db conn. */
  private static Connection dbConn;

  /**
   * Instantiates a new sim config.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Config() throws IOException {
    this.config(DEFAULT_DB, DEFAULT_PORT);
  }

  /**
   * Instantiates a new sim config.
   *
   * @param db the db
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Config(String db) throws IOException {
    this.config(db, DEFAULT_PORT);
  }

  /**
   * Split len.
   *
   * @param token the token
   * @return the int
   */
  public static int splitLen(String token) {
    return splitToken(token).length;
  }

  /**
   * Execute python.
   *
   * @param token the token
   * @param script the script
   * @return the string
   */
  public static String executePython(String token, String script) {

    String results = script;

    if (validateInvoke(token)) {

      String[] tokens = splitToken(token);
      LOG.debug("Token: {}", token);

      PythonInterpreter python = new PythonInterpreter();
      PyObject config = new PyObject();

      String pythonClassInstance = MessageFormat.format("config = {0}()", new Object[] {tokens[1]});
      String pythonMethodInstance =
          MessageFormat.format("cfg = config.{0}()", new Object[] {tokens[2]});

      try {
        python.exec(script);
        python.exec(pythonClassInstance);
        python.exec(pythonMethodInstance);
        config = python.get("cfg");
        results = config.asString();

        LOG.debug("Result: {}", results);

      } catch (Exception ex) {
        results = "ERROR: There were error processing the Python script."
            + " Please, fix the errors and try again.\n"
            + "For more details, please check the log.\n" + ex.toString();
        LOG.error(results);

        ex.printStackTrace();
      }
    }

    return results;
  }

  /**
   * Split token.
   *
   * @param token the token
   * @return the string[]
   */
  public static String[] splitToken(String token) {
    return token.split("[\\:]");
  }

  /**
   * Validate invoke.
   *
   * @param invoke the invoke
   * @return true, if successful
   */
  public static boolean validateInvoke(String invoke) {
    boolean valid = false;

    if (StringUtils.isEmpty(invoke)) {
      return valid;
    }

    String[] invokes = splitToken(invoke);

    if (invokes.length == 3) {
      if (invokes[0].equals(new String("python"))) {
        if (StringUtils.isNotBlank(invokes[2])) {
          valid = true;
        }
      }
    }
    return valid;
  }

  /**
   * Gets the db connection.
   *
   * @param path the path
   * @return the db connection
   */
  public static Connection getDbConnection(String path) {

    Connection conn = null;
    path = "jdbc:sqlite:" + path;

    try {
      conn = DriverManager.getConnection(path);
      conn.setAutoCommit(false);
    } catch (SQLException ex) {
      LOG.error("Error while connection to database '{}'.", ex.getMessage());
    }
    LOG.debug("Connection open to database '{}' sucessfully.", path);

    return conn;
  }

  /**
   * Action run.
   *
   * @return true, if successful
   */
  public static boolean actionRun() {
    return false;
  }

  /**
   * Return fixed length string.
   *
   * @param field the field
   * @param len the len
   * @return the string
   */
  public static String fixedLen(String field, int len) {
    return String.format("%-" + len + "s", field);
  }

  public static String repeate(String src, int count) {
    return new String(new char[count]).replace("\0", src);
  }

  /**
   * List configs.
   *
   * @param conn the conn
   * @param key the key
   * @param title the title
   * @throws SQLException the SQL exception
   */
  public static void listConfigs(Connection conn, String key, String title) throws SQLException {

    // Connection conn = getDbConnection(db);

    String sql = "SELECT identifier, description, IFNULL(invoke, 'null') as token, config"
        + " FROM LablinkConfigSimple ";

    boolean hasKey = org.apache.commons.lang3.StringUtils.isNotBlank(key);

    if (hasKey) {
      LOG.debug("Listing only configuration(s) for key='{}'", key);
      sql += " WHERE identifier=?";
    } else {
      LOG.debug("Listing ALL configurations stored in the database.");
    }

    sql += " ORDER BY identifier ";

    // LOG.debug("Executing SQL {}", sql);
    PreparedStatement qry = conn.prepareStatement(sql);

    if (hasKey) {
      qry.setString(1, key);
    }

    Integer counter = 0;
    MessageFormat fmt = new MessageFormat("|{0}|{1}|{2}|{3}\n{5}\n{4}{5}");

    String line = "+" + repeate("-", 3) + "+" + repeate("-", 10) + "+" + repeate("-", 35) + "+"
        + repeate("-", 20);

    System.out.println("+" + repeate("-", 90) + "+");
    System.out.println("|" + fixedLen(title, 90) + "|");

    System.out.println(line);
    System.out.println("|S.#|Key" + repeate(" ", 27) + "|Invoke" + repeate(" ", 29) + "|Description"
        + repeate(" ", 10));
    System.out.println(line);

    ResultSet rs = qry.executeQuery();

    while (rs.next()) {

      counter++;

      System.out.println(fmt.format(new Object[] {fixedLen(counter.toString(), 3),
          fixedLen(rs.getString("identifier"), 30), fixedLen(rs.getString("token"), 35),
          rs.getString("description"), rs.getString("config"), line}));
    }

    System.out.println(counter + " record(s) listed.");
  }

  /**
   * Action update.
   *
   * @param db the db
   * @param key the key
   * @param newval the newval
   * @param desc the desc
   * @param invoke the invoke
   * @return true, if successful
   * @throws SQLException the SQL exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static boolean actionUpdate(String db, String key, String newval, String desc,
      String invoke) throws SQLException, IOException {
    Connection conn = Config.getDbConnection(db);
    if (conn == null) {
      LOG.error("Error opening/accessing database '{}'.", db);
      return false;
    } else {

      LOG.info("Updating configuration for '{}'", key);
      Config.listConfigs(conn, key, "OLD Values");

      String sql = null;
      PreparedStatement qry = null;
      desc = StringUtils.isEmpty(desc) ? key : desc;

      if (!StringUtils.isEmpty(invoke)) {
        sql = "UPDATE LablinkConfigSimple SET config=?, description=?, invoke=? WHERE identifier=?";
        qry = conn.prepareStatement(sql);

        qry.setString(1, getFileContents(newval));
        qry.setString(2, desc);
        qry.setString(3, invoke);
        qry.setString(3, key);
      } else {
        sql = "UPDATE LablinkConfigSimple SET config=? WHERE identifier=?";
        qry = conn.prepareStatement(sql);
        URL infile = new URL(newval);

        qry.setString(1, new Scanner(infile.openStream()).useDelimiter("\\Z").next());
        qry.setString(2, key);
      }

      qry.execute();

      Config.listConfigs(conn, key, "NEW Values");
      commitAndClose(conn);

    }
    return true;
  }

  /**
   * Gets the file contents.
   *
   * @param path the path
   * @return the file contents
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static String getFileContents(String path) throws IOException {
    File file = new File(path);
    return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
  }

  /**
   * Action add.
   *
   * @param db the db
   * @param key the key
   * @param newval the newval
   * @param desc the desc
   * @param invoke the invoke
   * @return true, if successful
   * @throws SQLException SQLException
   * @throws IOException IOException
   */
  public static boolean actionAdd(String db, String key, String newval, String desc, String invoke)
      throws SQLException, IOException {

    Connection conn = Config.getDbConnection(db);
    if (conn == null) {
      LOG.error("Error opening/accessing database '{}'.", db);
      return false;
    } else {

      LOG.info("Selected database is '{}'", db);
      LOG.info("Adding new configuration for key '{}'", key);

      String sql = null;
      PreparedStatement qry = null;
      desc = StringUtils.isEmpty(desc) ? "Configuration for " + key : desc;
      String value = getFileContents(newval);

      if (!StringUtils.isEmpty(invoke)) {
        sql = "INSERT INTO LablinkConfigSimple VALUES (?, ?, ?, ?)";

        qry = conn.prepareStatement(sql);

        qry.setString(1, key);
        qry.setString(2, value);
        qry.setString(3, invoke);
        qry.setString(4, desc);

        LOG.debug("Key={}", key);
        LOG.debug("Value={}", value);
        LOG.debug("Desc={}", desc);
        LOG.debug("Invoke={}", invoke);

      } else {
        sql = "INSERT INTO LablinkConfigSimple (identifier, config, description) VALUES (?, ?, ?)";

        qry = conn.prepareStatement(sql);

        qry.setString(1, key);
        qry.setString(2, value);
        qry.setString(3, desc);

        LOG.debug("Key={}", key);
        LOG.debug("Value={}", value);
        LOG.debug("Desc={}", desc);
      }

      qry.execute();

      Config.listConfigs(conn, key, "Entry added; the values are:");
      commitAndClose(conn);
    }
    return true;
  }

  /**
   * Commit and close.
   *
   * @param conn the conn
   * @throws SQLException the SQL exception
   */
  public static void commitAndClose(Connection conn) throws SQLException {
    LOG.info("Saving changes and closing connection...");
    conn.commit();
    conn.close();
    LOG.info("Done");
  }

  /**
   * Action delete.
   *
   * @param db the db
   * @param key the key
   * @return true, if successful
   * @throws SQLException SQLException
   */
  public static boolean actionDelete(String db, String key) throws SQLException {
    Connection conn = Config.getDbConnection(db);
    if (conn == null) {
      LOG.error("Error opening/accessing database '{}'.", db);
      return false;
    } else {
      LOG.info("Updating configuration for '{}'", key);
      Config.listConfigs(conn, key, "OLD Values");

      String sql = "DELETE FROM LablinkConfigSimple WHERE identifier=?";
      PreparedStatement qry = conn.prepareStatement(sql);

      qry.setString(1, key);

      qry.execute();

      Config.listConfigs(conn, key, "Updated Values are:");
      commitAndClose(conn);
    }
    return true;
  }

  /**
   * Action blank db.
   *
   * @param db the db
   * @return true, if successful
   * @throws SQLException the SQL exception
   */
  public static boolean actionBlankDb(String db) throws SQLException {
    Connection conn = getDbConnection(db);
    if (conn == null) {
      LOG.error("Error creating/accessing database '{}'.", db);
      return false;
    } else {
      LOG.info("A Blank database has been created at '{}'. Now creating tables..", db);
      String sql = "CREATE TABLE \"LablinkConfigSimple\" (" + " `identifier` TEXT NOT NULL UNIQUE,"
          + " `config` TEXT NOT NULL," + " `invoke` TEXT," + " `description` TEXT NOT NULL,"
          + " PRIMARY KEY(`identifier`) )";
      PreparedStatement qry = conn.prepareStatement(sql);

      qry.execute();
      commitAndClose(conn);
    }
    return true;
  }

  /**
   * Instantiates a new sim config.
   *
   * @param prt the prt
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Config(int prt) throws IOException {
    this.config(DEFAULT_DB, prt);
  }

  /**
   * Instantiates a new sim config.
   *
   * @param dbpath the dbpath
   * @param lport the lport
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Config(String dbpath, int lport) throws IOException {
    this.config(dbpath, lport);
  }

  /**
   * Run.
   *
   * @param dbpath the dbpath
   * @param lport the lport
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void config(String dbpath, int lport) throws IOException {
    this.setDbPath(dbpath);
    this.setPort(lport);
    this.setName("LablinkConfigServer");
    LOG.debug("DB location set to '{}'.", dbpath);
    LOG.debug("Webserver port set to {}.", port);
    // LOG.debug("Using the log4j2 configuration file :: {}.",
    // System.getProperty("log4j.configurationFile"));
  }

  /**
   * Gets the db path.
   *
   * @return the dbPath
   */
  private static String getDbPath() {
    return dbPath;
  }

  /**
   * Sets the db path.
   *
   * @param dbPath the dbPath to set
   */
  private void setDbPath(String dbPath) {
    Config.dbPath = "jdbc:sqlite:" + dbPath;
  }

  /**
   * Creates the webserver.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("restriction")
  private void createWebserver() throws IOException {
    webServer = HttpServer.create(new InetSocketAddress(this.getPort()), 0);
    webServer.createContext("/view", new ViewHandler());
    webServer.createContext("/get", new GetHandler());
    webServer.createContext("/", new ViewAllHandler());

    webServer.setExecutor(null); // creates a default executor

    LOG.debug("Webserver running at {}.", webServer.getAddress());
  }

  /**
   * Connect db.
   *
   * @return the connection
   */
  private static Connection connectDb() {

    Connection conn = null;

    try {
      conn = DriverManager.getConnection(getDbPath());
      conn.setAutoCommit(false);
    } catch (SQLException ex) {
      LOG.error("Error while connection to database '{}'.", ex.getMessage());
    }
    LOG.debug("Connected to DB '{}' for query execution.", getDbPath());

    return conn;
  }

  /**
   * Get the configuration.
   *
   * @param id the id
   * @return the config
   */
  String getConfig(String id) {
    return getSimpleConfig(id, SQL_SIMPLE_GET);
  }

  /**
   * View the configuration.
   *
   * @param id the id
   * @return the string
   */
  String viewConfig(String id) {
    return getSimpleConfig(id, SQL_SIMPLE_VIEW, 1);
  }

  /**
   * View all configurations.
   *
   * @param id the id
   * @return the string
   */
  String viewAllConfig(String id) {
    return getSimpleConfig(id, SQL_SIMPLE_VIEW_ALL, 3);
  }

  /**
   * View all config.
   *
   * @return the string
   */
  String viewAllConfig() {

    String config = "";
    Connection conn = null;
    int ii = 1;

    try {
      conn = connectDb();
      PreparedStatement pstmt = conn.prepareStatement(SQL_SIMPLE_VIEW_ALL);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        String id = rs.getString("identifier");
        config += MessageFormat.format(
            "<tr><td>{0}</td><td><b>{1}</b></td><td>{2}</td><td>{3}</td><td><a href=view?id={1}>VIEW</a>,&nbsp;<a href=get?id={1}>GET</a></td></tr>\n",
            new Object[] {ii++, id, rs.getString("description"), rs.getString("token")}
            );
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        LOG.error("Error closing DB: {}", ex.getMessage());
      }
      LOG.debug("Connection to DB closed gracefully after query execuation.");
    }

    return (config.equals("") ? null
        : "<table border=1><tr><th>S#</th><th>ID</th><th>Description</th>"
            + "<th>Invoke</th><th>Action</th></tr>\n" + config + "</table>\n");
  }

  /**
   * Gets the simple config.
   *
   * @param id the id
   * @param sql the sql
   * @param type the type
   * @return the simple config
   */
  private String getSimpleConfig(String id, String sql, int type) {
    String config = null;
    try {
      Connection conn = connectDb();
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, id);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        if (type == 1) {
          config = "<h3>Description:" + rs.getString("description")
              + "</h3><br><textarea rows='40' style='width:80%';>"
              + rs.getString(DEFAULT_CONFIG_COL) + "</textarea><br/>";
        } else if (type == 2) {
          config = rs.getString(DEFAULT_CONFIG_COL);
          if (!rs.getString("token").equals(new String("null"))) {
            config = executePython(rs.getString("token"), rs.getString(DEFAULT_CONFIG_COL));
          }

        } else if (type == 3) {
          config += "<tr><td><a href=view?id=" + rs.getString("identifier") + "</a></td><td>"
              + rs.getString("description") + "</td>/tr>";
        } else {
          LOG.error("Invalid ID={}", id);
        }
      }

      conn.close();
      LOG.debug("Connection to DB closed gracefully after query execuation.");

    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return config;
  }

  /**
   * Gets the simple config.
   *
   * @param id the id
   * @param sql the sql
   * @return the simple config
   */
  private String getSimpleConfig(String id, String sql) {
    String config = null;
    try {
      Connection conn = connectDb();
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, id);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        config = rs.getString(DEFAULT_CONFIG_COL);
        if (!rs.getString("token").equals(new String("RAW"))) {
          config = executePython(rs.getString("token"), rs.getString(DEFAULT_CONFIG_COL));
        }
      }

      conn.close();
      LOG.debug("Connection to DB closed gracefully after query execuation.");

    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return config;
  }

  /**
   * Query to map.
   *
   * @param query the query
   * @return the map
   */
  Map<String, String> queryToMap(String query) {
    Map<String, String> result = new HashMap<String, String>();
    for (String param : query.split("&")) {
      String[] pair = param.split("=");
      if (pair.length > 1) {
        result.put(pair[0], pair[1]);
      } else {
        result.put(pair[0], "");
      }
    }
    return result;
  }

  /**
   * Write response.
   *
   * @param httpExchange the http exchange
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
  void writeResponse(HttpExchange httpExchange, String response) throws IOException {
    httpExchange.sendResponseHeaders(200, response.length());
    OutputStream os = httpExchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  /**
   * Gets the port.
   *
   * @return the port
   */
  private int getPort() {
    return port;
  }

  /**
   * Sets the port.
   *
   * @param port the port to set
   */
  private void setPort(int port) {
    this.port = port;
  }

  /**
   * The main method.
   *
   * @param argv the arguments
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   * @throws SQLException the SQL exception
   */
  public static void main(String[] argv) throws IOException, ParseException, SQLException {

    Options cliOptions = new Options();
    CommandLineParser parser = new BasicParser();

    cliOptions.addOption("h", "help", true, "print usage information");

    cliOptions.addOption("k", "key", true, "the key/identifier for configuration");

    cliOptions.addOption("v", "value", true, "path to an existing text file");

    cliOptions.addOption("i", "invoke", true, "invoke, <language>:<class>:<method>");

    cliOptions.addOption("n", "note", true, "description for new configuration");

    cliOptions.addOption("a", "action", true, "action to be performed");

    cliOptions.addOption("d", "database", true,
        "path to and name of the database (default is " + DEFAULT_DB + ")");

    cliOptions.addOption("p", "port", true,
        "port for the webserver (default is " + DEFAULT_PORT + ")");

    CommandLine commandLine = parser.parse(cliOptions, argv);

    System.out.println(Utility.INFO_COPYRIGHTS_TEXT);

    // Help
    if (commandLine.hasOption('h')) {
      printUsage(cliOptions);
      System.exit(0);
    }

    String action = null;
    if (commandLine.hasOption('a')) {
      action = commandLine.getOptionValue('a');
    } else {
      printUsage(cliOptions);
      System.exit(0);
    }

    String db = commandLine.getOptionValue('d');
    String key = commandLine.getOptionValue('k');
    String value = commandLine.getOptionValue('v');
    String note = commandLine.getOptionValue('n');
    String invoke = commandLine.getOptionValue('i');
    int port = Integer.parseInt(commandLine.getOptionValue('p', "10101"));

    switch (action) {

      case "run":
        LOG.info("Executing action 'run'.");
        Config server = null;
        if (commandLine.hasOption('d') && commandLine.hasOption('p')) {
          server = new Config(db, port);
        } else if (commandLine.hasOption('d')) {
          server = new Config(db);
        } else if (commandLine.hasOption('p')) {
          server = new Config(port);
        } else {
          server = new Config();
        }
        server.run();
        break;

      case "update":
        LOG.info("Executing action 'update'...");
        if (!commandLine.hasOption('d') || !commandLine.hasOption('k')
            || !commandLine.hasOption('v')) {
          System.out.println("Not all the necessary parameters were provided:");
          System.out.println("usage");
          System.out.println(
              "<executable.jar> -a update -d database -k key -v newvalue [-n description]");
          System.exit(0);
        }

        if (!Files.isRegularFile(Paths.get(db))) {
          System.out.println("The file '" + Paths.get(db) + "' do not exists.");
          System.exit(0);
        }

        Config.actionUpdate(db, key, value, note, invoke);
        break;


      case "add":
        LOG.info("Executing action 'add'.");
        if (!commandLine.hasOption('d') || !commandLine.hasOption('k')
            || !commandLine.hasOption('v') || !commandLine.hasOption('n')) {
          System.out.println("Not all the necessary parameters were provided:");
          System.out.println("usage");
          System.out
              .println("<executable.jar> -a add -d database -k key -v newvalue -n description");
          System.exit(0);
        }

        if (!Files.isRegularFile(Paths.get(db))) {
          System.out.println("The file '" + Paths.get(db) + "' do not exists.");
          System.exit(0);
        }

        LOG.debug("DB={}, KEY={}, VALUE={}, NOTE={}", db, key, value, note);

        Config.actionAdd(db, key, value, note, invoke);

        break;

      case "delete":
        LOG.info("Executing action 'delete'.");
        if (!commandLine.hasOption('d') || !commandLine.hasOption('k')) {
          System.out.println("Not all the necessary parameters were provided:");
          System.out.println("usage");
          System.out.println("<executable.jar> -a delete -d database -k key");
          System.exit(0);
        }

        if (!Files.isRegularFile(Paths.get(db))) {
          System.out.println("The file '" + Paths.get(db) + "' do not exists.");
          System.exit(0);
        }

        Config.actionDelete(db, key);
        break;

      case "blank":
        LOG.info("Executing action 'blank'.");
        if (!commandLine.hasOption('d')) {
          System.out.println("Not all the necessary parameters were provided:");
          System.out.println("usage");
          System.out.println("<executable.jar> -a blank -d database");
          System.exit(0);
        }

        if (Files.isRegularFile(Paths.get(db))) {
          LOG.error("The file '{}' already exists.", Paths.get(db));
          System.out.println("The file '" + Paths.get(db) + "' already exists.");
          System.exit(0);
        }

        Config.actionBlankDb(db);
        break;

      case "list":
        LOG.info("Executing action 'list'.");
        if (!commandLine.hasOption('d')) {
          System.out.println("Not all the necessary parameters were provided:");
          System.out.println("usage");
          System.out.println("<executable.jar> -a list -d database [-k key]");
          System.exit(0);
        }

        Config.listConfigs(getDbConnection(db), key, "Stored configurations");
        break;
      default:
        System.out.println("The action '" + action + "' is not supported");
        System.exit(0);
        break;

    }
  }

  /**
   * Prints the usage.
   *
   * @param options the options
   */
  static void printUsage(Options options) {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.setWidth(80);
    helpFormatter.printHelp(USAGE, options);
  }

  /**
   * @see java.lang.Thread#run()
   */
  @Override
  public void run() {
    try {
      createWebserver();
      webServer.start();
    } catch (IOException ex) {
      LOG.error("Error while executing run {}.", ex.getMessage());
    }
  }


  /**
   * The Class GetHandler.
   */
  class GetHandler implements HttpHandler {

    /**
     * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
     */
    @SuppressWarnings("restriction")
    public void handle(HttpExchange httpStream) throws IOException {
      StringBuilder response = new StringBuilder();
      Map<String, String> parms = queryToMap(httpStream.getRequestURI().getQuery());

      String id = "Error: no id provided";

      if (parms.containsKey("id")) {
        id = parms.get("id");
        response.append(getConfig(id));
      } else {
        response.append("<b>Please provide the id for the configuration</b>");
      }

      writeResponse(httpStream, response.toString());

      Config.LOG.info("Configuration GET request for id='{}' to client='{}'.", id,
          httpStream.getRemoteAddress().toString());
    }
  }


  /**
   * The Class ViewHandler.
   */
  class ViewHandler implements HttpHandler {

    /**
     * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
     */
    public void handle(HttpExchange httpExchange) throws IOException {
      StringBuilder response = new StringBuilder();
      String id = "Error: no id provided";

      response.append(MessageFormat.format(
          "<html>\n<head><title>{0}</title></head>\n<body>\n"
              + "<h1><img src=\"{2}\">{0}</h1><h2>{1}</h2>\n",
          new Object[] {HTML_PAGE_TITLE, "Stored Configurations", LOGO_PATH}));

      Map<String, String> parms;

      if (httpExchange.getRequestURI().getQuery() == null) {
        response.append("<h2>Request not valid.</h1>");
      } else {
        parms = queryToMap(httpExchange.getRequestURI().getQuery());

        if (parms.containsKey("id")) {
          id = parms.get("id");
          response.append("<b>ID</b> : " + id + "<br/></br>");
          String config = viewConfig(id);
          if (config == null) {
            response.append("<h2>No configuration found with this id.</h2></br>");
          } else {
            response.append(config);
          }

        } else {
          response.append("<b>Please provide the id for the configuration</b>");
        }
      }

      response.append("Response generated on: " + (new Date()).toString());
      response.append("</body></html>");

      writeResponse(httpExchange, response.toString());

      Config.LOG.info("Configuration VIEW request for Id={} served to the client at '{}'.", id,
          httpExchange.getRemoteAddress().getAddress().toString());
    }
  }


  /**
   * The Class ViewHandler.
   */
  class ViewAllHandler implements HttpHandler {

    /**
     * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
     */
    public void handle(HttpExchange httpExchange) throws IOException {
      StringBuilder response = new StringBuilder();

      String htmlHeader = "<html>\n<head><title>{0}</title></head>\n"
          + "<body>\n<h1><img src=\"{2}\">{0}</h1><h2>{1}</h2>\n";
      response.append(MessageFormat.format(
          htmlHeader,
          new Object[] {HTML_PAGE_TITLE, "Stored Configurations", LOGO_PATH}));

      String config = viewAllConfig();
      if (config == null) {
        response.append("<h2>No configuration found.</h2></br>\n");
      } else {
        response.append(config);
      }

      response.append("<p>Response generated on: " + (new Date()).toString() + "</p>");
      response.append("</body>\n</html>\n");

      writeResponse(httpExchange, response.toString());

      Config.LOG.info("Configuration VIEW ALL request served to the client at '{}'.",
          httpExchange.getRemoteAddress().getAddress().toString());

    }


  }

}
