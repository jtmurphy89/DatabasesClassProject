package HospitalDB;

import javafx.event.EventHandler;
import javafx.fxml.FXML;

import java.io.File;
import java.sql.*;
import java.util.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import javafx.scene.paint.Color;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;


public class HospitalDBController extends AnchorPane implements Initializable {

    /**
     *
     * Variables and such...
     *
     */
    @FXML private AnchorPane patientAnchorPane, doctorAnchorPane, adminAnchorPane;
    @FXML private TextField patientSearch, doctorSearch, substanceIDField, substanceField, reactionField, statusField, planIDField, activityField, datePlannedField;
    @FXML private Label sqlOutput, drGrover;
    @FXML private TabPane mainTabPane, patientTabPane, doctorTabPane, adminTabPane;
    @FXML private TableView<Map> pPatientHasGuardView, pGuardianView, pPatientPaysView, pPlannedView,
            pLabTestReportView, pAllergicToView, pFamilyHistoryView, pAssignedView, dPatientHasGuardView, dGuardianView,
            dPatientPaysView, dPlannedView, dLabTestReportView, dAllergicToView, dFamilyHistoryView, dAssignedView,
            adminQueryView1, adminQueryView2, adminQueryView3, adminQueryView4;
    @FXML private Tab patientTab, doctorTab, adminTab, pPatientHasGuardTab, pGuardianTab, pPatientPaysTab, pPlannedTab, pLabTestReportTab,
            pAllergicToTab, pFamilyHistoryTab, pAssignedTab, dPatientHasGuardTab, dGuardianTab, dPatientPaysTab, dPlannedTab, dLabTestReportTab,
            dAllergicToTab, dFamilyHistoryTab, dAssignedTab, adminQueryTab1, adminQueryTab2, adminQueryTab3, adminQueryTab4;
    private static Connection healthMessagesConn;
    private static Connection hospitalDBConn;
    private static String hostname = "localhost:3306";
    private static String username = "root";
    private static String pw = "";
    private static String messagesDBName = "healthmessagesexchange2";
    private static String hospitalDBName = "Proj2_Health_Information_System";
    private static DBHelper dbHelper = new DBHelper();
    private String adminQuery1 = "SELECT Proj2_Health_Information_System.AllergicTo.Substance, COUNT(DISTINCT Proj2_Health_Information_System.AllergicTo.PatientID) FROM Proj2_Health_Information_System.AllergicTo WHERE Proj2_Health_Information_System.AllergicTo.Substance IS NOT NULL GROUP BY Proj2_Health_Information_System.AllergicTo.Substance";
    private String adminQuery2 = "SELECT Proj2_Health_Information_System.AllergicTo.PatientID, COUNT(*) FROM Proj2_Health_Information_System.AllergicTo WHERE Proj2_Health_Information_System.AllergicTo.Substance IS NOT NULL GROUP BY Proj2_Health_Information_System.AllergicTo.PatientID HAVING COUNT(*) > 1";
    private String adminQuery3 = "SELECT DISTINCT Proj2_Health_Information_System.Planned.PatientID FROM Proj2_Health_Information_System.Planned WHERE DATE(Proj2_Health_Information_System.Planned.DatePlanned) = DATE(CURDATE())";
    private String adminQuery4 = "SELECT Proj2_Health_Information_System.Assigned.AuthorID, COUNT(*) FROM Proj2_Health_Information_System.Assigned GROUP BY Proj2_Health_Information_System.Assigned.AuthorID HAVING COUNT(*) > 1";
    private List<TableView> pdTableViewList, patientEditableTables, doctorEditableTables;
    private List<String> tableNames, adminColList;
    private List<TextField> patientTFs, doctorTFs;
    private ObservableList<ObservableList> data;
    private String patientIDNum, guardianNo, user;
    private ObservableList<Map> nullData = FXCollections.observableArrayList();
    private Map<String, List<TableView>> userTableListMap;
    private Map<Integer, String> planIDMap = new HashMap<>(); // in case we set a planID to NULL for deletion, we need to remember the id number for the sql update

    /**
     *
     * Create/initialize/destroy functions
     *
     */

