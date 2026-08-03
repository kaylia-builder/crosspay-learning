<template>
  <div>
    <h2>🤖 AI 支付助手</h2>
    <p style="color: #999; margin-bottom: 20px">
      用自然语言查询订单状态。例如：「订单 PAY20240803000001 为什么失败了？」「帮我查一下 PAY20240803000001」
    </p>

    <div style="max-width: 700px">
      <el-input
        v-model="question"
        placeholder="输入你的问题，例如：订单 PAY20240803000001 为什么失败？"
        :rows="2"
        type="textarea"
      />
      <el-button type="primary" :loading="loading" @click="ask" style="margin-top: 10px">
        查询
      </el-button>
    </div>

    <el-card v-if="answer" style="margin-top: 20px; max-width: 700px" shadow="hover">
      <template #header>AI 分析结果</template>
      <div style="white-space: pre-wrap; font-size: 14px; line-height: 1.8" v-html="renderedAnswer"></div>
    </el-card>

    <el-card style="margin-top: 20px; max-width: 700px" shadow="never">
      <template #header>💡 使用示例</template>
      <el-tag
        v-for="q in examples"
        :key="q"
        style="margin: 5px; cursor: pointer"
        @click="question = q"
      >
        {{ q }}
      </el-tag>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { aiQuery } from '@/api/admin'

const question = ref('')
const answer = ref('')
const loading = ref(false)

const examples = [
  '订单 PAY20240803000001 为什么失败？',
  '帮我查一下 PAY20240803000001 的状态',
  '最近有哪些失败的订单？',
  '商户的结算情况怎么样？',
]

// Simple markdown-like rendering (table + bold)
function renderMarkdown(text: string): string {
  return text
    .replace(/## (.*)/g, '<h3>$1</h3>')
    .replace(/### (.*)/g, '<h4>$1</h4>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/✅/g, '<span style="font-size:18px">✅</span>')
    .replace(/❌/g, '<span style="font-size:18px">❌</span>')
    .replace(/\|(.+)\|/g, (match) => {
      const cells = match.split('|').filter(c => c.trim())
      return '<tr>' + cells.map(c => {
        const trimmed = c.trim()
        return trimmed.startsWith('--') ? '<th></th>' : `<td style="padding:4px 12px;border:1px solid #ddd">${trimmed}</td>`
      }).join('') + '</tr>'
    })
    .replace(/(<tr>.*<\/tr>\n?)+/g, '<table style="border-collapse:collapse;margin:10px 0">$&</table>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/\n/g, '<br/>')
}

const renderedAnswer = ref('')

async function ask() {
  if (!question.value.trim()) return
  loading.value = true
  try {
    const res = await aiQuery(question.value)
    answer.value = res.data.data.answer
    renderedAnswer.value = renderMarkdown(answer.value)
  } finally {
    loading.value = false
  }
}
</script>
