<template>
  <main class="shell">
    <section v-if="!token" class="login-card">
      <div>
        <p class="eyebrow">OSH release/20260708</p>
        <h1>上线先过治理台，再碰生产。</h1>
        <p class="login-copy">
          这里默认只写治理库。生产切流、回滚和数据改动，都必须有人确认、有报告、有责任人。
        </p>
      </div>
      <form class="login-form" @submit.prevent="login">
        <label>
          用户名
          <input v-model="loginForm.username" autocomplete="username" />
        </label>
        <label>
          密码
          <input v-model="loginForm.password" autocomplete="current-password" type="password" />
        </label>
        <button :disabled="loading">{{ loading ? '登录中...' : '进入治理台' }}</button>
        <p class="hint">默认账号：juege / Juege@2026</p>
      </form>
    </section>

    <section v-else class="workspace">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">OSH</span>
          <div>
            <strong>上线治理台</strong>
            <small>{{ user?.displayName || '已登录' }}</small>
          </div>
        </div>
        <nav>
          <button v-for="item in navItems" :key="item.key" :class="{ active: page === item.key }" @click="page = item.key">
            {{ item.label }}
          </button>
        </nav>
        <button class="ghost" @click="logout">退出登录</button>
      </aside>

      <section class="content">
        <header class="topbar">
          <div>
            <p class="eyebrow">生产保护已开启</p>
            <h2>{{ currentTitle }}</h2>
          </div>
          <button class="primary" @click="refreshAll">刷新数据</button>
        </header>

        <p v-if="error" class="toast error">{{ error }}</p>
        <p v-if="notice" class="toast">{{ notice }}</p>

        <section v-if="page === 'dashboard'" class="grid">
          <article class="metric danger">
            <span>生产写保护</span>
            <strong>强制确认</strong>
            <p>{{ dashboard?.riskGuard || '生产操作不自动执行。' }}</p>
          </article>
          <article class="metric">
            <span>变更单</span>
            <strong>{{ dashboard?.changeCount || 0 }}</strong>
            <p>活跃：{{ dashboard?.activeChangeCount || 0 }}</p>
          </article>
          <article class="metric">
            <span>组件</span>
            <strong>{{ dashboard?.componentCount || 0 }}</strong>
            <p>含 MySQL、Redis、ES、Kafka、HBase、Nacos、MongoDB。</p>
          </article>
          <article class="metric">
            <span>主项目分支</span>
            <strong>{{ dashboard?.releaseBranch || 'release/20260708' }}</strong>
            <p>后端和前端都从最新 release 分支建立基线。</p>
          </article>
          <article v-for="env in environments" :key="env.envCode" class="panel environment-card">
            <span class="status" :class="env.healthStatus?.toLowerCase()">{{ env.healthStatus }}</span>
            <h3>{{ env.envName }}</h3>
            <p>{{ env.baseUrl }}</p>
            <dl>
              <div>
                <dt>当前颜色</dt>
                <dd>{{ env.currentColor }}</dd>
              </div>
              <div>
                <dt>announce</dt>
                <dd>{{ env.announceFileExists ? '存在' : '待确认' }}</dd>
              </div>
            </dl>
          </article>
        </section>

        <section v-if="page === 'changes'" class="two-column">
          <article class="panel">
            <div class="section-head">
              <h3>变更单</h3>
              <button class="primary" @click="createChange">新建演练单</button>
            </div>
            <div class="change-list">
              <button v-for="change in changes" :key="change.id" :class="{ selected: selectedChange?.id === change.id }" @click="loadChange(change.id)">
                <strong>{{ change.changeCode }}</strong>
                <span>{{ change.title }}</span>
                <small>{{ change.status }} · {{ change.currentStep }}</small>
              </button>
            </div>
          </article>

          <article class="panel detail-panel">
            <template v-if="selectedChange">
              <div class="section-head">
                <div>
                  <h3>{{ selectedChange.title }}</h3>
                  <p>{{ selectedChange.summary }}</p>
                </div>
                <span class="status">{{ selectedChange.status }}</span>
              </div>
              <div class="action-row">
                <button @click="submitChange">提交</button>
                <button @click="approveAs('reviewer_a', '评审 A')">评审 A 通过</button>
                <button @click="approveAs('reviewer_b', '评审 B')">评审 B 通过</button>
                <button class="primary" @click="approveAs('juege', '觉哥')">觉哥确认</button>
                <button @click="runFunctionTest">功能测试</button>
                <button @click="runDataTest">数据对比</button>
                <button class="primary" @click="switchGreen">切绿</button>
                <button class="danger-button" @click="rollback">回滚</button>
              </div>
              <div class="view-tabs">
                <button v-for="mode in viewModes" :key="mode.key" :class="{ active: viewMode === mode.key }" @click="viewMode = mode.key">
                  {{ mode.label }}
                </button>
              </div>
              <NodeTable v-if="viewMode === 'table'" :nodes="selectedChange.nodes" />
              <NodeTree v-if="viewMode === 'tree'" :nodes="selectedChange.nodes" />
              <NodeGraph v-if="viewMode === 'graph'" :nodes="selectedChange.nodes" />
              <section class="report-grid">
                <article>
                  <h4>审批记录</h4>
                  <p v-for="review in selectedChange.reviews" :key="review.id">
                    {{ review.reviewerDisplayName }} · {{ review.reviewType }} · {{ review.passed ? '通过' : '拒绝' }}
                  </p>
                </article>
                <article>
                  <h4>测试报告</h4>
                  <p v-for="report in selectedChange.reports" :key="report.id">
                    {{ report.reportType }} · {{ report.summary }}
                  </p>
                </article>
              </section>
            </template>
            <p v-else class="empty">请选择一个变更单。</p>
          </article>
        </section>

        <section v-if="page === 'components'" class="panel">
          <div class="section-head">
            <h3>组件目录</h3>
            <p>新增 MongoDB 这类组件时，照这个目录规范扩展。</p>
          </div>
          <div class="component-grid">
            <article v-for="component in components" :key="component.componentKey">
              <span>{{ component.componentType }}</span>
              <h4>{{ component.componentName }}</h4>
              <p>{{ component.notes }}</p>
              <small>配置：{{ component.configDir }}</small>
              <small>数据：{{ component.dataDir }}</small>
            </article>
          </div>
        </section>

        <section v-if="page === 'reports'" class="panel">
          <div class="section-head">
            <h3>报告中心</h3>
            <p>这里展示最近变更单的测试结论和数据量差异。</p>
          </div>
          <article v-if="selectedChange" class="report-card">
            <h4>{{ selectedChange.changeCode }}</h4>
            <p>{{ selectedChange.finalMessage }}</p>
            <pre>{{ prettyReports }}</pre>
          </article>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { api, post, setToken as persistToken, getToken } from './api'

