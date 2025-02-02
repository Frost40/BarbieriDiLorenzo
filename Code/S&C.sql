/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS  */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION  */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE  */;
/*!40103 SET TIME_ZONE='+00:00'  */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE  */;
/*!40101 SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO'  */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS  */;
/*!40014 SET UNIQUE_CHECKS=0  */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS  */;
/*!40014 SET FOREIGN_KEY_CHECKS=0  */;
/*!40101 SET @OLD_SQL_NOTES=@@SQL_NOTES  */;
/*!40101 SET SQL_NOTES=0  */;

--
-- Database: `your_database_name`
--
CREATE DATABASE IF NOT EXISTS `your_database_name`;
USE `your_database_name`;

-------------------------------------------------------------------------
-- Table structure for table `company`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar (100) NOT NULL,
  `password`varchar(10) NOT NULL,
  `name` varchar(45) NOT NULL,
  `mainOffice` varchar(45) NOT NULL,
  `offices` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `Email_UNIQUE` (`Email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `complaint`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `complaint`;
CREATE TABLE `complaint` (
  `id` int NOT NULL AUTO_INCREMENT,
  `text` varchar(255) NOT NULL,
  `closedByStudent` tinyint NOT NULL,
  `closedByCompany` tinyint NOT NULL,
  `studentHasInternshipId` int NOT NULL,
  `readByStudent` tinyint NOT NULL,
  `readByInternship` tinyint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `cv`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `cv`;
CREATE TABLE `cv` (
  `id` int NOT NULL AUTO_INCREMENT,
  `file` mediumblob NOT NULL,
  `experiences` varchar(255),
  `programmingSkills` varchar(255),
  `otherSkills` varchar(255),
  `student_id` int NOT NULL,
  `fileType` varchar(10) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `feedback`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` int NOT NULL AUTO_INCREMENT,
  `text` varchar(45) DEFAULT NULL,
  `read` tinyint NOT NULL,
  `rating` int NOT NULL,
  `studentHasInternshipId` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `internship`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `internship`;
CREATE TABLE `internship` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `description` varchar(500) NOT NULL,
  `rating` double DEFAULT NULL,
  `companyId` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `interviewschedule`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `interviewschedule`;
CREATE TABLE `interviewschedule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL,
  `evaluation` tinyint DEFAULT NULL,
  `studentHasInternshipId` int NOT NULL,
  `hasBeenRejected` tinyint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `message`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` int NOT NULL AUTO_INCREMENT,
  `message` varchar(255) NOT NULL,
  `readByStudent` tinyint NOT NULL,
  `senderId` int NOT NULL,
  `receiverId` int NOT NULL,
  `fromInternship` tinyint NOT NULL,
  `readByInternship` tinyint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `questionnaire`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `questionnaire`;
CREATE TABLE `questionnaire` (
  `id` int NOT NULL AUTO_INCREMENT,
  `file` mediumblob NOT NULL,
  `evaluation` tinyint DEFAULT NULL,
  `studentHasInternshipId` int NOT NULL,
  `fileType` varchar(10) NOT NULL,
  `completed` tinyint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `student`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(100) NOT NULL,
  `password` varchar(10) NOT NULL,
  `name` varchar(45) NOT NULL,
  `surname` varchar(45) NOT NULL,
  `nationality` varchar(45) NOT NULL,
  `cityOfBirth` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `Email_UNIQUE_S` (`Email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Table structure for table `studenthasinternship`
-------------------------------------------------------------------------
DROP TABLE IF EXISTS `studenthasinternship`;
CREATE TABLE `studenthasinternship` (
  `id` int NOT NULL AUTO_INCREMENT,
  `startDate` datetime DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `companyAccepted` tinyint NOT NULL,
  `applicationStatus` varchar(45) DEFAULT NULL,
  `studentId` int NOT NULL,
  `internshipId` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-------------------------------------------------------------------------
-- Finalizing dump
-------------------------------------------------------------------------

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE  */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE  */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS  */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS  */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT  */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS  */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION  */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES  */;

-- Dump completed successfully
