/**
 * 
 */
package cc.naos.btrcs.data;

/**
 * @author carlos
 *
 */

//import java.awt.Image;
//import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class StoredData
{
	private static final String dbPath = "db/naos.sqlite";
	private static final String driver = "org.sqlite.JDBC";
	private static Connection connection = null;
	private static boolean connected = false;
	public static String pathToNaosFolder = null;
	
	//private PreparedStatement pstatement = null;
	/*private static final String psql = "SELECT religion_amt, violence_amt, patriotism_amt, filepath," +
			"classification, EEG, GSR, EMG FROM images ORDER BY id";*/
	
	private static void createConnection() {
		try {
			// JDBC Driver to Use
			Class.forName(driver);
			
			System.out.println("Connecting to database...");
			// Create Connection Object to SQLite Database
			// If you want to only create a database in memory, exclude the +fileName
			connection = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			connected = true;
			System.out.println("Database connection established...");
		} catch(SQLException e) {
			// Print some generic debug info
			System.out.println(e.getMessage());
			e.printStackTrace();
			connected = false;
		} catch(ClassNotFoundException cnfe) {
			// Print some generic debug info
			System.out.println(cnfe.getMessage());
			connected = false;
		}
		
	}
	
	public static boolean isConnected() {
		return connected;
	}
	
	public static Connection getConnection() {
		if(connection == null || connected == false) {
			createConnection();
		}
		return connection;
	}
	
	
	// returns entire training set
	public static ArrayList<HashMap<String,Object>> retrieveTrainingSet() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM data ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(8);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("stimpath", pathToNaosFolder + rs.getString("stimpath"));
				hash.put("eeg", new Integer(rs.getInt("EEG")));
				hash.put("gsr", new Integer(rs.getInt("GSR")));
				hash.put("emg", new Integer(rs.getInt("EMG")));
				hash.put("question", new Integer(rs.getInt("question")));
				hash.put("participantID", new Integer(rs.getInt("participant")));
				hash.put("class", new Integer(rs.getInt("class")));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	// returns all stimuli in db
	public static ArrayList<HashMap<String,Object>> retrieveStimuli() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM stimuli ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(6);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("stimpath", new String(pathToNaosFolder + rs.getString("stimpath")));
				hash.put("technique", new Integer(rs.getInt("stimtechnique")));
				hash.put("ethnicity", new Integer(rs.getInt("ethnicity")));
				hash.put("age", new Integer(rs.getInt("age")));
				hash.put("gender", new Integer(rs.getInt("gender")));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	// returns all vis. techniques in db
	public static ArrayList<HashMap<String,Object>> retrieveTechniques() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM technique ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(2);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("name", rs.getString("name"));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	// returns all ethnicities in db
	public static ArrayList<HashMap<String,Object>> retrieveEthnicities() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM ethnic_group ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(2);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("ethnicity", rs.getString("ethnicity"));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}

	// returns all age groups in db
	public static ArrayList<HashMap<String,Object>> retrieveAgeGroups() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM age_group ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(2);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("age", rs.getString("age"));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	// returns all genders in db
	public static ArrayList<HashMap<String,Object>> retrieveGenders() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM gender ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(2);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("gender", rs.getString("gender"));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	// returns all questions in db
	public static ArrayList<HashMap<String,Object>> retrieveQuestions() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>(10);
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM questions ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(3);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("question", rs.getString("question"));
				//hash.put("yesAnswer", new Integer(rs.getInt("yes_answer")));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}

	// returns all classes in db
	public static ArrayList<HashMap<String,Object>> retrieveClasses() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM classes ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(2);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("name", rs.getString("name"));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}

	// returns all participants in db
	public static ArrayList<HashMap<String,Object>> retrieveParticipants() {
		ArrayList<HashMap<String,Object>> elems = new ArrayList<HashMap<String,Object>>();
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM participants ORDER BY id");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(4);
				hash.put("id", new Integer(rs.getInt("id")));
				hash.put("ethnicity", new Integer(rs.getInt("ethnicity")));
				hash.put("age", new Integer(rs.getInt("age")));
				hash.put("gender", new Integer(rs.getInt("gender")));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	public static void write(String sql) {      		
		try {
			if(connection == null || connected == false) {
				createConnection();
			}
			Statement statement = connection.createStatement();
			int rows = statement.executeUpdate(sql);
			statement.close();
		} catch(SQLException sqlex) {
			System.out.println(sqlex.getMessage());
			sqlex.printStackTrace();
		}
	}
}