    static void getHealthMessagesConnection(){

        Properties connectionProps = new Properties();
        connectionProps.put("user", username);
        connectionProps.put("password", pw);

        try {
            healthMessagesConn = DriverManager.getConnection("jdbc:mysql://" + hostname + "/" + messagesDBName, connectionProps);
            System.out.println("... connected as '" + username + "' to '" + hostname + "/" + messagesDBName + "'");
        } catch (SQLException e) {
            System.out.println("ERROR: Could not connect to healthMessagesDB");
            e.printStackTrace();
        }
    }
    static void getHospitalDBConnection(){
        Properties connectionProps = new Properties();
        connectionProps.put("user", username);
        connectionProps.put("password", pw);
        try {
            hospitalDBConn = DriverManager.getConnection("jdbc:mysql://" + hostname + "/" + hospitalDBName, connectionProps);
            System.out.println("... connected as '" + username + "' to '" + hostname + "/" + hospitalDBName + "'");
        } catch (SQLException e) {
            System.out.println("ERROR: Could not connect to hospitalDB");
            e.printStackTrace();
        }
    }

    static void endConnection() {
        try {
            for(AutoCloseable c: new AutoCloseable[]{healthMessagesConn, hospitalDBConn})
                if (c != null) c.close();
            System.out.println("... closed connection.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void initializeHospitalDB() {
        try { dbHelper.initializeDBData(healthMessagesConn, messagesDBName, hospitalDBConn, hospitalDBName);}
        catch (Exception e) { e.printStackTrace(); }
    }


    public List<TableColumn<Map,String>> createTableCols(List<String> cols){
        List<TableColumn<Map,String>> allCols = new ArrayList<>();
        Callback<TableColumn<Map, String>, TableCell<Map, String>> cellFactoryForMap = new Callback<TableColumn<Map, String>,TableCell<Map, String>>() {
            @Override
            public TableCell call(TableColumn p) {
                return new TextFieldTableCell(new StringConverter() {
                    @Override
                    public String toString(Object t) {
                        return t.toString();

                    };
                    @Override
                    public Object fromString(String string) {
                        return string;
                    }
                });
            }
        };
        for(String colName: cols){
            TableColumn<Map, String> col = new TableColumn<>(colName);
            col.setCellValueFactory(new MapValueFactory(colName));
            col.setCellFactory(cellFactoryForMap);
            EventHandler<TableColumn.CellEditEvent<Map, String>> cellEditor = new EventHandler<TableColumn.CellEditEvent<Map, String>>() {
                @Override
                public void handle(TableColumn.CellEditEvent<Map, String> t) {
                    t.getTableView().getItems().get(t.getTablePosition().getRow()).replace(colName, t.getNewValue());
                }
            };
            col.setOnEditCommit(cellEditor);
            allCols.add(col);
        }
        return allCols;
    }

    @FXML @Override
    public void initialize(URL location, ResourceBundle resources) {
        try{
            patientTabPane = new TabPane(); doctorTabPane = new TabPane(); adminTabPane = new TabPane();
            dbHelper.initializeDBData(healthMessagesConn, messagesDBName, hospitalDBConn, hospitalDBName);
            tableNames = Arrays.asList("PatientHasGuard", "Guardian", "PatientPays", "Planned", "LabTestReport", "AllergicTo", "FamilyHistory", "Assigned");
            List<String> pdtableNames = Arrays.asList("pPatientHasGuard", "pGuardian", "pPatientPays", "pPlanned", "pLabTestReport", "pAllergicTo", "pFamilyHistory", "pAssigned",
                    "dPatientHasGuard", "dGuardian", "dPatientPays", "dPlanned", "dLabTestReport", "dAllergicTo", "dFamilyHistory", "dAssigned");
            // create patient/doctor table views/tabs
            for(String tName: pdtableNames){
                char pOrD = tName.charAt(0);
                tName = tName.substring(1);
                TableView<Map> tView = new TableView<>();
                Tab tTab = new Tab(tName);
                tView.setItems(nullData);
                tView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                List<String> cols = dbHelper.newParamsMap.get(tName);
                tView.getColumns().setAll(createTableCols(cols));
                Field tViewField = getClass().getDeclaredField(pOrD + tName + "View");
                Field tTabField = getClass().getDeclaredField(pOrD + tName + "Tab");
                tViewField.set(this, tView);
                tTabField.set(this, tTab);
            }
            // create admin table views/tabs
            adminColList = Arrays.asList("Substance, COUNT(DISTINCT Proj2_Health_Information_System.AllergicTo.PatientID)", "PatientID, COUNT(*)", "PatientID", "AuthorID, COUNT(*)");
            int i = 1;
            for(String adminCols: adminColList){
                TableView<Map> tView = new TableView<>();
                Tab tTab = new Tab("Query " + i);
                tView.setItems(nullData);
                for(String adminc: adminCols.split(", ")){System.out.println(adminc);}
                tView.getColumns().setAll(createTableCols(Arrays.asList(adminCols.split(", "))));
                System.out.println("adminQueryView" + i + ", " + "adminQueryTab" + i);
                Field tViewField = getClass().getDeclaredField("adminQueryView" + i);
                Field tTabField = getClass().getDeclaredField("adminQueryTab" + i);
                tViewField.set(this, tView);
                tTabField.set(this, tTab);
                i++;
            }
            pdTableViewList = Arrays.asList(pPatientHasGuardView, pGuardianView, pPatientPaysView, pPlannedView,
                    pLabTestReportView, pAllergicToView, pFamilyHistoryView, pAssignedView, dPatientHasGuardView, dGuardianView,
                    dPatientPaysView, dPlannedView, dLabTestReportView, dAllergicToView, dFamilyHistoryView, dAssignedView);
            List<Tab> tabList = Arrays.asList(pPatientHasGuardTab, pGuardianTab, pPatientPaysTab, pPlannedTab, pLabTestReportTab,
                    pAllergicToTab, pFamilyHistoryTab, pAssignedTab, dPatientHasGuardTab, dGuardianTab, dPatientPaysTab, dPlannedTab, dLabTestReportTab,
                    dAllergicToTab, dFamilyHistoryTab, dAssignedTab);
            i=0;
            for(Tab tabName: tabList){
                if(i < 8){ patientTabPane.getTabs().add(tabName); }
                else{ doctorTabPane.getTabs().add(tabName); }
                tabName.setContent(pdTableViewList.get(i));
                i++;
            }
            List<TableView> adminViewList = Arrays.asList(adminQueryView1, adminQueryView2, adminQueryView3, adminQueryView4);
            List<Tab> adminTabList = Arrays.asList(adminQueryTab1, adminQueryTab2, adminQueryTab3, adminQueryTab4);
            i = 0;
            for(Tab tabName: adminTabList){
                adminTabPane.getTabs().add(tabName);
                tabName.setContent(adminViewList.get(i));
                i++;
            }
            // cosmetic stuff mostly...
            patientTabPane.setLayoutX(11); patientTabPane.setLayoutY(56); patientTabPane.setPrefWidth(722); patientTabPane.setPrefHeight(266);
            doctorTabPane.setLayoutX(11); doctorTabPane.setLayoutY(56); doctorTabPane.setPrefWidth(722); doctorTabPane.setPrefHeight(266);
            adminTabPane.setLayoutX(11); adminTabPane.setLayoutY(171); adminTabPane.setPrefWidth(722); adminTabPane.setPrefHeight(266);
            patientAnchorPane.getChildren().add(patientTabPane); doctorAnchorPane.getChildren().add(doctorTabPane); adminAnchorPane.getChildren().add(adminTabPane);
            patientTab.setContent(patientAnchorPane); doctorTab.setContent(doctorAnchorPane); adminTab.setContent(adminAnchorPane);
            patientEditableTables = Arrays.asList(pPatientHasGuardView, pGuardianView); doctorEditableTables = Arrays.asList(dAllergicToView,dPlannedView);
            userTableListMap = new HashMap<>();
            userTableListMap.put("Patient", pdTableViewList.subList(0, 7)); userTableListMap.put("Doctor", pdTableViewList.subList(8, 15));
            mainTabPane.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
            tabChangeListener.changed(null, null, patientTab); pPatientHasGuardView.setEditable(true); pGuardianView.setEditable(true);
            ((TableColumn<Map,String>)pPatientHasGuardView.getColumns().get(0)).setEditable(false);
            ((TableColumn<Map,String>)pPatientHasGuardView.getColumns().get(1)).setEditable(false);
            ((TableColumn<Map,String>)pPatientHasGuardView.getColumns().get(3)).setEditable(false);
            ((TableColumn<Map,String>)pPatientHasGuardView.getColumns().get(5)).setEditable(false);
            ((TableColumn<Map,String>)pGuardianView.getColumns().get(0)).setEditable(false);
            ((TableColumn<Map,String>)pGuardianView.getColumns().get(1)).setEditable(false);
            dAllergicToView.setEditable(true); dPlannedView.setEditable(true);
            ((TableColumn<Map,String>)dAllergicToView.getColumns().get(0)).setEditable(false);
            ((TableColumn<Map,String>)dAllergicToView.getColumns().get(1)).setEditable(false);
            ((TableColumn<Map,String>)dPlannedView.getColumns().get(0)).setEditable(false);
            substanceField.setPromptText("Substance"); substanceIDField.setPromptText("SubstanceID"); statusField.setPromptText("Status"); reactionField.setPromptText("Reaction");
            planIDField.setPromptText("PlanID"); activityField.setPromptText("Activity"); datePlannedField.setPromptText("YYYY-MM-DD HH:MM:SS");
            patientSearch.setPromptText("PatientID (e.g. try 12345)");
            drGrover.setText("Welcome to The Hospital's Database! Please choose your user type below:");
            doctorSearch.setPromptText("PatientID (e.g. try 12345)");
            substanceField.setPromptText("Substance"); substanceIDField.setPromptText("SubstanceID"); statusField.setPromptText("Status"); reactionField.setPromptText("Reaction");
            planIDField.setPromptText("PlanID"); activityField.setPromptText("Activity"); datePlannedField.setPromptText("YYYY-MM-DD HH:MM:SS");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     *
     * Search for and update a specific patient record:
     *
     *
     */

    // function to query all tables in the database and the grab table data for a given patient id
    // it then auto-populates all the table views of the current patient/doctor interface
    @FXML
    private void searchPatient() throws SQLException {
        planIDMap.clear(); // clear the values of the planid's
        patientIDNum = (user == "Patient") ? patientSearch.getText().trim() : doctorSearch.getText().trim();
        System.out.println(patientIDNum);
        if(!patientIDNum.equals("")){ // you may have accidentally pressed search, in which case we'd like to not throw an error
            getGuardianNo();
            int tableIndex = 0;
            List<TableView> tViewList = userTableListMap.get(user);
            for(TableView tView: tViewList){
                String query = "SELECT * FROM " + hospitalDBName + "." + tableNames.get(tableIndex) + " WHERE " + hospitalDBName + "." + tableNames.get(tableIndex) + ".PatientID = " + patientIDNum;
                if(tView == pGuardianView || tView == dGuardianView){
                    query = "SELECT * FROM " + hospitalDBName + ".Guardian WHERE " + hospitalDBName + ".Guardian.GuardianNo = " + guardianNo;
                }
                System.out.println(query);
                String tName = tableNames.get(tableIndex);
                List<String> colNames = dbHelper.newParamsMap.get(tName);
                generateDataInMap(query, tName, tView, colNames);
                tableIndex++;
            }
        }
    }

    // function to execute a SQL update and refresh the table views
    // of editable tables (specified in initialize) for the current patient id of the patient/doctor interface
    @FXML
    private void updatePatient() throws SQLException {
        List<TableView> userTable = (user == "Patient") ? patientEditableTables : doctorEditableTables;
        Integer count = 0;
        for(TableView tView: userTable){
            String tableName = tableNames.get(userTableListMap.get(user).indexOf(tView));
            List<String> colNames = dbHelper.newParamsMap.get(tableName);
            boolean wasDelete = false; // this signifies if we do a delete so that we know to reset the planID map
            for(int i = 0; i < tView.getItems().size(); i++){
                String updateString = "UPDATE " + hospitalDBName + "." + tableName + " SET ";
                for(int j = 2; j < colNames.size(); j++){
                    String col = colNames.get(j);
                    String param = (String)((Map)tView.getItems().get(i)).get(col);
                    updateString = (param == null)? updateString.concat(col + " = '" + "NULL" + "', "): updateString.concat(col + " = '" + param + "', ");
                }
                String whereString = " WHERE " + hospitalDBName + "." + tableName + "." + ((tableName == "Guardian") ? "GuardianNo = " + guardianNo : "PatientID = " + patientIDNum);
                updateString = updateString.substring(0, updateString.length() - 2) + whereString;
                // special case of deleting an activity
                if(tView == dPlannedView && ((Map)tView.getItems().get(i)).get("PlanID").equals("NULL")){
                    wasDelete = true;
                    updateString = "DELETE FROM " + hospitalDBName + ".Planned" + " WHERE " + hospitalDBName
                            + ".Planned.PatientID = " + patientIDNum + " AND " + hospitalDBName + ".Planned.PlanID = " + planIDMap.get(i);
                }
                count = updateHelper(updateString, count);
                System.out.println(updateString);
            }
            if(wasDelete) { planIDMap.clear(); } // clear the planID map so it can be refilled by generateDataInMap
            String query = "SELECT * FROM " + hospitalDBName + "." + tableName + " WHERE " + hospitalDBName + "."
                    + tableName + "." + ((tableName == "Guardian") ? "GuardianNo = " + guardianNo : "PatientID = " + patientIDNum);
            generateDataInMap(query, tableName, tView, dbHelper.newParamsMap.get(tableName));
        }
        sqlOutput.setText("Successfully updated!");
        sqlOutput.setTextFill(Color.GREEN);
    }


    // function to execute a particular SQL update. Count specifies the current number of rows that have been updated
    private Integer updateHelper(String updateString, Integer count) throws SQLException {
        PreparedStatement stmt = null;
        try{
            hospitalDBConn.setAutoCommit(false);
            stmt = hospitalDBConn.prepareStatement(updateString);
            stmt.executeUpdate();
            hospitalDBConn.commit();
        }
        catch (SQLException e1) {
            System.out.println("... could not execute query!");
            sqlOutput.setText(e1.getMessage());
            sqlOutput.setTextFill(Color.RED);
            e1.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        hospitalDBConn.setAutoCommit(true);
        return count ++;
    }

    // function to auto-populate the rows of a table in a specific table view given a SQL query
    private void generateDataInMap(String query, String tName, TableView tView, List<String> colNames) throws SQLException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        ObservableList<Map> allData = FXCollections.observableArrayList();
        tView.setItems(nullData);
        try{
            stmt = hospitalDBConn.prepareStatement(query);
            System.out.println(query);
            rs = stmt.executeQuery();
            while(rs.next()){
                Map<String, String> dataRow = new HashMap<>();
                for(String colName: colNames){
                    String dataVal = (rs.getString(colName) == null) ? "NULL" : rs.getString(colName);
                    dataRow.put(colName, dataVal);
                    if(colName.equals("PlanID")) { planIDMap.put(colNames.indexOf(colName), dataVal); }
                    //((TableColumn<Map,String>) tView.getColumns().get(colNames.indexOf(colName))).prefWidthProperty().bind();
                }
                allData.add(dataRow);
            }
            if(!allData.isEmpty()) { tView.setItems(allData); }
        } catch (SQLException e1) {
            System.out.println("... could not execute query!");
            sqlOutput.setText(e1.getMessage());
            sqlOutput.setTextFill(Color.RED);
            e1.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        }
    }

    // add that plan!
    @FXML
    public void addPlan() throws SQLException {
        if(!patientIDNum.equals("")){
            if(!activityField.getText().trim().equals("")){
                String activity = activityField.getText().trim(); String datePlanned = datePlannedField.getText().trim(); String planID = planIDField.getText().trim();
                String addString = "INSERT INTO " + hospitalDBName + ".Planned(PatientID, PlanID, DatePlanned, Activity) VALUES ("
                        + patientIDNum + ", " + planID + ", '" + ((datePlanned.equals("")) ? "NULL" : datePlanned)
                        + "', '" + ((planID.equals("")) ? "NULL" : activity) + "')";
                System.out.println(addString);
                System.out.flush();
                System.out.println("ONLY GOT HERE");
                System.out.flush();
                updateHelper(addString, 1);
                searchPatient();
            }
        }
    }

    // add that allergy!
    @FXML
    public void addAllergy() throws SQLException {
        if(!patientIDNum.equals("")){
            if(!substanceIDField.getText().trim().equals("")){
                String substanceID = substanceIDField.getText().trim(); String substance = substanceField.getText().trim(); String reaction = reactionField.getText().trim(); String status = statusField.getText().trim();
                String addString = "INSERT INTO " + hospitalDBName + ".AllergicTo(SubstanceID, PatientID, Substance, Reaction, SubstanceStatus) VALUES ("
                        + substanceID + ", " + patientIDNum + ", '" + ((substance.equals("")) ? "NULL" : substance)
                        + "', '" + ((reaction.equals("")) ? "NULL" : reaction) + "', '" + ((status.equals("")) ? "NULL" : status) + "')";
                updateHelper(addString, 1);
                String selectString = "SELECT * FROM " + hospitalDBName + ".AllergicTo WHERE " + hospitalDBName + ".AllergicTo.PatientID = " + patientIDNum;
                searchPatient();
            }
        }
    }

    // should probably know the guardian number so we can actually query the guardian table...
    public void getGuardianNo() throws SQLException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try{
            String query = "SELECT Proj2_Health_Information_System.PatientHasGuard.GuardianNo FROM Proj2_Health_Information_System.PatientHasGuard  WHERE Proj2_Health_Information_System.PatientHasGuard.PatientID = "
                    + patientIDNum;
            stmt = hospitalDBConn.prepareStatement(query);
            rs = stmt.executeQuery();
            rs.next();
            guardianNo = rs.getString("GuardianNo");
        } catch (SQLException e1) {
            System.out.println("... could not execute query!");
            sqlOutput.setText(e1.getMessage());
            sqlOutput.setTextFill(Color.RED);
            e1.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        }
    }

    // refresh the admin queries each time we click on the admin tab
    public void adminQueryUpdate() throws SQLException {
        generateDataInMap(adminQuery1, "Query 1", adminQueryView1, Arrays.asList(adminColList.get(0).split(", ")));
        generateDataInMap(adminQuery2, "Query 2", adminQueryView2, Arrays.asList(adminColList.get(1).split(", ")));
        generateDataInMap(adminQuery3, "Query 3", adminQueryView3, Arrays.asList(adminColList.get(2).split(", ")));
        generateDataInMap(adminQuery4, "Query 4", adminQueryView4, Arrays.asList(adminColList.get(3).split(", ")));
    }

    // refresh the rows in each table view
    public void clearTables(){
        for(TableView tView: pdTableViewList){
            tView.setItems(nullData);
            adminQueryView1.setItems(nullData);
            adminQueryView2.setItems(nullData);
            adminQueryView3.setItems(nullData);
            adminQueryView4.setItems(nullData);
        }
    }

    /**
     *
     *
     * add a custom tab change listener to main tab so we can re-initialize table views and such
     *
     *
     */
    private final ChangeListener<Tab> tabChangeListener = new ChangeListener<Tab>() {
        @Override
        public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
            String s = newValue.getText();
            System.out.println(s);
            switch(newValue.getText()){
                case "Patient":
                    user = "Patient";
                    clearTables();
                    planIDMap.clear();
                    patientSearch.clear();
                    patientSearch.setPromptText("PatientID (e.g. try 12345)");
                    drGrover.setTextFill(Color.GOLD);
                    drGrover.setText("Dr. Grover says: To update, simply enter your desired value in the table cell" +
                            "\n" + "and click Update. Editable tables: PatientHasGuard and Guardian.");
                    patientIDNum = null;
                    guardianNo = null;
                    break;
                case "Doctor":
                    user = "Doctor";
                    clearTables();
                    planIDMap.clear();
                    doctorSearch.clear(); substanceField.clear(); substanceIDField.clear(); activityField.clear(); datePlannedField.clear();
                    reactionField.clear(); statusField.clear(); planIDField.clear();
                    sqlOutput.setText("");
                    drGrover.setTextFill(Color.GOLD);
                    drGrover.setText("Dr. Grover says: To delete a plan, you MUST click on the specific row" + "\n"
                            +"of the PlanID column and enter NULL. Editable tables: Planned and AllergicTo.");
                    patientIDNum = null;
                    guardianNo = null;
                    break;
                case "Administrator":
                    user = "Administrator";
                    planIDMap.clear();
                    sqlOutput.setText("");
                    drGrover.setTextFill(Color.GOLD);
                    drGrover.setText("Dr Grover says: See the results of 4 meager queries below. " + "\n"
                            + "Why administrators at this hospital can only see 4 queries is beyond me...");
                    break;
                default:
            }
        }
    };
}
