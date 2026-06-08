USE campus_assistant;

-- 先清掉重新来
DELETE FROM square_comment;

-- ===== 帖子17: 大一新生选课 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(17, 3, NULL, NULL, '太实用了！大一的时候完全不知道选课还分预选正选补退选三个阶段，第一次选课手忙脚乱的', 15, '2026-05-28 09:00:00');
SET @c1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(17, 5, NULL, NULL, '补充一点：体育课一定要抢羽毛球的李老师，人好考试还松', 28, '2026-05-28 09:30:00');
SET @c2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(17, 6, @c2, 5, '李老师确实好！不过他的课太难抢了，正选阶段30秒就没了', 12, '2026-05-28 10:00:00');
SET @c3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(17, 4, @c2, 5, '抢不到就去上王老师的篮球课也行，虽然严格但能学到东西', 7, '2026-05-28 10:15:00');

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(17, 3, @c3, 6, '30秒也太夸张了吧 学校服务器撑得住吗', 8, '2026-05-28 10:30:00');
SET @c5 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(17, 6, @c5, 3, '选课那天图书馆电子阅览室直接爆满，大家都是去抢课的', 6, '2026-05-28 11:00:00');

-- ===== 帖子18: 考研自习位 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(18, 4, NULL, NULL, '24小时自习室是真的香，考研那半年我基本住里面了', 22, '2026-05-28 08:00:00');
SET @d1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(18, 2, NULL, NULL, '提醒一下大家，占座不要太过分，放本书就走一个小时的真的过分了，管理人员真的会清的', 35, '2026-05-28 08:30:00');
SET @d2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(18, 10, @d2, 2, '同意！见过有人放个水杯占了一整天，水都凉了人还没来', 18, '2026-05-28 09:00:00');

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(18, 5, @d1, 4, '考研季去晚了还有位子吗？一般几点去比较好', 3, '2026-05-28 09:20:00');
SET @d4 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(18, 4, @d4, 5, '建议早上6:30之前到，7点之后基本就没位子了。考试周更夸张，有人5:30就在门口排队', 14, '2026-05-28 10:00:00');

-- ===== 帖子19: 校园卡盗刷 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(19, 2, NULL, NULL, '之前我室友卡丢了没挂失，被刷了200多，心疼死了。大家丢卡一定要第一时间挂失！', 19, '2026-05-28 10:00:00');
SET @e1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(19, 6, NULL, NULL, '校园助手APP挂失超快的，10秒搞定。别跑去服务中心了，线上挂失就行', 25, '2026-05-28 10:30:00');
SET @e2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(19, 10, @e2, 6, '那如果APP挂失之后找到了原来的卡还能解挂吗', 5, '2026-05-28 11:00:00');
SET @e3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(19, 6, @e3, 10, '可以的，APP上挂失后可以解挂，但如果已经补办了新卡旧卡就废了', 11, '2026-05-28 11:30:00');

-- ===== 帖子20: 学校附近好吃的 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(20, 5, NULL, NULL, '蜀九香的毛肚和鹅肠太绝了，每周都想吃', 16, '2026-05-25 18:00:00');
SET @f1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(20, 3, NULL, NULL, '还有南门出去右转的柳州螺蛳粉，12块一碗，加个炸蛋才15，绝了', 21, '2026-05-25 18:30:00');
SET @f2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(20, 4, @f2, 3, '那家螺蛳粉排队太长了，每次经过都是一堆人', 8, '2026-05-25 19:00:00');
SET @f3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(20, 3, @f3, 4, '错峰去啊，下午两三点基本不用排队', 6, '2026-05-25 19:30:00');

-- ===== 帖子22: 游泳馆 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(22, 6, NULL, NULL, '游泳馆的水质怎么样？干净吗？', 7, '2026-05-22 10:00:00');
SET @g1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(22, 2, NULL, NULL, '水质很干净，每天都会检测。救生员也很负责，我游了大半年了', 20, '2026-05-22 10:30:00');
SET @g2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(22, 10, @g1, 6, '谢谢！那我放心去了，周末人多不多？', 3, '2026-05-22 11:00:00');
SET @g3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(22, 2, @g3, 10, '周末下午人多，建议上午去或者晚上七点以后，人会少一些', 9, '2026-05-22 12:00:00');

-- ===== 帖子25: 补考重修 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(25, 10, NULL, NULL, '大物挂了40分，刚好卡在补考线上面，不知道该庆幸还是该难过', 31, '2026-05-26 14:00:00');
SET @h1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(25, 3, NULL, NULL, '补考最高60分这个太坑了，我一个同学补考卷面考了85，成绩出来还是60', 18, '2026-05-26 14:30:00');
SET @h2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(25, 4, @h2, 3, '所以要刷GPA的话直接重修更划算，虽然花点钱但至少分数是自己的', 12, '2026-05-26 15:00:00');
SET @h3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(25, 5, @h3, 4, '重修每学分100元，一门3学分的课就要300，也不是小钱啊', 7, '2026-05-26 15:30:00');
SET @h4 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(25, 4, @h4, 5, '比起GPA对保研的影响，300块算便宜了。保研差0.1个绩点可能就进不了面试', 15, '2026-05-26 16:00:00');

-- ===== 帖子27: 小锅煮面 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(27, 4, NULL, NULL, '宿管阿姨是真的很严格，我有次用电热水壶烧水被没收了，还通报了', 26, '2026-05-21 10:00:00');
SET @i1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(27, 2, NULL, NULL, '其实每层都有饮水机免费提供开水，泡面足够了。想煮东西就去北区公共厨房', 14, '2026-05-21 11:00:00');
SET @i2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(27, 6, @i1, 4, '北区公共厨房真的可以随便用吗？不住北区的也能去？', 5, '2026-05-21 12:00:00');
SET @i3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(27, 4, @i3, 6, '可以，全校学生都能用。我不住北区也经常去煮饺子', 9, '2026-05-21 13:00:00');

-- ===== 帖子28: 心理咨询 =====
INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(28, 5, NULL, NULL, '去过一次，老师真的很温柔很专业。有困扰的同学不要犹豫，咨询完感觉整个人都轻松了很多', 42, '2026-05-24 09:00:00');
SET @j1 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(28, 10, NULL, NULL, '请问是完全免费的吗？需要带什么证件吗', 8, '2026-05-24 09:30:00');
SET @j2 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(28, 5, @j2, 10, '完全免费！带上校园卡就行。第一次去会先做个简单的登记和评估', 16, '2026-05-24 10:00:00');
SET @j3 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(28, 3, @j1, 5, '看了你的评论我也想去试试了，最近压力真的好大', 11, '2026-05-24 11:00:00');
SET @j4 = LAST_INSERT_ID();

INSERT INTO square_comment (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at) VALUES
(28, 5, @j4, 3, '去吧，去了就知道了，比自己一个人扛着好太多了', 13, '2026-05-24 12:00:00');
