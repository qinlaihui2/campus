#!/bin/bash
# 自动下载免费占位图 → 上传到 MinIO → 更新演示数据封面
# 在服务器上执行：bash docs/上传演示图片.sh

API="http://localhost:8080"
TOKEN=$(curl -s -X POST "$API/api/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

upload_img() {
  local url="$1"
  local fname="/tmp/$(date +%s)_$RANDOM.jpg"
  curl -sL "$url" -o "$fname" 2>/dev/null
  if [ -s "$fname" ]; then
    local result=$(curl -s -X POST "$API/api/upload/image" -H "Authorization: Bearer $TOKEN" -F "file=@$fname" 2>/dev/null)
    echo "$result" | grep -o '"data":"[^"]*"' | cut -d'"' -f4
    rm -f "$fname"
  else
    echo ""
  fi
}

echo "=== 上传二手市场图片 ==="
# 键盘
IMG1=$(upload_img "https://picsum.photos/seed/keyboard/400/300")
[ -n "$IMG1" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG1\"]' WHERE title='机械键盘 IKBC C87 青轴';" campus_assistant && echo "键盘 OK"

# 教材
IMG2=$(upload_img "https://picsum.photos/seed/book/400/300")
[ -n "$IMG2" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG2\"]' WHERE title='高等数学第七版（上下册）';" campus_assistant && echo "教材 OK"

# iPad
IMG3=$(upload_img "https://picsum.photos/seed/ipad/400/300")
[ -n "$IMG3" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG3\"]' WHERE title='iPad 第九代 64G WIFI版';" campus_assistant && echo "iPad OK"

# 真题
IMG4=$(upload_img "https://picsum.photos/seed/exam/400/300")
[ -n "$IMG4" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG4\"]' WHERE title='考研英语真题 2025版（全新未拆）';" campus_assistant && echo "真题 OK"

# 电煮锅
IMG5=$(upload_img "https://picsum.photos/seed/pot/400/300")
[ -n "$IMG5" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG5\"]' WHERE title='小电煮锅 1.5L';" campus_assistant && echo "锅 OK"

# 鼠标
IMG6=$(upload_img "https://picsum.photos/seed/mouse/400/300")
[ -n "$IMG6" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG6\"]' WHERE title='雷蛇鼠标 DeathAdder Essential';" campus_assistant && echo "鼠标 OK"

# 床上桌
IMG7=$(upload_img "https://picsum.photos/seed/desk/400/300")
[ -n "$IMG7" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG7\"]' WHERE title='床上书桌 折叠款';" campus_assistant && echo "书桌 OK"

# Java书
IMG8=$(upload_img "https://picsum.photos/seed/java/400/300")
[ -n "$IMG8" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG8\"]' WHERE title='Java核心技术 卷1（第12版）';" campus_assistant && echo "Java OK"

# 吉他
IMG9=$(upload_img "https://picsum.photos/seed/guitar/400/300")
[ -n "$IMG9" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG9\"]' WHERE title='吉他 Yamaha F310 民谣吉他';" campus_assistant && echo "吉他 OK"

# 台灯
IMG10=$(upload_img "https://picsum.photos/seed/lamp/400/300")
[ -n "$IMG10" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG10\"]' WHERE title='台灯 护眼LED 三档调光';" campus_assistant && echo "台灯 OK"

# 自行车
IMG11=$(upload_img "https://picsum.photos/seed/bike/400/300")
[ -n "$IMG11" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG11\"]' WHERE title='公路自行车 捷安特 Escape 3';" campus_assistant && echo "自行车 OK"

# 四级
IMG12=$(upload_img "https://picsum.photos/seed/cet4/400/300")
[ -n "$IMG12" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE market_item SET images='[\"$IMG12\"]' WHERE title='大学英语四级词汇书+真题套装';" campus_assistant && echo "四级 OK"

echo ""
echo "=== 失物招领图片 ==="

# AirPods
LF1=$(upload_img "https://picsum.photos/seed/airpods/400/300")
[ -n "$LF1" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF1' WHERE title='捡到一副黑色 AirPods';" campus_assistant && echo "AirPods OK"

# 钥匙
LF2=$(upload_img "https://picsum.photos/seed/keys/400/300")
[ -n "$LF2" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF2' WHERE title='捡到一串钥匙';" campus_assistant && echo "钥匙 OK"

# 双肩包
LF3=$(upload_img "https://picsum.photos/seed/backpack/400/300")
[ -n "$LF3" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF3' WHERE title='丢失灰色双肩包';" campus_assistant && echo "双肩包 OK"

# U盘
LF4=$(upload_img "https://picsum.photos/seed/usb/400/300")
[ -n "$LF4" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF4' WHERE title='实验楼门口捡到U盘';" campus_assistant && echo "U盘 OK"

# 笔记本
LF5=$(upload_img "https://picsum.photos/seed/notebook/400/300")
[ -n "$LF5" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF5' WHERE title='表白墙捡到一本笔记本';" campus_assistant && echo "笔记本 OK"

# 橘猫
LF6=$(upload_img "https://picsum.photos/seed/orangecat/400/300")
[ -n "$LF6" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF6' WHERE title='小猫走失（橘猫）';" campus_assistant && echo "橘猫 OK"

# 雨伞
LF7=$(upload_img "https://picsum.photos/seed/umbrella/400/300")
[ -n "$LF7" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF7' WHERE title='食堂捡到一把雨伞';" campus_assistant && echo "雨伞 OK"

# 耳机索尼
LF8=$(upload_img "https://picsum.photos/seed/sony/400/300")
[ -n "$LF8" ] && docker exec mysql_twwr-mysql_TwWr-1 mysql -u root -proot -e "UPDATE lost_found_post SET image_urls='$LF8' WHERE title='丢失耳机 索尼 WH-1000XM4';" campus_assistant && echo "耳机 OK"

echo ""
echo "=== 全部完成！==="
echo "二手市场 12 件 + 失物招领 8 件已配图"
