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
        <p class="hint">默认账号：juege，密码不在这里写。</p>
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
              <section class="payload-guide">
                <article v-for="entry in payloadGuides" :key="entry.type">
                  <strong>{{ entry.title }}</strong>
                  <p>{{ entry.copy }}</p>
                  <button @click="startNewItem(entry.type)">{{ entry.button }}</button>
                </article>
              </section>
              <div class="action-row">
                <button @click="submitChange">提交</button>
                <button class="primary" @click="startNewItem('SQL')">新增 SQL</button>
                <button @click="startNewItem('CONFIG')">新增配置</button>
                <button @click="startNewItem('CODE')">新增代码</button>
                <button @click="recordDemo">演示确认</button>
                <button @click="approveAs('reviewer_a', '评审 A')">评审 A 通过</button>
                <button @click="approveAs('reviewer_b', '评审 B')">评审 B 通过</button>
                <button class="primary" @click="approveAs('juege', '觉哥')">觉哥确认</button>
                <button @click="validateSpecs">规范校验</button>
                <button @click="recordAllReviewerTests">补齐评审测试</button>
                <button @click="runEnvDiff">环境差异</button>
                <button @click="runAnnounceCheck">announce</button>
                <button @click="runFunctionTest">功能测试</button>
                <button @click="runDataTest">数据对比</button>
                <button class="primary" @click="switchGreen">切绿</button>
                <button @click="manualVerify">生产人工验证</button>
                <button @click="syncBlue">同步蓝</button>
                <button @click="switchBlue">回蓝</button>
                <button class="danger-button" @click="rollback">回滚</button>
              </div>
              <div class="view-tabs">
                <button v-for="mode in viewModes" :key="mode.key" :class="{ active: viewMode === mode.key }" @click="viewMode = mode.key">
                  {{ mode.label }}
                </button>
              </div>
              <NodeTable
                v-if="viewMode === 'table'"
                :nodes="selectedChange.nodes"
                :items="selectedChange.items"
                @edit-item="openItemEditor"
                @operate-item="operateItem"
              />
              <NodeTree v-if="viewMode === 'tree'" :nodes="selectedChange.nodes" :items="selectedChange.items" />
              <NodeGraph v-if="viewMode === 'graph'" :nodes="selectedChange.nodes" :items="selectedChange.items" />
              <form v-if="creatingItem" class="item-editor payload-editor" @submit.prevent="createItem">
                <div class="section-head">
                  <h4>新增上线项：{{ itemTypeName(itemForm.itemType) }}</h4>
                  <button type="button" class="ghost" @click="creatingItem = false">收起</button>
                </div>
                <div class="form-grid">
                  <label>
                    类型
                    <select v-model="itemForm.itemType" @change="applyItemTypeDefaults">
                      <option value="SQL">SQL</option>
                      <option value="CONFIG">配置</option>
                      <option value="CODE">代码</option>
                      <option value="COMPONENT">组件</option>
                    </select>
                  </label>
                  <label>
                    组件
                    <select v-model="itemForm.componentKey">
                      <option v-for="component in components" :key="component.componentKey" :value="component.componentKey">
                        {{ component.componentName }}
                      </option>
                    </select>
                  </label>
                  <label>
                    标题
                    <input v-model="itemForm.title" />
                  </label>
                  <label>
                    负责人
                    <input v-model="itemForm.ownerDisplayName" />
                  </label>
                </div>
                <PayloadFields v-model:item="itemForm" />
                <button class="primary" type="submit">加入上线计划</button>
              </form>
              <form v-if="editingItem" class="item-editor" @submit.prevent="saveItem">
                <div class="section-head">
                  <h4>更新子 change：{{ editingItem.componentName }}</h4>
                  <button type="button" class="ghost" @click="editingItem = null">收起</button>
                </div>
                <label>
                  负责人
                  <input v-model="itemForm.ownerDisplayName" />
                </label>
                <label>
                  上线类型
                  <select v-model="itemForm.itemType">
                    <option value="SQL">SQL</option>
                    <option value="CONFIG">配置</option>
                    <option value="CODE">代码</option>
                    <option value="COMPONENT">组件</option>
                  </select>
                </label>
                <label>
                  载荷路径/文件/分支
                  <input v-model="itemForm.payloadPath" />
                </label>
                <label>
                  上线内容
                  <textarea v-model="itemForm.changeContent" rows="3" />
                </label>
                <PayloadFields v-model:item="itemForm" />
                <label>
                  增量计划
                  <textarea v-model="itemForm.incrementalPlan" rows="3" />
                </label>
                <label>
                  回滚计划
                  <textarea v-model="itemForm.rollbackPlan" rows="3" />
                </label>
                <label>
                  测试计划
                  <textarea v-model="itemForm.testPlan" rows="3" />
                </label>
                <label>
                  数据采集计划
                  <textarea v-model="itemForm.dataProbePlan" rows="3" />
                </label>
                <button class="primary" type="submit">保存子 change</button>
              </form>
              <section class="report-grid">
                <article>
                  <h4>审批记录</h4>
                  <p v-for="review in selectedChange.reviews" :key="review.id">
                    {{ review.reviewerDisplayName }} · {{ review.reviewType }} · {{ review.passed ? '通过' : '拒绝' }}
                  </p>
                </article>
                <article>
                  <h4>评审测试证据</h4>
                  <p v-for="evidence in selectedChange.evidences" :key="evidence.id">
                    {{ evidence.componentKey }} · {{ evidence.reviewerDisplayName }} · {{ evidence.environmentCode }} · {{ evidence.passed ? '通过' : '失败' }}
                  </p>
                </article>
                <article>
                  <h4>演示确认</h4>
                  <p v-for="demo in selectedChange.demos || []" :key="demo.id">
                    {{ demo.developerUsername }} → {{ demo.reviewerUsername }} · {{ demo.content }}
                  </p>
                  <p v-if="!(selectedChange.demos && selectedChange.demos.length)">还没有记录演示确认。</p>
                </article>
                <article>
                  <h4>测试报告</h4>
                  <p v-for="report in selectedChange.reports" :key="report.id">
                    {{ report.reportType }} · {{ report.summary }}
                  </p>
                </article>
                <article>
                  <h4>操作链</h4>
                  <p v-for="operation in selectedChange.operations" :key="operation.id">
                    {{ operation.operationType }} · {{ operation.operationStatus }} · {{ operation.safeMode ? '安全模式' : '真实执行' }}
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
            <div v-if="releaseGate" class="gate-card" :class="{ ready: releaseGate.readyForGreen }">
              <strong>{{ releaseGate.readyForGreen ? '可以切绿' : '暂不能切绿' }}</strong>
              <p>{{ releaseGate.prodSafety }}</p>
              <ul v-if="releaseGate.blockers && releaseGate.blockers.length">
                <li v-for="blocker in releaseGate.blockers" :key="blocker">{{ blocker }}</li>
              </ul>
            </div>
            <pre>{{ prettyReports }}</pre>
          </article>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, h, onMounted, ref, watch } from 'vue'
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
const releaseGate = ref(null)
const viewMode = ref('table')
const loginForm = ref({ username: 'juege', password: '' })
const creatingItem = ref(false)
const editingItem = ref(null)
const itemForm = ref({})

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

