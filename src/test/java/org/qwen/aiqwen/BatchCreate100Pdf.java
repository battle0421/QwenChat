import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BatchCreate100Pdf {

    // 内部类：存储 文件名、标题、内容
    static class PdfItem {
        String fileName;
        String title;
        String content;

        public PdfItem(String fileName, String title, String content) {
            this.fileName = fileName;
            this.title = title;
            this.content = content;
        }
    }

    public static void main(String[] args) throws IOException {
        // 1. 加载 100 个文档数据
        List<PdfItem> list = init100PdfData();

        // 2. 批量生成 PDF
        for (PdfItem item : list) {
            // 2. 批量生成 PDF

            createPdf(item.title, item.content,  item.fileName);

            System.out.println("已生成：" + item.fileName);
        }

        System.out.println("✅ 100 个 PDF 生成完成！");
    }

    public static void createPdf(String title, String content, String outputPath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // 使用 Windows 系统字体（黑体）
            File fontFile = new File("C:/Windows/Fonts/simhei.ttf");
            if (!fontFile.exists()) {
                throw new IOException("系统字体不存在：" + fontFile.getAbsolutePath());
            }

            PDFont chineseFont = PDType0Font.load(doc, fontFile);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                // 标题
                stream.beginText();
                stream.setFont(chineseFont, 16);
                stream.newLineAtOffset(50, 700);
                stream.showText(title);
                stream.endText();

                // 内容（自动换行）
                stream.beginText();
                stream.setFont(chineseFont, 12);
                stream.newLineAtOffset(50, 650);

                // 按行分割，避免一行太长
                String[] lines = splitContent(content, 80);
                for (String line : lines) {
                    stream.newLineAtOffset(0, -20);
                    stream.showText(normalizeText(line));
                }
                stream.endText();
            }

            doc.save(outputPath);
        }
    }

    // =============== 文本规范化处理 ===============
    private static String normalizeText(String text) {
        return text.replace("₂", "2")
                .replace("₁", "1")
                .replace("₃", "3")
                .replace("₀", "0");
    }



    // =============== 简单文本换行切割 ===============
    private static String[] splitContent(String text, int maxLen) {
        List<String> list = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += maxLen) {
            int end = Math.min(i + maxLen, len);
            list.add(text.substring(i, end));
        }
        return list.toArray(new String[0]);
    }

    // =============== 100 个 PDF 数据（你要的全部内容）===============
    private static List<PdfItem> init100PdfData() {
        List<PdfItem> list = new ArrayList<>();

        // 下面直接把 100 个全部塞进去
        list.add(new PdfItem("常识_地球知识.pdf", "地球基本知识", "地球是太阳系八大行星之一，呈椭球形。地球表面约71%被水覆盖，29%为陆地。地球围绕太阳公转，同时进行自转，公转一周为一年，自转一周为一天。"));
        list.add(new PdfItem("常识_水的特性.pdf", "水的基本特性", "水的化学式为H₂O，常温下为无色无味液体。标准大气压下，水的沸点为100℃，冰点为0℃。水是生命之源，所有已知生命都离不开水。"));
        list.add(new PdfItem("常识_太阳知识.pdf", "太阳简介", "太阳是太阳系的中心天体，属于恒星。太阳通过核聚变产生光和热，为地球提供能量。太阳表面温度约5500摄氏度。"));
        list.add(new PdfItem("常识_月亮知识.pdf", "月球基本知识", "月球是地球唯一的天然卫星，本身不发光，我们看到的月光是反射的太阳光。月球围绕地球转动，引起地球潮汐现象。"));
        list.add(new PdfItem("常识_四季形成.pdf", "四季是如何形成的", "四季变化是由于地球公转和地轴倾斜造成的。不同季节太阳照射角度不同，导致温度差异，形成春夏秋冬。"));
        list.add(new PdfItem("常识_天气现象.pdf", "常见天气现象", "常见天气包括晴天、阴天、雨天、雪天、雾天、大风等。天气由温度、湿度、气压、气流等因素共同决定。"));
        list.add(new PdfItem("常识_植物生长.pdf", "植物生长条件", "植物生长需要阳光、水分、空气、适宜温度和养分。植物通过光合作用制造养分，维持自身生长。"));
        list.add(new PdfItem("常识_动物分类.pdf", "常见动物分类", "动物可分为脊椎动物和无脊椎动物。脊椎动物包括哺乳动物、鸟类、爬行动物、两栖动物、鱼类。"));
        list.add(new PdfItem("常识_人体结构.pdf", "人体基本结构", "人体由细胞、组织、器官、系统构成。主要系统包括循环系统、呼吸系统、消化系统、神经系统等。"));
        list.add(new PdfItem("常识_健康常识.pdf", "日常健康常识", "保持健康需要合理饮食、充足睡眠、适度运动和良好心态。每天多喝水、勤洗手、少熬夜有利于身体健康。"));

        list.add(new PdfItem("科技_计算机基础.pdf", "计算机基础知识", "计算机由硬件和软件组成。硬件包括CPU、内存、硬盘、显示器等；软件包括系统软件和应用软件。"));
        list.add(new PdfItem("科技_互联网介绍.pdf", "互联网简介", "互联网是连接全球计算机的网络系统，实现信息共享与通信。人们通过互联网浏览信息、社交、学习、工作。"));
        list.add(new PdfItem("科技_手机功能.pdf", "智能手机主要功能", "智能手机可用于通话、短信、拍照、上网、支付、导航、娱乐等。现代手机集成了多种传感器和高性能芯片。"));
        list.add(new PdfItem("科技_人工智能简介.pdf", "人工智能基础介绍", "人工智能是让机器模拟人类智能的技术。包括语音识别、图像识别、自然语言处理、智能推荐等应用。"));
        list.add(new PdfItem("科技_大数据介绍.pdf", "大数据基础知识", "大数据指规模巨大、类型复杂的数据集合。通过分析大数据可挖掘规律、辅助决策、优化服务。"));
        list.add(new PdfItem("科技_云计算基础.pdf", "云计算简单介绍", "云计算通过网络提供计算资源。用户无需自建服务器，可按需使用存储、算力、数据库等服务。"));
        list.add(new PdfItem("科技_区块链简介.pdf", "区块链基础知识", "区块链是去中心化、不可篡改的分布式账本技术。具有透明、安全、可追溯等特点。"));
        list.add(new PdfItem("科技_5G技术介绍.pdf", "5G通信技术", "5G是第五代移动通信技术，具有高速率、低延迟、广连接特点。广泛应用于智能设备、物联网、远程医疗。"));
        list.add(new PdfItem("科技_物联网介绍.pdf", "物联网基础概念", "物联网是物与物相连的互联网，通过传感器实现设备智能互联，如智能家居、智能城市。"));
        list.add(new PdfItem("科技_操作系统介绍.pdf", "常见操作系统", "常见操作系统有Windows、macOS、Linux、Android、iOS。操作系统管理硬件资源，提供运行环境。"));

        list.add(new PdfItem("历史_中国古代史简介.pdf", "中国古代史简述", "中国古代历史悠久，经历夏、商、周、秦、汉、隋、唐、宋、元、明、清等朝代，创造灿烂文明。"));
        list.add(new PdfItem("历史_四大发明.pdf", "中国古代四大发明", "四大发明为造纸术、印刷术、火药、指南针，对世界文明发展产生深远影响。"));
        list.add(new PdfItem("历史_长城介绍.pdf", "万里长城简介", "长城是中国古代军事防御工程，始建于春秋战国时期。长城气势雄伟，是世界文化遗产。"));
        list.add(new PdfItem("历史_故宫介绍.pdf", "北京故宫简介", "故宫是明清皇宫，位于北京中轴线中心，是世界现存规模最大、保存最完整的木质结构古建筑。"));
        list.add(new PdfItem("历史_丝绸之路.pdf", "古代丝绸之路", "丝绸之路是古代连接东西方的贸易通道，促进了经济文化交流，传播了技术、宗教与艺术。"));
        list.add(new PdfItem("历史_端午节介绍.pdf", "端午节文化知识", "端午节为农历五月初五，习俗有吃粽子、赛龙舟、挂艾草，纪念屈原，传承传统文化。"));
        list.add(new PdfItem("历史_春节习俗.pdf", "春节传统习俗", "春节是农历新年，习俗包括贴春联、守岁、拜年、放鞭炮、吃年夜饭，象征团圆与希望。"));
        list.add(new PdfItem("历史_中秋节介绍.pdf", "中秋节文化知识", "中秋节为农历八月十五，象征团圆，习俗有赏月、吃月饼、家人团聚，寄托美好愿望。"));
        list.add(new PdfItem("历史_孔子介绍.pdf", "孔子与儒家思想", "孔子是中国古代思想家、教育家，儒家学派创始人。主张仁、义、礼、智、信，影响深远。"));
        list.add(new PdfItem("历史_世界古代文明.pdf", "世界古代文明简介", "世界古代文明包括古埃及、古巴比伦、古印度、古中国等，各自创造了独特文字、建筑与文化。"));

        list.add(new PdfItem("文化_中国书法.pdf", "中国书法艺术", "书法是中国传统艺术，主要书体有篆书、隶书、楷书、行书、草书，讲究笔法、结构、章法。"));
        list.add(new PdfItem("文化_国画介绍.pdf", "中国国画简介", "国画是中国传统绘画，使用毛笔、水墨、颜料，题材包括山水、花鸟、人物，注重意境表达。"));
        list.add(new PdfItem("文化_京剧介绍.pdf", "京剧基本知识", "京剧是中国国粹，形成于清代，包含唱、念、做、打，角色分为生、旦、净、丑。"));
        list.add(new PdfItem("文化_诗词文化.pdf", "中国古典诗词", "古典诗词是中华文化瑰宝，包括唐诗、宋词、元曲，语言优美，意境深远，情感丰富。"));
        list.add(new PdfItem("文化_成语故事.pdf", "常见成语简介", "成语是汉语中固定短语，多来自历史典故、寓言故事。使用成语可使语言简洁生动。"));
        list.add(new PdfItem("文化_传统节日.pdf", "中国传统节日", "主要传统节日有春节、元宵节、清明节、端午节、中秋节、重阳节，承载民族文化记忆。"));
        list.add(new PdfItem("文化_茶文化介绍.pdf", "中国茶文化", "中国是茶的故乡，茶分为绿茶、红茶、乌龙茶、普洱茶等。饮茶讲究礼仪、品味与养生。"));
        list.add(new PdfItem("文化_美食文化.pdf", "中华美食文化", "中国美食历史悠久，八大菜系包括鲁、川、粤、苏、浙、闽、湘、徽菜，风味多样。"));
        list.add(new PdfItem("文化_武术介绍.pdf", "中国武术简介", "武术是中国传统体育项目，具有强身健体、防身自卫功能，包含太极、少林、武当等流派。"));
        list.add(new PdfItem("文化_民乐介绍.pdf", "中国民族乐器", "民族乐器分为吹、拉、弹、打四类，如二胡、古筝、琵琶、笛子、鼓、锣等。"));

        list.add(new PdfItem("自然_植物知识.pdf", "常见植物知识", "植物可分为草本、木本、藤本。植物通过根吸收水分，叶进行光合作用，花用于繁殖。"));
        list.add(new PdfItem("自然_树木知识.pdf", "树木基本知识", "树木由根、干、枝、叶、花、果组成。树木能净化空气、保持水土、提供氧气和木材。"));
        list.add(new PdfItem("自然_花卉知识.pdf", "常见花卉介绍", "常见花卉有玫瑰、牡丹、菊花、荷花、兰花、梅花。花卉美化环境，象征不同寓意。"));
        list.add(new PdfItem("自然_野生动物.pdf", "野生动物保护", "野生动物是自然生态重要部分。需要保护栖息地，禁止非法捕猎，维护生态平衡。"));
        list.add(new PdfItem("自然_鸟类知识.pdf", "鸟类基本知识", "鸟类体表覆羽，前肢成翼，多数会飞。鸟类产卵繁殖，多迁徙过冬，维持生态平衡。"));
        list.add(new PdfItem("自然_海洋知识.pdf", "海洋基本知识", "海洋占地球表面大部分，包含太平洋、大西洋、印度洋、北冰洋。海洋蕴藏丰富资源。"));
        list.add(new PdfItem("自然_山川河流.pdf", "中国主要山川河流", "著名山脉有喜马拉雅山、泰山、黄山；主要河流有长江、黄河、珠江、黑龙江。"));
        list.add(new PdfItem("自然_天气与气候.pdf", "天气与气候区别", "天气是短期大气状态，气候是长期平均天气状况。气候影响植被、农业与生活习惯。"));
        list.add(new PdfItem("自然_环境保护.pdf", "环境保护基本知识", "保护环境需节约资源、减少污染、垃圾分类、植树造林，共同维护地球生态。"));
        list.add(new PdfItem("自然_生态平衡.pdf", "生态平衡重要性", "生态平衡指生物与环境和谐稳定。破坏平衡会导致灾害，需保护生物多样性。"));

        list.add(new PdfItem("生活_饮食健康.pdf", "健康饮食指南", "健康饮食应荤素搭配，多吃蔬菜水果，少油少盐少糖，规律三餐，不暴饮暴食。"));
        list.add(new PdfItem("生活_作息规律.pdf", "良好作息习惯", "早睡早起，保证7-8小时睡眠，避免熬夜。规律作息提高免疫力，提升精神状态。"));
        list.add(new PdfItem("生活_运动健身.pdf", "日常运动建议", "适合日常的运动有散步、慢跑、跳绳、瑜伽、球类。每周坚持运动有益身心健康。"));
        list.add(new PdfItem("生活_卫生习惯.pdf", "个人卫生常识", "勤洗手、常通风、勤换衣物、保持口腔清洁、定期打扫环境，预防疾病传播。"));
        list.add(new PdfItem("生活_安全常识.pdf", "日常生活安全", "注意用电、用火、交通安全，防溺水、防诈骗、防高空坠落，提高安全意识。"));
        list.add(new PdfItem("生活_急救知识.pdf", "简单急救常识", "遇到烫伤、割伤、溺水、中暑应冷静处理。及时止血、呼救、心肺复苏可挽救生命。"));
        list.add(new PdfItem("生活_家务技巧.pdf", "实用家务小技巧", "合理收纳、快速清洁、衣物分类洗涤、厨具保养，可提高家务效率，保持整洁。"));
        list.add(new PdfItem("生活_购物常识.pdf", "理性消费知识", "购物按需购买，对比价格，查看质量，避免冲动消费。线上购物注意防范诈骗。"));
        list.add(new PdfItem("生活_交通规则.pdf", "基本交通规则", "红灯停、绿灯行，行人走人行道，骑车戴头盔，驾车系安全带，遵守交通法规。"));
        list.add(new PdfItem("生活_旅行常识.pdf", "旅行准备知识", "旅行前规划路线、预订住宿、准备证件药品衣物。注意安全，文明出游，保护环境。"));

        list.add(new PdfItem("学习_读书方法.pdf", "高效读书方法", "读书应先看目录，精读重点，做笔记，多思考，常复习，提高理解与记忆效果。"));
        list.add(new PdfItem("学习_记忆方法.pdf", "提高记忆力方法", "理解记忆、重复记忆、联想记忆、分段记忆，合理休息，提高记忆效率。"));
        list.add(new PdfItem("学习_时间管理.pdf", "时间管理技巧", "制定计划，分清主次，集中注意力，避免拖延，提高学习与工作效率。"));
        list.add(new PdfItem("学习_写作技巧.pdf", "基础写作方法", "写作先确定主题，搭建结构，丰富内容，修改润色。多阅读多练习提升写作能力。"));
        list.add(new PdfItem("学习_英语基础.pdf", "英语学习基础", "英语学习包括单词、语法、听说读写。多听多说多练，可逐步提高英语水平。"));
        list.add(new PdfItem("学习_数学基础.pdf", "数学基础知识", "数学包括算术、代数、几何、统计。学习数学培养逻辑思维与解决问题能力。"));
        list.add(new PdfItem("学习_科学常识.pdf", "中小学科学知识", "科学知识涵盖物理、化学、生物、地理。学习科学认识世界，培养探究精神。"));
        list.add(new PdfItem("学习_专注力培养.pdf", "提升专注力方法", "营造安静环境，定时学习，减少干扰，循序渐进，逐步提升专注时长。"));
        list.add(new PdfItem("学习_复习方法.pdf", "高效复习技巧", "定期复习，梳理知识框架，查漏补缺，做题巩固，提高考试成绩。"));
        list.add(new PdfItem("学习_兴趣培养.pdf", "培养学习兴趣", "发现乐趣，设定小目标，获得成就感，保持好奇心，让学习更主动轻松。"));

        list.add(new PdfItem("经济_货币知识.pdf", "货币基本知识", "货币是交换媒介，具有价值尺度、流通手段功能。现代货币包括纸币、硬币、电子货币。"));
        list.add(new PdfItem("经济_储蓄知识.pdf", "个人储蓄常识", "储蓄是将闲置资金存入银行，获得利息，保障资金安全，应对未来需求。"));
        list.add(new PdfItem("经济_理财基础.pdf", "基础理财知识", "理财包括储蓄、投资、保险等。理性理财可保值增值，需注意风险控制。"));
        list.add(new PdfItem("经济_税收常识.pdf", "税收基本知识", "税收是国家财政收入来源，用于公共服务。公民与企业应依法纳税。"));
        list.add(new PdfItem("经济_就业知识.pdf", "就业基本常识", "就业需提升技能，明确职业方向，遵守劳动法，维护自身合法权益。"));
        list.add(new PdfItem("经济_创业基础.pdf", "创业基本知识", "创业需明确项目，制定计划，整合资源，合法经营，承担风险，创造价值。"));
        list.add(new PdfItem("经济_市场介绍.pdf", "市场基本概念", "市场是商品交换场所，受供求关系影响价格。市场促进资源优化配置。"));
        list.add(new PdfItem("经济_国际贸易.pdf", "国际贸易简介", "国际贸易是国家间商品服务交换，促进经济发展，丰富各国商品选择。"));
        list.add(new PdfItem("经济_通货膨胀.pdf", "通货膨胀基本知识", "通货膨胀指货币购买力下降，物价普遍上涨。适度通胀利于经济，过高则不利。"));
        list.add(new PdfItem("经济_保险知识.pdf", "保险基础常识", "保险用于风险转移，应对意外、疾病、财产损失。常见保险有医疗、养老、车险。"));

        list.add(new PdfItem("法律_法律常识.pdf", "公民法律基本知识", "公民享有权利，履行义务，遵守法律。法律保护人身、财产、名誉等合法权益。"));
        list.add(new PdfItem("法律_未成年人保护.pdf", "未成年人保护法", "法律保护未成年人受教育权、健康权、人身安全。禁止虐待、遗弃、校园欺凌。"));
        list.add(new PdfItem("法律_劳动法常识.pdf", "劳动法基本知识", "劳动法保护劳动者权益，规定工资、工时、休息、社保、劳动合同等内容。"));
        list.add(new PdfItem("法律_交通安全法.pdf", "道路交通安全法", "规定车辆、行人通行规则，处罚违法行为，保障道路交通有序安全畅通。"));
        list.add(new PdfItem("法律_消费者权益.pdf", "消费者权益保护", "消费者享有安全权、知情权、公平交易权。遇到欺诈可投诉维权。"));
        list.add(new PdfItem("法律_网络安全法.pdf", "网络安全基本知识", "禁止网络诈骗、谣言、暴力信息。保护个人信息，维护网络空间安全。"));
        list.add(new PdfItem("法律_财产保护.pdf", "个人财产保护", "法律保护合法财产所有权、继承权。禁止盗窃、抢劫、诈骗等侵犯财产行为。"));
        list.add(new PdfItem("法律_合同常识.pdf", "合同基本知识", "合同是具有法律效力的协议。签订合同应明确条款，诚实守信，履行义务。"));
        list.add(new PdfItem("法律_治安管理.pdf", "治安管理常识", "禁止打架斗殴、寻衅滋事、损坏公物。维护公共秩序，保障社会安定。"));
        list.add(new PdfItem("法律_知识产权.pdf", "知识产权保护", "保护著作权、商标权、专利权。鼓励创新，禁止抄袭盗版侵权行为。"));

        list.add(new PdfItem("艺术_音乐基础.pdf", "音乐基本知识", "音乐由旋律、节奏、和声组成。可舒缓情绪、陶冶情操、丰富精神生活。"));
        list.add(new PdfItem("艺术_舞蹈基础.pdf", "舞蹈基本知识", "舞蹈通过肢体动作表达情感，强身健体，培养气质，展现艺术美感。"));
        list.add(new PdfItem("艺术_绘画基础.pdf", "绘画入门知识", "绘画使用线条、色彩、构图表现形象。培养观察力、创造力与审美能力。"));
        list.add(new PdfItem("艺术_摄影基础.pdf", "摄影入门技巧", "摄影通过光线、构图、角度记录美好瞬间。记录生活，保存回忆，传递美感。"));
        list.add(new PdfItem("艺术_手工制作.pdf", "简单手工制作", "手工可使用纸、布、黏土等材料。培养动手能力，创造有趣作品。"));
        list.add(new PdfItem("心理_情绪管理.pdf", "情绪管理知识", "学会调节情绪，保持乐观，缓解压力，处理焦虑，保持心理健康。"));
        list.add(new PdfItem("心理_人际交往.pdf", "人际交往技巧", "真诚尊重、善于倾听、礼貌沟通、换位思考，建立良好人际关系。"));
        list.add(new PdfItem("心理_自信培养.pdf", "自信心培养方法", "认识优点，正视不足，勇于尝试，积累成功，逐步提升自信心。"));
        list.add(new PdfItem("心理_压力调节.pdf", "缓解压力方法", "运动、倾诉、听音乐、深呼吸、合理休息，有效缓解生活学习压力。"));
        list.add(new PdfItem("综合_文明礼仪.pdf", "日常文明礼仪", "文明礼貌包括用语文明、尊老爱幼、遵守秩序、爱护环境、尊重他人。"));

        return list;
    }
}