# Coupon

Coupon 是一个基于 Spring Boot 3 / Spring Cloud 的优惠券微服务项目，覆盖商家端券模板管理、用户领券、优惠券批量分发、结算查询、网关路由等核心场景。

## 技术栈

- Java 17
- Spring Boot 3.0.7
- Spring Cloud 2022.0.3
- Spring Cloud Alibaba 2022.0.0.0-RC2
- Spring Cloud Gateway
- MyBatis-Plus 3.5.7
- Apache ShardingSphere-JDBC 5.3.2
- MySQL
- Redis / Redisson
- RocketMQ
- Knife4j / OpenAPI 3
- XXL-JOB
- EasyExcel
- Lombok

## 模块说明

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `framework` | - | 公共基础包，提供统一返回、异常处理、幂等注解、Web 自动配置等能力。 |
| `gateway` | `10000` | API 网关，按路径转发到各业务服务；当前 Nacos 注册发现默认关闭。 |
| `merchant-admin` | `10010` | 商家后台服务，负责优惠券模板创建、查询、增发、终止以及批量分发任务创建。 |
| `engine` | `10020` | 优惠券引擎服务，负责用户领券、优惠券查询、订单锁券、支付核销和退款返券。 |
| `settlement` | `10030` | 结算查询服务，负责查询用户可用/不可用优惠券。 |
| `distribution` | `10040` | 分发服务，消费任务消息并执行批量发券、库存扣减和用户券记录写入。 |
| `search` | `10050` | 搜索服务占位模块。 |

## 核心能力

- 商家创建、分页查询、增发、终止优惠券模板。
- 防重复提交和 MQ 消费幂等处理。
- 用户领券，支持直接处理和 MQ 异步处理两种路径。
- 下单锁券、支付核销、退款返券。
- 基于 ShardingSphere-JDBC 的分库分表访问。
- 基于 Redis Lua 脚本的库存扣减和用户券写入辅助逻辑。
- 基于 EasyExcel 的批量发券任务处理。
- 基于 Gateway 的统一入口路由。

## 本地环境

启动业务服务前，请准备以下依赖：

- JDK 17
- Maven 3.8+
- MySQL 8.x，默认连接：
  - `127.0.0.1:3306`
  - 用户名：`root`
  - 密码：`root`
  - 数据库：`one_coupon_rebuild_0`、`one_coupon_rebuild_1`
- Redis，默认连接：`127.0.0.1:6379`
- RocketMQ NameServer，默认连接：`127.0.0.1:9876`
- Nacos，默认连接：`127.0.0.1:8848`。当前 `gateway` 中 `spring.cloud.nacos.discovery.enabled=false`，如需通过服务发现转发，需要先开启并保证各服务注册到 Nacos。
- XXL-JOB Admin，默认地址：`http://localhost:8088/xxl-job-admin`。当前 `merchant-admin` 中 `xxl-job.enabled=false`，按需开启。

> 说明：仓库当前未包含数据库初始化 SQL。分库分表物理表需要按各模块 `src/main/resources/shardingsphere-config.yaml` 中的配置提前创建。

## 分库分表配置

各业务模块通过 `jdbc:shardingsphere:classpath:shardingsphere-config.yaml` 连接 ShardingSphere-JDBC，默认使用两个数据源：

- `ds_0` -> `one_coupon_rebuild_0`
- `ds_1` -> `one_coupon_rebuild_1`

主要逻辑表：

- `t_coupon_template_${0..15}`：优惠券模板表，按 `shop_number` 分片。
- `t_coupon_template_log_${0..15}`：优惠券模板日志表，按 `shop_number` 分片。
- `t_user_coupon_${0..31}`：用户优惠券表，按 `user_id` 分片。
- `t_coupon_settlement_${0..15}` 或 `t_coupon_settlement_${0..7}`：优惠券结算表，按 `user_id` 分片。不同模块配置中表数量存在差异，联调前建议统一核对。

## 快速启动

在项目根目录编译：

```bash
mvn clean package -DskipTests
```

启动单个模块：

