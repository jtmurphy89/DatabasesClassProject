package HospitalDB;


import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;


public class DBHelper {

    // Some maps for accessing db data
    public String tableNameList = "Guardian Insurance PatientHasGuard PatientPays Author Assigned LabTestReport Planned AllergicTo FamilyMember FamilyHistory";
    public HashMap<String,String> messagesMap = new HashMap<>();
    public Map<String, List<String>> newParamsMap = new HashMap<>();
    public Map<String, List<String>> isNullMap = new HashMap<>();
    public Map<String, List<Integer>> typeListMap = new HashMap<>();
    public HashMap<String, String> SQLInsertStringMap = new HashMap<>();
    private static final Map<String, List<String>> DBTableMap = new HashMap<String, List<String>>(){{
        // Hard-code the ugly out of the TA's healthmessagesexchange database
        put("Guardian", Arrays.asList("GuardianNo", "FirstName","LastName","Phone","address", "city", "state", "zip" ));
        put("Insurance", Arrays.asList("PayerId", "Name", "PolicyType"));
        put("PatientHasGuard", Arrays.asList("patientId", "GuardianNo", "Relationship", "BirthTime", "", "xmlHealthCreation", "GivenName", "FamilyName", "", "providerId", "PayerId", "PolicyType"));
        put("PatientPays", Arrays.asList("patientId", "PayerId", "Purpose"));
        put("Author", Arrays.asList("AuthorId", "AuthorTitle", "AuthorFirstName", "AuthorLastName"));
        put("Assigned", Arrays.asList("ParticipatingRole", "AuthorId", "patientId"));
        put("LabTestReport", Arrays.asList("LabTestResultId", "LabTestType", "PatientVisitId", "patientId", "TestResultValue", "LabTestPerformedDate", "ReferenceRangeHigh", "ReferenceRangeLow"));
        put("Planned", Arrays.asList("patientId", "PlanId", "ScheduledDate", "Activity"));
        put("AllergicTo", Arrays.asList("Id", "patientId", "Substance", "Reaction", "Status"));
        put("FamilyMember", Arrays.asList("RelativeId", "Diagnosis", "age"));
        put("FamilyHistory", Arrays.asList("RelativeId", "patientId", "Diagnosis", "Relation"));

    }};

    // Prepare the basic Insert/Update strings for tables in the hospitalDB
    public String getSQLInsertString(String databaseName, String tableName, List<String> paramArray){
        String beginString = "INSERT INTO " + databaseName + "." + tableName + "(";
        String middleString = "VALUES(";
        String endString = "ON DUPLICATE KEY UPDATE ";

        for(int i = 0; i < paramArray.size() -1; i++){
            beginString = beginString.concat(paramArray.get(i) + ", ");
            middleString = middleString.concat("?, ");
            endString = endString.concat(paramArray.get(i) + "= ?, ");
        }
        beginString = beginString.concat(paramArray.get(paramArray.size()-1) + ") ");
        middleString = middleString.concat("?) ");
        endString = endString.concat(paramArray.get(paramArray.size()-1) + "= ?");
        return beginString + middleString+ endString;

    }

