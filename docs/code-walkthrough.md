# CrossPay 完整代码实现讲解

> 逐文件、逐方法讲解全部 43 个 Java 文件的代码逻辑。配合 `business-flow.md` 阅读效果最佳。

---

## 项目骨架：3 个基础设施文件

在讲 7 个节点之前，先看最底层的 3 个基础设施，所有节点都依赖它们。

---

### 基础设施 1：统一响应体 `ApiResponse<T>`

**文件：** `common/dto/ApiResponse.java`

```java
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 字段不出现在 JSON 里
public class ApiResponse<T> {
    private int code;      // 200=成功, 40001=参数错误, 50000=系统异常
    private String message;
    private T data;        // 泛型——可以是 PaymentResponse、Page、Map 任意类型

    // 构造函数是 private 的，强制通过工厂方法创建
    private ApiResponse(int code, String message, T data) { ... }

    public static <T> ApiResponse<T> ok(T data) {       // 成功带数据
        return new ApiResponse<>(200, "success", data);
    }
    public static <T> ApiResponse<T> ok() {             // 成功无数据
        return new ApiResponse<>(200, "success", null);
    }
    public static <T> ApiResponse<T> fail(int code, String msg) {  // 失败
        return new ApiResponse<>(code, msg, null);
    }
}
```

**设计意图：** 前端只需要判断 `code === 200`，不需要针对每个接口写不同的判断逻辑。`T data` 是泛型，任何返回类型都能装进去。`@JsonInclude(NON_NULL)` 保证 `data` 为 null 时不序列化到 JSON 里。

---

### 基础设施 2：业务异常 + 全局捕获

**文件：** `common/exception/BusinessException.java`

```java
public class BusinessException extends RuntimeException {
    private final int code;  // 错误码，如 40004 表示"订单状态不允许此操作"

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

**文件：** `common/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice  // 拦截所有 Controller 的异常
public class GlobalExceptionHandler {

    // 支付系统的异常分三类：
    // 1. 参数错误 → 400，告诉调用方改什么
    // 2. 业务错误 → 400/409，状态冲突（如订单已支付又发起支付）
    // 3. 系统错误 → 500，需要告警排查

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 返回 HTTP 400
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ApiResponse.fail(40001, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("System error", e);
        return ApiResponse.fail(50000, "系统内部错误");  // 不暴露内部细节
    }
}
```

**设计意图：** Service 层只需要 `throw new BusinessException(40004, "...")`，不用在每个 Controller 方法里写 try-catch。异常自动变成标准 JSON 响应。

**关键点——回调接口的幂等性就靠这个机制：**
```java
// CallbackController 中：
try {
    order.markSuccess(channelOrderNo);   // 第二次调用会抛 BusinessException
} catch (BusinessException e) {
    log.warn("重复回调，忽略: {}", e.getMessage());  // 吞掉异常，返回 200
}
return ApiResponse.ok("OK");  // 渠道收到 200，停止重试
```

### 错误码规划

| Code | 含义 |
|------|------|
| 200 | 成功 |
| 40001 | 参数校验失败 |
| 40002 | 商户不存在 |
| 40003 | 订单不存在 |
| 40004 | 订单状态不允许此操作 |
| 40100 | 未登录 |
| 40101 | Token 过期 |
| 40300 | 无权限 |
| 50000 | 系统内部错误 |
| 50001 | 支付渠道异常 |

---

### 基础设施 3：订单号生成器

**文件：** `common/util/OrderNoGenerator.java`

```java
public final class OrderNoGenerator {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // PAY + 20240803 + 000001 = PAY20240803000001
    public static String generatePaymentNo() {
        return "PAY" + DATE_FMT.format(LocalDateTime.now())
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999999));
    }

    public static String generateMerchantNo() {   // M202408030001
        return "M" + DATE_FMT.format(LocalDateTime.now()) + String.format("%04d", ...);
    }

    public static String generateSettlementNo() {  // STL202408030001
        return "STL" + DATE_FMT.format(LocalDateTime.now()) + String.format("%04d", ...);
    }
}
```

**设计意图：**
1. **号里带日期**——一眼看出是哪天的订单，方便按日期路由和归档
2. **前缀区分类型**——PAY 是支付、STL 是结算、M 是商户
3. **真实系统会用 Snowflake 分布式 ID** 或号段模式防止并发冲突。本项目是学习用途，用随机数即可

---

### 基础设施 4：Redis 配置

**文件：** `common/config/RedisConfig.java`

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

**支付系统中 Redis 的用途：**
1. 幂等性控制——同一个订单号不能重复处理（SET NX）
2. 分布式锁——结算任务只在一台机器上执行
3. 缓存——商户信息、汇率等高频读取数据

---

## 节点①：Auth 认证模块（6 个文件）

这个节点回答一个问题：**"谁在操作这个平台？"**

### 完整认证数据流：

```
POST /api/auth/login {email, password}
  → AuthService.login()
    → MerchantRepository.findByEmail() 查数据库
    → PasswordEncoder.matches() 验证 BCrypt 哈希
    → JwtTokenProvider.generateMerchantToken() 签发 JWT
  → 返回 {token, merchantNo, name}

后续每个请求:
  → JwtAuthenticationFilter 拦截
    → 从 Header 取 "Authorization: Bearer <token>"
    → JwtTokenProvider.parseToken() 验证签名+有效期
    → 把 userId 和 role 放入 SecurityContext
  → SecurityConfig 检查 URL 权限匹配
```

### 1.1 `JwtTokenProvider.java`——JWT 的生成和验证

```java
@Component
public class JwtTokenProvider {
    private final SecretKey key;       // 从 application.yml 的 jwt.secret 生成 HMAC 密钥
    private final long expirationMs;   // 86400000 = 24 小时

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成商户 Token
     * 关键字段：subject = merchantId，claims 里放角色
     */
    public String generateMerchantToken(Long merchantId, String merchantNo, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(merchantId))              // sub = 商户ID
                .claim("merchantNo", merchantNo)                  // 自定义字段
                .claim("email", email)
                .claim("role", "MERCHANT")                        // 角色声明
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)                                    // HMAC-SHA256 签名
                .compact();
    }

    /**
     * 生成管理员 Token
     */
    public String generateAdminToken(Long adminId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中提取 Claims
     * 返回 null 表示 token 无效（过期、签名不对、格式错误）
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;  // 不抛异常，返回 null 让 Filter 判断
        }
    }

    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.getSubject()) : null;
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
}
```

**关键设计——role 存在 token 里，不用查数据库：**
每一笔支付 API 请求都要鉴权。如果把 role 存数据库，每次请求都 `SELECT role FROM merchants WHERE id = ?`——高并发下数据库是瓶颈。放在 JWT 的 claim 里，Filter 直接从 token 解析，零数据库查询。

**安全性权衡：**
Token 吊销困难——如果商户被禁用，已有的 token 在过期前仍然有效（24h）。生产环境会加 Redis 黑名单或使用短有效期（5-15分钟）+ refresh token。

### 1.2 `JwtAuthenticationFilter.java`——每个请求的守门人

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter 保证每个请求只执行一次（即使请求在内部被 forward 了多次）

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);  // 从 "Bearer xxx" 提取 token

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);   // 从 sub 取
            String role = jwtTokenProvider.getRole(token);      // 从 claim 取

            // 构建 Spring Security 的 Authentication 对象
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                                    // principal
                            null,                                      // credentials（无）
                            Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + role))
                            // ↑ "MERCHANT" → "ROLE_MERCHANT"
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 即使没有 token 也继续执行——SecurityConfig 会让没有权限的请求返回 403
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀（7个字符）
        }
        return null;
    }
}
```

