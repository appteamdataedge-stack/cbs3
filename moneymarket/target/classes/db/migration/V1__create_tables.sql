-- File: create_tables.sql (use as Flyway V1)
CREATE TABLE Cust_Master (
  Cust_Id INT AUTO_INCREMENT PRIMARY KEY,
  Ext_Cust_Id VARCHAR(20) NOT NULL,
  Cust_Type ENUM('Individual', 'Corporate', 'Bank') NOT NULL,
  First_Name VARCHAR(50),
  Last_Name VARCHAR(50),
  Trade_Name VARCHAR(100),
  Address_1 VARCHAR(200),
  Mobile VARCHAR(15),
  Branch_Code VARCHAR(10) DEFAULT '001' NOT NULL,
  Maker_Id VARCHAR(20) NOT NULL,
  Entry_Date DATE NOT NULL,
  Entry_Time TIME NOT NULL,
  Verifier_Id VARCHAR(20),
  Verification_Date DATE,
  Verification_Time TIME,
  CONSTRAINT chk_customer_name CHECK (
    (Cust_Type = 'Individual' AND First_Name IS NOT NULL AND Last_Name IS NOT NULL)
    OR (Cust_Type IN ('Corporate', 'Bank') AND Trade_Name IS NOT NULL)
  )
);

CREATE TABLE Prod_Master (
  Product_Id INT AUTO_INCREMENT PRIMARY KEY,
  Product_Code VARCHAR(10) NOT NULL UNIQUE,
  Product_Name VARCHAR(50) NOT NULL,
  Cum_GL_Num VARCHAR(20) NOT NULL,
  Customer_Product TINYINT(1) DEFAULT 1,
  Interest_Bearing TINYINT(1) DEFAULT 0,
  Maker_Id VARCHAR(20) NOT NULL,
  Entry_Date DATE NOT NULL,
  Entry_Time TIME NOT NULL,
  Verifier_Id VARCHAR(20),
  Verification_Date DATE,
  Verification_Time TIME
);

CREATE TABLE Sub_Prod_Master (
  Sub_Product_Id INT AUTO_INCREMENT PRIMARY KEY,
  Product_Id INT NOT NULL,
  Sub_Product_Code VARCHAR(10) NOT NULL UNIQUE,
  Sub_Product_Name VARCHAR(50) NOT NULL,
  Intt_Code VARCHAR(10),
  Cum_GL_Num VARCHAR(10) NOT NULL,
  Ext_GL_Num VARCHAR(10),
  Interest_Increment DECIMAL(10,4),
  Effective_Interest_Rate DECIMAL(10,4),
  Interest_Payable_GL_Num VARCHAR(20),
  Interest_Income_GL_Num VARCHAR(20),
  Sub_Product_Status ENUM('Active', 'Inactive', 'Deactive') NOT NULL,
  Maker_Id VARCHAR(20) NOT NULL,
  Entry_Date DATE NOT NULL,
  Entry_Time TIME NOT NULL,
  Verifier_Id VARCHAR(20),
  Verification_Date DATE,
  Verification_Time TIME,
  FOREIGN KEY (Product_Id) REFERENCES Prod_Master(Product_Id) ON DELETE CASCADE,
  FOREIGN KEY (Intt_Code) REFERENCES Interest_Rate_Master(Intt_Code)
);

CREATE TABLE Cust_Acct_Master (
  Account_No VARCHAR(13) PRIMARY KEY,
  Sub_Product_Id INT NOT NULL,
  GL_Num VARCHAR(20) NOT NULL,
  Cust_Id INT NOT NULL,
  Cust_Name VARCHAR(100),
  Acct_Name VARCHAR(100) NOT NULL,
  Date_Opening DATE NOT NULL,
  Tenor INT(3),
  Date_Maturity DATE,
  Date_Closure DATE,
  Branch_Code VARCHAR(10) NOT NULL,
  Account_Status ENUM('Active', 'Inactive', 'Closed', 'Dormant') NOT NULL,
  FOREIGN KEY (Sub_Product_Id) REFERENCES Sub_Prod_Master(Sub_Product_Id),
  FOREIGN KEY (Cust_Id) REFERENCES Cust_Master(Cust_Id),
  FOREIGN KEY (GL_Num) REFERENCES GL_setup(GL_Num)
);

CREATE TABLE OF_Acct_Master (
  Account_No VARCHAR(13) PRIMARY KEY,
  Sub_Product_Id INT NOT NULL,
  GL_Num VARCHAR(20) NOT NULL,
  Acct_Name VARCHAR(100) NOT NULL,
  Date_Opening DATE NOT NULL,
  Date_Closure DATE,
  Branch_Code VARCHAR(10) NOT NULL,
  Account_Status ENUM('Active', 'Inactive', 'Closed') NOT NULL,
  Reconciliation_Required BOOLEAN NOT NULL,
  FOREIGN KEY (Sub_Product_Id) REFERENCES Sub_Prod_Master(Sub_Product_Id),
  FOREIGN KEY (GL_Num) REFERENCES GL_setup(GL_Num)
);

