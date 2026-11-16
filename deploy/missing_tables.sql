-- MySQL dump 10.13  Distrib 8.0.31, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: nongyeplus
-- ------------------------------------------------------
-- Server version	8.0.31

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
-- Table structure for table `consumer_account`
--

DROP TABLE IF EXISTS `consumer_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consumer_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nickname` varchar(50) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `open_id` varchar(64) DEFAULT NULL,
  `trust_level` varchar(20) DEFAULT 'FOLLOWER',
  `invite_code` varchar(32) DEFAULT NULL,
  `inviter_type` varchar(20) DEFAULT NULL,
  `inviter_id` bigint DEFAULT NULL,
  `profile_json` varchar(1000) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `last_active_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consumer_phone` (`phone`),
  KEY `idx_consumer_inviter` (`inviter_type`,`inviter_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信任者账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumer_account`
--

/*!40000 ALTER TABLE `consumer_account` DISABLE KEYS */;
INSERT INTO `consumer_account` VALUES (1,'王姐','13800138001',NULL,'FOLLOWER','admin','admin',1,NULL,1,'2026-11-10 16:54:29','2026-11-09 18:15:08'),(3,'李叔','13800138002',NULL,'FOLLOWER','admin','admin',1,NULL,1,'2026-11-10 09:37:27','2026-11-10 09:37:27'),(4,'小陈','13800138003',NULL,'FOLLOWER','admin','admin',1,NULL,1,'2026-11-10 09:37:27','2026-11-10 09:37:27'),(5,'华大姐','15615614564',NULL,'FOLLOWER','admin','admin',1,NULL,1,'2026-11-10 16:54:36','2026-11-10 14:20:20'),(8,'小李','12145645655',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-10 17:01:15','2026-11-10 16:32:11'),(9,'大王','12312312314',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-10 17:20:21','2026-11-10 17:04:39'),(10,'妲己','145612314',NULL,'FOLLOWER','admin','admin',1,NULL,1,'2026-11-10 17:10:46','2026-11-10 17:10:46'),(11,'老二','11111111111',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-14 16:50:58','2026-11-10 17:34:43'),(12,'小二','22222222222',NULL,'FOLLOWER','admin','admin',1,NULL,1,'2026-11-10 18:12:34','2026-11-10 18:12:34'),(13,'小a','14714714711',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-14 16:16:47','2026-11-14 16:16:47'),(15,'wq','33333333333',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-14 16:25:57','2026-11-14 16:25:57'),(16,'ggf','66666666666',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-14 16:52:04','2026-11-14 16:52:04'),(17,'kj','88888888888',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-14 16:57:29','2026-11-14 16:57:29'),(18,'az','99999999999',NULL,'FOLLOWER',NULL,NULL,NULL,NULL,1,'2026-11-14 17:43:12','2026-11-14 17:43:12');
/*!40000 ALTER TABLE `consumer_account` ENABLE KEYS */;

--
-- Table structure for table `consumer_event`
--

DROP TABLE IF EXISTS `consumer_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consumer_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumer_id` bigint DEFAULT NULL,
  `event_type` varchar(30) DEFAULT NULL,
  `batch_id` bigint DEFAULT NULL,
  `payload` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_consumer` (`consumer_id`,`create_time`),
  KEY `idx_event_type` (`event_type`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消费者行为事件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumer_event`
--

/*!40000 ALTER TABLE `consumer_event` DISABLE KEYS */;
INSERT INTO `consumer_event` VALUES (1,1,'REGISTER',NULL,'registered-new','2026-11-09 18:15:08'),(3,5,'REGISTER',NULL,'registered-new','2026-11-10 14:20:20'),(4,1,'LOGIN',NULL,NULL,'2026-11-10 14:21:50'),(7,8,'REGISTER',NULL,'registered-new','2026-11-10 16:32:11'),(8,8,'FOLLOW',8,'targetType=FARMER','2026-11-10 16:33:02'),(9,8,'SUBSCRIBE',4,'crop=北瓜','2026-11-10 16:34:10'),(10,8,'SUBSCRIBE',4,'crop=北瓜','2026-11-10 16:34:30'),(11,8,'SUBSCRIBE',4,'crop=北瓜','2026-11-10 16:35:39'),(12,8,'FOLLOW',4,'targetType=BATCH','2026-11-10 16:41:00'),(13,1,'LOGIN',NULL,NULL,'2026-11-10 16:54:30'),(14,5,'LOGIN',NULL,NULL,'2026-11-10 16:54:36'),(15,8,'LOGIN',NULL,NULL,'2026-11-10 16:54:48'),(16,8,'LOGIN',NULL,NULL,'2026-11-10 16:55:27'),(17,8,'LOGIN',NULL,NULL,'2026-11-10 16:55:47'),(18,8,'LOGIN',NULL,NULL,'2026-11-10 17:00:56'),(19,8,'LOGIN',NULL,NULL,'2026-11-10 17:01:15'),(20,9,'REGISTER',NULL,'registered-new','2026-11-10 17:04:39'),(21,9,'FOLLOW',8,'targetType=FARMER','2026-11-10 17:05:21'),(22,9,'SUBSCRIBE',4,'crop=北瓜','2026-11-10 17:05:29'),(23,9,'FOLLOW',4,'targetType=BATCH','2026-11-10 17:05:30'),(24,10,'REGISTER',NULL,'registered-new','2026-11-10 17:10:46'),(25,9,'REGISTER',NULL,'registered-existing','2026-11-10 17:13:18'),(26,9,'LOGIN',NULL,NULL,'2026-11-10 17:19:34'),(27,9,'LOGIN',NULL,NULL,'2026-11-10 17:20:22'),(28,11,'REGISTER',NULL,'registered-new','2026-11-10 17:34:43'),(29,11,'SUBSCRIBE',4,'crop=北瓜','2026-11-10 17:35:23'),(30,11,'FOLLOW',8,'targetType=FARMER','2026-11-10 17:35:31'),(31,11,'FOLLOW',4,'targetType=BATCH','2026-11-10 17:36:12'),(32,12,'REGISTER',NULL,'registered-new','2026-11-10 18:12:34'),(33,11,'REGISTER',NULL,'registered-existing','2026-11-14 15:55:53'),(34,13,'REGISTER',NULL,'registered-new','2026-11-14 16:16:47'),(36,15,'REGISTER',NULL,'registered-new','2026-11-14 16:25:57'),(37,15,'FOLLOW',2,'targetType=FARMER','2026-11-14 16:26:06'),(38,15,'SUBSCRIBE',5,'crop=黄瓜','2026-11-14 16:26:09'),(39,15,'FOLLOW',5,'targetType=BATCH','2026-11-14 16:26:11'),(40,11,'REGISTER',NULL,'registered-existing','2026-11-14 16:50:58'),(41,11,'FOLLOW',2,'targetType=FARMER','2026-11-14 16:51:03'),(42,11,'SUBSCRIBE',5,'crop=黄瓜','2026-11-14 16:51:05'),(43,11,'FOLLOW',5,'targetType=BATCH','2026-11-14 16:51:05'),(44,16,'REGISTER',NULL,'registered-new','2026-11-14 16:52:04'),(45,16,'SUBSCRIBE',5,'crop=黄瓜','2026-11-14 16:52:07'),(46,16,'FOLLOW',5,'targetType=BATCH','2026-11-14 16:52:08'),(47,17,'REGISTER',NULL,'registered-new','2026-11-14 16:57:29'),(48,17,'SUBSCRIBE',1,'crop=西红柿','2026-11-14 16:57:33'),(49,17,'FOLLOW',1,'targetType=BATCH','2026-11-14 16:57:36'),(50,18,'REGISTER',NULL,'registered-new','2026-11-14 17:43:12'),(51,18,'SUBSCRIBE',1,'crop=西红柿','2026-11-14 17:43:20'),(52,18,'FOLLOW',1,'targetType=BATCH','2026-11-14 17:43:23');
/*!40000 ALTER TABLE `consumer_event` ENABLE KEYS */;

--
-- Table structure for table `consumer_follow`
--

DROP TABLE IF EXISTS `consumer_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consumer_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumer_id` bigint NOT NULL,
  `target_type` varchar(20) NOT NULL,
  `target_id` bigint NOT NULL,
  `notify_enabled` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`consumer_id`,`target_type`,`target_id`),
  KEY `idx_follow_target` (`target_type`,`target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumer_follow`
--

/*!40000 ALTER TABLE `consumer_follow` DISABLE KEYS */;
INSERT INTO `consumer_follow` VALUES (1,1,'FARMER',2,1,'2026-11-10 09:37:27'),(2,3,'BATCH',1,1,'2026-11-10 09:37:27'),(3,4,'FARMER',2,1,'2026-11-10 09:37:27'),(4,4,'BATCH',1,1,'2026-11-10 09:37:27'),(5,8,'FARMER',8,1,'2026-11-10 16:33:02'),(6,8,'BATCH',4,0,'2026-11-10 16:41:00'),(7,9,'FARMER',8,1,'2026-11-10 17:05:21'),(8,9,'BATCH',4,1,'2026-11-10 17:05:30'),(9,11,'FARMER',8,1,'2026-11-10 17:35:31'),(10,11,'BATCH',4,1,'2026-11-10 17:36:12'),(11,15,'FARMER',2,1,'2026-11-14 16:26:06'),(12,15,'BATCH',5,1,'2026-11-14 16:26:11'),(13,11,'FARMER',2,1,'2026-11-14 16:51:03'),(14,11,'BATCH',5,1,'2026-11-14 16:51:05'),(15,16,'BATCH',5,1,'2026-11-14 16:52:08'),(16,17,'BATCH',1,1,'2026-11-14 16:57:36'),(17,18,'BATCH',1,1,'2026-11-14 17:43:23');
/*!40000 ALTER TABLE `consumer_follow` ENABLE KEYS */;

--
-- Table structure for table `consumer_subscription`
--

DROP TABLE IF EXISTS `consumer_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consumer_subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumer_id` bigint NOT NULL,
  `batch_id` bigint DEFAULT NULL,
  `crop_name` varchar(50) NOT NULL,
  `qty` decimal(10,2) DEFAULT NULL,
  `unit` varchar(10) DEFAULT '斤',
  `cycle_days` int DEFAULT '7',
  `next_date` date DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sub_consumer` (`consumer_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='常购订阅表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumer_subscription`
--

/*!40000 ALTER TABLE `consumer_subscription` DISABLE KEYS */;
INSERT INTO `consumer_subscription` VALUES (1,1,1,'西红柿',5.00,'斤',7,'2026-11-19','ACTIVE','2026-11-10 09:37:27'),(2,1,5,'黄瓜',3.00,'斤',7,'2026-11-14','ACTIVE','2026-11-10 09:37:27'),(3,3,1,'西红柿',2.00,'斤',7,'2026-11-09','ACTIVE','2026-11-10 09:37:27'),(4,4,1,'西红柿',3.00,'斤',7,'2026-11-15','ACTIVE','2026-11-10 09:37:27'),(5,8,4,'北瓜',5.00,'斤',7,'2026-11-17','ACTIVE','2026-11-10 16:34:10'),(6,8,4,'北瓜',5.00,'斤',7,'2026-11-17','ACTIVE','2026-11-10 16:34:30'),(7,8,4,'北瓜',5.00,'斤',7,'2026-11-17','ACTIVE','2026-11-10 16:35:39'),(8,9,4,'北瓜',5.00,'斤',7,'2026-11-17','ACTIVE','2026-11-10 17:05:29'),(9,11,4,'北瓜',5.00,'斤',7,'2026-11-17','ACTIVE','2026-11-10 17:35:23'),(10,15,5,'黄瓜',5.00,'斤',7,'2026-11-21','ACTIVE','2026-11-14 16:26:09'),(11,11,5,'黄瓜',5.00,'斤',7,'2026-11-21','ACTIVE','2026-11-14 16:51:05'),(12,16,5,'黄瓜',5.00,'斤',7,'2026-11-21','ACTIVE','2026-11-14 16:52:07'),(13,17,1,'西红柿',5.00,'斤',7,'2026-11-21','ACTIVE','2026-11-14 16:57:33'),(14,18,1,'西红柿',5.00,'斤',7,'2026-11-21','ACTIVE','2026-11-14 17:43:20');
/*!40000 ALTER TABLE `consumer_subscription` ENABLE KEYS */;

--
-- Table structure for table `fulfillment_record`
--

DROP TABLE IF EXISTS `fulfillment_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fulfillment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint DEFAULT NULL,
  `dealer_id` bigint DEFAULT NULL,
  `consumer_id` bigint DEFAULT NULL,
  `ship_time` datetime DEFAULT NULL,
  `arrive_time` datetime DEFAULT NULL,
  `quality_flag` varchar(20) DEFAULT 'OK',
  `aftersale_hours` decimal(6,2) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fulfill_batch` (`batch_id`),
  KEY `idx_fulfill_consumer` (`consumer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履约记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fulfillment_record`
--

/*!40000 ALTER TABLE `fulfillment_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `fulfillment_record` ENABLE KEYS */;

--
-- Table structure for table `trust_promise`
--

DROP TABLE IF EXISTS `trust_promise`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trust_promise` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `farmer_id` bigint DEFAULT NULL,
  `promise_type` varchar(30) DEFAULT NULL,
  `promise_value` varchar(100) DEFAULT NULL,
  `evidence_path` varchar(200) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_promise_batch` (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批次信任承诺表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trust_promise`
--

/*!40000 ALTER TABLE `trust_promise` DISABLE KEYS */;
INSERT INTO `trust_promise` VALUES (1,1,2,'FRESH','24h','采收当日 9:00 采摘，同城冷链当日入库','ACTIVE','2026-11-10 09:28:49'),(2,1,2,'HARVEST_DATE','2026-06-15','按约定采收日前后 3 天内采收','ACTIVE','2026-11-10 09:28:49'),(3,1,2,'QUALITY','无农残、不打蜡，已通过第三方检测','检测报告编号见检测报告页','ACTIVE','2026-11-10 09:28:49'),(5,4,8,'FRESH','24h','0','ACTIVE','2026-11-10 16:47:40'),(6,4,8,'FRESH','24','0','ACTIVE','2026-11-10 17:36:55'),(7,1,2,'FRESH','12','123','ACTIVE','2026-11-14 17:07:51'),(8,5,2,'FRESH','24h','','ACTIVE','2026-11-14 17:46:02');
/*!40000 ALTER TABLE `trust_promise` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-16 16:46:46
