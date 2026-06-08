SET NAMES utf8mb4;
TRUNCATE TABLE campus_assistant.knowledge_base;
INSERT INTO campus_assistant.knowledge_base (name, description, icon, sort_order) VALUES
('规章制度', '学校各类规章制度、管理办法', 'rule', 1),
('办事流程', '校园办事指南、流程说明', 'guide', 2),
('课程信息', '课程大纲、选课说明、学分要求', 'course', 3),
('校园生活', '食宿、交通、社团等校园生活信息', 'life', 4);
SELECT name, HEX(name) FROM campus_assistant.knowledge_base LIMIT 1;