CREATE TABLE Tran_Table (
  Tran_Id VARCHAR(20) PRIMARY KEY,
  Tran_Date DATE NOT NULL,
  Value_Date DATE NOT NULL,
  Dr_Cr_Flag ENUM('D', 'C') NOT NULL,
  Tran_Status ENUM('Entry', 'Posted', 'Verified') NOT NULL,
  Account_No VARCHAR(20) NOT NULL,
  Tran_Ccy VARCHAR(3) NOT NULL,
  FCY_Amt DECIMAL(20, 2) NOT NULL,
  Exchange_Rate DECIMAL(10, 4) NOT NULL,
  LCY_Amt DECIMAL(20, 2) NOT NULL,
  Debit_Amount DECIMAL(20, 2),
  Credit_Amount DECIMAL(20, 2),
  Narration VARCHAR(100),
  UDF1 VARCHAR(50),
  Pointing_Id INT(4),
  FOREIGN KEY (Account_No) REFERENCES Cust_Acct_Master(Account_No)
);

CREATE TABLE GL_setup (
  GL_Name VARCHAR(50),
  Layer_Id INT(1),
  Layer_GL_Num VARCHAR(9),
  Parent_GL_Num VARCHAR(9),
  GL_Num VARCHAR(9) PRIMARY KEY
);

-- Additional tables required for Phase-1

-- Table for Account Balance
CREATE TABLE Acct_Bal (
  Account_No VARCHAR(13) PRIMARY KEY,
  Current_Balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
  Available_Balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
  Last_Updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (Account_No) REFERENCES Cust_Acct_Master(Account_No)
);

-- Table for GL Balance
CREATE TABLE GL_Balance (
  GL_Num VARCHAR(9) PRIMARY KEY,
  Current_Balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
  Last_Updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (GL_Num) REFERENCES GL_setup(GL_Num)
);

-- Table for GL Movements
CREATE TABLE GL_Movement (
  Movement_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
  Tran_Id VARCHAR(20) NOT NULL,
  GL_Num VARCHAR(9) NOT NULL,
  Dr_Cr_Flag ENUM('D', 'C') NOT NULL,
  Tran_Date DATE NOT NULL,
  Value_Date DATE NOT NULL,
  Amount DECIMAL(20, 2) NOT NULL,
  Balance_After DECIMAL(20, 2) NOT NULL,
  FOREIGN KEY (GL_Num) REFERENCES GL_setup(GL_Num),
  FOREIGN KEY (Tran_Id) REFERENCES Tran_Table(Tran_Id)
);

-- Table for Interest Accrual Transactions
CREATE TABLE Intt_Accr_Tran (
  Accr_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
  Tran_Id VARCHAR(20) NOT NULL,
  Account_No VARCHAR(13) NOT NULL,
  Accrual_Date DATE NOT NULL,
  Interest_Rate DECIMAL(10, 4) NOT NULL,
  Amount DECIMAL(20, 2) NOT NULL,
  Status ENUM('Pending', 'Posted', 'Verified') NOT NULL DEFAULT 'Pending',
  FOREIGN KEY (Account_No) REFERENCES Cust_Acct_Master(Account_No),
  FOREIGN KEY (Tran_Id) REFERENCES Tran_Table(Tran_Id)
);

-- Table for GL Movement for Accrual
CREATE TABLE GL_Movement_Accrual (
  Movement_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
  Accr_Id BIGINT NOT NULL,
  GL_Num VARCHAR(9) NOT NULL,
  Dr_Cr_Flag ENUM('D', 'C') NOT NULL,
  Accrual_Date DATE NOT NULL,
  Amount DECIMAL(20, 2) NOT NULL,
  Status ENUM('Pending', 'Posted', 'Verified') NOT NULL DEFAULT 'Pending',
  FOREIGN KEY (GL_Num) REFERENCES GL_setup(GL_Num),
  FOREIGN KEY (Accr_Id) REFERENCES Intt_Accr_Tran(Accr_Id)
);

-- Table for Account Balance for Accrual
CREATE TABLE Acct_Bal_Accrual (
  Accr_Bal_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
  Account_No VARCHAR(13) NOT NULL,
  Accrual_Date DATE NOT NULL,
  Interest_Amount DECIMAL(20, 2) NOT NULL,
  FOREIGN KEY (Account_No) REFERENCES Cust_Acct_Master(Account_No)
);

-- Table for Account Number Sequence
CREATE TABLE Account_Seq (
  GL_Num VARCHAR(9) PRIMARY KEY,
  Seq_Number INT NOT NULL DEFAULT 0,
  Last_Updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (GL_Num) REFERENCES GL_setup(GL_Num)
);