**请求链路：**
```
请求进入 → JwtAuthenticationFilter → SecurityConfig 的权限检查 → Controller
              ↓                              ↓
         解析 JWT 放 userId          检查 userId 的 role 是否
         到 SecurityContext          匹配当前 URL 的权限要求
```

### 1.3 `SecurityConfig.java`——URL 级别的权限矩阵

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // ↑ 支付 API 是 RESTful 的，用 JWT Bearer Token 认证，不需要 CSRF 保护

            .cors(cors -> cors.configurationSource(corsConfig()))
            // ↑ 允许前端 localhost:3000 跨域访问

            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ↑ 无状态——不用 Session，全部靠 JWT

            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/callback/**").permitAll()
                // ↑ 渠道回调不能要求 JWT——渠道不会带我们的 token

                // 运营端 — ADMIN 和 OPERATOR 均可访问
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "OPERATOR")

                // 商户端
                .requestMatchers("/api/merchant/**").hasRole("MERCHANT")
                .requestMatchers("/api/payment/**").hasRole("MERCHANT")
                .requestMatchers("/api/settlement/**").hasRole("MERCHANT")

                // 其他全部需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            // ↑ 关键：把 JWT Filter 插在 Spring 默认的表单登录 Filter 之前

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**权限矩阵一览：**

| URL Pattern | 权限要求 | 说明 |
|-------------|---------|------|
| `/api/auth/**` | 公开 | 注册、登录不需要认证 |
| `/api/callback/**` | 公开 | 渠道回调不能要求 JWT |
| `/api/admin/**` | ADMIN / OPERATOR | 运营后台 |
| `/api/ai/**` | ADMIN / OPERATOR | AI 助手 |
| `/api/merchant/**` | MERCHANT | 商户信息 |
| `/api/payment/**` | MERCHANT | 支付操作 |
| `/api/settlement/**` | MERCHANT | 结算查询 |

### 1.4 `AuthService.java`——登录和注册的业务逻辑

```java
@Service
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 商户注册
     *
     * 做了三件事：
     * 1. 生成商户编号（M + 日期 + 随机数）
     * 2. 密码 BCrypt 加密（绝不能存明文）
     * 3. 设置默认费率 3%
     *
     * 真实支付公司还会做：KYC 身份验证、资质审核、反洗钱筛查。
     */
    public LoginResponse register(RegisterRequest req) {
        // 邮箱唯一性检查
        if (merchantRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(40001, "该邮箱已注册");
        }

        Merchant merchant = new Merchant();
        merchant.setMerchantNo(OrderNoGenerator.generateMerchantNo());  // M202408030001
        merchant.setName(req.getName());
        merchant.setEmail(req.getEmail());
        merchant.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        // ↑ BCrypt 加密。哪怕数据库泄露，攻击者也看不到明文密码
        merchant.setCountry(req.getCountry());

        merchant = merchantRepository.save(merchant);

        // 注册成功自动登录——直接返回 token
        String token = jwtTokenProvider.generateMerchantToken(
                merchant.getId(), merchant.getMerchantNo(), merchant.getEmail());

        return new LoginResponse(token, merchant.getMerchantNo(), merchant.getName());
    }

    /**
     * 商户登录
     *
     * 安全注意事项：
     * - 邮箱不存在和密码错误返回同样的错误信息（防止用户枚举攻击）
     * - 登录前检查商户状态是否为 ACTIVE
     */
    public LoginResponse login(LoginRequest req) {
        Merchant merchant = merchantRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(40002, "邮箱或密码错误"));
        // ↑ 注意：不管邮箱不存在还是密码错误，返回同样的错误信息

        if (!passwordEncoder.matches(req.getPassword(), merchant.getPasswordHash())) {
            throw new BusinessException(40002, "邮箱或密码错误");
            // ↑ 密码错误也返回同样的错误——攻击者无法通过错误信息判断邮箱是否存在
        }

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new BusinessException(40004, "商户已被禁用，请联系客服");
        }

        String token = jwtTokenProvider.generateMerchantToken(
                merchant.getId(), merchant.getMerchantNo(), merchant.getEmail());

        return new LoginResponse(token, merchant.getMerchantNo(), merchant.getName());
    }
}
```

### 1.5 DTOs

**`RegisterRequest.java`：**

```java
public class RegisterRequest {
    @NotBlank(message = "商户名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 50)
    private String password;

    private String country = "Kenya";  // 默认肯尼亚
}
```

**`LoginRequest.java`：**

```java
public class LoginRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
```

**`LoginResponse.java`：**

```java
public class LoginResponse {
    private String token;
    private String merchantNo;
    private String name;