```bash
mvn -pl merchant-admin -am spring-boot:run
mvn -pl engine -am spring-boot:run
mvn -pl settlement -am spring-boot:run
mvn -pl distribution -am spring-boot:run
mvn -pl gateway -am spring-boot:run
mvn -pl search -am spring-boot:run
```

如果只想先验证编译：

```bash
mvn -q -DskipTests compile
```

## 网关路由

`gateway` 默认监听 `10000` 端口，路由规则如下：

| 路径 | 目标服务 |
| --- | --- |
| `/api/merchant-admin/**` | `oneCoupon-merchant-admin` |
| `/api/engine/**` | `oneCoupon-engine` |
| `/api/settlement/**` | `oneCoupon-settlement` |
| `/api/distribution/**` | `oneCoupon-distribution` |
| `/api/search/**` | `oneCoupon-search` |

当前网关路由使用 `lb://` 形式，依赖服务发现。若本地未启用 Nacos，可直接访问各服务端口进行调试。

## 主要接口

### 商家后台 `merchant-admin`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/merchant-admin/coupon-template/create` | 创建优惠券模板 |
| `GET` | `/api/merchant-admin/coupon-template/page` | 分页查询优惠券模板 |
| `POST` | `/api/merchant-admin/coupon-template/increaseNumber` | 增加优惠券发行数量 |
| `GET` | `/api/merchant-admin/coupon-template/find` | 查询优惠券模板详情 |
| `POST` | `/api/merchant-admin/coupon-template/terminate` | 终止优惠券模板 |
| `POST` | `/api/merchant-admin/coupon-task/create` | 创建优惠券分发任务 |

### 优惠券引擎 `engine`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/engine/coupon-template/query` | 查询优惠券模板 |
| `POST` | `/api/engine/user-coupon/redeem-v1` | 用户领券，直接处理 |
| `POST` | `/api/engine/user-coupon/redeem-v2` | 用户领券，MQ 异步处理 |
| `POST` | `/api/engine/user-coupon/create-payment-record` | 创建订单用券结算记录 |
| `POST` | `/api/engine/user-coupon/process-payment` | 支付完成后核销优惠券 |
| `POST` | `/api/engine/user-coupon/process-refund` | 退款后返还优惠券 |

### 结算查询 `settlement`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/settlement/coupon-query` | 查询用户可用/不可用优惠券 |
| `POST` | `/api/settlement/coupon-query-sync` | 同步查询用户可用/不可用优惠券 |

## 接口文档

引入 Knife4j 的模块可在服务启动后访问：

```text
http://localhost:{port}/doc.html
```

例如：

- `http://localhost:10010/doc.html`
- `http://localhost:10020/doc.html`
- `http://localhost:10030/doc.html`

## 常见开发注意事项

- 修改数据库地址、用户名或密码时，同步调整对应模块的 `src/main/resources/shardingsphere-config.yaml`。
- Redis、RocketMQ、XXL-JOB 等中间件地址在各模块 `application.yaml` 中配置。
- `gateway` 当前关闭 Nacos 服务发现；需要走网关联调时，请开启服务发现并确认服务名与路由中的 `oneCoupon-*` 名称一致。
- `engine` 模块包名存在 `org.pureglx`，其他模块多为 `org.puregxl`，新增代码时注意沿用所在模块已有包名。
- `settlement` 与 `engine` 中 `t_coupon_settlement` 的分表数量配置不完全一致，涉及结算表联调时建议先统一确认。
- 仓库根目录当前没有数据库建表脚本，建议补充 `sql/` 或 `docs/` 目录沉淀初始化脚本和样例数据。

## 项目结构

```text
Coupon
├── distribution       # 优惠券批量分发服务
├── engine             # 优惠券核心引擎服务
├── framework          # 通用基础框架模块
├── gateway            # Spring Cloud Gateway 网关
├── merchant-admin     # 商家后台管理服务
├── search             # 搜索服务模块
├── settlement         # 优惠券结算查询服务
├── pom.xml            # Maven 父工程
└── README.md
```
