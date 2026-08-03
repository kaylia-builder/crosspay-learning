package com.crosspay.ai;

import com.crosspay.module.payment.PaymentRepository;
import com.crosspay.module.payment.entity.PaymentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 支付助手
 *
 * === 为什么运营系统需要 AI 助手？ ===
 *
 * 支付公司的运营人员每天面对成百上千笔订单。
 * 当一笔订单出问题时（客户投诉没到账、商户问为什么失败），
 * 运营需要：
 * 1. 记住订单号
 * 2. 到后台搜索
 * 3. 看状态、看日志、看渠道返回
 * 4. 判断原因并回复客户
 *
 * 一个 AI 助手可以让运营直接输入自然语言查询：
 * "订单 PAY20240803001 为什么失败了？"
 *
 * AI 自动查数据库、分析状态、给出建议 → 效率提升 10 倍。
 *
 * === 实现说明 ===
 *
 * 本学习项目用规则引擎模拟 AI：
 * 1. 正则提取订单号
 * 2. 查数据库
 * 3. 按模板生成回答
 *
 * 真实项目会替换为调用 AI API（如 Claude API），
 * 让模型理解更复杂的查询意图。
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final PaymentRepository paymentRepository;

    // 匹配订单号：PAY + 8位日期 + 6位数字
    private static final Pattern ORDER_NO_PATTERN =
            Pattern.compile("(PAY\\d{14})");

    public AiAssistantService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * 处理自然语言查询
     *
     * @param question 运营人员的自然语言问题
     * @return AI 生成的回答
     */
    public String answer(String question) {
        log.info("AI Assistant received: {}", question);

        // 1. 尝试从问题中提取订单号
        String orderNo = extractOrderNo(question);

        if (orderNo == null) {
            return buildNoOrderFoundResponse(question);
        }

        // 2. 查询订单
        PaymentOrder order = paymentRepository.findByOrderNo(orderNo).orElse(null);

        if (order == null) {
            return "未找到订单 " + orderNo + "。\n\n"
                    + "请检查订单号是否正确。订单号格式为 PAY + 日期 + 序号，例如 PAY20240803000001。";
        }

        // 3. 按订单状态生成分析
        return buildOrderAnalysis(order, question);
    }

    /**
     * 从问题文本中提取订单号
     */
    private String extractOrderNo(String text) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 当没找到订单号时的回答
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
                + "- "订单 PAY20240803000001 为什么失败了？"\n"
                + "- "帮我查一下 PAY20240803000001 的状态"\n"
                + "- "PAY20240803000001 到账了吗？"";
    }

    /**
     * 按订单状态生成详细分析
     */
    private String buildOrderAnalysis(PaymentOrder order, String question) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 订单 ").append(order.getOrderNo()).append(" 分析\n\n");

        // 基本信息
        sb.append("| 字段 | 值 |\n");
        sb.append("|------|----|\n");
        sb.append("| 订单号 | ").append(order.getOrderNo()).append(" |\n");
        sb.append("| 金额 | ").append(order.getAmount()).append(" ").append(order.getCurrency()).append(" |\n");
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
                sb.append("该订单已发送到支付渠道（").append(order.getChannel()).append("），正在等待渠道返回结果。\n\n");
                sb.append("**可能原因：** 渠道处理中，或回调尚未到达。\n\n");
                sb.append("**建议操作：** \n");
                sb.append("1. 等待 1-2 分钟，渠道通常在 30 秒内返回\n");
                sb.append("2. 超过 5 分钟可联系渠道确认订单状态\n");
                sb.append("3. 若渠道确认已处理但平台未更新，可能为回调丢失，需手动补单\n");
                break;

            case "SUCCESS":
                sb.append("✅ 该订单已成功支付。\n\n");
                sb.append("**建议操作：** 无需处理。订单将在 T+1 日进入结算流程，商户届时将收到款项（扣除手续费）。\n");
                break;

            case "FAILED":
                sb.append("❌ 该订单支付失败。\n\n");
                if (order.getFailReason() != null) {
                    sb.append("**失败原因：** ").append(order.getFailReason()).append("\n\n");
                    sb.append(getFailReasonAdvice(order.getFailReason()));
                } else {
                    sb.append("**失败原因：** 未记录（可能是系统异常导致）\n\n");
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
     * 根据失败原因给出具体建议
     */
    private String getFailReasonAdvice(String failReason) {
        if (failReason.contains("Insufficient")) {
            return "**解读：** 买家账户余额不足。\n\n";
        } else if (failReason.contains("expired")) {
            return "**解读：** 支付卡已过期。通知买家更换有效卡片。\n\n";
        } else if (failReason.contains("declined")) {
            return "**解读：** 发卡行拒绝了该笔交易。可能原因：风控拦截、超出限额。建议买家联系发卡行。\n\n";
        } else if (failReason.contains("Do not honor")) {
            return "**解读：** 发卡行出于安全原因拒绝了交易（通用拒绝码）。建议买家联系发卡行获取具体原因。\n\n";
        }
        return "**解读：** 支付渠道返回失败。\n\n";
    }
}