    public LoginResponse(String token, String merchantNo, String name) {
        this.token = token;
        this.merchantNo = merchantNo;
        this.name = name;
    }
}
```

### 1.6 `AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminService adminService;

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest req) {
        // @Valid 触发 Bean Validation，失败抛 MethodArgumentNotValidException
        // GlobalExceptionHandler 捕获后返回 40001
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    /**
     * 管理员登录——使用独立的 admin_users 表
     */
    @PostMapping("/admin/login")
    public ApiResponse<Map<String, String>> adminLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return ApiResponse.ok(adminService.adminLogin(username, password));
    }
}
```

---

## 节点②：Merchant 商户模块（4 个文件）

这个节点回答：**"这笔钱最终归谁？"**

### 2.1 `Merchant.java`——核心实体

```java
@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_no", nullable = false, unique = true, length = 20)
    private String merchantNo;    // M202408030001——对外暴露，比 ID 更安全（防遍历）

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;         // 登录名

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;  // BCrypt 加密，永不存明文

    @Column(length = 50)
    private String country;

    @Column(length = 10)
    private String currency = "USD";

    /**
     * 每商户独立费率
     *
     * 这是支付公司的核心商业逻辑：
     * - 大商户（月交易额 > 100 万）→ 1.5% 费率
     * - 中小商户 → 3.5% 费率
     * - 特定行业可能有特殊费率
     */
    @Column(name = "fee_rate", precision = 5, scale = 4)
    private BigDecimal feeRate = new BigDecimal("0.0300");  // 默认 3%

    @Column(length = 20)
    private String status = "ACTIVE";  // ACTIVE | SUSPENDED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA 生命周期回调——自动填充时间戳
    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & Setters ...
}
```

### 2.2 `MerchantRepository.java`

```java
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByEmail(String email);

    Optional<Merchant> findByMerchantNo(String merchantNo);

    boolean existsByEmail(String email);
}
```

### 2.3 `MerchantService.java`

```java
@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;

    /**
     * 获取商户信息
     */
    public Merchant getMerchant(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(40002, "商户不存在"));
    }

    /**
     * 商户首页 Dashboard 数据
     * 真实系统这会是一堆聚合查询（今日交易额、成功率、待结算金额等）
     */
    public Map<String, Object> getDashboard(Long merchantId) {
        Merchant merchant = getMerchant(merchantId);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("merchantNo", merchant.getMerchantNo());
        dashboard.put("name", merchant.getName());
        dashboard.put("country", merchant.getCountry());
        dashboard.put("currency", merchant.getCurrency());
        dashboard.put("feeRate", merchant.getFeeRate().toString());
        dashboard.put("status", merchant.getStatus());
        // 以下为占位数据，后续由 Payment 模块填充
        dashboard.put("todayTransactionCount", 0);
        dashboard.put("todayTransactionAmount", "0.00");
        dashboard.put("successRate", "0%");
        return dashboard;
    }
}
```

### 2.4 `MerchantController.java`——如何获取当前用户身份

```java
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 获取当前登录商户信息
     * 商户 ID 从 JWT 中解析并由 SecurityContext 传入
     */
    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(merchantService.getMerchant(merchantId));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(merchantService.getDashboard(merchantId));
    }

    /**
     * 从 SecurityContext 获取当前商户 ID
     * JWT Filter 在请求进来时已经把 userId 放进 Authentication 了
     */
    private Long getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();  // 这就是 JwtAuthenticationFilter 放进去的 userId
    }
}
```

**这个模式的精髓——Controller 不需要知道"谁在调用我"：**
- JWT Filter 已经把 `userId` 放进 `SecurityContext`
- Controller 直接从 `SecurityContext` 取
- 请求 A（商户 1）和请求 B（商户 2）同时调用 `profile()`，各自取到自己的 `merchantId`
- 线程安全——`SecurityContextHolder` 默认用 `ThreadLocal` 存储

---

## 节点③：Payment Order 支付订单模块（6 个文件）

这是整个项目的核心。回答：**"这笔钱现在在哪一步？"**

### 3.1 `PaymentOrder.java`——状态机实体（最重要）

```java
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;           // PAY20240803000001

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;          // 归属商户

    @Column(name = "merchant_order_no", length = 100)
    private String merchantOrderNo;   // 商户侧订单号（如 "order-12345"）

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;        // BigDecimal 而不是 double——资金计算必须精确

    @Column(length = 10)
    private String currency = "USD";

    @Column(nullable = false, length = 20)
    private String status = "CREATED";  // CREATED | PROCESSING | SUCCESS | FAILED

    @Column(length = 50)
    private String channel;             // 使用的渠道名

    @Column(name = "channel_order_no", length = 100)
    private String channelOrderNo;      // 渠道侧订单号

    @Column(name = "fail_reason", length = 500)
    private String failReason;          // 失败原因

    @Column(name = "callback_received_at")
    private LocalDateTime callbackReceivedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA lifecycle
    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 下面是状态机的三个核心方法 ==========

    /**
     * CREATED → PROCESSING
     * 表示：订单已发送到支付渠道，等待渠道返回结果
     */
    public void markProcessing(String channel) {
        if (!"CREATED".equals(this.status)) {
            throw new BusinessException(40004,
                    "订单状态不允许此操作，当前状态: " + this.status);
        }
        this.status = "PROCESSING";
        this.channel = channel;
    }

    /**
     * PROCESSING → SUCCESS
     * 表示：渠道确认支付成功
     *
     * 幂等性保证：如果订单已经是 SUCCESS（重复回调），
     * 此方法抛异常，调用方可以捕获后返回 200 给渠道
     */
    public void markSuccess(String channelOrderNo) {
        if (!"PROCESSING".equals(this.status)) {
            throw new BusinessException(40004,
                    "订单状态不允许标记成功，当前状态: " + this.status);
        }
        this.status = "SUCCESS";
        this.channelOrderNo = channelOrderNo;
        this.callbackReceivedAt = LocalDateTime.now();
    }

    /**
     * PROCESSING → FAILED
     * 表示：渠道返回支付失败
     */
    public void markFailed(String failReason) {
        if (!"PROCESSING".equals(this.status)) {
            throw new BusinessException(40004,
                    "订单状态不允许标记失败，当前状态: " + this.status);
        }
        this.status = "FAILED";
        this.failReason = failReason;
        this.callbackReceivedAt = LocalDateTime.now();
    }

    // ========== 没有 public setStatus() 方法——这是关键！ ==========

    // Getters & Setters（status 只有 getter，没有 setter）
    public String getStatus() { return status; }
    // 没有 public void setStatus(String s) ！！！
}
```

**为什么不用一个简单的 `setStatus(String s)`？**

假设有人这么写：
```java
order.setStatus("SUCCESS");  // 从 CREATED 直接跳到 SUCCESS——跳过了渠道调用！
order.setStatus("CREATED");  // 把一个已支付的订单改回创建态——资金差错！
```

状态机方法的守卫条件让这些非法操作在对象层面就不可能发生。这不是"建议你不要这么做"，而是"你做不到"。

**面试可以讲的点：**
1. 幂等性——重复回调被状态机拒绝，系统返回 200 停止渠道重试
2. 可追溯——每次状态转换都有明确的调用者和时间点
3. 回滚/补偿——将来加 `markRefunded()`，只有 SUCCESS 状态能退款

### 3.2 `PaymentRepository.java`

```java
public interface PaymentRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderNo(String orderNo);

    Page<PaymentOrder> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);
    // ↑ Spring Data JPA 自动生成 SQL:
    //   SELECT * FROM payment_orders WHERE merchant_id = ? ORDER BY created_at DESC LIMIT ?, ?
}
```

### 3.3 `PaymentService.java`——一笔支付的完整生命周期

```java
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final GatewayRouter gatewayRouter;

    /**
     * 创建支付订单——完整流程
     *
     * 1. 验证商户存在且状态正常
     * 2. 生成平台订单号（PAY + 日期 + 随机数）
     * 3. 创建订单（CREATED）
     * 4. 调用 GatewayRouter 路由到合适的支付渠道
     * 5. 按渠道返回更新订单状态
     *
     * @Transactional 保证原子性——中间任何一步失败，前面 INSERT 的数据回滚
     */
    @Transactional
    public PaymentResponse createPayment(Long merchantId, CreatePaymentRequest req) {
        // 步骤1: 验证商户
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(40002, "商户不存在"));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new BusinessException(40004, "商户已被禁用，无法发起支付");
        }

        // 步骤2: 创建订单（状态 = CREATED）
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(OrderNoGenerator.generatePaymentNo());  // PAY20240803000001
        order.setMerchantId(merchantId);
        order.setMerchantOrderNo(req.getMerchantOrderNo());
        order.setAmount(req.getAmount());
        order.setCurrency(req.getCurrency());

        order = paymentRepository.save(order);  // INSERT INTO payment_orders
        log.info("Payment order created: orderNo={}, merchantId={}, amount={} {}",
                order.getOrderNo(), merchantId, order.getAmount(), order.getCurrency());

        // 步骤3: 发送到支付渠道
        order.markProcessing("MOCK_AFRICA");     // CREATED → PROCESSING（状态机守卫）
        paymentRepository.save(order);           // UPDATE status='PROCESSING'

        GatewayPayResponse gatewayResp = gatewayRouter.route(
                new GatewayPayRequest(
                    order.getOrderNo(),
                    order.getAmount(),
                    order.getCurrency(),
                    "http://localhost:8080/api/callback/MOCK_AFRICA"
                ));

        // 步骤4: 按渠道返回更新订单状态
        switch (gatewayResp.getStatus()) {
            case "SUCCESS":
                order.markSuccess(gatewayResp.getChannelOrderNo());
                break;
            case "FAILED":
                order.markFailed(gatewayResp.getMessage());
                break;
            case "PROCESSING":
                // 渠道异步处理中，订单保持 PROCESSING，等待后续回调通知
                log.info("Payment order waiting for callback: orderNo={}", order.getOrderNo());
                break;
        }
        paymentRepository.save(order);  // 最终 UPDATE

        return PaymentResponse.from(order);
    }

    /**
     * 查询支付订单
     * 安全要点：只有属于该商户的订单才能被查询（防止越权）
     */
    public PaymentResponse queryPayment(Long merchantId, String orderNo) {
        PaymentOrder order = paymentRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(40003, "订单不存在"));

        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(40300, "无权查询此订单");
            // ↑ 商户 A 不能查询商户 B 的订单
        }

        return PaymentResponse.from(order);
    }

    /**
     * 查看商户交易记录（分页）
     */
    public Page<PaymentResponse> getTransactions(Long merchantId, int page, int size) {
        return paymentRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size))
                .map(PaymentResponse::from);
    }
}
```

### 3.4 `PaymentResponse.java`——Entity 和 DTO 的分离

```java
/**
 * 支付订单响应
 * 用独立 DTO 而不是直接返回 Entity，这样：
 * 1. 不暴露内部字段（如 id, merchantId, updatedAt）
 * 2. API 契约独立演化（加字段不影响 entity）
 */