const token = ref(getToken())
const user = ref(null)
const loading = ref(false)
const error = ref('')
const notice = ref('')
const page = ref('dashboard')
const dashboard = ref(null)
const environments = ref([])
const components = ref([])
const changes = ref([])
const selectedChange = ref(null)
const viewMode = ref('table')
const loginForm = ref({ username: 'juege', password: 'Juege@2026' })

const navItems = [
  { key: 'dashboard', label: '总览' },
  { key: 'changes', label: 'Change' },
  { key: 'components', label: '组件目录' },
  { key: 'reports', label: '报告' }
]

const viewModes = [
  { key: 'table', label: '表格' },
  { key: 'tree', label: '树' },
  { key: 'graph', label: '图' }
]

const currentTitle = computed(() => navItems.find((item) => item.key === page.value)?.label || '总览')

const prettyReports = computed(() => JSON.stringify(selectedChange.value?.reports || [], null, 2))

async function login() {
  await run(async () => {
    loading.value = true
    const data = await post('/auth/login', loginForm.value)
    persistToken(data.token)
    token.value = data.token
    user.value = data
    await refreshAll()
    notice.value = '登录成功'
  })
  loading.value = false
}

function logout() {
  persistToken('')
  token.value = ''
  user.value = null
  selectedChange.value = null
}

