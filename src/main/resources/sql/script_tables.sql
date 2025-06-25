-- 创建剧本相关表结构

-- 用户表 (如果不存在)
CREATE TABLE `users` (
    `Id` int NOT NULL AUTO_INCREMENT,
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `password_hash` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

-- 剧本表
CREATE TABLE `script` (
                          `id` int NOT NULL AUTO_INCREMENT,
                          `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
                          `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
                          `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
                          `last_updated` datetime(6) NOT NULL,
                          `user_id` int NOT NULL,
                          `Stage` int NOT NULL,
                          PRIMARY KEY (`id`),
                          KEY `IX_script_user_id` (`user_id`),
                          CONSTRAINT `FK_script_users_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`Id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

-- 剧本历史表
CREATE TABLE `script_history` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
                                  `response` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
                                  `created_at` datetime(6) NOT NULL,
                                  `script_id` int NOT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `IX_script_history_script_id` (`script_id`),
                                  CONSTRAINT `FK_script_history_script_script_id` FOREIGN KEY (`script_id`) REFERENCES `script` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

-- 剧本分析表  
CREATE TABLE `script_analysis` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `analysis_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
                                   `analyzed_at` datetime(6) NOT NULL,
                                   `script_id` int NOT NULL,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `IX_script_analysis_script_id` (`script_id`),
                                   CONSTRAINT `FK_script_analysis_script_script_id` FOREIGN KEY (`script_id`) REFERENCES `script` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

-- 可视化元素表
CREATE TABLE `visual_element` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `type` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                  `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
                                  `image_url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
                                  `image_generated_at` datetime(6) DEFAULT NULL,
                                  `script_id` int NOT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `IX_visual_element_script_id` (`script_id`),
                                  CONSTRAINT `FK_visual_element_script_script_id` FOREIGN KEY (`script_id`) REFERENCES `script` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=152 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