public class PaymentResponse {
    private String orderNo;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String channel;
    private String channelOrderNo;
    private String failReason;
    private LocalDateTime createdAt;

    public static PaymentResponse from(PaymentOrder order) {
        PaymentResponse r = new PaymentResponse();
        r.orderNo = order.getOrderNo();
        r.amount = order.getAmount();
        r.currency = order.getCurrency();
        r.status = order.getStatus();
        r.channel = order.getChannel();
        r.channelOrderNo = order.getChannelOrderNo();
        r.failReason = order.getFailReason();
        r.createdAt = order.getCreatedAt();
        // 注意：没有复制 id, merchantId, updatedAt——这些内部字段不暴露
        return r;
    }

    // Getters...
}
```

### 3.5 接口

**`CreatePaymentRequest.java`：**

```java
public class CreatePaymentRequest {
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    private String currency = "USD";

    private String merchantOrderNo;  // 商户自己的订单号
}
```

**`PaymentController.java`：**

```java
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest req) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(paymentService.createPayment(merchantId, req));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<PaymentResponse> query(@PathVariable String orderNo) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(paymentService.queryPayment(merchantId, orderNo));
    }

    @GetMapping("/transactions")
    public ApiResponse<Page<PaymentResponse>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(paymentService.getTransactions(merchantId, page, size));
    }

    private Long getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

---

## 节点④：Gateway Adapter 网关适配器（5 个文件）

这个节点回答：**"实际谁来处理资金？"**

### 4.1 `PaymentGateway.java`——核心接口

```java
/**
 * 支付渠道统一接口
 *
 * === 为什么支付系统需要 Adapter 模式 ===
 *
 * 真实支付公司对接的不是一家银行，而是几十上百家：
 * - 非洲 → M-Pesa, Flutterwave, Paystack
 * - 东南亚 → GrabPay, Dana, GCash
 * - 拉美   → Pix, Mercado Pago
 * - 欧美   → Stripe, Adyen, PayPal
 *
 * 每家接口不一样：
 * - Stripe:      POST /v1/payment_intents
 * - PayPal:      POST /v2/checkout/orders
 * - Flutterwave: POST /v3/payments
 *
 * 如果 PaymentService 里直接写 if-stripe-else-paypal...
 * 每加一个渠道就要改支付核心代码 → 风险极高。
 *
 * Adapter 模式的核心思想：
 * 1. 定义统一接口（这个 Interface）
 * 2. 每个渠道有自己的实现
 * 3. GatewayRouter 决定用哪个渠道
 * 4. PaymentService 只依赖接口，不依赖具体实现
 *
 * 结果：加一个渠道 = 新建一个类，不碰任何已有代码。
 *
 * 接口刻意最小化——只有 pay() 和 getChannelName()。
 * 没有 refund(), query(), void()——
 * 在没有第二个实现之前，你不知道这些方法应该收什么参数。
 * 避免过早抽象。
 */
public interface PaymentGateway {
    GatewayPayResponse pay(GatewayPayRequest request);
    String getChannelName();
}
```

### 4.2 `MockAfricaGateway.java`——模拟渠道实现

```java
@Component
public class MockAfricaGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockAfricaGateway.class);

    /**
     * === 模拟逻辑 ===
     * - 80% 概率返回 SUCCESS
     * - 15% 概率返回 FAILED（模拟真实场景：余额不足、卡过期等）
     * - 5%  概率返回 PROCESSING（模拟异步处理中）
     *
     * 真实渠道对接时，这里会是 HTTP 调用第三方 API：
     *   restTemplate.postForEntity("https://api.flutterwave.com/v3/payments", ...)
     */
    @Override
    public GatewayPayResponse pay(GatewayPayRequest request) {
        log.info("MockAfricaGateway: processing payment orderNo={}, amount={} {}",
                request.getPlatformOrderNo(), request.getAmount(), request.getCurrency());

        // 模拟网络延迟（真实场景 200ms ~ 2s）
        simulateLatency();

        int random = ThreadLocalRandom.current().nextInt(100);
        String channelOrderNo = "AFR_" + request.getPlatformOrderNo();

        if (random < 80) {
            // 80% 成功
            log.info("MockAfricaGateway: SUCCESS orderNo={}", request.getPlatformOrderNo());
            return new GatewayPayResponse(true, channelOrderNo, "SUCCESS",
                    "Payment processed successfully");
        } else if (random < 95) {
            // 15% 失败（模拟各种真实失败原因）
            String[] reasons = {
                "Insufficient funds",           // 余额不足
                "Card expired",                 // 卡过期
                "Transaction declined by issuer", // 发卡行拒绝
                "Do not honor"                  // 通用拒绝码
            };
            String reason = reasons[ThreadLocalRandom.current().nextInt(reasons.length)];
            log.warn("MockAfricaGateway: FAILED orderNo={}, reason={}",
                    request.getPlatformOrderNo(), reason);
            return new GatewayPayResponse(false, channelOrderNo, "FAILED", reason);
        } else {
            // 5% 处理中
            log.info("MockAfricaGateway: PROCESSING orderNo={}", request.getPlatformOrderNo());
            return new GatewayPayResponse(true, channelOrderNo, "PROCESSING",
                    "Payment is being processed asynchronously");
        }
    }

    @Override
    public String getChannelName() {
        return "MOCK_AFRICA";
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(300, 800));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4.3 `GatewayRouter.java`——渠道选择器

```java
@Component
public class GatewayRouter {

    private final MockAfricaGateway mockAfricaGateway;
    // 真实系统这里会注入一个 List<PaymentGateway>，Spring 自动收集所有实现类