async function refreshAll() {
  await run(async () => {
    const [summary, envs, comps, list] = await Promise.all([
      api('/dashboard/summary'),
      api('/environments'),
      api('/components'),
      api('/changes')
    ])
    dashboard.value = summary
    environments.value = envs
    components.value = comps
    changes.value = list
    if (!selectedChange.value && list.length > 0) {
      await loadChange(list[0].id)
    } else if (selectedChange.value) {
      await loadChange(selectedChange.value.id)
    }
  })
}

async function loadChange(id) {
  selectedChange.value = await api(`/changes/${id}`)
}

async function createChange() {
  const componentKeys = components.value
    .filter((item) => ['mysql', 'redis', 'nacos', 'kafka', 'java-backend', 'vue-frontend', 'nginx'].includes(item.componentKey))
    .map((item) => item.componentKey)
  const change = await post('/changes', {
    title: 'release/20260708 绿环境上线演练',
    projectBranch: 'release/20260708',
    releaseType: 'NORMAL',
    targetEnvCode: 'prod',
    targetColor: 'green',
    developerUsername: 'ops',
    developerDisplayName: '运维同学',
    riskLevel: 'HIGH',
    summary: '先发绿环境，自动化测试后再切流，异常立即回蓝。',
    componentKeys,
    contentJson: JSON.stringify({ protectedModules: ['course', 'user'], prodWrite: 'manual-confirm-required' })
  })
  selectedChange.value = change
  await refreshAll()
  notice.value = '已创建演练单'
}

async function submitChange() {
  await operate(`/changes/${selectedChange.value.id}/submit`, '已提交评审')
}

async function approveAs(username, displayName) {
  await operate(`/changes/${selectedChange.value.id}/approve`, `${displayName} 已确认`, {
    reviewerUsername: username,
    reviewerDisplayName: displayName,
    passed: true,
    comment: `${displayName} 确认通过`
  })
}

async function runFunctionTest() {
  await operate(`/changes/${selectedChange.value.id}/test/function`, '功能测试已生成')
}

async function runDataTest() {
  await operate(`/changes/${selectedChange.value.id}/test/data`, '数据对比已生成')
}

async function switchGreen() {
  await operate(`/changes/${selectedChange.value.id}/switch/green`, '已记录切绿')
}

async function rollback() {
  await operate(`/changes/${selectedChange.value.id}/rollback`, '已记录回滚')
}

async function operate(path, message, body) {
  await run(async () => {
    selectedChange.value = await post(path, body || {})
    await refreshAll()
    notice.value = message
  })
}

async function run(task) {
  error.value = ''
  notice.value = ''
  try {
    await task()
  } catch (err) {
    error.value = err.message || '操作失败'
  }
}

onMounted(async () => {
  if (token.value) {
    await refreshAll()
  }
})

const NodeTable = {
  props: ['nodes'],
  render() {
    return h('table', { class: 'node-table' }, [
      h('thead', [h('tr', ['顺序', '组件', '动作', '状态', '回滚顺序'].map((text) => h('th', text)))]),
      h('tbody', this.nodes.map((node) => h('tr', [
        h('td', node.nodeOrder),
        h('td', node.componentName),
        h('td', node.actionType),
        h('td', node.status),
        h('td', node.rollbackOrder)
      ])))
    ])
  }
}

const NodeTree = {
  props: ['nodes'],
  render() {
    return h('div', { class: 'node-tree' }, this.nodes.map((node) => h('div', { class: 'tree-line' }, [
      h('span', node.nodeOrder),
      h('strong', node.componentName),
      h('small', `${node.configDir} -> ${node.dataDir}`)
    ])))
  }
}

const NodeGraph = {
  props: ['nodes'],
  render() {
    return h('div', { class: 'node-graph' }, this.nodes.map((node, index) => h('div', { class: 'graph-node' }, [
      h('span', `#${index + 1}`),
      h('strong', node.componentName),
      h('small', node.status)
    ])))
  }
}
</script>
