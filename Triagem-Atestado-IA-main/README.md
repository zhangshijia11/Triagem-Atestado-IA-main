# 🏥 职安通 — 智能职业健康安全管理平台

面向中国企业的职业健康与安全（OHS）一站式管理平台。通过 **DeepSeek AI** 智能识别病假单，
自动提取执业证号、ICD 编码和休假天数，结合体检管理、劳动防护用品管理和职业风险评估，
帮助企业高效履行《工伤保险条例》等法规要求。

---

## 🛠️ 技术栈

* **后端:** Java 17, Spring Boot 4.x (Spring Web, Spring Data JPA)
* **数据库:** H2 Database（内存数据库，适合开发与测试）
* **AI 引擎:** DeepSeek API（自然语言处理，智能提取病假单信息）
* **前端:** HTML5, CSS3, Thymeleaf 模板引擎, Chart.js 图表, Bootstrap 5

---

## 💡 核心功能

### 👷 员工门户
* **病假单提交:** 上传图片或手动输入文字，AI 自动识别执业证号、ICD 编码和休假天数
* **体检预约:** 在线预约入职体检、定期体检、离职体检、返岗体检
* **劳保用品签收:** 电子签收安全帽、手套、安全靴等劳动防护用品，支持在线申请更换

### 🏢 企业管理看板
* **综合首页:** 聚合病假条、体检、劳保三大模块 KPI，CID 分布柱状图和审核状态饼图
* **病假条管理:** AI 智能审核队列，一键批准/拒绝，自动标记超过 15 天的劳动能力鉴定预警
* **员工管理:** 员工名册，查看每位员工的病假条、体检、劳保历史
* **体检管理:** 录入体检结果（合格/不合格），上传体检报告
* **劳保管理:** 发放劳动防护用品，跟踪有效期和更换请求
* **职业风险评估:** 基于 ICD 编码分布的岗位风险分析，劳动能力鉴定预警清单，月度缺勤趋势

### 📊 合规规则
* 病假超过 **15 天**：系统自动标记，建议申请**劳动能力鉴定**（依据《工伤保险条例》）
* ICD-10 编码校验：确保提交的诊断编码在国际疾病分类标准中

---

## 🚀 快速启动

### 环境要求
* Java JDK 17+
* Maven（或使用项目自带的 `./mvnw`）

### 启动步骤

```bash
# 1. 进入项目目录
cd Triagem-Atestado-IA-main

# 2. 配置 DeepSeek API Key
#    编辑 src/main/resources/env.properties，填入你的 API Key：
#    DEEPSEEK_API_KEY=你的key

# 3. 启动应用
./mvnw spring-boot:run
```

访问 **http://localhost:8080**

### 测试账号

| 角色 | 邮箱 | 密码 |
|---|---|---|
| 员工 | zhangli@zhian.com | 123 |
| 企业管理员 | hr@zhian.com | admin |

---

## 📐 项目结构

```
src/main/java/br/com/soc/triagem_atestados/
├── controller/     # Web 路由控制器
├── service/        # 业务逻辑层（含 DeepSeek AI 调用）
├── model/          # JPA 实体类
│   └── enums/      # 枚举：状态、类型、角色
├── repository/     # Spring Data JPA 数据访问层
└── config/         # 安全拦截器、数据初始化、ICD 编码加载

src/main/resources/
├── templates/      # Thymeleaf 模板（共 12 个页面）
├── static/css/     # 样式文件
├── cid10.csv       # ICD-10 编码数据
└── application.properties
```