    /**
     * 路由支付请求到合适的渠道
     *
     * 现阶段只有一个 Mock 渠道。真实系统会在这里：
     * 1. 查 merchant 的国家 → 确定可用渠道列表
     * 2. 按优先级排序（费率低的优先、成功率高的优先）
     * 3. 选择第一个 → 调用
     * 4. 如果失败 → 尝试下一个（fallback 机制）
     */
    public GatewayPayResponse route(GatewayPayRequest request) {
        return mockAfricaGateway.pay(request);
    }
}
```

### 4.4 DTOs

**`GatewayPayRequest.java`：**

```java
public class GatewayPayRequest {
    private String platformOrderNo;   // 平台订单号
    private BigDecimal amount;
    private String currency;
    private String callbackUrl;       // 渠道回调地址——渠道处理完后调这个 URL

    public GatewayPayRequest(String platformOrderNo, BigDecimal amount,
                             String currency, String callbackUrl) {
        this.platformOrderNo = platformOrderNo;
        this.amount = amount;
        this.currency = currency;
        this.callbackUrl = callbackUrl;
    }
}
```

**`GatewayPayResponse.java`：**

```java
public class GatewayPayResponse {
    private boolean success;
    private String channelOrderNo;    // 渠道侧订单号（对账用）
    private String status;            // PROCESSING | SUCCESS | FAILED
    private String message;           // 描述信息（成功时是提示，失败时是原因）

    public GatewayPayResponse(boolean success, String channelOrderNo,
                              String status, String message) {
        this.success = success;
        this.channelOrderNo = channelOrderNo;
        this.status = status;
        this.message = message;
    }
}
```

---

## 节点⑤：Callback 回调接口（1 个文件）

这个节点回答：**"渠道通知我们结果了吗？"**

### 5.1 `CallbackController.java`

```java
@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);
    private final PaymentRepository paymentRepository;

    /**
     * 接收渠道的异步通知
     *
     * === 为什么需要回调（Callback / Webhook）===
     * 支付不是同步的。你调了渠道的 API，对方可能：
     * - 当时就返回结果（同步模式）
     * - 过几秒/几分钟才通知你（异步模式）
     *
     * 所以支付系统需要暴露一个公开的 HTTP 端点，
     * 让渠道在支付完成后"回调"我们。
     *
     * === 安全问题 ===
     * - 真实系统必须验证回调签名（HMAC-SHA256）
     * - 这里简化为检查 Header: X-Channel-Signature
     *
     * === 幂等性 ===
     * - 同一个回调可能因为网络重试被发送多次
     * - 状态机保证 PROCESSING→SUCCESS 只能执行一次
     * - 第二次回调时会抛异常 → 捕获后返回 200（防止渠道无限重试）
     */
    @PostMapping("/{channel}")
    @Transactional
    public ApiResponse<String> handleCallback(
            @PathVariable String channel,
            @RequestHeader(value = "X-Channel-Signature", required = false) String signature,
            @RequestBody Map<String, String> body) {

        String platformOrderNo = body.get("platformOrderNo");
        String channelOrderNo = body.get("channelOrderNo");
        String resultStatus = body.get("status");  // SUCCESS or FAILED

        log.info("Callback received: channel={}, orderNo={}, status={}",
                channel, platformOrderNo, resultStatus);

        // 查找订单
        PaymentOrder order = paymentRepository.findByOrderNo(platformOrderNo)
                .orElse(null);

        if (order == null) {
            log.error("Callback: order not found: {}", platformOrderNo);
            return ApiResponse.fail(40003, "订单不存在");
        }

        // ========== 核心幂等性逻辑 ==========
        try {
            if ("SUCCESS".equals(resultStatus)) {
                order.markSuccess(channelOrderNo);
                // ↑ 如果订单已经是 SUCCESS（重复回调），这里会抛 BusinessException
            } else if ("FAILED".equals(resultStatus)) {
                order.markFailed(body.getOrDefault("failReason", ""));
            }
            paymentRepository.save(order);
            log.info("Callback processed: orderNo={}, newStatus={}",
                    platformOrderNo, order.getStatus());
        } catch (BusinessException e) {
            // 重复回调或非法状态转换 → 不报错，吞掉异常
            log.warn("Callback: state transition rejected (likely duplicate): orderNo={}, error={}",
                    platformOrderNo, e.getMessage());
        }
        // =====================================

        // 始终返回成功（防止渠道因为我们的错误而无限重试）
        return ApiResponse.ok("OK");
    }
}
```

**为什么要吞掉异常？**

场景模拟：
```
1. 渠道发送回调 → 你的服务器处理成功 → 订单变成 SUCCESS
2. 返回 200 的网络包丢了 → 渠道认为你没有收到
3. 渠道重发回调 → 订单已经是 SUCCESS → markSuccess() 抛异常
   - 如果你返回 500 → 渠道继续重试，可能重试几十次
   - 如果你返回 200 → 渠道认为"通知已送达"，停止重试 ← 这是正确的做法
