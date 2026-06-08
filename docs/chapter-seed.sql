-- 课程章节种子数据
-- Java 从入门到精通 (24)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(24, '第1章：Java 环境搭建', 'JDK 安装、IDE 配置、Hello World 程序', 'sample-video.mp4', 540, 1),
(24, '第2章：变量与数据类型', '基本数据类型、引用类型、类型转换', 'sample-video.mp4', 600, 2),
(24, '第3章：流程控制', 'if/else、switch、for/while 循环', 'sample-video.mp4', 480, 3),
(24, '第4章：面向对象编程', '类与对象、继承、多态、封装', 'sample-video.mp4', 720, 4),
(24, '第5章：Spring Boot 实战', 'Spring Boot 项目搭建、RESTful API 开发', 'sample-video.mp4', 660, 5);

-- Python 数据分析实战 (25)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(25, '第1章：Python 快速入门', 'Python 语法基础、Jupyter Notebook 使用', 'sample-video.mp4', 480, 1),
(25, '第2章：NumPy 科学计算', '数组操作、矩阵运算、广播机制', 'sample-video.mp4', 600, 2),
(25, '第3章：Pandas 数据处理', 'DataFrame 操作、数据清洗、分组聚合', 'sample-video.mp4', 660, 3),
(25, '第4章：数据可视化', 'Matplotlib/Seaborn 图表绘制实战', 'sample-video.mp4', 540, 4);

-- Web 前端开发进阶 (26)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(26, '第1章：HTML5 语义化', '语义化标签、表单增强、多媒体元素', 'sample-video.mp4', 420, 1),
(26, '第2章：CSS3 高级特性', 'Flexbox/Grid 布局、动画与过渡', 'sample-video.mp4', 540, 2),
(26, '第3章：TypeScript 入门', '类型系统、泛型、接口与装饰器', 'sample-video.mp4', 600, 3),
(26, '第4章：Vue 3 组件开发', '组合式 API、响应式系统、路由与状态管理', 'sample-video.mp4', 720, 4);

-- 机器学习基础 (27)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(27, '第1章：机器学习概述', '监督学习、无监督学习、强化学习概念', 'sample-video.mp4', 480, 1),
(27, '第2章：线性回归与逻辑回归', '损失函数、梯度下降、正则化', 'sample-video.mp4', 660, 2),
(27, '第3章：决策树与随机森林', '信息增益、剪枝、集成学习', 'sample-video.mp4', 600, 3),
(27, '第4章：SVM 与聚类算法', '支持向量机、K-Means、DBSCAN', 'sample-video.mp4', 600, 4);

-- 深度学习与神经网络 (28)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(28, '第1章：神经网络基础', '感知机、激活函数、反向传播', 'sample-video.mp4', 600, 1),
(28, '第2章：CNN 卷积神经网络', '卷积层、池化层、经典网络架构', 'sample-video.mp4', 660, 2),
(28, '第3章：RNN 与 LSTM', '循环网络、长短期记忆、序列建模', 'sample-video.mp4', 600, 3),
(28, '第4章：Transformer 与注意力机制', 'Self-Attention、多头注意力、BERT/GPT', 'sample-video.mp4', 720, 4);

-- 高等数学精讲 (29)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(29, '第1章：函数与极限', '函数概念、极限定义与计算', 'sample-video.mp4', 600, 1),
(29, '第2章：导数与微分', '导数公式、求导法则、高阶导数', 'sample-video.mp4', 660, 2),
(29, '第3章：积分学', '不定积分、定积分、广义积分', 'sample-video.mp4', 700, 3),
(29, '第4章：无穷级数', '数项级数、幂级数、傅里叶级数', 'sample-video.mp4', 600, 4);

-- 线性代数与矩阵理论 (30)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(30, '第1章：向量与空间', '向量运算、线性相关、向量空间', 'sample-video.mp4', 540, 1),
(30, '第2章：矩阵运算', '矩阵乘法、逆矩阵、行列式', 'sample-video.mp4', 600, 2),
(30, '第3章：特征值与特征向量', '特征分解、对角化、SVD 分解', 'sample-video.mp4', 660, 3);

-- 大学英语四级冲刺 (31)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(31, '第1章：四级听力技巧', '短对话、长对话、短文听力解题策略', 'sample-video.mp4', 540, 1),
(31, '第2章：阅读与选词填空', '快速阅读、精读定位、选词逻辑', 'sample-video.mp4', 600, 2),
(31, '第3章：翻译题型突破', '中译英常见句型、文化词汇翻译', 'sample-video.mp4', 480, 3),
(31, '第4章：写作模板精讲', '议论文、书信、图表作文模板', 'sample-video.mp4', 540, 4),
(31, '第5章：真题冲刺训练', '近三年真题精讲与模拟测试', 'sample-video.mp4', 720, 5);

-- 日语入门五十音 (32)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(32, '第1章：平假名入门', 'あ行〜な行 平假名读写', 'sample-video.mp4', 480, 1),
(32, '第2章：平假名进阶', 'は行〜わ行 平假名读写', 'sample-video.mp4', 480, 2),
(32, '第3章：片假名全掌握', '片假名读写与外来语', 'sample-video.mp4', 540, 3),
(32, '第4章：日常会话基础', '问候语、自我介绍、简单对话', 'sample-video.mp4', 600, 4);

-- 考研数学全程班 (33)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(33, '第1章：高等数学核心', '极限、微积分、级数重点回顾', 'sample-video.mp4', 720, 1),
(33, '第2章：线性代数精练', '矩阵、方程组、二次型考点', 'sample-video.mp4', 660, 2),
(33, '第3章：概率论与数理统计', '随机变量、分布函数、参数估计', 'sample-video.mp4', 600, 3),
(33, '第4章：真题精讲', '近五年真题逐题解析', 'sample-video.mp4', 900, 4),
(33, '第5章：模拟冲刺', '全真模拟卷训练与考前押题', 'sample-video.mp4', 840, 5);

-- 考研英语长难句突破 (34)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(34, '第1章：长难句结构分析', '主干识别、修饰成分拆分', 'sample-video.mp4', 540, 1),
(34, '第2章：定语从句破解', '关系代词/副词、限定与非限定', 'sample-video.mp4', 600, 2),
(34, '第3章：状语从句与名词性从句', '时间/条件/让步状语、主/宾/表/同位语从句', 'sample-video.mp4', 600, 3),
(34, '第4章：阅读真题实战', '历年真题长难句逐句精读', 'sample-video.mp4', 660, 4);

-- 学术论文写作指南 (35)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(35, '第1章：选题与文献综述', '如何确定研究方向与检索文献', 'sample-video.mp4', 540, 1),
(35, '第2章：研究方法与数据处理', '实验设计、数据分析工具', 'sample-video.mp4', 600, 2),
(35, '第3章：论文撰写与投稿', '论文结构、写作规范、期刊选择', 'sample-video.mp4', 660, 3);

-- 校园摄影入门 (36)
INSERT INTO course_chapter (course_id, title, description, video_url, duration, sort_order) VALUES
(36, '第1章：相机基础操作', '曝光三要素：光圈、快门、ISO', 'sample-video.mp4', 480, 1),
(36, '第2章：构图法则', '三分法、对称、引导线、留白', 'sample-video.mp4', 540, 2),
(36, '第3章：光线与色彩', '自然光、人工光、白平衡与色调', 'sample-video.mp4', 480, 3),
(36, '第4章：手机摄影与后期', '手机拍摄技巧、VSCO/Lightroom 调色', 'sample-video.mp4', 540, 4);
