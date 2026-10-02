-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: userdetail
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bills`
--

DROP TABLE IF EXISTS `bills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bills` (
  `transotionno` int NOT NULL AUTO_INCREMENT,
  `username` varchar(45) NOT NULL,
  `billtype` varchar(45) NOT NULL,
  `billid` int NOT NULL,
  `billamount` varchar(45) NOT NULL,
  `whopaid` varchar(45) NOT NULL,
  `accountno` varchar(45) NOT NULL,
  `payeddate` datetime NOT NULL,
  `duedate` varchar(45) NOT NULL,
  PRIMARY KEY (`transotionno`),
  UNIQUE KEY `transotionno_UNIQUE` (`transotionno`),
  UNIQUE KEY `billid_UNIQUE` (`billid`)
) ENGINE=InnoDB AUTO_INCREMENT=674967 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bossinfo`
--

DROP TABLE IF EXISTS `bossinfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bossinfo` (
  `username` int NOT NULL AUTO_INCREMENT,
  `password` varchar(45) NOT NULL,
  `fullname` varchar(45) NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=409737 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `charge`
--

DROP TABLE IF EXISTS `charge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `charge` (
  `transotionno` int NOT NULL AUTO_INCREMENT,
  `username` varchar(10) NOT NULL,
  `accountno` varchar(45) NOT NULL,
  `telephoneno` varchar(45) NOT NULL,
  `amount` varchar(45) NOT NULL,
  `time` datetime NOT NULL,
  PRIMARY KEY (`transotionno`),
  UNIQUE KEY `transotionno_UNIQUE` (`transotionno`)
) ENGINE=InnoDB AUTO_INCREMENT=995443 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `check`
--

DROP TABLE IF EXISTS `check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `check` (
  `checknumber` int NOT NULL AUTO_INCREMENT,
  `issuername` varchar(45) NOT NULL,
  `checkamount` varchar(45) NOT NULL,
  `recipientdate` varchar(45) NOT NULL,
  `date` datetime NOT NULL,
  `duedate` datetime NOT NULL,
  `status` varchar(45) NOT NULL DEFAULT 'Pending',
  PRIMARY KEY (`checknumber`)
) ENGINE=InnoDB AUTO_INCREMENT=813281 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `deposit`
--

DROP TABLE IF EXISTS `deposit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deposit` (
  `number` int NOT NULL AUTO_INCREMENT,
  `username` int NOT NULL,
  `type` varchar(45) NOT NULL,
  `amount` varchar(45) NOT NULL,
  `duration` varchar(45) NOT NULL,
  `interest` varchar(45) DEFAULT NULL,
  `payment` varchar(45) DEFAULT NULL,
  `currency` varchar(45) NOT NULL,
  `date` varchar(20) NOT NULL,
  PRIMARY KEY (`number`),
  UNIQUE KEY `number_UNIQUE` (`number`)
) ENGINE=InnoDB AUTO_INCREMENT=99560 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loan`
--

DROP TABLE IF EXISTS `loan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan` (
  `username` int NOT NULL,
  `loanamount` varchar(45) NOT NULL,
  `term` varchar(45) NOT NULL,
  `intrest` varchar(45) NOT NULL,
  `date` varchar(45) NOT NULL,
  `id` blob NOT NULL,
  `employment` blob NOT NULL,
  `collateral` blob NOT NULL,
  `deed` blob NOT NULL,
  `status` varchar(45) NOT NULL DEFAULT 'pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transitions`
--

DROP TABLE IF EXISTS `transitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transitions` (
  `transitiontype` varchar(45) NOT NULL,
  `transitionid` int NOT NULL AUTO_INCREMENT,
  `sourcefullname` varchar(45) NOT NULL,
  `sourcecard` varchar(45) DEFAULT NULL,
  `sourceaccount` varchar(45) DEFAULT NULL,
  `sourceiban` varchar(45) DEFAULT NULL,
  `moneyamount` varchar(45) NOT NULL,
  `destinationfullname` varchar(45) NOT NULL,
  `destinationcard` varchar(45) DEFAULT NULL,
  `destinationaccount` varchar(45) DEFAULT NULL,
  `destinationiban` varchar(45) DEFAULT NULL,
  `destinationbankname` varchar(45) NOT NULL,
  `time` datetime NOT NULL,
  PRIMARY KEY (`transitionid`),
  UNIQUE KEY `transitionid_UNIQUE` (`transitionid`)
) ENGINE=InnoDB AUTO_INCREMENT=986248 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userinfo`
--

DROP TABLE IF EXISTS `userinfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userinfo` (
  `username` int NOT NULL,
  `password` varchar(45) NOT NULL,
  `fullname` varchar(45) NOT NULL,
  `id` varchar(10) NOT NULL,
  `birth` varchar(15) NOT NULL,
  `gender` varchar(6) NOT NULL,
  `marital` varchar(10) NOT NULL,
  `national` varchar(45) NOT NULL,
  `address` varchar(45) NOT NULL,
  `postalcode` varchar(15) NOT NULL,
  `phonenumber` varchar(11) NOT NULL,
  `email` varchar(45) NOT NULL,
  `occupation` varchar(40) NOT NULL,
  `employeraddress` varchar(45) NOT NULL,
  `annualincome` varchar(40) NOT NULL,
  `creationdate` varchar(45) NOT NULL,
  `accounttype` varchar(10) NOT NULL,
  `accountnumber` varchar(45) DEFAULT NULL,
  `cardnumber` varchar(16) NOT NULL,
  `cvv` varchar(3) NOT NULL,
  `iban` varchar(30) NOT NULL,
  `expiredate` varchar(45) NOT NULL,
  `deposit` varchar(40) NOT NULL,
  `idphoto` blob NOT NULL,
  `statement` varchar(20) NOT NULL,
  UNIQUE KEY `account number_UNIQUE` (`cardnumber`),
  UNIQUE KEY `cvv_UNIQUE` (`cvv`),
  UNIQUE KEY `iban_UNIQUE` (`iban`),
  UNIQUE KEY `userinfocol_UNIQUE` (`accountnumber`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workerinfo`
--

DROP TABLE IF EXISTS `workerinfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workerinfo` (
  `username` varchar(10) NOT NULL,
  `password` varchar(45) NOT NULL,
  `fullname` varchar(45) NOT NULL,
  PRIMARY KEY (`username`),
  UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-28 10:03:49