```

---

## 节点⑥：Settlement 结算模块（6 个文件）

这个节点回答：**"平台抽多少？商户到手多少？"**

### 6.1 `Settlement.java`——结算记录

```java
@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_no", nullable = false, unique = true, length = 30)
    private String settlementNo;    // STL202408030001

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // 交易总额 = 所有参与订单金额之和

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;   // 手续费 = totalAmount × feeRate

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;   // 商户实收 = totalAmount - feeAmount

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;  // 结算日期（T+1）

    @Column(length = 20)
    private String status = "PENDING";  // PENDING → COMPLETED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters...
}
```

### 6.2 `SettlementDetail.java`——结算与订单的关联

```java
@Entity
@Table(name = "settlement_details")
public class SettlementDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "payment_order_id", nullable = false)
    private Long paymentOrderId;

    public SettlementDetail() {}

    public SettlementDetail(Long settlementId, Long paymentOrderId) {
        this.settlementId = settlementId;
        this.paymentOrderId = paymentOrderId;
    }
}
```

**表关系：**
```
settlements                         settlement_details
┌──────────────────────┐           ┌──────────────────────────┐
│ id: 1                │◄──────────│ settlement_id: 1         │
│ settlement_no: STL.. │           │ payment_order_id: 101    │
│ total_amount: 1000   │           ├──────────────────────────┤
│ fee_amount: 30       │◄──────────│ settlement_id: 1         │
│ net_amount: 970      │           │ payment_order_id: 102    │
└──────────────────────┘           └──────────────────────────┘
```

### 6.3 `SettlementRepository.java`

```java
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Page<Settlement> findByMerchantIdOrderBySettlementDateDesc(Long merchantId, Pageable pageable);
}
```

### 6.4 `SettlementService.java`——核心计算逻辑

```java
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettlementRepository settlementRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;

    /**
     * 对指定商户执行日终结算
     *
     * === 结算的核心公式 ===
     * netAmount = totalAmount − (totalAmount × feeRate)
     *
     * 例如: totalAmount = 1000 USD, feeRate = 3%
     *       feeAmount = 1000 × 0.03 = 30 USD
     *       netAmount = 1000 − 30 = 970 USD
     *
     * === 真实结算的复杂度 ===
     * - 不同支付方式的费率不同（信用卡 3.5%, 本地钱包 1.5%）
     * - 汇率波动（USD 结算但商户收 KES 肯尼亚先令）
     * - 拒付扣减（Chargeback 要从结算里扣）
     * - 最低结算金额（不满足最低金额则顺延到下个周期）
     * - 结算周期（T+1, T+3, 每周二等）
     *
     * 真实系统这是一个定时任务（每日凌晨跑），不会手动触发。
     */
    @Transactional
    public SettlementResponse createSettlement(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId).orElse(null);
        if (merchant == null) {
            throw new RuntimeException("商户不存在");
        }

        // 步骤1: 查找该商户所有未结算的 SUCCESS 订单
        // TODO: 真实系统需要记录哪些订单已被结算，避免重复结算
        List<PaymentOrder> orders = paymentRepository.findAll().stream()
                .filter(o -> o.getMerchantId().equals(merchantId))
                .filter(o -> "SUCCESS".equals(o.getStatus()))
                .toList();

        if (orders.isEmpty()) {
            log.info("No orders to settle for merchant: {}", merchantId);
            return null;
        }

        // 步骤2: 汇总金额
        BigDecimal totalAmount = orders.stream()
                .map(PaymentOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 示例: 10 笔订单，总额 $1,000.00

        // 步骤3: 计算费用
        BigDecimal feeRate = merchant.getFeeRate();    // 例如 0.0300 = 3%
        BigDecimal feeAmount = totalAmount.multiply(feeRate)
                .setScale(2, RoundingMode.HALF_UP);    // $1,000 × 3% = $30.00
        BigDecimal netAmount = totalAmount.subtract(feeAmount);  // $1,000 − $30 = $970.00

        log.info("Settlement for merchant {}: total={}, fee={}, net={}",
                merchant.getMerchantNo(), totalAmount, feeAmount, netAmount);

        // 步骤4: 生成结算记录
        Settlement settlement = new Settlement();
        settlement.setSettlementNo(OrderNoGenerator.generateSettlementNo());  // STL202408030001
        settlement.setMerchantId(merchantId);
        settlement.setTotalAmount(totalAmount);     // 1000.00
        settlement.setFeeAmount(feeAmount);          // 30.00
        settlement.setNetAmount(netAmount);          // 970.00
        settlement.setCurrency(merchant.getCurrency());
        settlement.setSettlementDate(LocalDate.now());
        settlement.setStatus("PENDING");             // 待审核

        settlement = settlementRepository.save(settlement);

        // 步骤5: 记录结算明细——哪几笔订单参与了这次结算
        for (PaymentOrder order : orders) {
            settlementDetailRepository.save(
                    new SettlementDetail(settlement.getId(), order.getId()));
        }

        return SettlementResponse.from(settlement);
    }

    public Page<SettlementResponse> getSettlements(Long merchantId, int page, int size) {
        return settlementRepository
                .findByMerchantIdOrderBySettlementDateDesc(merchantId, PageRequest.of(page, size))
                .map(SettlementResponse::from);
    }
}
```

**注意：`payment_orders` 表只被读取，从未被 UPDATE。**
SettlementService 用了 `paymentRepository.findAll()` 然后 filter，最后只写了 `settlements` 表和 `settlement_details` 表。这就是 ADR-3 的核心——结算读支付数据但不修改支付数据。

### 6.5 `SettlementResponse.java`

```java
public class SettlementResponse {
    private String settlementNo;
    private BigDecimal totalAmount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String currency;
    private LocalDate settlementDate;
    private String status;

    public static SettlementResponse from(Settlement s) {
        SettlementResponse r = new SettlementResponse();
        r.settlementNo = s.getSettlementNo();
        r.totalAmount = s.getTotalAmount();
        r.feeAmount = s.getFeeAmount();
        r.netAmount = s.getNetAmount();
        r.currency = s.getCurrency();
        r.settlementDate = s.getSettlementDate();
        r.status = s.getStatus();
        return r;
    }
}
```

### 6.6 `SettlementController.java`

```java
@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/list")
    public ApiResponse<Page<SettlementResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(settlementService.getSettlements(merchantId, page, size));
    }

    /**
     * 手动触发结算（学习用，真实系统是定时任务 + 分布式锁保证单机执行）
     */
    @PostMapping("/trigger")
    public ApiResponse<SettlementResponse> trigger() {
        Long merchantId = getCurrentMerchantId();
        SettlementResponse result = settlementService.createSettlement(merchantId);
        return ApiResponse.ok(result);
    }

    private Long getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

---

## 节点⑦：Admin Console 运营后台 + AI 助手（7 个文件）

这个节点回答：**"出问题了谁来查？"**

### 7.1 `AdminUser.java`

```java
@Entity
@Table(name = "admin_users")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 20)
    private String role = "OPERATOR";  // ADMIN | OPERATOR

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
```

**注意：管理员和商户使用不同的表**——`admin_users` 和 `merchants`。它们的认证逻辑也是分开的，admin 走 `AdminService.adminLogin()`。

### 7.2 `AdminService.java`

```java
@Service
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // ---- 管理员登录 ----

    public Map<String, String> adminLogin(String username, String password) {
        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(40002, "用户名或密码错误"));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(40002, "用户名或密码错误");
            // ↑ 同样的安全策略：不区分用户名不存在还是密码错误
        }

        String token = jwtTokenProvider.generateAdminToken(
                admin.getId(), admin.getUsername(), admin.getRole());
        // ↑ 用的是 generateAdminToken，不是 generateMerchantToken

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("username", admin.getUsername());
        result.put("role", admin.getRole());
        return result;
    }

    // ---- 商户管理 ----

    public Page<Merchant> listMerchants(int page, int size) {
        return merchantRepository.findAll(PageRequest.of(page, size));
    }

    // ---- 订单管理 ----

    public Page<PaymentOrder> listOrders(String status, int page, int size) {
        // 简化：真实系统用 Specification 或 QueryDSL 做动态查询
        return paymentRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * 查看任意订单——管理员不受商户归属限制
     * 对比 PaymentService.queryPayment() 有 merchantId 校验
     */
    public PaymentOrder getOrderDetail(String orderNo) {
        return paymentRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(40003, "订单不存在"));
    }

    // ---- 结算管理 ----

    public Page<Settlement> listSettlements(int page, int size) {
        return settlementRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * 确认结算完成——模拟向商户打款
     */
    public void completeSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(40003, "结算记录不存在"));
        settlement.setStatus("COMPLETED");
        settlementRepository.save(settlement);
    }
}
```

### 7.3 `AdminController.java`

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/merchants")
    public ApiResponse<Page<Merchant>> listMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.listMerchants(page, size));
    }

    @GetMapping("/orders")
    public ApiResponse<Page<PaymentOrder>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.listOrders(status, page, size));
    }

    /**
     * 订单详情——管理员可以查看任何商户的订单（不校验归属）
     */
    @GetMapping("/orders/{orderNo}")
    public ApiResponse<PaymentOrder> getOrderDetail(@PathVariable String orderNo) {
        return ApiResponse.ok(adminService.getOrderDetail(orderNo));
    }

    @GetMapping("/settlements")
    public ApiResponse<Page<Settlement>> listSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.listSettlements(page, size));
    }

    @PostMapping("/settlement/{id}/complete")
    public ApiResponse<String> completeSettlement(@PathVariable Long id) {
        adminService.completeSettlement(id);
        return ApiResponse.ok("结算已确认完成");
    }
}
```

### 7.4 权限校验的完整链路

```
请求: GET /api/admin/orders/PAY20240803000001
       Header: Authorization: Bearer <admin_token>

  → JwtAuthenticationFilter
      parseToken → role = "ADMIN"
      SecurityContext.setAuthentication(userId, "ROLE_ADMIN")

  → SecurityConfig
      .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OPERATOR")
      role 是 ROLE_ADMIN ✓ → 放行

  → AdminController.getOrderDetail()
      → AdminService.getOrderDetail()