    // Initialize all those hashmaps...
    public void initializeHospitalDBMetaData(Connection hospitalConn, String hospitalDBName){
        try {
            DatabaseMetaData hospitalDBMetadata = hospitalConn.getMetaData();
            for (String hospitalTableName : tableNameList.split(" ")) {
                ResultSet colSet = hospitalDBMetadata.getColumns(null, hospitalDBName, hospitalTableName, null);
                List<String> newParams = new ArrayList<>();
                List<String> isNullable = new ArrayList<>();
                List<Integer> typeList = new ArrayList<>();
                while (colSet.next()) {
                    newParams.add(colSet.getString("COLUMN_NAME"));
                    isNullable.add(colSet.getString("IS_NULLABLE"));
                    typeList.add(colSet.getInt("DATA_TYPE"));
                }
                String insertString = getSQLInsertString(hospitalDBName, hospitalTableName, newParams);
                newParamsMap.put(hospitalTableName, newParams);
                isNullMap.put(hospitalTableName, isNullable);
                typeListMap.put(hospitalTableName, typeList);
                SQLInsertStringMap.put(hospitalTableName, insertString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // this guy extracts/parses data from healthmessagesexchange db and "maps" it into the hospital db
    public void initializeDBData(Connection messagesConn, String messagesDBName, Connection hospitalConn, String hospitalDBName){
        try{
            ResultSet rs;
            initializeHospitalDBMetaData(hospitalConn, hospitalDBName);
            DatabaseMetaData messagesDBMetadata = messagesConn.getMetaData();
            ResultSet tables = messagesDBMetadata.getTables(null, null, "%", null);
            while(tables.next()){
                String tableName = tables.getString("TABLE_NAME");

                // this is so we can pass in the current date for Patient.xmlCreationDate
                String currentDateString = getCurrentTimestampString();

                // initialize query string, and statement, run query
                String queryString = "SELECT * FROM " + messagesDBName + "." + tableName;
                Statement stmt = messagesConn.createStatement();
                rs = stmt.executeQuery(queryString);
                ResultSetMetaData messageTableMetaData = rs.getMetaData();
                int columnCount = messageTableMetaData.getColumnCount();
                while(rs.next()){
                    // Update the Last_Accessed attribute of the healthmessagesexchange table
                    String msgId = rs.getString("MsgId");
                    String messageUpdateString = "UPDATE "
                            + messagesDBName + "." + tableName + " "
                            + "SET " + messagesDBName + "." + tableName
                            + ".Last_Accessed = " + "\"" + currentDateString
                            + "\"" + " WHERE " + messagesDBName + "."
                            + tableName + ".MsgId = " + "\'" + msgId + "\'";
                    executeUpdate(messagesConn, messageUpdateString);

                    // Populate the healthmessagesexchange hash map with the row data, which are all varchars apparently...
                    messagesMap.put("xmlHealthCreation", currentDateString);
                    for(int i = 1; i <= columnCount; i++){
                        messagesMap.put(messageTableMetaData.getColumnName(i), rs.getString(i));
                    }
                    // Now iterate over all tables in hospital database, populating them with info from messages database
                    for(String hospitalTableName: tableNameList.split(" ")){
                        List<String> oldParams = DBTableMap.get(hospitalTableName);
                        List<String> newParams = newParamsMap.get(hospitalTableName);
                        List<String> isNullable = isNullMap.get(hospitalTableName);
                        List<Integer> typeList = typeListMap.get(hospitalTableName);
                        String insertString = SQLInsertStringMap.get(hospitalTableName);
                        PreparedStatement insertStatement = null;
                        try{
                            hospitalConn.setAutoCommit(false);
                            insertStatement = hospitalConn.prepareStatement(insertString);
                            int numParams = newParams.size();
                            // to flag rows that have NULL values for primary keys
                            boolean doNothing = false;
                            for(int i = 0; i < numParams; i++){
                                int offset = i + numParams;
                                String obj = messagesMap.get(oldParams.get(i));
                                if(obj != null){
                                    // should probs make those strings into DATETIMEs
                                    if(typeList.get(i) == 93){
                                        SimpleDateFormat dateFormatter = new SimpleDateFormat(
                                                "MM/dd/yyyy h:mm:ss a");
                                        Date date = dateFormatter.parse(obj);
                                        Timestamp dateObj = new Timestamp(date.getTime());
                                        insertStatement.setObject(i+1, dateObj, typeList.get(i));
                                        insertStatement.setObject(offset +1, dateObj, typeList.get(i));

                                    }
                                    // Everything else
                                    else{
                                        insertStatement.setObject(i+1, obj, typeList.get(i));
                                        insertStatement.setObject(offset +1, obj, typeList.get(i));
                                    }
                                }
                                // Nope, nope, nope, nope, nope...
                                else if(isNullable.get(i).equals("NO")){doNothing = true;}
                                else{
                                    insertStatement.setNull(i+1,Types.NULL);
                                    insertStatement.setNull(offset + 1, Types.NULL);}
                            }
                            // do something, ffs
                            if(!doNothing){ insertStatement.executeUpdate(); hospitalConn.commit(); }
                        }finally {
                            if (insertStatement != null) {
                                insertStatement.close();
                            }
                            hospitalConn.setAutoCommit(true);
                        }
                    }
                    messagesMap.clear();
                }
            }
        } catch (Exception e) { e.printStackTrace();}
    }


    public String getCurrentTimestampString() {
        java.util.Date curDate = new java.util.Date();
        Timestamp timestamp = new Timestamp(curDate.getTime());
        SimpleDateFormat dateFormatter = new SimpleDateFormat(
                "MM/dd/yyyy h:mm:ss a");
        return dateFormatter.format(timestamp);
    }

    public boolean executeUpdate(Connection conn, String command)
            throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(command); // This will throw a SQLException if it fails
            return true;
        } finally {

            // This will run whether we throw an exception or not
            if (stmt != null) {
                stmt.close();
            }
        }
    }
}