const payloadGuides = [
  {
    type: 'SQL',
    title: '上线几条 SQL',
    copy: '粘贴执行 SQL、回滚 SQL、影响行数和验证 SQL。平台会生成独立 MySQL 节点。',
    button: '新增 SQL'
  },
  {
    type: 'CONFIG',
    title: '更新组件配置',
    copy: '填配置路径、diff、回滚配置和刷新命令。Nacos、Nginx、Compose 都按配置项走。',
    button: '新增配置'
  },
  {
    type: 'CODE',
    title: '发布代码',
    copy: '填分支、commit 范围、改动大纲、疑似 bug 和构建产物。先发绿系统。',
    button: '新增代码'
  }
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
  await loadReleaseGate()
}

async function loadReleaseGate() {
  if (page.value === 'reports' && selectedChange.value) {
    releaseGate.value = await api(`/changes/${selectedChange.value.id}/reports`)
    return
  }
  releaseGate.value = null
}

async function createChange() {
  const componentKeys = components.value
    .filter((item) => ['mysql', 'redis', 'nacos', 'kafka', 'elasticsearch', 'hbase', 'java-backend', 'vue-frontend', 'nginx', 'docker-compose', 'mongodb'].includes(item.componentKey))
    .map((item) => item.componentKey)
  const change = await post('/changes', {
    title: 'release/20260708 绿环境上线演练',
    projectBranch: 'release/20260708',
    releaseType: 'NORMAL',
    targetEnvCode: 'prod',
    targetColor: 'green',
    developerUsername: 'reviewer_a',
    developerDisplayName: '评审 A',
    demoRequired: true,
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

function startNewItem(itemType) {
  creatingItem.value = true
  editingItem.value = null
  itemForm.value = defaultItemForm(itemType)
}

function applyItemTypeDefaults() {
  const current = itemForm.value
  const defaults = defaultItemForm(current.itemType)
  itemForm.value = {
    ...defaults,
    ownerUsername: current.ownerUsername || defaults.ownerUsername,
    ownerDisplayName: current.ownerDisplayName || defaults.ownerDisplayName
  }
}

async function createItem() {
  await run(async () => {
    selectedChange.value = await post(`/changes/${selectedChange.value.id}/items`, itemForm.value)
    creatingItem.value = false
    await refreshAll()
    notice.value = '上线项已加入计划'
  })
}

async function approveAs(username, displayName) {
  await operate(`/changes/${selectedChange.value.id}/approve`, `${displayName} 已确认`, {
    reviewerUsername: username,
    reviewerDisplayName: displayName,
    passed: true,
    comment: `${displayName} 确认通过`
  })
}

async function recordDemo() {
  await operate(`/changes/${selectedChange.value.id}/demo`, '演示确认已记录', {
    reviewerUsername: 'reviewer_b',
    reviewerDisplayName: '评审 B',
    actorUsername: selectedChange.value.developerUsername || 'reviewer_a',
    actorDisplayName: selectedChange.value.developerDisplayName || '评审 A',
    comment: `${selectedChange.value.developerDisplayName || '评审 A'} 已向评审 B 演示本次上线内容`
  })
}

async function validateSpecs() {
  await operate(`/changes/${selectedChange.value.id}/validate-specs`, '组件规范校验已生成')
}

async function recordAllReviewerTests() {
  await run(async () => {
    for (const item of selectedChange.value.items || []) {
      await post(`/changes/${selectedChange.value.id}/reviewer-test`, {
        itemId: item.id,
        reviewerUsername: 'reviewer_a',
        reviewerDisplayName: '评审 A',
        testType: 'FUNCTION',
        environmentCode: 'test',
        passed: true,
        demoObserved: true,
        responsibilityAccepted: true,
        evidence: `${item.componentName} 已在测试环境验证功能和回滚口径`
      })
      await post(`/changes/${selectedChange.value.id}/reviewer-test`, {
        itemId: item.id,
        reviewerUsername: 'reviewer_b',
        reviewerDisplayName: '评审 B',
        testType: 'FUNCTION',
        environmentCode: 'prod-green',
        passed: true,
        demoObserved: true,
        responsibilityAccepted: true,
        evidence: `${item.componentName} 已在绿环境复核`
      })
    }
    await loadChange(selectedChange.value.id)
    await refreshAll()
    notice.value = '两位评审的测试证据已补齐'
  })
}

async function runEnvDiff() {
  await operate(`/changes/${selectedChange.value.id}/test/env-diff`, '环境差异报告已生成')
}

async function runAnnounceCheck() {
  await operate(`/changes/${selectedChange.value.id}/test/announce`, 'announce 检查已生成')
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

async function switchBlue() {
  await operate(`/changes/${selectedChange.value.id}/switch/blue`, '已记录回蓝')
}

async function manualVerify() {
  await operate(`/changes/${selectedChange.value.id}/verify/manual`, '生产人工验证已记录', {
    actorUsername: 'ops',
    actorDisplayName: '运维同学',
    comment: '绿系统页面、接口、组件功能已人工验证通过'
  })
}

async function syncBlue() {
  await operate(`/changes/${selectedChange.value.id}/sync/blue`, '已记录同步蓝系统', {
    actorUsername: 'ops',
    actorDisplayName: '运维同学',
    comment: '绿系统验证通过后同步蓝系统'
  })
}

async function rollback() {
  await operate(`/changes/${selectedChange.value.id}/rollback`, '已记录回滚')
}

function openItemEditor(item) {
  creatingItem.value = false
  editingItem.value = item
  itemForm.value = { ...item }
}

async function saveItem() {
  await operate(`/changes/${selectedChange.value.id}/items/${editingItem.value.id}`, '子 change 已保存', {
    ...itemForm.value,
    ownerUsername: itemForm.value.ownerUsername || 'ops'
  })
  editingItem.value = null
}

async function operateItem(payload) {
  const { item, action } = payload
  if (!item || !item.id) {
    error.value = '找不到上线项'
    return
  }
  const labels = {
    analyze: '上线项分析已生成',
    'dry-run': 'dry-run 已记录',
    execute: '绿环境执行记录已保存',
    verify: '验证结果已保存',
    rollback: '单项回滚已记录'
  }
  const body = {
    actorUsername: item.ownerUsername || 'ops',
    actorDisplayName: item.ownerDisplayName || '运维同学',
    environmentCode: 'prod',
    targetColor: 'green',
    result: `${item.title || item.componentName} ${labels[action] || action}`,
    evidence: `${item.itemType || 'COMPONENT'}：${item.payloadPath || item.componentKey}，安全模式只记录治理证据。`,
    passed: true,
    safeMode: true
  }
  await operate(`/changes/${selectedChange.value.id}/items/${item.id}/${action}`, labels[action] || '上线项操作已记录', body)
}

function defaultItemForm(itemType) {
  const normalized = itemType || 'SQL'
  const componentKey = normalized === 'SQL' ? 'mysql' : normalized === 'CODE' ? 'java-backend' : 'nacos'
  return {
    itemType: normalized,
    componentKey,
    title: itemTypeName(normalized) + '上线项',
    ownerUsername: 'ops',
    ownerDisplayName: '运维同学',
    payloadPath: normalized === 'CODE' ? 'release/20260708' : '/data/osh/config',
    changeContent: '写清楚这次要上线什么、影响哪些模块、为什么要上。',
    executionContent: defaultExecutionContent(normalized),
    incrementalPlan: '先上绿环境，先 dry-run，再执行；每一步都要留下结果。',
    rollbackContent: defaultRollbackContent(normalized),
    rollbackPlan: '按节点逆序回滚；先恢复配置/数据/代码，再验证课程和用户模块没有异常。',
    codeChangeSummary: normalized === 'CODE' ? '写代码改动大纲：模块、接口、配置、数据库兼容性、前后端联动点。' : '非代码上线项；如脚本或配置影响代码路径，也要写清楚。',
    riskAnalysis: '写风险分析：是否影响课程/用户模块、是否有数据迁移、是否可灰度、是否可快速回滚。',
    bugAnalysis: '写疑似 bug 分析：空值、并发、索引、缓存、消息重复、配置拼写、前后端字段不一致。',
    verificationCommands: 'dry-run：\n健康检查：\n回滚验证：',
    testPlan: '两位评审分别在测试环境和绿环境验证功能、数据影响和回滚口径。',
    dataProbePlan: '采集上线前后数量摘要；课程模块和用户模块 added/removed/changed 必须为 0。'
  }
}

function defaultExecutionContent(itemType) {
  if (itemType === 'SQL') {
    return '-- 粘贴要上线的 SQL\n-- 必须说明 WHERE、影响行数、幂等判断\n'
  }
  if (itemType === 'CONFIG') {
    return '# 粘贴配置 diff 或目标配置片段\n'
  }
  if (itemType === 'CODE') {
    return '分支：release/20260708\n提交范围：\n构建产物：\n'
  }
  return '填写组件增量执行内容。'
}

function defaultRollbackContent(itemType) {
  if (itemType === 'SQL') {
    return '-- 粘贴 SQL 回滚语句\n-- 必须说明备份表、恢复条件、影响行数\n'
  }
  if (itemType === 'CONFIG') {
    return '# 粘贴回滚配置 diff 或上一版配置路径\n'
  }
  if (itemType === 'CODE') {
    return '回滚版本：\n回滚命令：\n'
  }
  return '填写组件回滚内容。'
}

function itemTypeName(itemType) {
  return { SQL: 'SQL', CONFIG: '配置', CODE: '代码', COMPONENT: '组件' }[itemType] || '组件'
}

async function operate(path, message, body) {
  await run(async () => {
    selectedChange.value = await post(path, body || {})
    await refreshAll()
    await loadReleaseGate()
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

watch(page, async (nextPage) => {
  if (nextPage === 'reports' && selectedChange.value) {
    await run(async () => {
      await loadReleaseGate()
    })
  }
})

const NodeTable = {
  props: ['nodes', 'items'],
  emits: ['edit-item', 'operate-item'],
  methods: {
    findItem(node) {
      return (this.items || []).find((item) => item.componentKey === node.componentKey && item.itemOrder === node.nodeOrder)
        || (this.items || []).find((item) => item.componentKey === node.componentKey)
        || {}
    },
    shortText(value) {
      const text = value || '-'
      return text.length > 80 ? `${text.slice(0, 80)}...` : text
    }
  },
  render() {
    return h('table', { class: 'node-table' }, [
      h('thead', [h('tr', ['顺序', '类型', '组件/上线项', '负责人', '载荷/风险', '规范', '双评审', '节点状态', '上线项状态', '单项操作', '编辑'].map((text) => h('th', text)))]),
      h('tbody', this.nodes.map((node) => {
        const item = this.findItem(node)
        return h('tr', [
        h('td', node.nodeOrder),
        h('td', item.itemType || node.nodeType),
        h('td', [
          h('strong', item.title || node.componentName),
          h('small', { class: 'block-muted' }, node.componentName)
        ]),
        h('td', item.ownerDisplayName || '-'),
        h('td', [
          h('small', { class: 'block-muted strong-muted' }, item.payloadPath || '-'),
          h('small', { class: 'block-muted' }, this.shortText(item.riskAnalysis))
        ]),
        h('td', item.specStatus || '-'),
        h('td', item.reviewerAConfirmed && item.reviewerBConfirmed ? '已齐' : '缺证据'),
        h('td', node.status),
        h('td', item.lifecycleStatus || '-'),
        h('td', h('div', { class: 'item-actions' }, [
          h('button', { class: 'mini-button', onClick: () => this.$emit('operate-item', { item, action: 'analyze' }) }, '分析'),
          h('button', { class: 'mini-button', onClick: () => this.$emit('operate-item', { item, action: 'dry-run' }) }, 'dry-run'),
          h('button', { class: 'mini-button', onClick: () => this.$emit('operate-item', { item, action: 'execute' }) }, '执行'),
          h('button', { class: 'mini-button', onClick: () => this.$emit('operate-item', { item, action: 'verify' }) }, '验证'),
          h('button', { class: 'mini-button danger-mini', onClick: () => this.$emit('operate-item', { item, action: 'rollback' }) }, '回滚')
        ])),
        h('td', h('button', { class: 'mini-button', onClick: () => this.$emit('edit-item', item) }, '编辑'))
      ])
      }))
    ])
  }
}

const PayloadFields = {
  props: ['item'],
  emits: ['update:item'],
  methods: {
    update(key, value) {
      this.$emit('update:item', { ...this.item, [key]: value })
    }
  },
  render() {
    const item = this.item || {}
    return h('div', { class: 'payload-fields' }, [
      h('label', [
        h('span', item.itemType === 'SQL' ? 'SQL 执行内容' : item.itemType === 'CONFIG' ? '配置 diff/片段' : item.itemType === 'CODE' ? '代码上线内容' : '执行内容'),
        h('textarea', {
          rows: 6,
          value: item.executionContent || '',
          onInput: (event) => this.update('executionContent', event.target.value)
        })
      ]),
      h('label', [
        h('span', '回滚内容'),
        h('textarea', {
          rows: 5,
          value: item.rollbackContent || '',
          onInput: (event) => this.update('rollbackContent', event.target.value)
        })
      ]),
      h('label', [
        h('span', '代码改动大纲'),
        h('textarea', {
          rows: 4,
          value: item.codeChangeSummary || '',
          onInput: (event) => this.update('codeChangeSummary', event.target.value)
        })
      ]),
      h('label', [
        h('span', '风险分析'),
        h('textarea', {
          rows: 4,
          value: item.riskAnalysis || '',
          onInput: (event) => this.update('riskAnalysis', event.target.value)
        })
      ]),
      h('label', [
        h('span', '疑似 bug 分析'),
        h('textarea', {
          rows: 4,
          value: item.bugAnalysis || '',
          onInput: (event) => this.update('bugAnalysis', event.target.value)
        })
      ]),
      h('label', [
        h('span', '验证命令'),
        h('textarea', {
          rows: 4,
          value: item.verificationCommands || '',
          onInput: (event) => this.update('verificationCommands', event.target.value)
        })
      ])
    ])
  }
}

const NodeTree = {
  props: ['nodes', 'items'],
  render() {
    return h('div', { class: 'node-tree' }, this.nodes.map((node) => {
      const item = (this.items || []).find((entry) => entry.componentKey === node.componentKey && entry.itemOrder === node.nodeOrder)
        || (this.items || []).find((entry) => entry.componentKey === node.componentKey)
        || {}
      return h('div', { class: 'tree-line' }, [
      h('span', node.nodeOrder),
      h('strong', node.componentName),
      h('small', `${item.title || node.actionType} | ${node.configDir} -> ${node.dataDir}`)
    ])
    }))
  }
}

const NodeGraph = {
  props: ['nodes', 'items'],
  render() {
    return h('div', { class: 'node-graph' }, this.nodes.map((node, index) => {
      const item = (this.items || []).find((entry) => entry.componentKey === node.componentKey && entry.itemOrder === node.nodeOrder)
        || (this.items || []).find((entry) => entry.componentKey === node.componentKey)
        || {}
      return h('div', { class: 'graph-node' }, [
      h('span', `#${index + 1}`),
      h('strong', node.componentName),
      h('small', `${node.status} · ${item.specStatus || '待校验'}`)
    ])
    }))
  }
}
</script>
