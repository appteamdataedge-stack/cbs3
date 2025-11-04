CREATE DATABASE  IF NOT EXISTS `moneymarketdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `moneymarketdb`;
-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: moneymarketdb
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account_seq`
--

DROP TABLE IF EXISTS `account_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_seq` (
  `GL_Num` varchar(9) NOT NULL,
  `Seq_Number` int NOT NULL DEFAULT '0',
  `Last_Updated` timestamp NOT NULL,
  PRIMARY KEY (`GL_Num`),
  CONSTRAINT `account_seq_ibfk_1` FOREIGN KEY (`GL_Num`) REFERENCES `gl_setup` (`GL_Num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Account sequences - CBS Compliance: Last_Updated controlled by SystemDateService';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_seq`
--

LOCK TABLES `account_seq` WRITE;
/*!40000 ALTER TABLE `account_seq` DISABLE KEYS */;
INSERT INTO `account_seq` VALUES ('210201001',1,'2025-10-19 06:51:46'),('220202001',1,'2025-10-25 04:55:16'),('230201001',1,'2025-10-23 13:18:28');
/*!40000 ALTER TABLE `account_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `acct_bal`
--

DROP TABLE IF EXISTS `acct_bal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `acct_bal` (
  `Tran_Date` date NOT NULL,
  `Account_No` varchar(13) NOT NULL,
  `Opening_Bal` decimal(20,2) DEFAULT '0.00',
  `DR_Summation` decimal(20,2) DEFAULT '0.00',
  `CR_Summation` decimal(20,2) DEFAULT '0.00',
  `Closing_Bal` decimal(20,2) DEFAULT '0.00',
  `Current_Balance` decimal(20,2) NOT NULL DEFAULT '0.00',
  `Available_Balance` decimal(20,2) NOT NULL DEFAULT '0.00',
  `Last_Updated` timestamp NOT NULL,
  PRIMARY KEY (`Tran_Date`,`Account_No`),
  KEY `Account_No` (`Account_No`),
  KEY `idx_acct_bal_date` (`Tran_Date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Account balances - CBS Compliance: Last_Updated controlled by SystemDateService';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `acct_bal`
--

LOCK TABLES `acct_bal` WRITE;
/*!40000 ALTER TABLE `acct_bal` DISABLE KEYS */;
/*!40000 ALTER TABLE `acct_bal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `acct_bal_accrual`
--

DROP TABLE IF EXISTS `acct_bal_accrual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `acct_bal_accrual` (
  `Accr_Bal_Id` bigint NOT NULL AUTO_INCREMENT,
  `Account_No` varchar(13) NOT NULL,
  `GL_Num` varchar(9) DEFAULT NULL,
  `Tran_date` date DEFAULT NULL,
  `Opening_Bal` decimal(20,2) DEFAULT '0.00',
  `DR_Summation` decimal(20,2) DEFAULT '0.00',
  `CR_Summation` decimal(20,2) DEFAULT '0.00',
  `Closing_Bal` decimal(20,2) DEFAULT '0.00',
  `Accrual_Date` date NOT NULL,
  `Interest_Amount` decimal(20,2) NOT NULL,
  PRIMARY KEY (`Accr_Bal_Id`),
  KEY `Account_No` (`Account_No`),
  KEY `idx_acct_bal_accrual_gl_num` (`GL_Num`),
  CONSTRAINT `acct_bal_accrual_ibfk_1` FOREIGN KEY (`Account_No`) REFERENCES `cust_acct_master` (`Account_No`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `acct_bal_accrual`
--

LOCK TABLES `acct_bal_accrual` WRITE;
/*!40000 ALTER TABLE `acct_bal_accrual` DISABLE KEYS */;
/*!40000 ALTER TABLE `acct_bal_accrual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `acct_bal_bkp`
--

DROP TABLE IF EXISTS `acct_bal_bkp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `acct_bal_bkp` (
  `Tran_Date` date NOT NULL,
  `Account_No` varchar(13) NOT NULL,
  `Opening_Bal` decimal(20,2) DEFAULT '0.00',
  `DR_Summation` decimal(20,2) DEFAULT '0.00',
  `CR_Summation` decimal(20,2) DEFAULT '0.00',
  `Closing_Bal` decimal(20,2) DEFAULT '0.00',
  `Current_Balance` decimal(20,2) NOT NULL DEFAULT '0.00',
  `Available_Balance` decimal(20,2) NOT NULL DEFAULT '0.00',
  `Last_Updated` timestamp NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `acct_bal_bkp`
--

LOCK TABLES `acct_bal_bkp` WRITE;
/*!40000 ALTER TABLE `acct_bal_bkp` DISABLE KEYS */;
INSERT INTO `acct_bal_bkp` VALUES ('2025-01-08','100000001002',0.00,0.00,100.00,100.00,100.00,100.00,'2025-01-08 09:08:58'),('2025-01-08','100000002001',NULL,NULL,NULL,NULL,0.00,0.00,'2025-01-08 05:30:35'),('2025-01-08','921020100101',0.00,300.00,0.00,-300.00,-300.00,-300.00,'2025-01-08 09:08:58'),('2025-01-09','100000061001',200.00,0.00,200.00,400.00,400.00,400.00,'2025-01-09 06:53:22'),('2025-01-09','921020100101',-300.00,200.00,0.00,-500.00,-500.00,-500.00,'2025-01-09 06:53:22'),('2025-01-10','100000002001',0.00,0.00,200.00,200.00,200.00,200.00,'2025-01-10 03:54:34'),('2025-01-10','100000061001',400.00,100.00,1000.00,1300.00,1300.00,1300.00,'2025-01-10 03:54:34'),('2025-01-10','921020100101',-500.00,1100.00,0.00,-1600.00,-1600.00,-1600.00,'2025-01-10 03:54:34'),('2025-01-11','100000002001',200.00,25.00,50.00,225.00,225.00,225.00,'2025-01-11 07:51:52'),('2025-01-11','100000061001',1300.00,50.00,25.00,1275.00,1275.00,1275.00,'2025-01-11 07:51:52'),('2025-01-12','100000002001',225.00,0.00,250.00,475.00,475.00,475.00,'2025-01-12 04:05:52'),('2025-01-12','100000061001',1275.00,250.00,10000.00,11025.00,11025.00,11025.00,'2025-01-12 04:05:52'),('2025-01-12','921020100101',-1600.00,10000.00,0.00,-11600.00,-11600.00,-11600.00,'2025-01-12 04:05:52'),('2025-01-13','100000002001',475.00,100.00,0.00,375.00,375.00,375.00,'2025-01-13 12:11:32'),('2025-01-13','100000061001',11025.00,0.00,100.00,11125.00,11125.00,11125.00,'2025-01-13 12:11:32'),('2025-10-15','100000036001',0.00,0.00,0.00,0.00,0.00,0.00,'2025-10-09 09:19:56'),('2025-10-15','100000046001',-600.00,0.00,0.00,-600.00,-600.00,-600.00,'2025-10-14 09:45:37'),('2025-10-19','911010100102',0.00,0.00,0.00,0.00,0.00,0.00,'2025-10-19 08:01:05'),('2025-10-19','921020100101',0.00,0.00,0.00,0.00,0.00,0.00,'2025-10-19 08:01:05');
/*!40000 ALTER TABLE `acct_bal_bkp` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_acct_master`
--

DROP TABLE IF EXISTS `cust_acct_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_acct_master` (
  `Account_No` varchar(13) NOT NULL,
  `Sub_Product_Id` int NOT NULL,
  `GL_Num` varchar(20) NOT NULL,
  `Cust_Id` int NOT NULL,
  `Cust_Name` varchar(100) DEFAULT NULL,
  `Acct_Name` varchar(100) NOT NULL,
  `Date_Opening` date NOT NULL,
  `Tenor` int DEFAULT NULL,
  `Date_Maturity` date DEFAULT NULL,
  `Date_Closure` date DEFAULT NULL,
  `Branch_Code` varchar(10) NOT NULL,
  `Account_Status` enum('Active','Inactive','Closed','Dormant') NOT NULL,
  `Loan_Limit` decimal(18,2) DEFAULT '0.00' COMMENT 'Loan/Limit Amount for Asset-side customer accounts (GL starting with 2)',
  PRIMARY KEY (`Account_No`),
  KEY `Sub_Product_Id` (`Sub_Product_Id`),
  KEY `Cust_Id` (`Cust_Id`),
  CONSTRAINT `cust_acct_master_ibfk_1` FOREIGN KEY (`Sub_Product_Id`) REFERENCES `sub_prod_master` (`Sub_Product_Id`),
  CONSTRAINT `cust_acct_master_ibfk_2` FOREIGN KEY (`Cust_Id`) REFERENCES `cust_master` (`Cust_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_acct_master`
--

LOCK TABLES `cust_acct_master` WRITE;
/*!40000 ALTER TABLE `cust_acct_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `cust_acct_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cust_master`
--

DROP TABLE IF EXISTS `cust_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cust_master` (
  `Cust_Id` int NOT NULL AUTO_INCREMENT,
  `Ext_Cust_Id` varchar(20) NOT NULL,
  `Cust_Type` enum('Individual','Corporate','Bank') NOT NULL,
  `First_Name` varchar(50) DEFAULT NULL,
  `Last_Name` varchar(50) DEFAULT NULL,
  `Trade_Name` varchar(100) DEFAULT NULL,
  `Address_1` varchar(200) DEFAULT NULL,
  `Mobile` varchar(15) DEFAULT NULL,
  `Branch_Code` varchar(10) NOT NULL DEFAULT '001',
  `Maker_Id` varchar(20) NOT NULL,
  `Entry_Date` date NOT NULL,
  `Entry_Time` time NOT NULL,
  `Verifier_Id` varchar(20) DEFAULT NULL,
  `Verification_Date` date DEFAULT NULL,
  `Verification_Time` time DEFAULT NULL,
  PRIMARY KEY (`Cust_Id`),
  CONSTRAINT `chk_customer_name` CHECK ((((`Cust_Type` = _utf8mb4'Individual') and (`First_Name` is not null) and (`Last_Name` is not null)) or ((`Cust_Type` in (_utf8mb4'Corporate',_utf8mb4'Bank')) and (`Trade_Name` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=20000003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cust_master`
--

LOCK TABLES `cust_master` WRITE;
/*!40000 ALTER TABLE `cust_master` DISABLE KEYS */;
INSERT INTO `cust_master` VALUES (10000000,'21366','Individual','Yasir','Abrar','','dhaka','01203013123','001','FRONTEND_USER','2025-09-30','16:16:37','Asif','2025-10-05','17:15:21'),(10000001,'234335','Individual','Aroshi','Terro','','Dghajka','01778635779','001','FRONTEND_USER','2025-10-05','15:18:22',NULL,NULL,NULL),(10000002,'23437','Individual','Mahir','Labib','','Dhaka','01778635779','001','FRONTEND_USER','2025-10-05','15:26:33','Asif','2025-10-05','18:00:54'),(10000003,'213234','Individual','Mahir','Mahir','','Dhaka','01778635779','001','FRONTEND_USER','2025-10-05','15:29:20',NULL,NULL,NULL),(10000004,'','Individual','Yasir','Khan','','Dhaka','01778635779','001','FRONTEND_USER','2025-10-08','16:44:22',NULL,NULL,NULL),(10000005,'423454','Individual','Mahir','Khan','','Motijheel','01778635779','001','FRONTEND_USER','2025-10-09','15:19:00',NULL,NULL,NULL),(10000006,'23433534','Individual','Jamshed','Akash','','Shantinagar','01778543779','001','FRONTEND_USER','2025-10-15','17:42:34','Asif','2025-10-15','17:55:45'),(10000007,'67788','Individual','Asim','Talukdar','','Dhaka','01833320888','001','FRONTEND_USER','2025-10-25','10:43:40',NULL,NULL,NULL),(10000008,'66585','Individual','Shahrukh','Khan','','Andheri, Mumbai','01733320222','001','FRONTEND_USER','2025-10-25','10:44:21',NULL,NULL,NULL),(10000009,'2023','Individual','SHANKAR','DHAR','','Dhaka','01841320962','001','FRONTEND_USER','2025-10-26','15:01:18','Asif','2025-10-26','15:01:42'),(10000010,'4234324','Individual','Zubair','Hasan','','Baridhara','01833320877','001','FRONTEND_USER','2025-10-27','12:48:31','Asif','2025-10-27','12:48:54'),(20000000,'1123','Corporate','','','EBL Traders','dhaka','012030135564','001','FRONTEND_USER','2025-09-30','17:22:44',NULL,NULL,NULL),(20000001,'21323234','Corporate','','','PBL','Dhaka','01778617889','001','FRONTEND_USER','2025-10-05','15:16:43',NULL,NULL,NULL),(20000002,'56565','Corporate','','','data edge Limited','Baridhara','01755520888','001','FRONTEND_USER','2025-10-25','10:44:56',NULL,NULL,NULL);
/*!40000 ALTER TABLE `cust_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eod_log_table`
--

DROP TABLE IF EXISTS `eod_log_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eod_log_table` (
  `EOD_Log_Id` bigint NOT NULL AUTO_INCREMENT,
  `EOD_Date` date NOT NULL,
  `Job_Name` varchar(50) NOT NULL,
  `Start_Timestamp` timestamp NOT NULL,
  `End_Timestamp` timestamp NULL DEFAULT NULL,
  `System_Date` date NOT NULL,
  `User_ID` varchar(20) NOT NULL,
  `Records_Processed` int DEFAULT '0',
  `Status` enum('Running','Success','Failed') NOT NULL DEFAULT 'Running',
  `Error_Message` text,
  `Failed_At_Step` varchar(100) DEFAULT NULL,
  `Created_Timestamp` timestamp NOT NULL,
  PRIMARY KEY (`EOD_Log_Id`),
  KEY `idx_eod_log_date` (`EOD_Date`),
  KEY `idx_eod_log_job_name` (`Job_Name`),
  KEY `idx_eod_log_status` (`Status`)
) ENGINE=InnoDB AUTO_INCREMENT=518 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='EOD processing logs - CBS Compliance: Created_Timestamp controlled by SystemDateService';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eod_log_table`
--

LOCK TABLES `eod_log_table` WRITE;
/*!40000 ALTER TABLE `eod_log_table` DISABLE KEYS */;
INSERT INTO `eod_log_table` VALUES (1,'2025-01-10','Batch Job 2 - Interest Accrual','2025-01-10 09:55:28','2025-01-10 09:55:29','2025-01-10','SYSTEM',4,'Success',NULL,NULL,'2025-01-10 09:55:28'),(2,'2025-01-10','Batch Job 3 - GL Movement','2025-01-10 09:55:43','2025-01-10 09:55:43','2025-01-10','SYSTEM',16,'Success',NULL,NULL,'2025-01-10 09:55:43'),(72,'2025-01-10','Account Balance Update','2025-01-10 03:54:33','2025-01-10 03:54:34','2025-01-10','ADMIN',3,'Success',NULL,NULL,'2025-01-10 03:54:33'),(78,'2025-01-10','Interest Accrual Transaction Update','2025-01-10 04:10:24','2025-01-10 04:10:24','2025-01-10','ADMIN',4,'Success',NULL,NULL,'2025-01-10 04:10:24'),(79,'2025-01-10','Interest Accrual Transaction Update','2025-01-10 04:10:24','2025-01-10 04:10:24','2025-01-10','SYSTEM',4,'Success',NULL,NULL,'2025-01-10 04:10:24'),(80,'2025-01-10','Interest Accrual GL Movement Update','2025-01-10 04:11:00','2025-01-10 04:11:00','2025-01-10','ADMIN',4,'Success',NULL,NULL,'2025-01-10 04:11:00'),(81,'2025-01-10','Interest Accrual GL Movement Update','2025-01-10 04:11:00','2025-01-10 04:11:00','2025-01-10','SYSTEM',4,'Success',NULL,NULL,'2025-01-10 04:11:00'),(82,'2025-01-10','GL Movement Update','2025-01-10 04:12:55','2025-01-10 04:12:56','2025-01-10','ADMIN',6,'Success',NULL,NULL,'2025-01-10 04:12:55'),(105,'2025-01-10','GL Balance Update','2025-01-10 05:06:00','2025-01-10 05:06:01','2025-01-10','ADMIN',6,'Success',NULL,NULL,'2025-01-10 05:06:00'),(106,'2025-01-10','Batch Job 5 - GL Balance','2025-01-10 05:06:00','2025-01-10 05:06:01','2025-01-10','SYSTEM',6,'Success',NULL,NULL,'2025-01-10 05:06:00'),(107,'2025-01-10','Interest Accrual Account Balance Update','2025-01-10 05:11:42','2025-01-10 05:11:42','2025-01-10','ADMIN',2,'Success',NULL,NULL,'2025-01-10 05:11:42'),(111,'2025-01-10','Financial Reports Generation','2025-01-10 06:58:50','2025-01-10 06:58:50','2025-01-10','ADMIN',2,'Success',NULL,NULL,'2025-01-10 06:58:50'),(112,'2025-01-10','System Date Increment','2025-01-10 07:14:50','2025-01-10 07:14:50','2025-01-10','ADMIN',1,'Success',NULL,NULL,'2025-01-10 07:14:50'),(113,'2025-01-11','Account Balance Update','2025-01-11 07:51:52','2025-01-11 07:51:52','2025-01-11','ADMIN',2,'Success',NULL,NULL,'2025-01-11 07:51:52'),(114,'2025-01-11','Interest Accrual Transaction Update','2025-01-11 07:52:30','2025-01-11 07:52:31','2025-01-11','ADMIN',4,'Success',NULL,NULL,'2025-01-11 07:52:30'),(115,'2025-01-11','Interest Accrual Transaction Update','2025-01-11 07:52:30','2025-01-11 07:52:31','2025-01-11','SYSTEM',4,'Success',NULL,NULL,'2025-01-11 07:52:30'),(116,'2025-01-11','Interest Accrual GL Movement Update','2025-01-11 07:52:55','2025-01-11 07:52:56','2025-01-11','ADMIN',4,'Success',NULL,NULL,'2025-01-11 07:52:55'),(117,'2025-01-11','Interest Accrual GL Movement Update','2025-01-11 07:52:55','2025-01-11 07:52:56','2025-01-11','SYSTEM',4,'Success',NULL,NULL,'2025-01-11 07:52:55'),(118,'2025-01-11','GL Movement Update','2025-01-11 07:52:58','2025-01-11 07:52:58','2025-01-11','ADMIN',4,'Success',NULL,NULL,'2025-01-11 07:52:58'),(184,'2025-01-11','Interest Accrual Account Balance Update','2025-01-11 09:28:22','2025-01-11 09:28:23','2025-01-11','ADMIN',2,'Success',NULL,NULL,'2025-01-11 09:28:22'),(185,'2025-01-11','Financial Reports Generation','2025-01-11 09:28:28','2025-01-11 09:28:28','2025-01-11','ADMIN',2,'Success',NULL,NULL,'2025-01-11 09:28:28'),(186,'2025-01-11','System Date Increment','2025-01-11 09:28:34','2025-01-12 09:28:34','2025-01-11','ADMIN',1,'Success',NULL,NULL,'2025-01-11 09:28:34'),(187,'2025-01-11','System Date Increment','2025-01-12 09:28:34','2025-01-12 09:28:34','2025-01-11','SYSTEM',1,'Success',NULL,'Completed','2025-01-12 09:28:34'),(188,'2025-01-11','EOD Cycle Complete','2025-01-12 09:28:35','2025-01-12 09:28:35','2025-01-11','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-12','2025-01-12 09:28:35'),(195,'2025-01-12','Account Balance Update','2025-01-12 04:05:51','2025-01-12 04:05:52','2025-01-12','ADMIN',3,'Success',NULL,NULL,'2025-01-12 04:05:51'),(196,'2025-01-12','Interest Accrual Transaction Update','2025-01-12 04:07:06','2025-01-12 04:07:07','2025-01-12','ADMIN',4,'Success',NULL,NULL,'2025-01-12 04:07:06'),(197,'2025-01-12','Interest Accrual GL Movement Update','2025-01-12 04:07:44','2025-01-12 04:07:44','2025-01-12','ADMIN',4,'Success',NULL,NULL,'2025-01-12 04:07:44'),(198,'2025-01-12','GL Movement Update','2025-01-12 04:08:07','2025-01-12 04:08:07','2025-01-12','ADMIN',4,'Success',NULL,NULL,'2025-01-12 04:08:07'),(200,'2025-01-11','GL Balance Update','2025-01-11 04:29:35','2025-01-11 04:29:36','2025-01-11','ADMIN',5,'Success',NULL,NULL,'2025-01-11 04:29:35'),(207,'2025-01-12','GL Balance Update','2025-01-12 09:32:01','2025-01-12 09:32:03','2025-01-12','ADMIN',6,'Success',NULL,NULL,'2025-01-12 09:32:01'),(208,'2025-01-12','Interest Accrual Account Balance Update','2025-01-12 10:08:00','2025-01-12 10:08:02','2025-01-12','ADMIN',2,'Success',NULL,NULL,'2025-01-12 10:08:00'),(209,'2025-01-12','Financial Reports Generation','2025-01-12 10:08:42','2025-01-12 10:08:42','2025-01-12','ADMIN',2,'Success',NULL,NULL,'2025-01-12 10:08:42'),(210,'2025-01-12','System Date Increment','2025-01-12 10:08:58','2025-01-13 10:08:58','2025-01-12','ADMIN',1,'Success',NULL,NULL,'2025-01-12 10:08:58'),(211,'2025-01-12','System Date Increment','2025-01-13 10:08:58','2025-01-13 10:08:58','2025-01-12','SYSTEM',1,'Success',NULL,'Completed','2025-01-13 10:08:58'),(212,'2025-01-12','EOD Cycle Complete','2025-01-13 10:08:59','2025-01-13 10:08:59','2025-01-12','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-13','2025-01-13 10:08:59'),(213,'2025-01-13','Account Balance Update','2025-01-13 12:11:32','2025-01-13 12:11:32','2025-01-13','ADMIN',2,'Success',NULL,NULL,'2025-01-13 12:11:32'),(214,'2025-01-13','Interest Accrual Transaction Update','2025-01-13 12:11:34','2025-01-13 12:11:35','2025-01-13','ADMIN',4,'Success',NULL,NULL,'2025-01-13 12:11:34'),(215,'2025-01-13','Interest Accrual GL Movement Update','2025-01-13 12:11:38','2025-01-13 12:11:38','2025-01-13','ADMIN',4,'Success',NULL,NULL,'2025-01-13 12:11:38'),(216,'2025-01-13','GL Movement Update','2025-01-13 12:11:40','2025-01-13 12:11:40','2025-01-13','ADMIN',2,'Success',NULL,NULL,'2025-01-13 12:11:40'),(217,'2025-01-13','GL Balance Update','2025-01-13 12:11:43','2025-01-13 12:11:44','2025-01-13','ADMIN',5,'Success',NULL,NULL,'2025-01-13 12:11:43'),(218,'2025-01-13','Interest Accrual Account Balance Update','2025-01-13 12:11:46','2025-01-13 12:11:46','2025-01-13','ADMIN',2,'Success',NULL,NULL,'2025-01-13 12:11:46'),(219,'2025-01-13','Financial Reports Generation','2025-01-13 12:11:49','2025-01-13 12:11:49','2025-01-13','ADMIN',2,'Success',NULL,NULL,'2025-01-13 12:11:49'),(220,'2025-01-13','System Date Increment','2025-01-13 12:11:55','2025-01-14 12:11:55','2025-01-13','ADMIN',1,'Success',NULL,NULL,'2025-01-13 12:11:55'),(221,'2025-01-13','System Date Increment','2025-01-14 12:11:55','2025-01-14 12:11:55','2025-01-13','SYSTEM',1,'Success',NULL,'Completed','2025-01-14 12:11:55'),(222,'2025-01-13','EOD Cycle Complete','2025-01-14 12:11:55','2025-01-14 12:11:55','2025-01-13','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-14','2025-01-14 12:11:55'),(223,'2025-01-14','Account Balance Update','2025-01-14 13:21:37','2025-01-14 13:21:38','2025-01-14','ADMIN',2,'Success',NULL,NULL,'2025-01-14 13:21:37'),(224,'2025-01-14','Interest Accrual Transaction Update','2025-01-14 13:21:40','2025-01-14 13:21:40','2025-01-14','ADMIN',2,'Success',NULL,NULL,'2025-01-14 13:21:40'),(225,'2025-01-14','Interest Accrual GL Movement Update','2025-01-14 13:21:42','2025-01-14 13:21:42','2025-01-14','ADMIN',2,'Success',NULL,NULL,'2025-01-14 13:21:42'),(226,'2025-01-14','GL Movement Update','2025-01-14 13:21:45','2025-01-14 13:21:45','2025-01-14','ADMIN',2,'Success',NULL,NULL,'2025-01-14 13:21:45'),(227,'2025-01-14','GL Balance Update','2025-01-14 13:21:47','2025-01-14 13:21:48','2025-01-14','ADMIN',4,'Success',NULL,NULL,'2025-01-14 13:21:47'),(228,'2025-01-14','Interest Accrual Account Balance Update','2025-01-14 13:21:50','2025-01-14 13:21:50','2025-01-14','ADMIN',1,'Success',NULL,NULL,'2025-01-14 13:21:50'),(229,'2025-01-14','Financial Reports Generation','2025-01-14 13:21:53','2025-01-14 13:21:53','2025-01-14','ADMIN',2,'Success',NULL,NULL,'2025-01-14 13:21:53'),(230,'2025-01-14','System Date Increment','2025-01-14 13:21:57','2025-01-15 13:21:57','2025-01-14','ADMIN',1,'Success',NULL,NULL,'2025-01-14 13:21:57'),(231,'2025-01-14','System Date Increment','2025-01-15 13:21:57','2025-01-15 13:21:57','2025-01-14','SYSTEM',1,'Success',NULL,'Completed','2025-01-15 13:21:57'),(232,'2025-01-14','EOD Cycle Complete','2025-01-15 13:21:57','2025-01-15 13:21:57','2025-01-14','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-15','2025-01-15 13:21:57'),(233,'2025-01-15','Account Balance Update','2025-01-15 13:28:50','2025-01-15 13:28:50','2025-01-15','ADMIN',2,'Success',NULL,NULL,'2025-01-15 13:28:50'),(234,'2025-01-15','Interest Accrual Transaction Update','2025-01-15 13:28:52','2025-01-15 13:28:52','2025-01-15','ADMIN',0,'Success',NULL,NULL,'2025-01-15 13:28:52'),(235,'2025-01-15','Interest Accrual GL Movement Update','2025-01-15 13:28:55','2025-01-15 13:28:55','2025-01-15','ADMIN',0,'Success',NULL,NULL,'2025-01-15 13:28:55'),(236,'2025-01-15','GL Movement Update','2025-01-15 13:28:58','2025-01-15 13:28:58','2025-01-15','ADMIN',0,'Success',NULL,NULL,'2025-01-15 13:28:58'),(237,'2025-01-15','GL Balance Update','2025-01-15 13:29:00','2025-01-15 13:29:00','2025-01-15','ADMIN',0,'Success',NULL,NULL,'2025-01-15 13:29:00'),(238,'2025-01-15','Interest Accrual Account Balance Update','2025-01-15 13:29:02','2025-01-15 13:29:02','2025-01-15','ADMIN',0,'Success',NULL,NULL,'2025-01-15 13:29:02'),(239,'2025-01-15','Financial Reports Generation','2025-01-15 13:29:04','2025-01-15 13:29:04','2025-01-15','ADMIN',2,'Success',NULL,NULL,'2025-01-15 13:29:04'),(240,'2025-01-15','System Date Increment','2025-01-15 13:29:08','2025-01-16 13:29:08','2025-01-15','ADMIN',1,'Success',NULL,NULL,'2025-01-15 13:29:08'),(241,'2025-01-15','System Date Increment','2025-01-16 13:29:08','2025-01-16 13:29:08','2025-01-15','SYSTEM',1,'Success',NULL,'Completed','2025-01-16 13:29:08'),(242,'2025-01-15','EOD Cycle Complete','2025-01-16 13:29:08','2025-01-16 13:29:08','2025-01-15','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-16','2025-01-16 13:29:08'),(243,'2025-01-16','Account Balance Update','2025-01-16 13:32:36','2025-01-16 13:32:37','2025-01-16','ADMIN',2,'Success',NULL,NULL,'2025-01-16 13:32:36'),(244,'2025-01-16','Interest Accrual Transaction Update','2025-01-16 13:32:39','2025-01-16 13:32:40','2025-01-16','ADMIN',2,'Success',NULL,NULL,'2025-01-16 13:32:39'),(245,'2025-01-16','Interest Accrual GL Movement Update','2025-01-16 13:32:42','2025-01-16 13:32:42','2025-01-16','ADMIN',2,'Success',NULL,NULL,'2025-01-16 13:32:42'),(246,'2025-01-16','GL Movement Update','2025-01-16 13:32:44','2025-01-16 13:32:45','2025-01-16','ADMIN',2,'Success',NULL,NULL,'2025-01-16 13:32:44'),(247,'2025-01-16','GL Balance Update','2025-01-16 13:32:46','2025-01-16 13:32:47','2025-01-16','ADMIN',4,'Success',NULL,NULL,'2025-01-16 13:32:46'),(248,'2025-01-16','Interest Accrual Account Balance Update','2025-01-16 13:32:49','2025-01-16 13:32:49','2025-01-16','ADMIN',1,'Success',NULL,NULL,'2025-01-16 13:32:49'),(249,'2025-01-16','Financial Reports Generation','2025-01-16 13:32:51','2025-01-16 13:32:51','2025-01-16','ADMIN',2,'Success',NULL,NULL,'2025-01-16 13:32:51'),(250,'2025-01-16','System Date Increment','2025-01-16 13:32:54','2025-01-17 13:32:55','2025-01-16','ADMIN',1,'Success',NULL,NULL,'2025-01-16 13:32:54'),(251,'2025-01-16','System Date Increment','2025-01-17 13:32:54','2025-01-17 13:32:54','2025-01-16','SYSTEM',1,'Success',NULL,'Completed','2025-01-17 13:32:54'),(252,'2025-01-16','EOD Cycle Complete','2025-01-17 13:32:55','2025-01-17 13:32:55','2025-01-16','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-17','2025-01-17 13:32:55'),(253,'2025-01-17','Account Balance Update','2025-01-17 04:49:33','2025-01-17 04:49:33','2025-01-17','ADMIN',2,'Success',NULL,NULL,'2025-01-17 04:49:33'),(254,'2025-01-17','Interest Accrual Transaction Update','2025-01-17 04:49:36','2025-01-17 04:49:36','2025-01-17','ADMIN',2,'Success',NULL,NULL,'2025-01-17 04:49:36'),(255,'2025-01-17','Interest Accrual GL Movement Update','2025-01-17 04:49:43','2025-01-17 04:49:43','2025-01-17','ADMIN',2,'Success',NULL,NULL,'2025-01-17 04:49:43'),(256,'2025-01-17','GL Movement Update','2025-01-17 04:49:50','2025-01-17 04:49:50','2025-01-17','ADMIN',0,'Success',NULL,NULL,'2025-01-17 04:49:50'),(257,'2025-01-17','GL Balance Update','2025-01-17 04:49:56','2025-01-17 04:49:56','2025-01-17','ADMIN',2,'Success',NULL,NULL,'2025-01-17 04:49:56'),(258,'2025-01-17','Interest Accrual Account Balance Update','2025-01-17 04:50:01','2025-01-17 04:50:01','2025-01-17','ADMIN',1,'Success',NULL,NULL,'2025-01-17 04:50:01'),(259,'2025-01-17','Financial Reports Generation','2025-01-17 04:50:06','2025-01-17 04:50:06','2025-01-17','ADMIN',2,'Success',NULL,NULL,'2025-01-17 04:50:06'),(260,'2025-01-17','System Date Increment','2025-01-17 04:50:11','2025-01-18 04:50:11','2025-01-17','ADMIN',1,'Success',NULL,NULL,'2025-01-17 04:50:11'),(261,'2025-01-17','System Date Increment','2025-01-18 04:50:11','2025-01-18 04:50:11','2025-01-17','SYSTEM',1,'Success',NULL,'Completed','2025-01-18 04:50:11'),(262,'2025-01-17','EOD Cycle Complete','2025-01-18 04:50:12','2025-01-18 04:50:12','2025-01-17','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-18','2025-01-18 04:50:12'),(263,'2025-01-18','Account Balance Update','2025-01-18 04:59:14','2025-01-18 04:59:14','2025-01-18','ADMIN',4,'Success',NULL,NULL,'2025-01-18 04:59:14'),(264,'2025-01-18','Interest Accrual Transaction Update','2025-01-18 04:59:17','2025-01-18 04:59:18','2025-01-18','ADMIN',4,'Success',NULL,NULL,'2025-01-18 04:59:17'),(265,'2025-01-18','Interest Accrual GL Movement Update','2025-01-18 04:59:21','2025-01-18 04:59:21','2025-01-18','ADMIN',4,'Success',NULL,NULL,'2025-01-18 04:59:21'),(266,'2025-01-18','GL Movement Update','2025-01-18 04:59:23','2025-01-18 04:59:23','2025-01-18','ADMIN',5,'Success',NULL,NULL,'2025-01-18 04:59:23'),(267,'2025-01-18','GL Balance Update','2025-01-18 04:59:26','2025-01-18 04:59:27','2025-01-18','ADMIN',5,'Success',NULL,NULL,'2025-01-18 04:59:26'),(268,'2025-01-18','Interest Accrual Account Balance Update','2025-01-18 04:59:30','2025-01-18 04:59:30','2025-01-18','ADMIN',2,'Success',NULL,NULL,'2025-01-18 04:59:30'),(269,'2025-01-18','Financial Reports Generation','2025-01-18 04:59:33','2025-01-18 04:59:33','2025-01-18','ADMIN',2,'Success',NULL,NULL,'2025-01-18 04:59:33'),(270,'2025-01-18','System Date Increment','2025-01-18 04:59:38','2025-01-19 04:59:38','2025-01-18','ADMIN',1,'Success',NULL,NULL,'2025-01-18 04:59:38'),(271,'2025-01-18','System Date Increment','2025-01-19 04:59:38','2025-01-19 04:59:38','2025-01-18','SYSTEM',1,'Success',NULL,'Completed','2025-01-19 04:59:38'),(272,'2025-01-18','EOD Cycle Complete','2025-01-19 04:59:38','2025-01-19 04:59:38','2025-01-18','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-19','2025-01-19 04:59:38'),(273,'2025-01-19','Account Balance Update','2025-01-19 05:08:31','2025-01-19 05:08:31','2025-01-19','ADMIN',5,'Success',NULL,NULL,'2025-01-19 05:08:31'),(274,'2025-01-19','Interest Accrual Transaction Update','2025-01-19 05:08:36','2025-01-19 05:08:36','2025-01-19','ADMIN',10,'Success',NULL,NULL,'2025-01-19 05:08:36'),(275,'2025-01-19','Interest Accrual GL Movement Update','2025-01-19 05:08:38','2025-01-19 05:08:39','2025-01-19','ADMIN',10,'Success',NULL,NULL,'2025-01-19 05:08:38'),(276,'2025-01-19','GL Movement Update','2025-01-19 05:08:41','2025-01-19 05:08:42','2025-01-19','ADMIN',5,'Success',NULL,NULL,'2025-01-19 05:08:41'),(277,'2025-01-19','GL Balance Update','2025-01-19 05:08:44','2025-01-19 05:08:45','2025-01-19','ADMIN',8,'Success',NULL,NULL,'2025-01-19 05:08:44'),(278,'2025-01-19','Interest Accrual Account Balance Update','2025-01-19 05:08:47','2025-01-19 05:08:47','2025-01-19','ADMIN',5,'Success',NULL,NULL,'2025-01-19 05:08:47'),(279,'2025-01-19','Financial Reports Generation','2025-01-19 05:08:50','2025-01-19 05:08:50','2025-01-19','ADMIN',2,'Success',NULL,NULL,'2025-01-19 05:08:50'),(280,'2025-01-19','System Date Increment','2025-01-19 05:08:54','2025-01-20 05:08:55','2025-01-19','ADMIN',1,'Success',NULL,NULL,'2025-01-19 05:08:54'),(281,'2025-01-19','System Date Increment','2025-01-20 05:08:54','2025-01-20 05:08:54','2025-01-19','SYSTEM',1,'Success',NULL,'Completed','2025-01-20 05:08:54'),(282,'2025-01-19','EOD Cycle Complete','2025-01-20 05:08:55','2025-01-20 05:08:55','2025-01-19','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-20','2025-01-20 05:08:55'),(283,'2025-01-20','Account Balance Update','2025-01-20 05:12:08','2025-01-20 05:12:08','2025-01-20','ADMIN',0,'Success',NULL,NULL,'2025-01-20 05:12:08'),(284,'2025-01-20','Interest Accrual Transaction Update','2025-01-20 05:12:10','2025-01-20 05:12:11','2025-01-20','ADMIN',10,'Success',NULL,NULL,'2025-01-20 05:12:10'),(285,'2025-01-20','Interest Accrual GL Movement Update','2025-01-20 05:12:14','2025-01-20 05:12:14','2025-01-20','ADMIN',10,'Success',NULL,NULL,'2025-01-20 05:12:14'),(286,'2025-01-20','GL Movement Update','2025-01-20 05:12:17','2025-01-20 05:12:17','2025-01-20','ADMIN',0,'Success',NULL,NULL,'2025-01-20 05:12:17'),(287,'2025-01-20','GL Balance Update','2025-01-20 05:12:19','2025-01-20 05:12:19','2025-01-20','ADMIN',4,'Success',NULL,NULL,'2025-01-20 05:12:19'),(288,'2025-01-20','Interest Accrual Account Balance Update','2025-01-20 05:12:22','2025-01-20 05:12:22','2025-01-20','ADMIN',5,'Success',NULL,NULL,'2025-01-20 05:12:22'),(289,'2025-01-20','Financial Reports Generation','2025-01-20 05:12:24','2025-01-20 05:12:24','2025-01-20','ADMIN',2,'Success',NULL,NULL,'2025-01-20 05:12:24'),(290,'2025-01-20','System Date Increment','2025-01-20 05:12:28','2025-01-21 05:12:28','2025-01-20','ADMIN',1,'Success',NULL,NULL,'2025-01-20 05:12:28'),(291,'2025-01-20','System Date Increment','2025-01-21 05:12:28','2025-01-21 05:12:28','2025-01-20','SYSTEM',1,'Success',NULL,'Completed','2025-01-21 05:12:28'),(292,'2025-01-20','EOD Cycle Complete','2025-01-21 05:12:28','2025-01-21 05:12:28','2025-01-20','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-21','2025-01-21 05:12:28'),(293,'2025-01-21','Account Balance Update','2025-01-21 05:15:17','2025-01-21 05:15:17','2025-01-21','ADMIN',4,'Success',NULL,NULL,'2025-01-21 05:15:17'),(294,'2025-01-21','Interest Accrual Transaction Update','2025-01-21 05:15:19','2025-01-21 05:15:20','2025-01-21','ADMIN',10,'Success',NULL,NULL,'2025-01-21 05:15:19'),(295,'2025-01-21','Interest Accrual GL Movement Update','2025-01-21 05:15:22','2025-01-21 05:15:22','2025-01-21','ADMIN',10,'Success',NULL,NULL,'2025-01-21 05:15:22'),(296,'2025-01-21','GL Movement Update','2025-01-21 05:15:24','2025-01-21 05:15:24','2025-01-21','ADMIN',4,'Success',NULL,NULL,'2025-01-21 05:15:24'),(297,'2025-01-21','GL Balance Update','2025-01-21 05:15:26','2025-01-21 05:15:27','2025-01-21','ADMIN',6,'Success',NULL,NULL,'2025-01-21 05:15:26'),(298,'2025-01-21','Interest Accrual Account Balance Update','2025-01-21 05:15:29','2025-01-21 05:15:29','2025-01-21','ADMIN',5,'Success',NULL,NULL,'2025-01-21 05:15:29'),(299,'2025-01-21','Financial Reports Generation','2025-01-21 05:15:31','2025-01-21 05:15:32','2025-01-21','ADMIN',2,'Success',NULL,NULL,'2025-01-21 05:15:31'),(300,'2025-01-21','System Date Increment','2025-01-21 05:15:35','2025-01-22 05:15:35','2025-01-21','ADMIN',1,'Success',NULL,NULL,'2025-01-21 05:15:35'),(301,'2025-01-21','System Date Increment','2025-01-22 05:15:35','2025-01-22 05:15:35','2025-01-21','SYSTEM',1,'Success',NULL,'Completed','2025-01-22 05:15:35'),(302,'2025-01-21','EOD Cycle Complete','2025-01-22 05:15:35','2025-01-22 05:15:35','2025-01-21','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-22','2025-01-22 05:15:35'),(303,'2025-01-22','Account Balance Update','2025-01-22 05:17:02','2025-01-22 05:17:02','2025-01-22','ADMIN',0,'Success',NULL,NULL,'2025-01-22 05:17:02'),(304,'2025-01-22','Interest Accrual Transaction Update','2025-01-22 05:17:04','2025-01-22 05:17:04','2025-01-22','ADMIN',10,'Success',NULL,NULL,'2025-01-22 05:17:04'),(305,'2025-01-22','Interest Accrual GL Movement Update','2025-01-22 05:17:07','2025-01-22 05:17:07','2025-01-22','ADMIN',10,'Success',NULL,NULL,'2025-01-22 05:17:07'),(306,'2025-01-22','GL Movement Update','2025-01-22 05:17:09','2025-01-22 05:17:09','2025-01-22','ADMIN',0,'Success',NULL,NULL,'2025-01-22 05:17:09'),(307,'2025-01-22','GL Balance Update','2025-01-22 05:17:11','2025-01-22 05:17:12','2025-01-22','ADMIN',4,'Success',NULL,NULL,'2025-01-22 05:17:11'),(308,'2025-01-22','Interest Accrual Account Balance Update','2025-01-22 05:17:16','2025-01-22 05:17:16','2025-01-22','ADMIN',5,'Success',NULL,NULL,'2025-01-22 05:17:16'),(309,'2025-01-22','Financial Reports Generation','2025-01-22 05:17:18','2025-01-22 05:17:18','2025-01-22','ADMIN',2,'Success',NULL,NULL,'2025-01-22 05:17:18'),(310,'2025-01-22','System Date Increment','2025-01-22 05:17:22','2025-01-23 05:17:22','2025-01-22','ADMIN',1,'Success',NULL,NULL,'2025-01-22 05:17:22'),(311,'2025-01-22','System Date Increment','2025-01-23 05:17:22','2025-01-23 05:17:22','2025-01-22','SYSTEM',1,'Success',NULL,'Completed','2025-01-23 05:17:22'),(312,'2025-01-22','EOD Cycle Complete','2025-01-23 05:17:22','2025-01-23 05:17:22','2025-01-22','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-23','2025-01-23 05:17:22'),(313,'2025-01-23','Account Balance Update','2025-01-23 05:18:04','2025-01-23 05:18:04','2025-01-23','ADMIN',0,'Success',NULL,NULL,'2025-01-23 05:18:04'),(314,'2025-01-23','Interest Accrual Transaction Update','2025-01-23 05:18:07','2025-01-23 05:18:07','2025-01-23','ADMIN',10,'Success',NULL,NULL,'2025-01-23 05:18:07'),(315,'2025-01-23','Interest Accrual GL Movement Update','2025-01-23 05:18:09','2025-01-23 05:18:09','2025-01-23','ADMIN',10,'Success',NULL,NULL,'2025-01-23 05:18:09'),(316,'2025-01-23','GL Movement Update','2025-01-23 05:18:11','2025-01-23 05:18:12','2025-01-23','ADMIN',0,'Success',NULL,NULL,'2025-01-23 05:18:11'),(317,'2025-01-23','GL Balance Update','2025-01-23 05:18:14','2025-01-23 05:18:14','2025-01-23','ADMIN',4,'Success',NULL,NULL,'2025-01-23 05:18:14'),(318,'2025-01-23','Interest Accrual Account Balance Update','2025-01-23 05:18:17','2025-01-23 05:18:17','2025-01-23','ADMIN',5,'Success',NULL,NULL,'2025-01-23 05:18:17'),(319,'2025-01-23','Financial Reports Generation','2025-01-23 05:18:19','2025-01-23 05:18:19','2025-01-23','ADMIN',2,'Success',NULL,NULL,'2025-01-23 05:18:19'),(320,'2025-01-23','System Date Increment','2025-01-23 05:18:22','2025-01-24 05:18:22','2025-01-23','ADMIN',1,'Success',NULL,NULL,'2025-01-23 05:18:22'),(321,'2025-01-23','System Date Increment','2025-01-24 05:18:22','2025-01-24 05:18:22','2025-01-23','SYSTEM',1,'Success',NULL,'Completed','2025-01-24 05:18:22'),(322,'2025-01-23','EOD Cycle Complete','2025-01-24 05:18:23','2025-01-24 05:18:23','2025-01-23','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-24','2025-01-24 05:18:23'),(323,'2025-01-24','Account Balance Update','2025-01-24 05:23:28','2025-01-24 05:23:28','2025-01-24','ADMIN',5,'Success',NULL,NULL,'2025-01-24 05:23:28'),(324,'2025-01-24','Interest Accrual Transaction Update','2025-01-24 05:23:31','2025-01-24 05:23:31','2025-01-24','ADMIN',10,'Success',NULL,NULL,'2025-01-24 05:23:31'),(325,'2025-01-24','Interest Accrual GL Movement Update','2025-01-24 05:23:33','2025-01-24 05:23:33','2025-01-24','ADMIN',10,'Success',NULL,NULL,'2025-01-24 05:23:33'),(326,'2025-01-24','GL Movement Update','2025-01-24 05:23:36','2025-01-24 05:23:36','2025-01-24','ADMIN',5,'Success',NULL,NULL,'2025-01-24 05:23:36'),(327,'2025-01-24','GL Balance Update','2025-01-24 05:23:38','2025-01-24 05:23:38','2025-01-24','ADMIN',7,'Success',NULL,NULL,'2025-01-24 05:23:38'),(328,'2025-01-24','Interest Accrual Account Balance Update','2025-01-24 05:23:40','2025-01-24 05:23:41','2025-01-24','ADMIN',5,'Success',NULL,NULL,'2025-01-24 05:23:40'),(329,'2025-01-24','Financial Reports Generation','2025-01-24 05:23:43','2025-01-24 05:23:43','2025-01-24','ADMIN',2,'Success',NULL,NULL,'2025-01-24 05:23:43'),(330,'2025-01-24','System Date Increment','2025-01-24 05:23:47','2025-01-25 05:23:47','2025-01-24','ADMIN',1,'Success',NULL,NULL,'2025-01-24 05:23:47'),(331,'2025-01-24','System Date Increment','2025-01-25 05:23:47','2025-01-25 05:23:47','2025-01-24','SYSTEM',1,'Success',NULL,'Completed','2025-01-25 05:23:47'),(332,'2025-01-24','EOD Cycle Complete','2025-01-25 05:23:47','2025-01-25 05:23:47','2025-01-24','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-25','2025-01-25 05:23:47'),(333,'2025-01-25','Account Balance Update','2025-01-25 05:30:44','2025-01-25 05:30:44','2025-01-25','ADMIN',6,'Success',NULL,NULL,'2025-01-25 05:30:44'),(334,'2025-01-25','Interest Accrual Transaction Update','2025-01-25 05:30:46','2025-01-25 05:30:46','2025-01-25','ADMIN',4,'Success',NULL,NULL,'2025-01-25 05:30:46'),(335,'2025-01-25','Interest Accrual GL Movement Update','2025-01-25 05:30:49','2025-01-25 05:30:49','2025-01-25','ADMIN',4,'Success',NULL,NULL,'2025-01-25 05:30:49'),(336,'2025-01-25','GL Movement Update','2025-01-25 05:30:52','2025-01-25 05:30:53','2025-01-25','ADMIN',6,'Success',NULL,NULL,'2025-01-25 05:30:52'),(337,'2025-01-25','GL Balance Update','2025-01-25 05:30:55','2025-01-25 05:30:56','2025-01-25','ADMIN',8,'Success',NULL,NULL,'2025-01-25 05:30:55'),(338,'2025-01-25','Interest Accrual Account Balance Update','2025-01-25 05:30:58','2025-01-25 05:30:59','2025-01-25','ADMIN',2,'Success',NULL,NULL,'2025-01-25 05:30:58'),(339,'2025-01-25','Financial Reports Generation','2025-01-25 05:31:01','2025-01-25 05:31:01','2025-01-25','ADMIN',2,'Success',NULL,NULL,'2025-01-25 05:31:01'),(340,'2025-01-25','System Date Increment','2025-01-25 05:31:05','2025-01-26 05:31:05','2025-01-25','ADMIN',1,'Success',NULL,NULL,'2025-01-25 05:31:05'),(341,'2025-01-25','System Date Increment','2025-01-26 05:31:05','2025-01-26 05:31:05','2025-01-25','SYSTEM',1,'Success',NULL,'Completed','2025-01-26 05:31:05'),(342,'2025-01-25','EOD Cycle Complete','2025-01-26 05:31:05','2025-01-26 05:31:05','2025-01-25','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-26','2025-01-26 05:31:05'),(343,'2025-01-26','Account Balance Update','2025-01-26 07:28:07','2025-01-26 07:28:07','2025-01-26','ADMIN',3,'Success',NULL,NULL,'2025-01-26 07:28:07'),(344,'2025-01-26','Interest Accrual Transaction Update','2025-01-26 07:28:09','2025-01-26 07:28:09','2025-01-26','ADMIN',6,'Success',NULL,NULL,'2025-01-26 07:28:09'),(345,'2025-01-26','Interest Accrual GL Movement Update','2025-01-26 07:28:12','2025-01-26 07:28:12','2025-01-26','ADMIN',6,'Success',NULL,NULL,'2025-01-26 07:28:12'),(346,'2025-01-26','GL Movement Update','2025-01-26 07:28:14','2025-01-26 07:28:14','2025-01-26','ADMIN',3,'Success',NULL,NULL,'2025-01-26 07:28:14'),(347,'2025-01-26','GL Balance Update','2025-01-26 07:28:17','2025-01-26 07:28:18','2025-01-26','ADMIN',6,'Success',NULL,NULL,'2025-01-26 07:28:17'),(348,'2025-01-26','Interest Accrual Account Balance Update','2025-01-26 07:28:20','2025-01-26 07:28:20','2025-01-26','ADMIN',3,'Success',NULL,NULL,'2025-01-26 07:28:20'),(349,'2025-01-26','Financial Reports Generation','2025-01-26 07:28:23','2025-01-26 07:28:23','2025-01-26','ADMIN',2,'Success',NULL,NULL,'2025-01-26 07:28:23'),(350,'2025-01-26','System Date Increment','2025-01-26 07:28:26','2025-01-27 07:28:26','2025-01-26','ADMIN',1,'Success',NULL,NULL,'2025-01-26 07:28:26'),(351,'2025-01-26','System Date Increment','2025-01-27 07:28:26','2025-01-27 07:28:26','2025-01-26','SYSTEM',1,'Success',NULL,'Completed','2025-01-27 07:28:26'),(352,'2025-01-26','EOD Cycle Complete','2025-01-27 07:28:26','2025-01-27 07:28:26','2025-01-26','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-27','2025-01-27 07:28:26'),(353,'2025-01-27','Account Balance Update','2025-01-27 07:28:57','2025-01-27 07:28:58','2025-01-27','ADMIN',0,'Success',NULL,NULL,'2025-01-27 07:28:57'),(354,'2025-01-27','Interest Accrual Transaction Update','2025-01-27 07:28:59','2025-01-27 07:28:59','2025-01-27','ADMIN',6,'Success',NULL,NULL,'2025-01-27 07:28:59'),(355,'2025-01-27','Interest Accrual GL Movement Update','2025-01-27 07:29:05','2025-01-27 07:29:06','2025-01-27','ADMIN',6,'Success',NULL,NULL,'2025-01-27 07:29:05'),(356,'2025-01-27','GL Movement Update','2025-01-27 07:29:13','2025-01-27 07:29:13','2025-01-27','ADMIN',0,'Success',NULL,NULL,'2025-01-27 07:29:13'),(357,'2025-01-27','GL Balance Update','2025-01-27 07:29:15','2025-01-27 07:29:16','2025-01-27','ADMIN',4,'Success',NULL,NULL,'2025-01-27 07:29:15'),(358,'2025-01-27','Interest Accrual Account Balance Update','2025-01-27 07:29:18','2025-01-27 07:29:18','2025-01-27','ADMIN',3,'Success',NULL,NULL,'2025-01-27 07:29:18'),(359,'2025-01-27','Financial Reports Generation','2025-01-27 07:29:20','2025-01-27 07:29:20','2025-01-27','ADMIN',2,'Success',NULL,NULL,'2025-01-27 07:29:20'),(360,'2025-01-27','System Date Increment','2025-01-27 07:29:24','2025-01-28 07:29:24','2025-01-27','ADMIN',1,'Success',NULL,NULL,'2025-01-27 07:29:24'),(361,'2025-01-27','System Date Increment','2025-01-28 07:29:24','2025-01-28 07:29:24','2025-01-27','SYSTEM',1,'Success',NULL,'Completed','2025-01-28 07:29:24'),(362,'2025-01-27','EOD Cycle Complete','2025-01-28 07:29:24','2025-01-28 07:29:24','2025-01-27','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-28','2025-01-28 07:29:24'),(363,'2025-01-28','Account Balance Update','2025-01-28 07:44:38','2025-01-28 07:44:39','2025-01-28','ADMIN',2,'Success',NULL,NULL,'2025-01-28 07:44:38'),(364,'2025-01-28','Interest Accrual Transaction Update','2025-01-28 07:44:41','2025-01-28 07:44:41','2025-01-28','ADMIN',8,'Success',NULL,NULL,'2025-01-28 07:44:41'),(365,'2025-01-28','Interest Accrual GL Movement Update','2025-01-28 07:44:43','2025-01-28 07:44:43','2025-01-28','ADMIN',8,'Success',NULL,NULL,'2025-01-28 07:44:43'),(366,'2025-01-28','GL Movement Update','2025-01-28 07:44:45','2025-01-28 07:44:45','2025-01-28','ADMIN',2,'Success',NULL,NULL,'2025-01-28 07:44:45'),(367,'2025-01-28','GL Balance Update','2025-01-28 07:44:47','2025-01-28 07:44:47','2025-01-28','ADMIN',6,'Success',NULL,NULL,'2025-01-28 07:44:47'),(368,'2025-01-28','Interest Accrual Account Balance Update','2025-01-28 07:44:50','2025-01-28 07:44:50','2025-01-28','ADMIN',4,'Success',NULL,NULL,'2025-01-28 07:44:50'),(369,'2025-01-28','Financial Reports Generation','2025-01-28 07:44:52','2025-01-28 07:44:53','2025-01-28','ADMIN',2,'Success',NULL,NULL,'2025-01-28 07:44:52'),(370,'2025-01-28','System Date Increment','2025-01-28 07:44:55','2025-01-29 07:44:55','2025-01-28','ADMIN',1,'Success',NULL,NULL,'2025-01-28 07:44:55'),(371,'2025-01-28','System Date Increment','2025-01-29 07:44:55','2025-01-29 07:44:55','2025-01-28','SYSTEM',1,'Success',NULL,'Completed','2025-01-29 07:44:55'),(372,'2025-01-28','EOD Cycle Complete','2025-01-29 07:44:55','2025-01-29 07:44:55','2025-01-28','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-29','2025-01-29 07:44:55'),(373,'2025-01-29','Account Balance Update','2025-01-29 08:32:51','2025-01-29 08:32:51','2025-01-29','ADMIN',0,'Success',NULL,NULL,'2025-01-29 08:32:51'),(374,'2025-01-29','Interest Accrual Transaction Update','2025-01-29 08:32:53','2025-01-29 08:32:53','2025-01-29','ADMIN',8,'Success',NULL,NULL,'2025-01-29 08:32:53'),(375,'2025-01-29','Interest Accrual GL Movement Update','2025-01-29 08:32:55','2025-01-29 08:32:55','2025-01-29','ADMIN',8,'Success',NULL,NULL,'2025-01-29 08:32:55'),(376,'2025-01-29','GL Movement Update','2025-01-29 08:32:57','2025-01-29 08:32:57','2025-01-29','ADMIN',0,'Success',NULL,NULL,'2025-01-29 08:32:57'),(377,'2025-01-29','GL Balance Update','2025-01-29 08:32:59','2025-01-29 08:33:00','2025-01-29','ADMIN',4,'Success',NULL,NULL,'2025-01-29 08:32:59'),(378,'2025-01-29','Interest Accrual Account Balance Update','2025-01-29 08:33:02','2025-01-29 08:33:02','2025-01-29','ADMIN',4,'Success',NULL,NULL,'2025-01-29 08:33:02'),(379,'2025-01-29','Financial Reports Generation','2025-01-29 08:33:05','2025-01-29 08:33:05','2025-01-29','ADMIN',2,'Success',NULL,NULL,'2025-01-29 08:33:05'),(380,'2025-01-29','System Date Increment','2025-01-29 08:33:11','2025-01-30 08:33:11','2025-01-29','ADMIN',1,'Success',NULL,NULL,'2025-01-29 08:33:11'),(381,'2025-01-29','System Date Increment','2025-01-30 08:33:11','2025-01-30 08:33:11','2025-01-29','SYSTEM',1,'Success',NULL,'Completed','2025-01-30 08:33:11'),(382,'2025-01-29','EOD Cycle Complete','2025-01-30 08:33:11','2025-01-30 08:33:11','2025-01-29','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-30','2025-01-30 08:33:11'),(383,'2025-01-30','Account Balance Update','2025-01-30 08:38:30','2025-01-30 08:38:30','2025-01-30','ADMIN',2,'Success',NULL,NULL,'2025-01-30 08:38:30'),(384,'2025-01-30','Interest Accrual Transaction Update','2025-01-30 08:38:32','2025-01-30 08:38:33','2025-01-30','ADMIN',10,'Success',NULL,NULL,'2025-01-30 08:38:32'),(385,'2025-01-30','Interest Accrual GL Movement Update','2025-01-30 08:38:35','2025-01-30 08:38:35','2025-01-30','ADMIN',10,'Success',NULL,NULL,'2025-01-30 08:38:35'),(386,'2025-01-30','GL Movement Update','2025-01-30 08:38:37','2025-01-30 08:38:37','2025-01-30','ADMIN',2,'Success',NULL,NULL,'2025-01-30 08:38:37'),(387,'2025-01-30','GL Balance Update','2025-01-30 08:38:39','2025-01-30 08:38:40','2025-01-30','ADMIN',6,'Success',NULL,NULL,'2025-01-30 08:38:39'),(388,'2025-01-30','Interest Accrual Account Balance Update','2025-01-30 08:38:42','2025-01-30 08:38:42','2025-01-30','ADMIN',5,'Success',NULL,NULL,'2025-01-30 08:38:42'),(389,'2025-01-30','Financial Reports Generation','2025-01-30 08:38:44','2025-01-30 08:38:45','2025-01-30','ADMIN',2,'Success',NULL,NULL,'2025-01-30 08:38:44'),(390,'2025-01-30','System Date Increment','2025-01-30 08:38:48','2025-01-31 08:38:48','2025-01-30','ADMIN',1,'Success',NULL,NULL,'2025-01-30 08:38:48'),(391,'2025-01-30','System Date Increment','2025-01-31 08:38:48','2025-01-31 08:38:48','2025-01-30','SYSTEM',1,'Success',NULL,'Completed','2025-01-31 08:38:48'),(392,'2025-01-30','EOD Cycle Complete','2025-01-31 08:38:48','2025-01-31 08:38:48','2025-01-30','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-01-31','2025-01-31 08:38:48'),(393,'2025-01-31','Account Balance Update','2025-01-31 08:42:51','2025-01-31 08:42:52','2025-01-31','ADMIN',7,'Success',NULL,NULL,'2025-01-31 08:42:51'),(394,'2025-01-31','Interest Accrual Transaction Update','2025-01-31 08:42:53','2025-01-31 08:42:53','2025-01-31','ADMIN',10,'Success',NULL,NULL,'2025-01-31 08:42:53'),(395,'2025-01-31','Interest Accrual GL Movement Update','2025-01-31 08:42:55','2025-01-31 08:42:55','2025-01-31','ADMIN',10,'Success',NULL,NULL,'2025-01-31 08:42:55'),(396,'2025-01-31','GL Movement Update','2025-01-31 08:42:59','2025-01-31 08:42:59','2025-01-31','ADMIN',7,'Success',NULL,NULL,'2025-01-31 08:42:59'),(397,'2025-01-31','GL Balance Update','2025-01-31 08:43:01','2025-01-31 08:43:02','2025-01-31','ADMIN',9,'Success',NULL,NULL,'2025-01-31 08:43:01'),(398,'2025-01-31','Interest Accrual Account Balance Update','2025-01-31 08:43:04','2025-01-31 08:43:04','2025-01-31','ADMIN',5,'Success',NULL,NULL,'2025-01-31 08:43:04'),(399,'2025-01-31','Financial Reports Generation','2025-01-31 08:43:06','2025-01-31 08:43:06','2025-01-31','ADMIN',2,'Success',NULL,NULL,'2025-01-31 08:43:06'),(400,'2025-01-31','System Date Increment','2025-01-31 08:43:09','2025-02-01 08:43:09','2025-01-31','ADMIN',1,'Success',NULL,NULL,'2025-01-31 08:43:09'),(401,'2025-01-31','System Date Increment','2025-02-01 08:43:09','2025-02-01 08:43:09','2025-01-31','SYSTEM',1,'Success',NULL,'Completed','2025-02-01 08:43:09'),(402,'2025-01-31','EOD Cycle Complete','2025-02-01 08:43:10','2025-02-01 08:43:10','2025-01-31','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-01','2025-02-01 08:43:10'),(403,'2025-02-01','Account Balance Update','2025-02-01 08:49:49','2025-02-01 08:49:49','2025-02-01','ADMIN',2,'Success',NULL,NULL,'2025-02-01 08:49:49'),(404,'2025-02-01','Interest Accrual Transaction Update','2025-02-01 08:49:51','2025-02-01 08:49:51','2025-02-01','ADMIN',12,'Success',NULL,NULL,'2025-02-01 08:49:51'),(405,'2025-02-01','Interest Accrual GL Movement Update','2025-02-01 08:49:53','2025-02-01 08:49:53','2025-02-01','ADMIN',12,'Success',NULL,NULL,'2025-02-01 08:49:53'),(406,'2025-02-01','GL Movement Update','2025-02-01 08:49:55','2025-02-01 08:49:55','2025-02-01','ADMIN',2,'Success',NULL,NULL,'2025-02-01 08:49:55'),(407,'2025-02-01','GL Balance Update','2025-02-01 08:49:57','2025-02-01 08:49:58','2025-02-01','ADMIN',6,'Success',NULL,NULL,'2025-02-01 08:49:57'),(408,'2025-02-01','Interest Accrual Account Balance Update','2025-02-01 08:49:59','2025-02-01 08:50:00','2025-02-01','ADMIN',6,'Success',NULL,NULL,'2025-02-01 08:49:59'),(409,'2025-02-01','Financial Reports Generation','2025-02-01 08:50:02','2025-02-01 08:50:02','2025-02-01','ADMIN',2,'Success',NULL,NULL,'2025-02-01 08:50:02'),(410,'2025-02-01','System Date Increment','2025-02-01 08:50:06','2025-02-02 08:50:06','2025-02-01','ADMIN',1,'Success',NULL,NULL,'2025-02-01 08:50:06'),(411,'2025-02-01','System Date Increment','2025-02-02 08:50:06','2025-02-02 08:50:06','2025-02-01','SYSTEM',1,'Success',NULL,'Completed','2025-02-02 08:50:06'),(412,'2025-02-01','EOD Cycle Complete','2025-02-02 08:50:06','2025-02-02 08:50:06','2025-02-01','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-02','2025-02-02 08:50:06'),(413,'2025-02-02','Account Balance Update','2025-02-02 09:04:42','2025-02-02 09:04:42','2025-02-02','ADMIN',2,'Success',NULL,NULL,'2025-02-02 09:04:42'),(414,'2025-02-02','Interest Accrual Transaction Update','2025-02-02 09:04:45','2025-02-02 09:04:46','2025-02-02','ADMIN',14,'Success',NULL,NULL,'2025-02-02 09:04:45'),(415,'2025-02-02','Interest Accrual GL Movement Update','2025-02-02 09:04:48','2025-02-02 09:04:49','2025-02-02','ADMIN',14,'Success',NULL,NULL,'2025-02-02 09:04:48'),(416,'2025-02-02','GL Movement Update','2025-02-02 09:04:52','2025-02-02 09:04:52','2025-02-02','ADMIN',2,'Success',NULL,NULL,'2025-02-02 09:04:52'),(417,'2025-02-02','GL Balance Update','2025-02-02 09:04:55','2025-02-02 09:04:56','2025-02-02','ADMIN',6,'Success',NULL,NULL,'2025-02-02 09:04:55'),(418,'2025-02-02','Interest Accrual Account Balance Update','2025-02-02 09:05:00','2025-02-02 09:05:00','2025-02-02','ADMIN',7,'Success',NULL,NULL,'2025-02-02 09:05:00'),(419,'2025-02-02','Financial Reports Generation','2025-02-02 09:05:06','2025-02-02 09:05:06','2025-02-02','ADMIN',2,'Success',NULL,NULL,'2025-02-02 09:05:06'),(420,'2025-02-02','System Date Increment','2025-02-02 09:05:10','2025-02-03 09:05:10','2025-02-02','ADMIN',1,'Success',NULL,NULL,'2025-02-02 09:05:10'),(421,'2025-02-02','System Date Increment','2025-02-03 09:05:10','2025-02-03 09:05:10','2025-02-02','SYSTEM',1,'Success',NULL,'Completed','2025-02-03 09:05:10'),(422,'2025-02-02','EOD Cycle Complete','2025-02-03 09:05:10','2025-02-03 09:05:10','2025-02-02','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-03','2025-02-03 09:05:10'),(423,'2025-02-03','Account Balance Update','2025-02-03 09:30:40','2025-02-03 09:30:40','2025-02-03','ADMIN',2,'Success',NULL,NULL,'2025-02-03 09:30:40'),(424,'2025-02-03','Interest Accrual Transaction Update','2025-02-03 09:30:45','2025-02-03 09:30:45','2025-02-03','ADMIN',16,'Success',NULL,NULL,'2025-02-03 09:30:45'),(425,'2025-02-03','Interest Accrual GL Movement Update','2025-02-03 09:30:47','2025-02-03 09:30:48','2025-02-03','ADMIN',16,'Success',NULL,NULL,'2025-02-03 09:30:47'),(426,'2025-02-03','GL Movement Update','2025-02-03 09:30:51','2025-02-03 09:30:51','2025-02-03','ADMIN',2,'Success',NULL,NULL,'2025-02-03 09:30:51'),(427,'2025-02-03','GL Balance Update','2025-02-03 09:30:54','2025-02-03 09:30:55','2025-02-03','ADMIN',6,'Success',NULL,NULL,'2025-02-03 09:30:54'),(428,'2025-02-03','Interest Accrual Account Balance Update','2025-02-03 09:30:59','2025-02-03 09:30:59','2025-02-03','ADMIN',8,'Success',NULL,NULL,'2025-02-03 09:30:59'),(429,'2025-02-03','Financial Reports Generation','2025-02-03 09:31:01','2025-02-03 09:31:01','2025-02-03','ADMIN',2,'Success',NULL,NULL,'2025-02-03 09:31:01'),(430,'2025-02-03','System Date Increment','2025-02-03 09:31:05','2025-02-04 09:31:05','2025-02-03','ADMIN',1,'Success',NULL,NULL,'2025-02-03 09:31:05'),(431,'2025-02-03','System Date Increment','2025-02-04 09:31:05','2025-02-04 09:31:05','2025-02-03','SYSTEM',1,'Success',NULL,'Completed','2025-02-04 09:31:05'),(432,'2025-02-03','EOD Cycle Complete','2025-02-04 09:31:05','2025-02-04 09:31:05','2025-02-03','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-04','2025-02-04 09:31:05'),(433,'2025-02-04','Account Balance Update','2025-02-04 09:40:26','2025-02-04 09:40:27','2025-02-04','ADMIN',0,'Success',NULL,NULL,'2025-02-04 09:40:26'),(434,'2025-02-04','Interest Accrual Transaction Update','2025-02-04 09:40:29','2025-02-04 09:40:29','2025-02-04','ADMIN',16,'Success',NULL,NULL,'2025-02-04 09:40:29'),(435,'2025-02-04','Interest Accrual GL Movement Update','2025-02-04 09:40:31','2025-02-04 09:40:31','2025-02-04','ADMIN',16,'Success',NULL,NULL,'2025-02-04 09:40:31'),(436,'2025-02-04','GL Movement Update','2025-02-04 09:40:33','2025-02-04 09:40:33','2025-02-04','ADMIN',0,'Success',NULL,NULL,'2025-02-04 09:40:33'),(437,'2025-02-04','GL Balance Update','2025-02-04 09:40:36','2025-02-04 09:40:37','2025-02-04','ADMIN',4,'Success',NULL,NULL,'2025-02-04 09:40:36'),(438,'2025-02-04','Interest Accrual Account Balance Update','2025-02-04 09:40:39','2025-02-04 09:40:39','2025-02-04','ADMIN',8,'Success',NULL,NULL,'2025-02-04 09:40:39'),(439,'2025-02-04','Financial Reports Generation','2025-02-04 09:40:41','2025-02-04 09:40:41','2025-02-04','ADMIN',2,'Success',NULL,NULL,'2025-02-04 09:40:41'),(440,'2025-02-04','System Date Increment','2025-02-04 09:40:45','2025-02-05 09:40:45','2025-02-04','ADMIN',1,'Success',NULL,NULL,'2025-02-04 09:40:45'),(441,'2025-02-04','System Date Increment','2025-02-05 09:40:45','2025-02-05 09:40:45','2025-02-04','SYSTEM',1,'Success',NULL,'Completed','2025-02-05 09:40:45'),(442,'2025-02-04','EOD Cycle Complete','2025-02-05 09:40:45','2025-02-05 09:40:45','2025-02-04','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-05','2025-02-05 09:40:45'),(443,'2025-02-05','Account Balance Update','2025-02-05 07:22:44','2025-02-05 07:22:45','2025-02-05','ADMIN',2,'Success',NULL,NULL,'2025-02-05 07:22:44'),(444,'2025-02-05','Interest Accrual Transaction Update','2025-02-05 07:22:47','2025-02-05 07:22:48','2025-02-05','ADMIN',18,'Success',NULL,NULL,'2025-02-05 07:22:47'),(445,'2025-02-05','Interest Accrual GL Movement Update','2025-02-05 07:22:51','2025-02-05 07:22:52','2025-02-05','ADMIN',18,'Success',NULL,NULL,'2025-02-05 07:22:51'),(446,'2025-02-05','GL Movement Update','2025-02-05 07:22:55','2025-02-05 07:22:55','2025-02-05','ADMIN',2,'Success',NULL,NULL,'2025-02-05 07:22:55'),(447,'2025-02-05','GL Balance Update','2025-02-05 07:22:57','2025-02-05 07:22:58','2025-02-05','ADMIN',6,'Success',NULL,NULL,'2025-02-05 07:22:57'),(448,'2025-02-05','Interest Accrual Account Balance Update','2025-02-05 07:23:00','2025-02-05 07:23:01','2025-02-05','ADMIN',9,'Success',NULL,NULL,'2025-02-05 07:23:00'),(449,'2025-02-05','Financial Reports Generation','2025-02-05 07:23:04','2025-02-05 07:23:04','2025-02-05','ADMIN',2,'Success',NULL,NULL,'2025-02-05 07:23:04'),(450,'2025-02-05','System Date Increment','2025-02-05 07:23:10','2025-02-06 07:23:10','2025-02-05','ADMIN',1,'Success',NULL,NULL,'2025-02-05 07:23:10'),(451,'2025-02-05','System Date Increment','2025-02-06 07:23:10','2025-02-06 07:23:10','2025-02-05','SYSTEM',1,'Success',NULL,'Completed','2025-02-06 07:23:10'),(452,'2025-02-05','EOD Cycle Complete','2025-02-06 07:23:10','2025-02-06 07:23:10','2025-02-05','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-06','2025-02-06 07:23:10'),(453,'2025-02-06','Account Balance Update','2025-02-06 10:05:43','2025-02-06 10:05:43','2025-02-06','ADMIN',2,'Success',NULL,NULL,'2025-02-06 10:05:43'),(454,'2025-02-06','Interest Accrual Transaction Update','2025-02-06 10:05:45','2025-02-06 10:05:46','2025-02-06','ADMIN',18,'Success',NULL,NULL,'2025-02-06 10:05:45'),(455,'2025-02-06','Interest Accrual GL Movement Update','2025-02-06 10:05:47','2025-02-06 10:05:48','2025-02-06','ADMIN',18,'Success',NULL,NULL,'2025-02-06 10:05:47'),(456,'2025-02-06','GL Movement Update','2025-02-06 10:05:52','2025-02-06 10:05:53','2025-02-06','ADMIN',2,'Success',NULL,NULL,'2025-02-06 10:05:52'),(457,'2025-02-06','GL Balance Update','2025-02-06 10:05:54','2025-02-06 10:05:55','2025-02-06','ADMIN',6,'Success',NULL,NULL,'2025-02-06 10:05:54'),(458,'2025-02-06','Interest Accrual Account Balance Update','2025-02-06 10:05:57','2025-02-06 10:05:57','2025-02-06','ADMIN',9,'Success',NULL,NULL,'2025-02-06 10:05:57'),(459,'2025-02-06','Financial Reports Generation','2025-02-06 10:05:59','2025-02-06 10:05:59','2025-02-06','ADMIN',2,'Success',NULL,NULL,'2025-02-06 10:05:59'),(460,'2025-02-06','System Date Increment','2025-02-06 10:06:05','2025-02-07 10:06:05','2025-02-06','ADMIN',1,'Success',NULL,NULL,'2025-02-06 10:06:05'),(461,'2025-02-06','System Date Increment','2025-02-07 10:06:05','2025-02-07 10:06:05','2025-02-06','SYSTEM',1,'Success',NULL,'Completed','2025-02-07 10:06:05'),(462,'2025-02-06','EOD Cycle Complete','2025-02-07 10:06:06','2025-02-07 10:06:06','2025-02-06','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-07','2025-02-07 10:06:06'),(463,'2025-02-07','Account Balance Update','2025-02-07 09:54:43','2025-02-07 09:54:43','2025-02-07','ADMIN',4,'Success',NULL,NULL,'2025-02-07 09:54:43'),(464,'2025-02-07','Interest Accrual Transaction Update','2025-02-07 09:56:16','2025-02-07 09:56:17','2025-02-07','ADMIN',18,'Success',NULL,NULL,'2025-02-07 09:56:16'),(465,'2025-02-07','Interest Accrual GL Movement Update','2025-02-07 09:59:10','2025-02-07 09:59:10','2025-02-07','ADMIN',18,'Success',NULL,NULL,'2025-02-07 09:59:10'),(466,'2025-02-07','GL Movement Update','2025-02-07 10:00:57','2025-02-07 10:00:57','2025-02-07','ADMIN',5,'Success',NULL,NULL,'2025-02-07 10:00:57'),(467,'2025-02-07','GL Balance Update','2025-02-07 10:02:03','2025-02-07 10:02:11','2025-02-07','ADMIN',72,'Success',NULL,NULL,'2025-02-07 10:02:03'),(468,'2025-02-07','GL Balance Update','2025-02-07 10:02:08','2025-02-07 10:02:17','2025-02-07','ADMIN',72,'Success',NULL,NULL,'2025-02-07 10:02:08'),(469,'2025-02-07','Interest Accrual Account Balance Update','2025-02-07 10:41:07','2025-02-07 10:41:08','2025-02-07','ADMIN',9,'Success',NULL,NULL,'2025-02-07 10:41:07'),(470,'2025-02-07','Financial Reports Generation','2025-02-07 10:41:17','2025-02-07 10:41:20','2025-02-07','ADMIN',2,'Success',NULL,NULL,'2025-02-07 10:41:17'),(471,'2025-02-07','System Date Increment','2025-02-07 10:42:42','2025-02-08 10:42:42','2025-02-07','ADMIN',1,'Success',NULL,NULL,'2025-02-07 10:42:42'),(472,'2025-02-07','System Date Increment','2025-02-08 10:42:42','2025-02-08 10:42:42','2025-02-07','SYSTEM',1,'Success',NULL,'Completed','2025-02-08 10:42:42'),(473,'2025-02-07','EOD Cycle Complete','2025-02-08 10:42:42','2025-02-08 10:42:42','2025-02-07','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-08','2025-02-08 10:42:42'),(474,'2025-02-08','Account Balance Update','2025-02-08 11:41:38','2025-02-08 11:41:39','2025-02-08','ADMIN',0,'Success',NULL,NULL,'2025-02-08 11:41:38'),(475,'2025-02-08','Interest Accrual Transaction Update','2025-02-08 11:41:42','2025-02-08 11:41:44','2025-02-08','ADMIN',18,'Success',NULL,NULL,'2025-02-08 11:41:42'),(476,'2025-02-08','Interest Accrual GL Movement Update','2025-02-08 11:41:48','2025-02-08 11:41:50','2025-02-08','ADMIN',18,'Success',NULL,NULL,'2025-02-08 11:41:48'),(477,'2025-02-08','GL Movement Update','2025-02-08 11:41:53','2025-02-08 11:41:53','2025-02-08','ADMIN',0,'Success',NULL,NULL,'2025-02-08 11:41:53'),(478,'2025-02-08','GL Balance Update','2025-02-08 11:41:56','2025-02-08 11:41:58','2025-02-08','ADMIN',11,'Success',NULL,NULL,'2025-02-08 11:41:56'),(479,'2025-02-08','Interest Accrual Account Balance Update','2025-02-08 11:42:01','2025-02-08 11:42:02','2025-02-08','ADMIN',9,'Success',NULL,NULL,'2025-02-08 11:42:01'),(480,'2025-02-08','Financial Reports Generation','2025-02-08 11:42:05','2025-02-08 11:42:09','2025-02-08','ADMIN',2,'Success',NULL,NULL,'2025-02-08 11:42:05'),(481,'2025-02-08','System Date Increment','2025-02-08 11:42:13','2025-02-09 11:42:13','2025-02-08','ADMIN',1,'Success',NULL,NULL,'2025-02-08 11:42:13'),(482,'2025-02-08','System Date Increment','2025-02-09 11:42:13','2025-02-09 11:42:13','2025-02-08','SYSTEM',1,'Success',NULL,'Completed','2025-02-09 11:42:13'),(483,'2025-02-08','EOD Cycle Complete','2025-02-09 11:42:14','2025-02-09 11:42:14','2025-02-08','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-09','2025-02-09 11:42:14'),(484,'2025-02-09','Account Balance Update','2025-02-09 11:48:28','2025-02-09 11:48:28','2025-02-09','ADMIN',3,'Success',NULL,NULL,'2025-02-09 11:48:28'),(485,'2025-02-09','Interest Accrual Transaction Update','2025-02-09 11:48:31','2025-02-09 11:48:32','2025-02-09','ADMIN',18,'Success',NULL,NULL,'2025-02-09 11:48:31'),(486,'2025-02-09','Interest Accrual GL Movement Update','2025-02-09 11:48:34','2025-02-09 11:48:35','2025-02-09','ADMIN',18,'Success',NULL,NULL,'2025-02-09 11:48:34'),(487,'2025-02-09','GL Movement Update','2025-02-09 11:48:37','2025-02-09 11:48:37','2025-02-09','ADMIN',4,'Success',NULL,NULL,'2025-02-09 11:48:37'),(488,'2025-02-09','GL Balance Update','2025-02-09 11:48:40','2025-02-09 11:48:42','2025-02-09','ADMIN',11,'Success',NULL,NULL,'2025-02-09 11:48:40'),(489,'2025-02-09','Interest Accrual Account Balance Update','2025-02-09 11:48:45','2025-02-09 11:48:45','2025-02-09','ADMIN',9,'Success',NULL,NULL,'2025-02-09 11:48:45'),(490,'2025-02-09','Financial Reports Generation','2025-02-09 11:48:48','2025-02-09 11:48:48','2025-02-09','ADMIN',2,'Success',NULL,NULL,'2025-02-09 11:48:48'),(491,'2025-02-09','System Date Increment','2025-02-09 11:48:53','2025-02-10 11:48:53','2025-02-09','ADMIN',1,'Success',NULL,NULL,'2025-02-09 11:48:53'),(492,'2025-02-09','System Date Increment','2025-02-10 11:48:53','2025-02-10 11:48:53','2025-02-09','SYSTEM',1,'Success',NULL,'Completed','2025-02-10 11:48:53'),(493,'2025-02-09','EOD Cycle Complete','2025-02-10 11:48:54','2025-02-10 11:48:54','2025-02-09','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-10','2025-02-10 11:48:54'),(494,'2025-02-10','Account Balance Update','2025-02-10 11:57:29','2025-02-10 11:57:30','2025-02-10','ADMIN',2,'Success',NULL,NULL,'2025-02-10 11:57:29'),(495,'2025-02-10','Interest Accrual Transaction Update','2025-02-10 11:57:32','2025-02-10 11:57:33','2025-02-10','ADMIN',20,'Success',NULL,NULL,'2025-02-10 11:57:32'),(496,'2025-02-10','Interest Accrual GL Movement Update','2025-02-10 11:57:35','2025-02-10 11:57:36','2025-02-10','ADMIN',20,'Success',NULL,NULL,'2025-02-10 11:57:35'),(497,'2025-02-10','GL Movement Update','2025-02-10 11:57:39','2025-02-10 11:57:39','2025-02-10','ADMIN',2,'Success',NULL,NULL,'2025-02-10 11:57:39'),(498,'2025-02-10','GL Balance Update','2025-02-10 11:57:42','2025-02-10 11:57:44','2025-02-10','ADMIN',13,'Success',NULL,NULL,'2025-02-10 11:57:42'),(499,'2025-02-10','Interest Accrual Account Balance Update','2025-02-10 11:57:46','2025-02-10 11:57:47','2025-02-10','ADMIN',10,'Success',NULL,NULL,'2025-02-10 11:57:46'),(500,'2025-02-10','Financial Reports Generation','2025-02-10 11:57:50','2025-02-10 11:57:50','2025-02-10','ADMIN',2,'Success',NULL,NULL,'2025-02-10 11:57:50'),(501,'2025-02-10','System Date Increment','2025-02-10 11:57:55','2025-02-11 11:57:56','2025-02-10','ADMIN',1,'Success',NULL,NULL,'2025-02-10 11:57:55'),(502,'2025-02-10','System Date Increment','2025-02-11 11:57:55','2025-02-11 11:57:55','2025-02-10','SYSTEM',1,'Success',NULL,'Completed','2025-02-11 11:57:55'),(503,'2025-02-10','EOD Cycle Complete','2025-02-11 11:57:56','2025-02-11 11:57:56','2025-02-10','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-11','2025-02-11 11:57:56'),(504,'2025-02-11','Account Balance Update','2025-02-11 12:07:53','2025-02-11 12:07:53','2025-02-11','ADMIN',2,'Success',NULL,NULL,'2025-02-11 12:07:53'),(505,'2025-02-11','Interest Accrual Transaction Update','2025-02-11 12:07:55','2025-02-11 12:07:56','2025-02-11','ADMIN',22,'Success',NULL,NULL,'2025-02-11 12:07:55'),(506,'2025-02-11','Interest Accrual GL Movement Update','2025-02-11 12:07:58','2025-02-11 12:07:58','2025-02-11','ADMIN',22,'Success',NULL,NULL,'2025-02-11 12:07:58'),(507,'2025-02-11','GL Movement Update','2025-02-11 12:08:00','2025-02-11 12:08:01','2025-02-11','ADMIN',2,'Success',NULL,NULL,'2025-02-11 12:08:00'),(508,'2025-02-11','GL Balance Update','2025-02-11 12:08:03','2025-02-11 12:08:05','2025-02-11','ADMIN',13,'Success',NULL,NULL,'2025-02-11 12:08:03'),(509,'2025-02-11','Interest Accrual Account Balance Update','2025-02-11 12:08:07','2025-02-11 12:08:07','2025-02-11','ADMIN',11,'Success',NULL,NULL,'2025-02-11 12:08:07'),(511,'2025-02-11','System Date Increment','2025-02-11 12:08:14','2025-02-12 12:08:14','2025-02-11','ADMIN',1,'Success',NULL,NULL,'2025-02-11 12:08:14'),(512,'2025-02-11','System Date Increment','2025-02-12 12:08:14','2025-02-12 12:08:14','2025-02-11','SYSTEM',1,'Success',NULL,'Completed','2025-02-12 12:08:14'),(513,'2025-02-11','EOD Cycle Complete','2025-02-12 12:08:15','2025-02-12 12:08:15','2025-02-11','ADMIN',8,'Success',NULL,'All jobs completed, system date incremented to 2025-02-12','2025-02-12 12:08:15'),(517,'2025-02-11','Financial Reports Generation','2025-02-11 08:33:04','2025-02-11 08:33:05','2025-02-11','ADMIN',2,'Success',NULL,NULL,'2025-02-11 08:33:04');
/*!40000 ALTER TABLE `eod_log_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2025-09-30 09:32:50',0,1),(2,'2','fix acct bal table for eod','SQL','V2__fix_acct_bal_table_for_eod.sql',-254915298,'root','2025-10-15 10:21:58',100,1),(4,'3','update gl setup data','SQL','V3__update_gl_setup_data.sql',1993405280,'root','2025-10-13 05:34:30',291,1),(5,'4','add interest rate master','SQL','V4__add_interest_rate_master.sql',1124062219,'root','2025-10-13 05:35:49',698,1),(6,'5','add parameter and eod log tables','SQL','V5__add_parameter_and_eod_log_tables.sql',2097120739,'root','2025-10-16 05:15:14',4057,1),(7,'6','remove current timestamp defaults','SQL','V6__remove_current_timestamp_defaults.sql',-1152685688,'root','2025-10-16 08:36:13',2017,1),(8,'7','add missing fields','SQL','V7__add_missing_fields.sql',-1855394343,'root','2025-10-19 05:18:06',4748,1),(9,'8','add missing office account balances','SQL','V8__add_missing_office_account_balances.sql',-538273628,'root','2025-10-19 07:11:44',456,0);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_balance`
--

DROP TABLE IF EXISTS `gl_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_balance` (
  `Id` bigint NOT NULL AUTO_INCREMENT,
  `GL_Num` varchar(9) NOT NULL,
  `Tran_date` date NOT NULL,
  `Opening_Bal` decimal(20,2) DEFAULT '0.00',
  `DR_Summation` decimal(20,2) DEFAULT '0.00',
  `CR_Summation` decimal(20,2) DEFAULT '0.00',
  `Closing_Bal` decimal(20,2) DEFAULT '0.00',
  `Current_Balance` decimal(20,2) NOT NULL DEFAULT '0.00',
  `Last_Updated` timestamp NOT NULL,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `uq_gl_balance_gl_num_tran_date` (`GL_Num`,`Tran_date`),
  KEY `idx_gl_balance_gl_num` (`GL_Num`),
  KEY `idx_gl_balance_tran_date` (`Tran_date`),
  CONSTRAINT `gl_balance_ibfk_1` FOREIGN KEY (`GL_Num`) REFERENCES `gl_setup` (`GL_Num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GL balances - CBS Compliance: Last_Updated controlled by SystemDateService';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_balance`
--

LOCK TABLES `gl_balance` WRITE;
/*!40000 ALTER TABLE `gl_balance` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_balance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_balance_backup_20251023`
--

DROP TABLE IF EXISTS `gl_balance_backup_20251023`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_balance_backup_20251023` (
  `GL_Num` varchar(9) NOT NULL,
  `Tran_date` date NOT NULL,
  `Opening_Bal` decimal(20,2) DEFAULT '0.00',
  `DR_Summation` decimal(20,2) DEFAULT '0.00',
  `CR_Summation` decimal(20,2) DEFAULT '0.00',
  `Closing_Bal` decimal(20,2) DEFAULT '0.00',
  `Current_Balance` decimal(20,2) NOT NULL DEFAULT '0.00',
  `Last_Updated` timestamp NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_balance_backup_20251023`
--

LOCK TABLES `gl_balance_backup_20251023` WRITE;
/*!40000 ALTER TABLE `gl_balance_backup_20251023` DISABLE KEYS */;
INSERT INTO `gl_balance_backup_20251023` VALUES ('110101001','2025-01-10',0.00,100.00,1000.00,900.00,900.00,'2025-01-10 05:06:00'),('110101001','2025-01-11',900.00,50.00,25.00,875.00,875.00,'2025-01-11 04:29:36'),('110102001','2025-01-10',0.00,0.00,200.00,200.00,200.00,'2025-01-10 05:06:01'),('110102001','2025-01-11',200.00,25.00,50.00,225.00,225.00,'2025-01-11 04:29:36'),('130101001','2025-01-10',0.00,0.00,0.25,0.25,0.25,'2025-01-10 05:06:00'),('130101001','2025-01-11',0.25,0.00,0.26,0.51,0.51,'2025-01-11 04:29:36'),('140102001','2025-01-10',0.00,0.04,0.00,-0.04,-0.04,'2025-01-10 05:06:00'),('140102001','2025-01-11',-0.04,0.05,0.00,-0.09,-0.09,'2025-01-11 04:29:35'),('210201001','2025-01-10',0.00,1100.00,0.00,-1100.00,-1100.00,'2025-01-10 05:06:01'),('210201001','2025-01-12',0.00,10000.00,0.00,-10000.00,-10000.00,'2025-01-12 08:40:50'),('240101001','2025-01-10',0.00,0.21,0.00,-0.21,-0.21,'2025-01-10 05:06:00'),('240101001','2025-01-11',-0.21,0.21,0.00,-0.42,-0.42,'2025-01-11 04:29:35');
/*!40000 ALTER TABLE `gl_balance_backup_20251023` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_movement`
--

DROP TABLE IF EXISTS `gl_movement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_movement` (
  `Movement_Id` bigint NOT NULL AUTO_INCREMENT,
  `Tran_Id` varchar(20) NOT NULL,
  `GL_Num` varchar(9) NOT NULL,
  `Dr_Cr_Flag` enum('D','C') NOT NULL,
  `Tran_Date` date NOT NULL,
  `Value_Date` date NOT NULL,
  `Tran_Ccy` varchar(3) DEFAULT NULL,
  `FCY_Amt` decimal(20,2) DEFAULT NULL,
  `LCY_Amt` decimal(20,2) DEFAULT NULL,
  `Amount` decimal(20,2) NOT NULL,
  `Balance_After` decimal(20,2) NOT NULL,
  `Narration` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`Movement_Id`),
  KEY `GL_Num` (`GL_Num`),
  KEY `Tran_Id` (`Tran_Id`),
  KEY `idx_gl_movement_date` (`Tran_Date`),
  CONSTRAINT `gl_movement_ibfk_1` FOREIGN KEY (`GL_Num`) REFERENCES `gl_setup` (`GL_Num`),
  CONSTRAINT `gl_movement_ibfk_2` FOREIGN KEY (`Tran_Id`) REFERENCES `tran_table` (`Tran_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_movement`
--

LOCK TABLES `gl_movement` WRITE;
/*!40000 ALTER TABLE `gl_movement` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_movement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_movement_accrual`
--

DROP TABLE IF EXISTS `gl_movement_accrual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_movement_accrual` (
  `Movement_Id` bigint NOT NULL AUTO_INCREMENT,
  `Accr_Tran_Id` varchar(20) NOT NULL,
  `GL_Num` varchar(9) NOT NULL,
  `Dr_Cr_Flag` enum('D','C') NOT NULL,
  `Accrual_Date` date NOT NULL,
  `Tran_date` date DEFAULT NULL,
  `Tran_Id` varchar(20) DEFAULT NULL,
  `Tran_Ccy` varchar(3) DEFAULT NULL,
  `FCY_Amt` decimal(20,2) DEFAULT '0.00',
  `Exchange_Rate` decimal(10,4) DEFAULT '1.0000',
  `LCY_Amt` decimal(20,2) DEFAULT '0.00',
  `Narration` varchar(100) DEFAULT NULL,
  `Amount` decimal(20,2) NOT NULL,
  `Status` enum('Pending','Posted','Verified') NOT NULL DEFAULT 'Pending',
  PRIMARY KEY (`Movement_Id`),
  UNIQUE KEY `idx_unique_accr_tran_id` (`Accr_Tran_Id`),
  KEY `GL_Num` (`GL_Num`),
  KEY `fk_glmva_tran` (`Tran_Id`),
  CONSTRAINT `fk_glmva_tran` FOREIGN KEY (`Tran_Id`) REFERENCES `tran_table` (`Tran_Id`) ON DELETE SET NULL,
  CONSTRAINT `gl_movement_accrual_ibfk_1` FOREIGN KEY (`GL_Num`) REFERENCES `gl_setup` (`GL_Num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_movement_accrual`
--

LOCK TABLES `gl_movement_accrual` WRITE;
/*!40000 ALTER TABLE `gl_movement_accrual` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_movement_accrual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_setup`
--

DROP TABLE IF EXISTS `gl_setup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_setup` (
  `GL_Name` varchar(50) DEFAULT NULL,
  `Layer_Id` int DEFAULT NULL,
  `Layer_GL_Num` varchar(9) DEFAULT NULL,
  `Parent_GL_Num` varchar(9) DEFAULT NULL,
  `GL_Num` varchar(9) NOT NULL,
  PRIMARY KEY (`GL_Num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_setup`
--

LOCK TABLES `gl_setup` WRITE;
/*!40000 ALTER TABLE `gl_setup` DISABLE KEYS */;
INSERT INTO `gl_setup` VALUES ('Liability',0,'100000000','-','100000000'),('Deposits',1,'10000000','100000000','110000000'),('Demand Deposit',2,'0100000','110000000','110100000'),('Savings Bank',3,'01000','110100000','110101000'),('Savings Bank Regular',4,'001','110101000','110101001'),('Savings Bank (Sr. Citizen)',4,'002','110101000','110101002'),('Current Account',3,'02000','110100000','110102000'),('Current Account Regular',4,'001','110102000','110102001'),('Time Deposit',2,'0200000','110000000','110200000'),('Term Deposit',3,'01000','110200000','110201000'),('Term Deposit Cum',4,'001','110201000','110201001'),('Term Deposit Non Cum',4,'002','110201000','110201002'),('Rec Dep',3,'02000','110200000','110202000'),('RDREG',4,'001','110202000','110202001'),('Office Liability',1,'20000000','100000000','120000000'),('Equity Shares',3,'01000','120100000','120101000'),('EQSHR1',4,'001','120101000','120101001'),('Office Payable',1,'30000000','100000000','130000000'),('Interest Payable',2,'0100000','130000000','130100000'),('Interest Payable Savings Bank',3,'01000','130100000','130101000'),('Interest Payable Savings Bank Regular',4,'001','130101000','130101001'),('IP TD',3,'02000','130100000','130102000'),('IPTDCUM',4,'001','130102000','130102001'),('Payable Oths',3,'02000','130200000','130201000'),('PBMIS',4,'001','130201000','130201001'),('Income',1,'40000000','100000000','140000000'),('Interest Income',2,'0100000','140000000','140100000'),('Overdraft Interest Income',3,'01000','140100000','140101000'),('OD against TD Interest Income',4,'001','140101000','140101001'),('Int Inc HL',3,'02000','140100000','140102000'),('IIHLWMN',4,'001','140102000','140102001'),('Int Inc STL',3,'03000','140100000','140103000'),('IISTLTR',4,'001','140103000','140103001'),('Mis Charges',3,'02000','140200000','140201000'),('MSCHG',4,'001','140201000','140201001'),('Asset',0,'200000000','-','200000000'),('Loans',1,'10000000','200000000','210000000'),('Housing Loan',3,'01000','210100000','210101000'),('HLWMN',4,'001','210101000','210101001'),('Short Term Loan',3,'02000','210100000','210102000'),('STLTR',4,'001','210102000','210102001'),('Demand Loans',2,'0200000','210000000','210200000'),('Overdraft',3,'01000','210200000','210201000'),('OD against TD',4,'001','210201000','210201001'),('FnF',3,'01000','220100000','220101000'),('FACHR',4,'001','220101000','220101001'),('Vault',3,'01000','220200000','220201000'),('CVLT1',4,'001','220201000','220201001'),('Teller',3,'02000','220200000','220202000'),('CTLR1',4,'001','220202000','220202001'),('In Transit',3,'03000','220200000','220203000'),('CAGC1',4,'001','220203000','220203001'),('Int Rcvbl OD',3,'01000','230100000','230101000'),('IRODATD',4,'001','230101000','230101001'),('Int Rcvbl HL',3,'02000','230100000','230102000'),('IRHLWMN',4,'001','230102000','230102001'),('Int Rcvbl STL',3,'03000','230100000','230103000'),('IRSTLTR',4,'001','230103000','230103001'),('Rcvbls Oths',3,'02000','230200000','230201000'),('RBMIS',4,'001','230201000','230201001'),('Expenditure',1,'40000000','200000000','240000000'),('Interest Expenditure',2,'0100000','240000000','240100000'),('Interest Expenditure Savings Bank',3,'01000','240100000','240101000'),('Interest Expenditure Savings Bank Regular',4,'001','240101000','240101001'),('Int Exp TD',3,'02000','240100000','240102000'),('IETDCUM',4,'001','240102000','240102001'),('Rent',3,'02000','240200000','240201000'),('OFRNT',4,'001','240201000','240201001'),('Misc Exp',3,'02000','240200000','240202000'),('EXMS1',4,'001','240202000','240202001'),('Position USD',3,'01000','920100000','920101000'),('PSUSD',4,'001','920101000','920101001');
/*!40000 ALTER TABLE `gl_setup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `interest_rate_master`
--

DROP TABLE IF EXISTS `interest_rate_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interest_rate_master` (
  `Intt_Code` varchar(20) NOT NULL,
  `Intt_Rate` decimal(5,2) NOT NULL,
  `Intt_Effctv_Date` date NOT NULL,
  PRIMARY KEY (`Intt_Code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `interest_rate_master`
--

LOCK TABLES `interest_rate_master` WRITE;
/*!40000 ALTER TABLE `interest_rate_master` DISABLE KEYS */;
INSERT INTO `interest_rate_master` VALUES ('Intt_OD',12.00,'2025-01-01'),('Intt_SB',6.00,'2025-01-01'),('Intt_TD_30D',9.00,'2025-01-01'),('Intt_Zero',0.00,'2025-01-01');
/*!40000 ALTER TABLE `interest_rate_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `intt_accr_tran`
--

DROP TABLE IF EXISTS `intt_accr_tran`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `intt_accr_tran` (
  `Accr_Tran_Id` varchar(20) NOT NULL,
  `Tran_Date` date DEFAULT NULL,
  `Value_Date` date DEFAULT NULL,
  `Dr_Cr_Flag` enum('D','C') DEFAULT NULL,
  `Tran_Status` enum('Entry','Posted','Verified') DEFAULT 'Verified',
  `Account_No` varchar(13) NOT NULL,
  `GL_Account_No` varchar(20) DEFAULT NULL,
  `Tran_Ccy` varchar(3) DEFAULT NULL,
  `FCY_Amt` decimal(20,2) DEFAULT '0.00',
  `Exchange_Rate` decimal(10,4) DEFAULT '1.0000',
  `LCY_Amt` decimal(20,2) DEFAULT '0.00',
  `Narration` varchar(100) DEFAULT NULL,
  `UDF1` varchar(50) DEFAULT NULL,
  `Accrual_Date` date NOT NULL,
  `Interest_Rate` decimal(10,4) NOT NULL,
  `Amount` decimal(20,2) NOT NULL,
  `Status` enum('Pending','Posted','Verified') NOT NULL DEFAULT 'Pending',
  PRIMARY KEY (`Accr_Tran_Id`),
  KEY `Account_No` (`Account_No`),
  KEY `idx_accr_date_id` (`Accrual_Date`,`Accr_Tran_Id`),
  CONSTRAINT `intt_accr_tran_ibfk_1` FOREIGN KEY (`Account_No`) REFERENCES `cust_acct_master` (`Account_No`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `intt_accr_tran`
--

LOCK TABLES `intt_accr_tran` WRITE;
/*!40000 ALTER TABLE `intt_accr_tran` DISABLE KEYS */;
/*!40000 ALTER TABLE `intt_accr_tran` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `of_acct_master`
--

DROP TABLE IF EXISTS `of_acct_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `of_acct_master` (
  `Account_No` varchar(13) NOT NULL,
  `Sub_Product_Id` int NOT NULL,
  `GL_Num` varchar(20) NOT NULL,
  `Acct_Name` varchar(100) NOT NULL,
  `Date_Opening` date NOT NULL,
  `Date_Closure` date DEFAULT NULL,
  `Branch_Code` varchar(10) NOT NULL,
  `Account_Status` enum('Active','Inactive','Closed') NOT NULL,
  `Reconciliation_Required` tinyint(1) NOT NULL,
  PRIMARY KEY (`Account_No`),
  KEY `Sub_Product_Id` (`Sub_Product_Id`),
  CONSTRAINT `of_acct_master_ibfk_1` FOREIGN KEY (`Sub_Product_Id`) REFERENCES `sub_prod_master` (`Sub_Product_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `of_acct_master`
--

LOCK TABLES `of_acct_master` WRITE;
/*!40000 ALTER TABLE `of_acct_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `of_acct_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parameter_table`
--

DROP TABLE IF EXISTS `parameter_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parameter_table` (
  `Parameter_Id` int NOT NULL AUTO_INCREMENT,
  `Parameter_Name` varchar(50) NOT NULL,
  `Parameter_Value` varchar(100) NOT NULL,
  `Parameter_Description` varchar(200) DEFAULT NULL,
  `Last_Updated` timestamp NOT NULL,
  `Updated_By` varchar(20) NOT NULL,
  PRIMARY KEY (`Parameter_Id`),
  UNIQUE KEY `Parameter_Name` (`Parameter_Name`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='System parameters - CBS Compliance: Last_Updated controlled by SystemDateService';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parameter_table`
--

LOCK TABLES `parameter_table` WRITE;
/*!40000 ALTER TABLE `parameter_table` DISABLE KEYS */;
INSERT INTO `parameter_table` VALUES (9,'System_Date','2025-02-11','Current system date for EOD processing','2025-11-03 08:25:38','ADMIN'),(10,'EOD_Admin_User','ADMIN','User ID authorized to run EOD','2025-10-16 08:55:48','SYSTEM'),(11,'Interest_Default_Divisor','36500','Default divisor for interest calculation','2025-10-16 08:55:48','SYSTEM'),(12,'Currency_Default','BDT','Default currency for the system','2025-10-16 08:55:48','SYSTEM'),(13,'Exchange_Rate_Default','1.0','Default exchange rate','2025-10-16 08:55:48','SYSTEM');
/*!40000 ALTER TABLE `parameter_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prod_master`
--

DROP TABLE IF EXISTS `prod_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_master` (
  `Product_Id` int NOT NULL AUTO_INCREMENT,
  `Product_Code` varchar(10) NOT NULL,
  `Product_Name` varchar(50) NOT NULL,
  `Cum_GL_Num` varchar(20) NOT NULL,
  `Maker_Id` varchar(20) NOT NULL,
  `Entry_Date` date NOT NULL,
  `Entry_Time` time NOT NULL,
  `Verifier_Id` varchar(20) DEFAULT NULL,
  `Verification_Date` date DEFAULT NULL,
  `Verification_Time` time DEFAULT NULL,
  `Customer_Product_Flag` tinyint(1) DEFAULT '1',
  `Interest_Bearing_Flag` tinyint(1) DEFAULT '0',
  `Deal_Or_Running` varchar(10) DEFAULT NULL,
  `Currency` varchar(3) DEFAULT 'BDT',
  PRIMARY KEY (`Product_Id`),
  UNIQUE KEY `Product_Code` (`Product_Code`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prod_master`
--

LOCK TABLES `prod_master` WRITE;
/*!40000 ALTER TABLE `prod_master` DISABLE KEYS */;
INSERT INTO `prod_master` VALUES (23,'CA-110','CASA','110102000','FRONTEND_USER','2025-01-08','11:27:31','Asif','2025-01-08','11:27:43',1,1,'Running','BDT'),(24,'OD-101','Test-OD','210201000','FRONTEND_USER','2025-01-08','11:32:20','Asif','2025-01-08','12:43:20',0,0,'','BDT'),(25,'SB-101','Savings Money','110101000','FRONTEND_USER','2025-01-10','13:57:33',NULL,NULL,NULL,1,1,'Running','BDT'),(26,'SB','Savings Bank','110101000','FRONTEND_USER','2025-01-13','18:07:09','Asif','2025-01-17','10:45:47',1,1,'Running','BDT'),(27,'RO','Receivable Others','230201000','FRONTEND_USER','2025-01-14','19:17:08','Asif','2025-01-17','10:45:58',0,0,'','BDT'),(28,'CS','Cash with Teller','220202000','FRONTEND_USER','2025-01-18','10:52:41','Asif','2025-01-18','10:56:03',0,0,'','BDT'),(29,'CA','Currenty Account','110102000','FRONTEND_USER','2025-01-18','10:53:13','Asif','2025-01-18','10:56:08',1,0,'','BDT'),(30,'TD','Term Deposit','110201000','FRONTEND_USER','2025-01-19','11:03:15','Asif','2025-01-19','11:03:29',1,1,'Deal','BDT'),(31,'HSL','House Loan','210101000','FRONTEND_USER','2025-02-07','14:22:39','Asif','2025-02-07','14:22:49',1,0,'','BDT'),(32,'OD','Oberdraft','210201000','FRONTEND_USER','2025-02-10','17:52:04','Asif','2025-02-10','17:52:28',1,1,'Running','BDT');
/*!40000 ALTER TABLE `prod_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sub_prod_master`
--

DROP TABLE IF EXISTS `sub_prod_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sub_prod_master` (
  `Sub_Product_Id` int NOT NULL AUTO_INCREMENT,
  `Product_Id` int NOT NULL,
  `Sub_Product_Code` varchar(10) NOT NULL,
  `Sub_Product_Name` varchar(50) NOT NULL,
  `Intt_Code` varchar(20) DEFAULT NULL,
  `Cum_GL_Num` varchar(10) NOT NULL,
  `Ext_GL_Num` varchar(10) DEFAULT NULL,
  `Sub_Product_Status` enum('Active','Inactive','Deactive') NOT NULL,
  `Maker_Id` varchar(20) NOT NULL,
  `Entry_Date` date NOT NULL,
  `Entry_Time` time NOT NULL,
  `Verifier_Id` varchar(20) DEFAULT NULL,
  `Verification_Date` date DEFAULT NULL,
  `Verification_Time` time DEFAULT NULL,
  `effective_interest_rate` decimal(10,4) DEFAULT NULL,
  `interest_increment` decimal(10,4) DEFAULT NULL,
  `interest_receivable_expenditure_gl_num` varchar(20) DEFAULT NULL,
  `interest_income_payable_gl_num` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`Sub_Product_Id`),
  UNIQUE KEY `Sub_Product_Code` (`Sub_Product_Code`),
  KEY `Product_Id` (`Product_Id`),
  CONSTRAINT `sub_prod_master_ibfk_1` FOREIGN KEY (`Product_Id`) REFERENCES `prod_master` (`Product_Id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sub_prod_master`
--

LOCK TABLES `sub_prod_master` WRITE;
/*!40000 ALTER TABLE `sub_prod_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `sub_prod_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tran_table`
--

DROP TABLE IF EXISTS `tran_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tran_table` (
  `Tran_Id` varchar(20) NOT NULL,
  `Tran_Date` date NOT NULL,
  `Value_Date` date NOT NULL,
  `Dr_Cr_Flag` enum('D','C') NOT NULL,
  `Tran_Status` enum('Entry','Posted','Verified') NOT NULL,
  `Account_No` varchar(20) NOT NULL,
  `Tran_Ccy` varchar(3) NOT NULL,
  `FCY_Amt` decimal(20,2) NOT NULL,
  `Exchange_Rate` decimal(10,4) NOT NULL,
  `LCY_Amt` decimal(20,2) NOT NULL,
  `Debit_Amount` decimal(20,2) DEFAULT NULL,
  `Credit_Amount` decimal(20,2) DEFAULT NULL,
  `Narration` varchar(100) DEFAULT NULL,
  `UDF1` varchar(50) DEFAULT NULL,
  `Pointing_Id` int DEFAULT NULL,
  PRIMARY KEY (`Tran_Id`),
  KEY `Account_No` (`Account_No`),
  KEY `idx_tran_table_date` (`Tran_Date`),
  KEY `idx_tran_table_status` (`Tran_Status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tran_table`
--

LOCK TABLES `tran_table` WRITE;
/*!40000 ALTER TABLE `tran_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `tran_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `txn_hist_acct`
--

DROP TABLE IF EXISTS `txn_hist_acct`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `txn_hist_acct` (
  `Hist_ID` bigint NOT NULL AUTO_INCREMENT COMMENT 'Unique history record identifier',
  `Branch_ID` varchar(10) NOT NULL COMMENT 'Branch identifier',
  `ACC_No` varchar(13) NOT NULL COMMENT 'Account number',
  `TRAN_ID` varchar(20) NOT NULL COMMENT 'Foreign Key to tran_table(Tran_Id)',
  `TRAN_DATE` date NOT NULL COMMENT 'Transaction date',
  `VALUE_DATE` date NOT NULL COMMENT 'Value date',
  `TRAN_SL_NO` int NOT NULL COMMENT 'Serial number for debit/credit leg',
  `NARRATION` varchar(100) DEFAULT NULL COMMENT 'Transaction description',
  `TRAN_TYPE` enum('D','C') NOT NULL COMMENT 'Debit or Credit',
  `TRAN_AMT` decimal(20,2) NOT NULL COMMENT 'Transaction amount',
  `Opening_Balance` decimal(20,2) NOT NULL COMMENT 'Balance before this transaction',
  `BALANCE_AFTER_TRAN` decimal(20,2) NOT NULL COMMENT 'Running balance after transaction',
  `ENTRY_USER_ID` varchar(20) NOT NULL COMMENT 'User who posted transaction',
  `AUTH_USER_ID` varchar(20) DEFAULT NULL COMMENT 'User who verified/authorized transaction',
  `CURRENCY_CODE` varchar(3) DEFAULT 'BDT' COMMENT 'Transaction currency',
  `GL_Num` varchar(9) DEFAULT NULL COMMENT 'GL number from account sub-product',
  `RCRE_DATE` date NOT NULL COMMENT 'Record creation date',
  `RCRE_TIME` time NOT NULL COMMENT 'Record creation time',
  PRIMARY KEY (`Hist_ID`),
  KEY `idx_acc_no` (`ACC_No`),
  KEY `idx_tran_date` (`TRAN_DATE`),
  KEY `idx_acc_tran_date` (`ACC_No`,`TRAN_DATE`),
  KEY `idx_tran_id` (`TRAN_ID`),
  CONSTRAINT `fk_txn_hist_tran_id` FOREIGN KEY (`TRAN_ID`) REFERENCES `tran_table` (`Tran_Id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Transaction history for Statement of Accounts';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `txn_hist_acct`
--

LOCK TABLES `txn_hist_acct` WRITE;
/*!40000 ALTER TABLE `txn_hist_acct` DISABLE KEYS */;
/*!40000 ALTER TABLE `txn_hist_acct` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `txn_hist_acct_backup_20251102`
--

DROP TABLE IF EXISTS `txn_hist_acct_backup_20251102`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `txn_hist_acct_backup_20251102` (
  `Hist_ID` bigint NOT NULL DEFAULT '0' COMMENT 'Unique history record identifier',
  `Branch_ID` varchar(10) NOT NULL COMMENT 'Branch identifier',
  `ACC_No` varchar(13) NOT NULL COMMENT 'Account number',
  `TRAN_ID` varchar(20) NOT NULL COMMENT 'Foreign Key to tran_table(Tran_Id)',
  `TRAN_DATE` date NOT NULL COMMENT 'Transaction date',
  `VALUE_DATE` date NOT NULL COMMENT 'Value date',
  `TRAN_SL_NO` int NOT NULL COMMENT 'Serial number for debit/credit leg',
  `NARRATION` varchar(100) DEFAULT NULL COMMENT 'Transaction description',
  `TRAN_TYPE` enum('D','C') NOT NULL COMMENT 'Debit or Credit',
  `TRAN_AMT` decimal(20,2) NOT NULL COMMENT 'Transaction amount',
  `Opening_Balance` decimal(20,2) NOT NULL COMMENT 'Balance before this transaction',
  `BALANCE_AFTER_TRAN` decimal(20,2) NOT NULL COMMENT 'Running balance after transaction',
  `ENTRY_USER_ID` varchar(20) NOT NULL COMMENT 'User who posted transaction',
  `AUTH_USER_ID` varchar(20) DEFAULT NULL COMMENT 'User who verified/authorized transaction',
  `CURRENCY_CODE` varchar(3) DEFAULT 'BDT' COMMENT 'Transaction currency',
  `GL_Num` varchar(9) DEFAULT NULL COMMENT 'GL number from account sub-product',
  `RCRE_DATE` date NOT NULL COMMENT 'Record creation date',
  `RCRE_TIME` time NOT NULL COMMENT 'Record creation time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `txn_hist_acct_backup_20251102`
--

LOCK TABLES `txn_hist_acct_backup_20251102` WRITE;
/*!40000 ALTER TABLE `txn_hist_acct_backup_20251102` DISABLE KEYS */;
INSERT INTO `txn_hist_acct_backup_20251102` VALUES (1,'DEFAULT','100000001002','T20250108000001700-2','2025-01-08','2025-01-08',1,'Test transaction with office account','C',100.00,-100.00,0.00,'SYSTEM','MIGRATION','BDT',NULL,'2025-11-02','15:40:59'),(2,'001','100000002001','T20250110000003781-2','2025-01-10','2025-10-20',1,'','C',100.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(3,'001','100000002001','T20250110000005266-2','2025-01-10','2025-10-20',1,'','C',100.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(4,'001','100000002001','T20250111000001032-2','2025-01-11','2025-10-22',1,'','C',50.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(5,'001','100000002001','T20250111000003653-1','2025-01-11','2025-10-22',1,'','D',25.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(6,'001','100000002001','T20250112000001375-2','2025-01-12','2025-10-22',1,'','C',250.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(7,'001','100000002001','T20250113000001879-1','2025-01-13','2025-10-23',1,'','D',100.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(8,'001','100000002001','T20250114000001256-2','2025-01-14','2025-10-23',1,'','C',100000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(9,'001','100000002001','T20250116000001319-2','2025-01-16','2025-10-23',1,'','C',100000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(10,'001','100000002001','T20250118000001407-1','2025-01-18','2025-10-25',1,'','D',25000.00,100000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(11,'001','100000002001','T20250121000001902-1','2025-01-21','2025-10-25',1,'','D',73000.00,75000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(12,'001','100000002001','T20250124000001134-3','2025-01-24','2025-10-25',1,'','C',1.00,2000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(13,'001','100000002001','T20250125000001336-1','2025-01-25','2025-10-25',1,'','D',2001.00,2001.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(14,'001','100000002001','T20250126000001519-2','2025-01-26','2025-10-26',1,'','C',200000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(15,'001','100000002001','T20250130000001190-1','2025-01-30','2025-10-26',1,'','D',77776.00,200000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(16,'001','100000002001','T20250201000001832-1','2025-02-01','2025-10-26',1,'','D',22224.00,122224.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(17,'001','100000002001','T20250206000001585-2','2025-02-06','2025-10-27',1,'','C',4000.00,100000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(18,'001','100000002001','T20250212000001741-1','2025-02-12','2025-10-29',1,'','D',4000.00,104000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(19,'001','100000003001','T20250119000001678-2','2025-01-19','2025-10-25',1,'','C',40000.00,-40000.00,0.00,'SYSTEM','MIGRATION','BDT','110201001','2025-11-02','15:40:59'),(20,'001','100000003001','T20250125000005614-1','2025-01-25','2025-10-25',1,'','D',40000.00,40000.00,0.00,'SYSTEM','MIGRATION','BDT','110201001','2025-11-02','15:40:59'),(21,'001','100000003001','T20250131000001381-2','2025-01-31','2025-10-26',1,'','C',10.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110201001','2025-11-02','15:40:59'),(22,'001','100000003002','T20250130000001190-2','2025-01-30','2025-10-26',1,'','C',77776.00,-77776.00,0.00,'SYSTEM','MIGRATION','BDT','110201002','2025-11-02','15:40:59'),(23,'001','100000003002','T20250131000001381-3','2025-01-31','2025-10-26',1,'','C',10.00,77776.00,0.00,'SYSTEM','MIGRATION','BDT','110201002','2025-11-02','15:40:59'),(24,'001','100000003002','T20250207000001061-3','2025-02-07','2025-02-07',1,'','C',500.00,77786.00,0.00,'SYSTEM','MIGRATION','BDT','110201002','2025-11-02','15:40:59'),(25,'001','100000003002','T20250207000004644-1','2025-02-07','2025-10-28',1,'','D',2000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110201002','2025-11-02','15:40:59'),(26,'001','100000003002','T20250213000001137-1','2025-02-13','2025-11-02',1,'','D',12000.00,76286.00,0.00,'SYSTEM','MIGRATION','BDT','110201002','2025-11-02','15:40:59'),(27,'001','100000013001','T20250203000001471-2','2025-02-03','2025-10-26',1,'','C',100000.00,-100000.00,0.00,'SYSTEM','MIGRATION','BDT','110201001','2025-11-02','15:40:59'),(28,'001','100000016001','T20250210000001094-1','2025-02-10','2025-10-28',1,'','D',10000.00,10000.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(29,'001','100000026001','T20250211000001188-1','2025-02-11','2025-10-28',1,'','D',2000000.00,2000000.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(30,'001','100000032001','T20250211000001188-2','2025-02-11','2025-10-28',1,'','C',2000000.00,-2000000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(31,'001','100000032001','T20250212000001741-2','2025-02-12','2025-10-29',1,'','C',4000.00,2000000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(32,'001','100000061001','T20250108000003498-2','2025-01-08','2025-10-19',1,'','C',200.00,-200.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(33,'001','100000061001','T20250109000001243-2','2025-01-09','2025-10-20',1,'','C',200.00,-200.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(34,'001','100000061001','T20250110000001319-2','2025-01-10','2025-10-20',1,'','C',1000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(35,'001','100000061001','T20250110000005266-1','2025-01-10','2025-10-20',1,'','D',100.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(36,'001','100000061001','T20250111000001032-1','2025-01-11','2025-10-22',1,'','D',50.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(37,'001','100000061001','T20250111000003653-2','2025-01-11','2025-10-22',1,'','C',25.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(38,'001','100000061001','T20250112000001375-1','2025-01-12','2025-10-22',1,'','D',250.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(39,'001','100000061001','T20250112000003466-2','2025-01-12','2025-10-23',1,'','C',10000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(40,'001','100000061001','T20250113000001879-2','2025-01-13','2025-10-23',1,'','C',100.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(41,'001','100000061001','T20250119000004982-2','2025-01-19','2025-10-25',1,'','C',12545.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(42,'001','100000061001','T20250121000001902-3','2025-01-21','2025-10-25',1,'','D',12499.00,12545.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(43,'001','100000061001','T20250124000001134-2','2025-01-24','2025-10-25',1,'','C',2050.67,46.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(44,'001','100000061001','T20250125000001336-2','2025-01-25','2025-10-25',1,'','C',48003.33,2096.67,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(45,'001','100000061001','T20250131000001381-4','2025-01-31','2025-10-26',1,'','C',10.00,50100.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(46,'001','100000071001','T20250118000001407-2','2025-01-18','2025-10-25',1,'','C',20000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(47,'001','100000071001','T20250121000001902-2','2025-01-21','2025-10-25',1,'','D',19900.00,20000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(48,'001','100000071001','T20250124000001134-4','2025-01-24','2025-10-25',1,'','C',1.25,100.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(49,'001','100000071001','T20250125000001336-3','2025-01-25','2025-10-25',1,'','D',101.25,101.25,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(50,'001','100000071001','T20250128000001572-2','2025-01-28','2025-10-26',1,'','C',20000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(51,'001','100000071001','T20250131000001381-5','2025-01-31','2025-10-26',1,'','C',10.00,20000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(52,'001','100000071001','T20250207000001061-2','2025-02-07','2025-02-07',1,'','C',500.00,20010.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(53,'001','100000071001','T20250209000003642-1','2025-02-09','2025-10-28',1,'','D',510.00,20510.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(54,'001','100000071001','T20250210000001094-2','2025-02-10','2025-10-28',1,'','C',10000.00,20000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(55,'001','100000071001','T20250213000003103-2','2025-02-13','2025-11-02',1,'','C',10001.00,30000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(56,'001','100000073001','T20250119000001678-3','2025-01-19','2025-10-25',1,'','C',50000.00,-50000.00,0.00,'SYSTEM','MIGRATION','BDT','110201001','2025-11-02','15:40:59'),(57,'001','100000073001','T20250131000001381-6','2025-01-31','2025-10-26',1,'','C',10.00,50000.00,0.00,'SYSTEM','MIGRATION','BDT','110201001','2025-11-02','15:40:59'),(58,'001','100000081001','T20250201000001832-2','2025-02-01','2025-10-26',1,'','C',22224.00,-22224.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(59,'001','100000081001','T20250213000001137-2','2025-02-13','2025-11-02',1,'','C',12000.00,22224.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(60,'001','100000091001','T20250202000001676-2','2025-02-02','2025-10-26',1,'','C',100000.00,-100000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(61,'001','100000091001','T20250207000004644-2','2025-02-07','2025-10-28',1,'','C',2000.00,100000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(62,'001','100000091001','T20250209000001756-2','2025-02-09','2025-10-28',1,'','C',10001.00,102000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(63,'001','100000091001','T20250209000003642-2','2025-02-09','2025-10-28',1,'','C',510.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(64,'001','100000101001','T20250205000001864-2','2025-02-05','2025-10-27',1,'','C',60000.00,-60000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(65,'001','100000101001','T20250213000003103-1','2025-02-13','2025-11-02',1,'','D',10001.00,60000.00,0.00,'SYSTEM','MIGRATION','BDT','110101001','2025-11-02','15:40:59'),(66,'001','200000022001','T20250118000001407-3','2025-01-18','2025-10-25',1,'','C',5000.00,-5000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(67,'001','200000022001','T20250118000004485-2','2025-01-18','2025-10-25',1,'','C',50000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(68,'001','200000022001','T20250119000004982-1','2025-01-19','2025-10-25',1,'','D',12545.00,55000.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(69,'001','200000022001','T20250121000001902-4','2025-01-21','2025-10-25',1,'','C',105399.00,42455.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(70,'001','200000022001','T20250124000001134-5','2025-01-24','2025-10-25',1,'','C',0.75,147854.00,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(71,'001','200000022001','T20250125000001336-4','2025-01-25','2025-10-25',1,'','D',45901.08,147854.75,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(72,'001','200000022001','T20250126000001519-3','2025-01-26','2025-10-26',1,'','C',800000.00,101953.67,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(73,'001','200000022001','T20250128000001572-1','2025-01-28','2025-10-26',1,'','D',20000.00,901953.67,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(74,'001','200000022001','T20250131000001381-7','2025-01-31','2025-10-26',1,'','C',50.00,881953.67,0.00,'SYSTEM','MIGRATION','BDT','110102001','2025-11-02','15:40:59'),(75,'001','921020100101','T20250108000001700-1','2025-01-08','2025-01-08',1,'Test transaction with office account','D',100.00,100.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(76,'001','921020100101','T20250108000003498-1','2025-01-08','2025-10-19',1,'','D',200.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(77,'001','921020100101','T20250109000001243-1','2025-01-09','2025-10-20',1,'','D',200.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(78,'001','921020100101','T20250110000001319-1','2025-01-10','2025-10-20',1,'','D',1000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(79,'001','921020100101','T20250110000003781-1','2025-01-10','2025-10-20',1,'','D',100.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(80,'001','921020100101','T20250112000003466-1','2025-01-12','2025-10-23',1,'','D',10000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(81,'001','921020100101','T20250114000001256-1','2025-01-14','2025-10-23',1,'','D',100000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(82,'001','921020100101','T20250116000001319-1','2025-01-16','2025-10-23',1,'','D',100000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(83,'001','921020100101','T20250118000004485-1','2025-01-18','2025-10-25',1,'','D',50000.00,-100000.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(84,'001','921020100101','T20250119000001678-1','2025-01-19','2025-10-25',1,'','D',90000.00,-150000.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(85,'001','921020100101','T20250124000001134-1','2025-01-24','2025-10-25',1,'','D',2053.67,-240000.00,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(86,'001','921020100101','T20250125000005614-2','2025-01-25','2025-10-25',1,'','C',40000.00,-242053.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(87,'001','921020100101','T20250126000001519-1','2025-01-26','2025-10-26',1,'','D',1000000.00,-202053.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(88,'001','921020100101','T20250131000001381-1','2025-01-31','2025-10-26',1,'','D',100.00,-1202053.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(89,'001','921020100101','T20250202000001676-1','2025-02-02','2025-10-26',1,'','D',100000.00,-1202153.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(90,'001','921020100101','T20250203000001471-1','2025-02-03','2025-10-26',1,'','D',100000.00,-1302153.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(91,'001','921020100101','T20250205000001864-1','2025-02-05','2025-10-27',1,'','D',60000.00,-1402153.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(92,'001','921020100101','T20250206000001585-1','2025-02-06','2025-10-27',1,'','D',4000.00,-1462153.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(93,'001','921020100101','T20250207000001061-1','2025-02-07','2025-02-07',1,'','D',1000.00,-1466153.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(94,'001','921020100101','T20250209000001756-1','2025-02-09','2025-10-28',1,'','D',10001.00,-1467153.67,0.00,'SYSTEM','MIGRATION','BDT','210201001','2025-11-02','15:40:59'),(95,'001','922020200101','T20250212000003470-2','2025-02-12','2025-10-29',1,'','C',4000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','220202001','2025-11-02','15:40:59'),(96,'001','923020100101','T20250212000003470-1','2025-02-12','2025-10-29',1,'','D',4000.00,0.00,0.00,'SYSTEM','MIGRATION','BDT','230201001','2025-11-02','15:40:59');
/*!40000 ALTER TABLE `txn_hist_acct_backup_20251102` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'moneymarketdb'
--

--
-- Dumping routines for database 'moneymarketdb'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-04 10:23:10