```

如果有人拿商户 token（role=MERCHANT）访问 `/api/admin/**`，SecurityConfig 直接返回 403 Forbidden，根本不会进入 Controller。

---

### 7.5 `AiAssistantService.java`——AI 支付助手

```java
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    private final PaymentRepository paymentRepository;

    // 匹配订单号：PAY + 8位日期 + 6位数字
    private static final Pattern ORDER_NO_PATTERN =
            Pattern.compile("(PAY\\d{14})");

    /**
     * 处理自然语言查询
     *
     * === 为什么运营系统需要 AI 助手？ ===
     * 运营人员每天面对成百上千笔订单。当一笔订单出问题时，
     * 需要：记住订单号 → 后台搜索 → 看状态/日志/渠道返回 → 判断原因 → 回复客户
     *
     * AI 助手让运营直接输入自然语言：
     * "订单 PAY20240803001 为什么失败了？"
     * → AI 自动查数据库、分析状态、给出建议
     *
     * === 实现说明 ===
     * 本学习项目用规则引擎模拟 AI：
     * 1. 正则提取订单号
     * 2. 查数据库
     * 3. 按模板生成回答
     *
     * 真实项目会替换为调用 AI API（如 Claude API），
     * 让模型理解更复杂的查询意图。
     */
    public String answer(String question) {
        log.info("AI Assistant received: {}", question);

        // 步骤1: 尝试从问题中提取订单号
        String orderNo = extractOrderNo(question);
        // "订单 PAY20240803000001 为什么失败？" → "PAY20240803000001"

        if (orderNo == null) {
            return buildNoOrderFoundResponse(question);
        }

        // 步骤2: 查询订单
        PaymentOrder order = paymentRepository.findByOrderNo(orderNo).orElse(null);

        if (order == null) {
            return "未找到订单 " + orderNo + "。\n\n"
                    + "请检查订单号是否正确。订单号格式为 PAY + 日期 + 序号，例如 PAY20240803000001。";
        }

        // 步骤3: 按订单状态生成分析
        return buildOrderAnalysis(order, question);
    }

    private String extractOrderNo(String text) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 当没找到订单号时的回答——根据问题中的关键词给出引导
     */
    private String buildNoOrderFoundResponse(String question) {
        if (question.contains("失败") || question.contains("FAILED")) {
            return "你想查询哪些失败的订单？请提供具体的订单号（如 PAY20240803000001）。\n\n"
                    + "你也可以在运营后台的订单管理页面按状态筛选失败订单。";
        }
        if (question.contains("结算") || question.contains("settlement")) {
            return "结算相关查询请前往【结算管理】页面查看。\n"
                    + "你可以查看每个商户的结算记录，包括交易总额、手续费和实际到账金额。";
        }
        return "我可以帮你查询支付订单的状态和失败原因。\n\n"
                + "请提供订单号，例如：\n"
                + "- \"订单 PAY20240803000001 为什么失败了？\"\n"
                + "- \"帮我查一下 PAY20240803000001 的状态\"\n"
                + "- \"PAY20240803000001 到账了吗？\"";
    }

    /**
     * 按订单状态生成详细分析——Markdown 格式输出
     */
    private String buildOrderAnalysis(PaymentOrder order, String question) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 订单 ").append(order.getOrderNo()).append(" 分析\n\n");

        // 基本信息表格
        sb.append("| 字段 | 值 |\n");
        sb.append("|------|----|\n");
        sb.append("| 订单号 | ").append(order.getOrderNo()).append(" |\n");
        sb.append("| 金额 | ").append(order.getAmount())
          .append(" ").append(order.getCurrency()).append(" |\n");
        sb.append("| 当前状态 | **").append(order.getStatus()).append("** |\n");
        if (order.getChannel() != null) {
            sb.append("| 支付渠道 | ").append(order.getChannel()).append(" |\n");
        }
        if (order.getChannelOrderNo() != null) {
            sb.append("| 渠道订单号 | ").append(order.getChannelOrderNo()).append(" |\n");
        }
        sb.append("| 创建时间 | ").append(order.getCreatedAt()).append(" |\n");
        if (order.getCallbackReceivedAt() != null) {
            sb.append("| 回调时间 | ").append(order.getCallbackReceivedAt()).append(" |\n");
        }
        sb.append("\n");

        // 状态分析 & 建议
        sb.append("### 状态分析\n\n");

        switch (order.getStatus()) {
            case "CREATED":
                sb.append("该订单已创建但尚未发送到支付渠道。\n\n");
                sb.append("**可能原因：** 系统队列延迟或处理中断。\n\n");
                sb.append("**建议操作：** 等待系统自动处理。如果超过 5 分钟仍未变化，请联系技术支持。\n");
                break;

            case "PROCESSING":
                sb.append("该订单已发送到支付渠道（").append(order.getChannel())
                  .append("），正在等待渠道返回结果。\n\n");
                sb.append("**可能原因：** 渠道处理中，或回调尚未到达。\n\n");
                sb.append("**建议操作：** \n");
                sb.append("1. 等待 1-2 分钟，渠道通常在 30 秒内返回\n");
                sb.append("2. 超过 5 分钟可联系渠道确认订单状态\n");
                sb.append("3. 若渠道确认已处理但平台未更新，可能为回调丢失，需手动补单\n");
                break;

            case "SUCCESS":
                sb.append("✅ 该订单已成功支付。\n\n");
                sb.append("**建议操作：** 无需处理。订单将在 T+1 日进入结算流程，"
                        + "商户届时将收到款项（扣除手续费）。\n");
                break;

            case "FAILED":
                sb.append("❌ 该订单支付失败。\n\n");
                if (order.getFailReason() != null) {
                    sb.append("**失败原因：** ").append(order.getFailReason()).append("\n\n");
                    sb.append(getFailReasonAdvice(order.getFailReason()));
                }
                sb.append("**建议操作：** \n");
                sb.append("1. 告知买家支付失败，建议更换支付方式或联系发卡行\n");
                sb.append("2. 商户可重新发起一笔新的支付订单\n");
                sb.append("3. 如买家确认已扣款但订单失败，请升级至技术支持核查\n");
                break;
        }

        return sb.toString();
    }

    /**
     * 根据失败原因给出具体建议——这是 AI 助手的核心价值
     */
    private String getFailReasonAdvice(String failReason) {
        if (failReason.contains("Insufficient")) {
            return "**解读：** 买家账户余额不足。\n\n";
        } else if (failReason.contains("expired")) {
            return "**解读：** 支付卡已过期。通知买家更换有效卡片。\n\n";
        } else if (failReason.contains("declined")) {
            return "**解读：** 发卡行拒绝了该笔交易。可能原因：风控拦截、超出限额。"
                    + "建议买家联系发卡行。\n\n";
        } else if (failReason.contains("Do not honor")) {
            return "**解读：** 发卡行出于安全原因拒绝了交易（通用拒绝码）。"
                    + "建议买家联系发卡行获取具体原因。\n\n";
        }
        return "**解读：** 支付渠道返回失败。\n\n";
    }
}
```

**为什么 AI 助手是顶层包 `com.crosspay.ai` 而不是放在 `module.admin` 下面？**

因为它要跨模块访问——它依赖 `PaymentRepository`（查订单）、将来还要依赖 `MerchantRepository`（查商户）、`SettlementRepository`（查结算）。它不是 admin 模块的附属品，它是横跨所有模块的能力。这是 ADR-6 的体现。

**真实替换方案：** 当你想接入真实 LLM 时——
```java
// 替换前（规则引擎）
String orderNo = extractOrderNo(question);

