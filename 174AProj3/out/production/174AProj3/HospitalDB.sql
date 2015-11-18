DROP SCHEMA IF EXISTS Proj2_Health_Information_System;
CREATE SCHEMA Proj2_Health_Information_System;
USE Proj2_Health_Information_System;
CREATE TABLE Guardian (
GuardianNo INTEGER,
FirstName CHAR(100),
LastName CHAR(100),
Phone CHAR(100),
Address CHAR(100),
City CHAR(100),
State CHAR(100),
Zip INTEGER,
PRIMARY KEY (GuardianNo)
);
CREATE TABLE Insurance (
PayerId INTEGER,
InsName CHAR(100),
PolicyType CHAR(100),
PRIMARY KEY (PayerId, PolicyType)
);
CREATE TABLE PatientHasGuard (
PatientID INTEGER,
GuardianNo INTEGER NOT NULL,
PatientRole CHAR(100),
BirthTime DATETIME NULL,
Suffix CHAR(100) DEFAULT NULL,
xmlHealthCreation DATETIME NULL,
GivenName CHAR(100),
FamilyName CHAR(100),
Gender Char(100),
ProviderID CHAR(100),
PayerID INTEGER NOT NULL,
PolicyType CHAR(100) NOT NULL,
PRIMARY KEY (PatientID),
FOREIGN KEY (GuardianNo) REFERENCES Guardian(GuardianNo)
ON DELETE NO ACTION,
FOREIGN KEY (PayerID, PolicyType) REFERENCES Insurance(PayerID, PolicyType)
ON DELETE NO ACTION
);
CREATE TABLE PatientPays (
PatientID INTEGER,
PayerID INTEGER,
Purpose CHAR(100),
PRIMARY KEY (PatientID, PayerID, Purpose),
FOREIGN KEY (PatientID) REFERENCES PatientHasGuard(PatientID),
FOREIGN KEY (PayerID) REFERENCES Insurance(PayerID)
);
CREATE TABLE Author (
AuthorID CHAR(100),
AuthorTitle CHAR(100),
AuthorFirstName CHAR(100),
AuthorLastName CHAR(100),
PRIMARY KEY (AuthorID)
);
CREATE TABLE Assigned (
ParticipatingRole CHAR(100),
AuthorID CHAR(100),
PatientID INTEGER,
PRIMARY KEY (ParticipatingRole, AuthorID, PatientID),
FOREIGN KEY (AuthorID) REFERENCES Author(AuthorID),
FOREIGN KEY (PatientID) REFERENCES PatientHasGuard(PatientID)
);
 
CREATE TABLE LabTestReport (
LabTestResultID INTEGER,
LabTestType CHAR(100),
PatientVisitID INTEGER,
PatientID INTEGER,
TestResultValue CHAR(50),
LabTestResultDate DATETIME NULL,
ReferenceRangeHigh CHAR(100),
ReferenceRangeLow CHAR(100),
PRIMARY KEY (LabTestResultID),
FOREIGN KEY (PatientID) REFERENCES PatientHasGuard(PatientID)
ON DELETE NO ACTION
);
CREATE TABLE Planned (
PatientID INTEGER,
PlanID INTEGER,
DatePlanned DATETIME NULL,
Activity CHAR(100),
PRIMARY KEY (PlanID, PatientID),
FOREIGN KEY (PatientID) REFERENCES PatientHasGuard(PatientID)
ON DELETE CASCADE
);
CREATE TABLE AllergicTo (
SubstanceID INTEGER,
PatientID INTEGER,
Substance CHAR(100),
Reaction CHAR(100),
SubstanceStatus CHAR(100),
PRIMARY KEY (SubstanceID, PatientID),
FOREIGN KEY (PatientID) REFERENCES PatientHasGuard(PatientID)
ON DELETE CASCADE
);
CREATE TABLE FamilyMember (
FamilyID INTEGER,
Diagnosis CHAR(100),
Age INTEGER,
PRIMARY KEY (FamilyID, Diagnosis)
);
CREATE TABLE FamilyHistory (
FamilyID INTEGER,
PatientID INTEGER,
Diagnosis CHAR(100),
Relationship CHAR(100),
PRIMARY KEY (FamilyID, Diagnosis, PatientID),
FOREIGN KEY (PatientID) REFERENCES PatientHasGuard(PatientID)
ON DELETE CASCADE,
FOREIGN KEY (FamilyID, Diagnosis) REFERENCES FamilyMember(FamilyID, Diagnosis)
ON DELETE CASCADE
);