// 替换后（LLM + 结构化输出）
String orderNo = llmClient.call(
    "Extract the payment order number from: " + question,
    OrderNoExtraction.class
).getOrderNo();
```
只改 `AiAssistantService`，Controller 和前端一行不动。

### 7.6 `AiAssistantController.java`

```java
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/query")
    public ApiResponse<Map<String, String>> query(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        String answer = aiAssistantService.answer(question);

        Map<String, String> result = Map.of(
                "question", question,
                "answer", answer
        );
        return ApiResponse.ok(result);
    }
}
```

---

## 完整调用链路

把上面所有节点串起来，一笔支付从前端点击"发起支付"到结算完成的完整调用栈：

```
前端 fetch('/api/payment/create', {amount: 100, currency: 'USD'})
  │  Header: Authorization: Bearer <merchant_jwt>
  │
  ▼
JwtAuthenticationFilter.doFilterInternal()
  ├─ 解析 JWT → userId=1, role=MERCHANT
  └─ SecurityContext.setAuthentication(1, ROLE_MERCHANT)
  │
  ▼
SecurityConfig 放行 (/api/payment/** + ROLE_MERCHANT)
  │
  ▼
PaymentController.create(req)
  ├─ getCurrentMerchantId() → 1
  └─ paymentService.createPayment(1, req)
       │
       ├─ merchantRepository.findById(1) → Merchant{status=ACTIVE, feeRate=0.03}
       ├─ OrderNoGenerator.generatePaymentNo() → "PAY20240804000001"
       ├─ paymentRepository.save(order)  // INSERT, status=CREATED
       ├─ order.markProcessing("MOCK_AFRICA")  // CREATED→PROCESSING
       ├─ gatewayRouter.route(req)
       │    └─ mockAfricaGateway.pay(req)
       │         ├─ simulateLatency(300-800ms)
       │         ├─ random < 80 → SUCCESS
       │         └─ return GatewayPayResponse{status: "SUCCESS", channelOrderNo: "AFR_..."}
       ├─ order.markSuccess("AFR_PAY20240804000001")  // PROCESSING→SUCCESS
       ├─ paymentRepository.save(order)  // UPDATE
       └─ return PaymentResponse{orderNo, status:"SUCCESS"}
  │
  ▼
ApiResponse.ok(paymentResponse)
  └─ JSON: {code:200, message:"success", data:{orderNo:"PAY...", status:"SUCCESS"}}

---
T+1 结算:
  POST /api/settlement/trigger
    └─ settlementService.createSettlement(1)
         ├─ 查找所有 SUCCESS 订单 → 10 笔，总额 $1,000
         ├─ feeAmount = $1,000 × 3% = $30
         ├─ netAmount = $1,000 − $30 = $970
         ├─ INSERT INTO settlements
         └─ INSERT INTO settlement_details (×10)
```

---

## 文件索引（43 个 Java 文件）

```
crosspay-backend/src/main/java/com/crosspay/

├── CrossPayApplication.java              # Spring Boot 入口

├── common/
│   ├── config/
│   │   ├── SecurityConfig.java           # Spring Security + JWT + CORS
│   │   └── RedisConfig.java              # Redis 连接配置
│   ├── dto/
│   │   └── ApiResponse.java              # 统一响应 {code, message, data}
│   ├── exception/
│   │   ├── BusinessException.java        # 业务异常（带错误码）
│   │   └── GlobalExceptionHandler.java   # 全局异常 → JSON 响应
│   └── util/
│       └── OrderNoGenerator.java         # 订单号/商户号/结算号生成器

├── module/
│   ├── auth/                             # 节点①
│   │   ├── AuthController.java           # /api/auth/login, /register
│   │   ├── AuthService.java              # 注册、登录逻辑
│   │   ├── JwtTokenProvider.java         # JWT 生成、解析、验证
│   │   ├── JwtAuthenticationFilter.java  # 每个请求拦截 + 解析 JWT
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       └── RegisterRequest.java
│   │
│   ├── merchant/                         # 节点②
│   │   ├── MerchantController.java       # /api/merchant/profile
│   │   ├── MerchantService.java          # 商户查询 + Dashboard
│   │   ├── MerchantRepository.java       # JPA Repository
│   │   └── entity/
│   │       └── Merchant.java             # 商户实体（含 feeRate）
│   │
│   ├── payment/                          # 节点③（核心）
│   │   ├── PaymentController.java        # /api/payment/create, /query
│   │   ├── PaymentService.java           # 支付创建 + 状态流转
│   │   ├── PaymentRepository.java        # JPA Repository
│   │   ├── entity/
│   │   │   └── PaymentOrder.java         # 支付订单 + 状态机
│   │   └── dto/
│   │       ├── CreatePaymentRequest.java
│   │       └── PaymentResponse.java      # Entity → DTO 转换
│   │
│   ├── gateway/                          # 节点④ + ⑤
│   │   ├── PaymentGateway.java           # 渠道统一接口
│   │   ├── GatewayRouter.java            # 渠道选择器
│   │   ├── MockAfricaGateway.java        # 模拟非洲支付渠道
│   │   ├── CallbackController.java       # /api/callback/{channel}
│   │   └── dto/
│   │       ├── GatewayPayRequest.java
│   │       └── GatewayPayResponse.java
│   │
│   ├── settlement/                       # 节点⑥
│   │   ├── SettlementController.java     # /api/settlement/trigger
│   │   ├── SettlementService.java        # 结算计算引擎
│   │   ├── SettlementRepository.java
│   │   ├── SettlementDetailRepository.java
│   │   ├── entity/
│   │   │   ├── Settlement.java           # 结算记录
│   │   │   └── SettlementDetail.java     # 结算-订单关联
│   │   └── dto/
│   │       └── SettlementResponse.java
│   │
│   └── admin/                            # 节点⑦（部分）
│       ├── AdminController.java          # /api/admin/*
│       ├── AdminService.java             # 运营管理逻辑
│       ├── AdminUserRepository.java
│       └── entity/
│           └── AdminUser.java            # 管理员实体

└── ai/                                   # 节点⑦（AI 助手）
    ├── AiAssistantController.java        # /api/ai/query
    └── AiAssistantService.java           # 自然语言 → 订单分析
```

---

*本文档是 CrossPay Learning Project 的学习资料之一。配合 `docs/business-flow.md` 阅读效果最佳。*
