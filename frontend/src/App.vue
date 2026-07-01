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
            <p>覆盖数据库、缓存、搜索、消息、配置、任务、网关、日志和扩展组件。</p>
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

              <section class="component-workbench">
                <div class="section-head compact-head">
                  <div>
                    <h4>组件上线工作台</h4>
                    <p>选具体动作，平台会生成独立上线项、节点、dry-run、验证和回滚记录。</p>
                  </div>
                </div>
                <div class="action-catalog">
                  <article v-for="entry in componentActions" :key="entry.type">
                    <span>{{ entry.componentLabel }}</span>
                    <strong>{{ entry.title }}</strong>
                    <p>{{ entry.copy }}</p>
                    <button @click="startNewItem(entry.type)">{{ entry.button }}</button>
                  </article>
                </div>
              </section>

              <section class="payload-guide legacy-guide">
                <article v-for="entry in payloadGuides" :key="entry.type">
                  <strong>{{ entry.title }}</strong>
                  <p>{{ entry.copy }}</p>
                  <button @click="startNewItem(entry.type)">{{ entry.button }}</button>
                </article>
              </section>

              <div class="action-row">
                <button @click="submitChange">提交</button>
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

              <form v-if="creatingItem || editingItem" class="item-editor payload-editor" @submit.prevent="editingItem ? saveItem() : createItem()">
                <div class="section-head">
                  <h4>{{ editingItem ? '更新子 change' : '新增上线项' }}：{{ itemTypeName(itemForm.itemType) }}</h4>
                  <button type="button" class="ghost" @click="closeItemEditor">收起</button>
                </div>
                <div class="form-grid">
                  <label>
                    类型
                    <select v-model="itemForm.itemType" @change="applyItemTypeDefaults">
                      <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">
                        {{ option.label }}
                      </option>
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
                  <label>
                    载荷路径/key/topic/index/table/job
                    <input v-model="itemForm.payloadPath" />
                  </label>
                  <label>
                    上线内容
                    <textarea v-model="itemForm.changeContent" rows="3" />
                  </label>
                </div>
                <PayloadFields v-model:item="itemForm" />
                <div class="form-grid">
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
                </div>
                <button class="primary" type="submit">{{ editingItem ? '保存子 change' : '加入上线计划' }}</button>
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
                    {{ demo.developerUsername }} -> {{ demo.reviewerUsername }} · {{ demo.content }}
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
            <p>这里不是摆设：后面所有 SQL、ES、HBase、Kafka、Nacos、XXLJob、配置和组件上线都从这些动作走。</p>
          </div>
          <div class="inventory-note">
            <strong>只读盘点结果</strong>
            <p>测试服是一套 `osh-*`；生产有蓝色 `osh-*` 和绿色 `osh-g-*` 两套。HBase、MongoDB 目前未在 docker ps 里发现运行容器，平台保留治理入口，真实执行前必须先确认实例位置。</p>
          </div>
          <div class="component-grid">
            <article v-for="component in components" :key="component.componentKey">
              <div class="component-card-head">
                <span>{{ component.componentType }}</span>
                <small :class="['observed-pill', component.observedStatus?.toLowerCase()]">{{ observedStatusText(component.observedStatus) }}</small>
              </div>
              <h4>{{ component.componentName }}</h4>
              <p>{{ component.notes }}</p>
              <div class="action-tags">
                <button v-for="type in actionTypes(component)" :key="`${component.componentKey}-${type}`" @click="openActionFromComponent(type, component)">
                  {{ itemTypeName(type) }}
                </button>
              </div>
              <small>服务器：{{ component.runtimeInventory || '待只读探测' }}</small>
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
import { computed, h, nextTick, onMounted, ref, watch } from 'vue'
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

const itemTypeOptions = [
  { value: 'MYSQL_SQL', label: 'MySQL SQL' },
  { value: 'REDIS_SCRIPT', label: 'Redis 脚本/Key' },
  { value: 'REDIS_CONFIG', label: 'Redis 配置' },
  { value: 'NACOS_CONFIG', label: 'Nacos 配置' },
  { value: 'KAFKA_TOPIC', label: 'Kafka 新增 Topic' },
  { value: 'KAFKA_CONFIG', label: 'Kafka 配置' },
  { value: 'ZOOKEEPER_CONFIG', label: 'Zookeeper 配置' },
  { value: 'ES_INDEX', label: 'ES 索引/别名' },
  { value: 'ES_CONFIG', label: 'ES 配置' },
  { value: 'KIBANA_CONFIG', label: 'Kibana 配置' },
  { value: 'HBASE_DDL', label: 'HBase DDL' },
  { value: 'HBASE_CONFIG', label: 'HBase 配置' },
  { value: 'XXLJOB_TASK', label: 'XXLJob 任务' },
  { value: 'XXLJOB_CONFIG', label: 'XXLJob 配置' },
  { value: 'FLINK_JOB', label: 'Flink 任务' },
  { value: 'FLINK_CONFIG', label: 'Flink 配置' },
  { value: 'CODE', label: '代码发布' },
  { value: 'FILEBEAT_CONFIG', label: 'Filebeat 配置' },
  { value: 'OTEL_CONFIG', label: 'OTel 配置' },
  { value: 'SECRET_CONFIG', label: '密钥服务配置' },
  { value: 'NGINX_CONFIG', label: 'Nginx 配置' },
  { value: 'COMPOSE_CHANGE', label: 'Compose 组件' },
  { value: 'QDRANT_COLLECTION', label: 'Qdrant Collection' },
  { value: 'QDRANT_CONFIG', label: 'Qdrant 配置' },
  { value: 'MONGODB_SCRIPT', label: 'MongoDB 脚本' },
  { value: 'SQL', label: '通用 SQL' },
  { value: 'CONFIG', label: '通用配置' },
  { value: 'COMPONENT', label: '通用组件' }
]

const componentActions = [
  { type: 'MYSQL_SQL', componentLabel: 'MySQL', title: '上线 SQL', copy: 'DDL/DML、影响行数、备份表、回滚 SQL 都要填。', button: '新增 MySQL SQL' },
  { type: 'REDIS_CONFIG', componentLabel: 'Redis', title: '修改 Redis 配置', copy: '填 redis.conf diff、重启范围、key 数和回滚配置。', button: '新增 Redis 配置' },
  { type: 'ES_INDEX', componentLabel: 'Elasticsearch', title: '新增索引/别名', copy: '填 mapping、settings、alias、reindex 和回切方案。', button: '新增 ES 索引' },
  { type: 'ES_CONFIG', componentLabel: 'Elasticsearch', title: '修改 ES 配置', copy: '填 elasticsearch.yml/jvm.options diff、滚动重启和回滚配置。', button: '新增 ES 配置' },
  { type: 'HBASE_DDL', componentLabel: 'HBase', title: '上线 DDL', copy: '填 namespace/table/列族/预分区，先 describe/exists。', button: '新增 HBase DDL' },
  { type: 'HBASE_CONFIG', componentLabel: 'HBase', title: '修改 HBase 配置', copy: '填 hbase-site.xml diff、滚动方式和 region 影响。', button: '新增 HBase 配置' },
  { type: 'KAFKA_TOPIC', componentLabel: 'Kafka', title: '新增 Topic', copy: '填 topic、分区、副本、retention、生产消费验证。', button: '新增 Kafka Topic' },
  { type: 'KAFKA_CONFIG', componentLabel: 'Kafka', title: '修改 Kafka 配置', copy: '填 broker/topic 配置 diff、重启范围、lag 观察。', button: '新增 Kafka 配置' },
  { type: 'NACOS_CONFIG', componentLabel: 'Nacos', title: '发布配置', copy: '填 dataId、group、namespace、diff 和回滚内容。', button: '新增 Nacos 配置' },
  { type: 'XXLJOB_TASK', componentLabel: 'XXLJob', title: '新增/修改任务', copy: '填 jobHandler、cron、路由、阻塞策略、停用回滚。', button: '新增 XXLJob 任务' },
  { type: 'XXLJOB_CONFIG', componentLabel: 'XXLJob', title: '修改 XXLJob 配置', copy: '填 admin 配置、执行器注册、回滚配置和日志验证。', button: '新增 XXLJob 配置' },
  { type: 'REDIS_SCRIPT', componentLabel: 'Redis', title: '脚本/Key 变更', copy: '填 Lua/命令、key 前缀、TTL、回滚和数量对比。', button: '新增 Redis 变更' },
  { type: 'CODE', componentLabel: 'Java/Vue', title: '发布代码', copy: '填分支、commit 范围、构建产物、改动大纲和疑似 bug。', button: '新增代码发布' },
  { type: 'NGINX_CONFIG', componentLabel: 'Nginx', title: '修改网关配置', copy: '填 server/upstream diff、nginx -t、回切配置和 reload 计划。', button: '新增 Nginx 配置' },
  { type: 'COMPOSE_CHANGE', componentLabel: 'Compose', title: '新增组件', copy: '填镜像、数据目录、配置目录、healthcheck 和回滚 compose。', button: '新增组件上线' }
]

const payloadGuides = [
  { type: 'SQL', title: '通用 SQL', copy: '临时 SQL 可走这里；推荐优先选 MySQL SQL 或 HBase DDL。', button: '新增 SQL' },
  { type: 'CONFIG', title: '通用配置', copy: '找不到专门组件时再用；Nacos、ES、HBase、Kafka 优先走上面的专用入口。', button: '新增配置' },
  { type: 'COMPONENT', title: '通用组件', copy: '新组件或非标准组件走这里，仍要填目录、compose、healthcheck 和回滚。', button: '新增组件' }
]

const typeTemplates = {
  MYSQL_SQL: {
    componentKey: 'mysql',
    title: 'MySQL SQL 上线',
    payloadPath: 'mysql/release/20260708/change.sql',
    changeContent: '说明本次 SQL 影响表、影响行数、是否碰课程/用户模块。',
    executionContent: '-- 粘贴 SQL\n-- 写清楚 WHERE、影响行数、幂等判断\n',
    rollbackContent: '-- 粘贴回滚 SQL\n-- 写清楚备份表、恢复条件、影响行数\n',
    codeChangeSummary: '非代码上线项；如果 SQL 依赖代码字段兼容，在这里说明。',
    riskAnalysis: '重点看锁表、无 WHERE、默认值兼容、课程/用户表是否变化。',
    bugAnalysis: '疑似 bug：重复执行、索引名冲突、锁等待、老代码读写新字段。',
    verificationCommands: 'dry-run：EXPLAIN 或事务回滚演练\n验证：select 影响行数，课程/用户表变化为 0\n回滚验证：执行回滚 SQL 后复查结构和行数'
  },
  ES_INDEX: {
    componentKey: 'elasticsearch',
    title: 'ES 索引/别名上线',
    payloadPath: 'es/index-name-or-alias',
    changeContent: '说明新增 index、mapping、settings、alias、reindex 范围。',
    executionContent: 'PUT /new_index\n{\n  "settings": {},\n  "mappings": {}\n}\nPOST /_aliases 切换别名草案',
    rollbackContent: 'POST /_aliases 切回旧索引；必要时删除新索引。',
    codeChangeSummary: '说明代码查询是否依赖新字段、新 analyzer 或 alias。',
    riskAnalysis: '重点看 mapping 兼容、alias 回切、reindex 数据量和查询性能。',
    bugAnalysis: '疑似 bug：alias 指错、mapping 类型不兼容、分片数不合理、reindex 漏字段。',
    verificationCommands: 'GET _cat/indices\nGET _alias/alias-name\n抽样查询新旧索引文档数和核心搜索结果'
  },
  ES_CONFIG: {
    componentKey: 'elasticsearch',
    title: 'ES 配置上线',
    payloadPath: '/data/osh/config/es/elasticsearch.yml',
    changeContent: '说明配置 key、旧值、新值、是否需要滚动重启。',
    executionContent: '# 粘贴 elasticsearch.yml 或 jvm.options diff\n',
    rollbackContent: '# 粘贴上一版配置 diff 或配置快照路径\n',
    codeChangeSummary: '非代码上线项；说明是否影响查询、写入或集群发现。',
    riskAnalysis: '重点看滚动重启、节点加入集群、磁盘水位和查询写入影响。',
    bugAnalysis: '疑似 bug：配置 key 拼错、节点无法加入、JVM 参数不兼容。',
    verificationCommands: 'GET _cluster/health\nGET _nodes/settings\nGET _cat/nodes'
  },
  KIBANA_CONFIG: componentConfigTemplate(
    'kibana',
    'Kibana 配置上线',
    '/data/osh/config/kibana/kibana.yml',
    '说明 kibana.yml diff、ES 地址和登录入口影响。',
    '重点看 ES 地址、basePath、认证配置和重启窗口。',
    '疑似 bug：basePath 错、ES 地址错、版本不兼容。',
    'curl -I http://127.0.0.1:5601\n检查 Kibana 状态页'
  ),
  HBASE_DDL: {
    componentKey: 'hbase',
    title: 'HBase DDL 上线',
    payloadPath: 'hbase:namespace/table',
    changeContent: '说明 namespace、table、列族、TTL、压缩、预分区。',
    executionContent: "create_namespace 'ns'\ncreate 'ns:table', {NAME => 'cf', VERSIONS => 1}",
    rollbackContent: "disable 'ns:table'\ndrop 'ns:table'",
    codeChangeSummary: '说明读写代码是否依赖新表、新列族或 TTL。',
    riskAnalysis: '重点看 disable/enable 窗口、列族兼容、region 数和线上读写影响。',
    bugAnalysis: '疑似 bug：列族名写错、预分区不合理、TTL/压缩配置不一致。',
    verificationCommands: "exists 'ns:table'\ndescribe 'ns:table'\n抽样 count 或核心 rowkey 查询"
  },
  HBASE_CONFIG: {
    componentKey: 'hbase',
    title: 'HBase 配置上线',
    payloadPath: '/data/osh/config/hbase/hbase-site.xml',
    changeContent: '说明 hbase-site.xml key、旧值、新值、滚动范围。',
    executionContent: '<!-- 粘贴 hbase-site.xml diff -->',
    rollbackContent: '<!-- 粘贴上一版 hbase-site.xml diff -->',
    codeChangeSummary: '非代码上线项；说明是否影响客户端读写或 regionserver。',
    riskAnalysis: '重点看 regionserver 滚动、读写延迟、meta 可用性。',
    bugAnalysis: '疑似 bug：配置未加载、节点参数不一致、客户端超时变大。',
    verificationCommands: 'hbase shell status\ndescribe 关键表\n检查 regionserver 日志'
  },
  KAFKA_TOPIC: {
    componentKey: 'kafka',
    title: 'Kafka Topic 上线',
    payloadPath: 'topic: osh.event.demo',
    changeContent: '说明 topic、分区、副本、retention、生产者和消费组。',
    executionContent: 'kafka-topics --create --topic osh.event.demo --partitions 3 --replication-factor 1',
    rollbackContent: '停生产者和消费者后，按确认结果删除或禁用新 topic。',
    codeChangeSummary: '说明生产者/消费者代码是否已兼容新 topic。',
    riskAnalysis: '重点看分区不可减少、lag、重复消费、副本不足。',
    bugAnalysis: '疑似 bug：topic 重名、分区并发不匹配、retention 配错。',
    verificationCommands: 'kafka-topics --describe --topic osh.event.demo\nkafka-consumer-groups --describe --group group-name'
  },
  KAFKA_CONFIG: {
    componentKey: 'kafka',
    title: 'Kafka 配置上线',
    payloadPath: '/data/osh/config/kafka/server.properties',
    changeContent: '说明 broker/topic 配置 diff、重启范围和兼容性。',
    executionContent: '# 粘贴 server.properties 或 topic config diff\n',
    rollbackContent: '# 粘贴上一版配置 diff\n',
    codeChangeSummary: '非代码上线项；说明生产消费是否受影响。',
    riskAnalysis: '重点看 broker 滚动、ISR、副本、消费组 lag。',
    bugAnalysis: '疑似 bug：配置未生效、broker 无法加入、topic 默认值变化。',
    verificationCommands: 'kafka-topics --describe\nkafka-consumer-groups --all-groups --describe'
  },
  ZOOKEEPER_CONFIG: componentConfigTemplate(
    'zookeeper',
    'Zookeeper 配置上线',
    '/data/osh/config/zookeeper/zoo.cfg',
    '说明 zoo.cfg 配置 diff、重启范围和 Kafka 影响。',
    '重点看 quorum、session timeout、数据目录和 Kafka 依赖。',
    '疑似 bug：节点无法加入 quorum、session 抖动、配置未加载。',
    'echo ruok | nc 127.0.0.1 2181\n检查 Kafka broker 状态'
  ),
  NACOS_CONFIG: {
    componentKey: 'nacos',
    title: 'Nacos 配置上线',
    payloadPath: 'dataId/group/namespace',
    changeContent: '说明 dataId、group、namespace、旧值、新值、刷新方式。',
    executionContent: '# 粘贴 Nacos 配置 diff 或完整配置片段\n',
    rollbackContent: '# 粘贴上一版 Nacos 配置\n',
    codeChangeSummary: '说明代码读取 key、默认值和兼容逻辑。',
    riskAnalysis: '重点看配置刷新、蓝绿一致性、默认值兼容。',
    bugAnalysis: '疑似 bug：dataId/group 写错、配置未刷新、字段名不一致。',
    verificationCommands: '读取 Nacos 配置\n调用应用健康检查\n确认蓝绿配置版本'
  },
  XXLJOB_TASK: {
    componentKey: 'xxl-job',
    title: 'XXLJob 任务上线',
    payloadPath: 'jobGroup/jobHandler',
    changeContent: '说明 jobHandler、cron、路由策略、阻塞策略、负责人。',
    executionContent: '新增或修改任务：jobHandler、cron、路由策略、阻塞策略，默认先停用。',
    rollbackContent: '停用新任务，恢复旧 cron/handler/路由策略。',
    codeChangeSummary: '说明任务代码是否已发布，是否幂等。',
    riskAnalysis: '重点看误触发、重复执行、失败重试和数据幂等。',
    bugAnalysis: '疑似 bug：cron 写错、路由策略错误、任务重复处理。',
    verificationCommands: '测试环境手动触发一次\n检查调度日志\n确认任务默认停用或灰度启用'
  },
  XXLJOB_CONFIG: {
    componentKey: 'xxl-job',
    title: 'XXLJob 配置上线',
    payloadPath: '/data/osh/config/xxl-job/application.properties',
    changeContent: '说明 XXLJob admin/执行器配置 key、旧值、新值和刷新方式。',
    executionContent: '# 粘贴 XXLJob 配置 diff\n',
    rollbackContent: '# 粘贴上一版 XXLJob 配置 diff\n',
    codeChangeSummary: '非代码上线项；说明是否影响执行器注册、任务触发或日志。',
    riskAnalysis: '重点看执行器注册、调度中心地址、日志路径、重启范围和任务误触发。',
    bugAnalysis: '疑似 bug：配置未加载、执行器离线、任务重复注册、日志路径错误。',
    verificationCommands: '打开 XXLJob admin\n检查执行器在线\n手动触发测试任务并查看日志'
  },
  FLINK_JOB: {
    componentKey: 'flink',
    title: 'Flink 任务上线',
    payloadPath: 'flink/jobs/order-stream.jar',
    changeContent: '说明 Flink job、jar、并发、checkpoint/savepoint 和输入输出 topic。',
    executionContent: 'flink run -d -p 2 -s savepoint-path order-stream.jar',
    rollbackContent: '停止新 job，从上一版 savepoint 恢复旧 job。',
    codeChangeSummary: '说明流任务代码改动、状态兼容和输入输出格式。',
    riskAnalysis: '重点看 savepoint 兼容、重复消费、checkpoint 路径和延迟。',
    bugAnalysis: '疑似 bug：状态反序列化失败、并发变化导致 key 分布变化、输出重复。',
    verificationCommands: 'Flink UI 检查 job running\n检查 checkpoint\n检查 Kafka lag 和输出数量'
  },
  FLINK_CONFIG: componentConfigTemplate(
    'flink',
    'Flink 配置上线',
    '/data/osh/config/flink/flink-conf.yaml',
    '说明 flink-conf.yaml diff、JobManager/TaskManager 重启范围。',
    '重点看 checkpoint、内存、slot、JM/TM 重启和任务恢复。',
    '疑似 bug：内存参数错误、slot 不够、checkpoint 目录不可写。',
    'Flink UI 检查集群\n检查 JM/TM 日志\n检查 checkpoint'
  ),
  REDIS_SCRIPT: {
    componentKey: 'redis',
    title: 'Redis 脚本/Key 上线',
    payloadPath: 'redis:key-prefix-or-script',
    changeContent: '说明 key 前缀、TTL、Lua/命令、影响 key 数。',
    executionContent: '# 粘贴 redis-cli 命令或 Lua 脚本\n',
    rollbackContent: '# 粘贴删除/恢复 key 或脚本回滚命令\n',
    codeChangeSummary: '说明代码是否读取新 key 或新缓存结构。',
    riskAnalysis: '重点看 key 前缀误删、TTL、缓存穿透和课程/用户 key。',
    bugAnalysis: '疑似 bug：key 前缀写错、TTL 配错、Lua 非幂等。',
    verificationCommands: 'SCAN/MEMORY USAGE/TTL 只读检查\n对比 key 数\n回滚后复查前缀'
  },
  REDIS_CONFIG: {
    componentKey: 'redis',
    title: 'Redis 配置上线',
    payloadPath: '/data/osh/config/redis/redis.conf',
    changeContent: '说明 redis.conf key、旧值、新值、是否需要重启。',
    executionContent: '# 粘贴 redis.conf diff\n',
    rollbackContent: '# 粘贴上一版 redis.conf diff\n',
    codeChangeSummary: '非代码上线项；说明是否影响缓存读写、TTL 或内存策略。',
    riskAnalysis: '重点看 maxmemory、持久化、淘汰策略、连接数和 key 数变化。',
    bugAnalysis: '疑似 bug：配置未生效、重启丢临时 key、淘汰策略误伤热点缓存。',
    verificationCommands: 'redis-cli CONFIG GET maxmemory\nredis-cli INFO memory\nSCAN 关键前缀'
  },
  CODE: {
    componentKey: 'java-backend',
    title: '代码发布',
    payloadPath: 'release/20260708 commit-range',
    changeContent: '说明改动模块、接口、数据库兼容、前后端联动。',
    executionContent: '分支：release/20260708\n提交范围：\n构建产物：backend.jar 或 dist 包',
    rollbackContent: '回滚版本：上一版 commit/镜像/包路径\n回滚命令：',
    codeChangeSummary: '写代码改动大纲：模块、接口、配置、数据库兼容性、前后端联动点。',
    riskAnalysis: '重点看空值、并发、接口兼容、缓存和 SQL 性能。',
    bugAnalysis: '疑似 bug：空值、枚举不匹配、缓存旧对象、字段不一致。',
    verificationCommands: 'mvn -q -pl backend test\nnpm run build\n绿环境接口和页面冒烟'
  },
  COMPOSE_CHANGE: {
    componentKey: 'docker-compose',
    title: 'Compose 组件上线',
    payloadPath: '/data/osh/compose/docker-compose.yml',
    changeContent: '说明新增组件、镜像、端口、数据目录、配置目录、healthcheck。',
    executionContent: '# 粘贴 docker-compose diff\n',
    rollbackContent: '# 粘贴回滚 compose diff 或删除组件步骤\n',
    codeChangeSummary: '非代码上线项；说明是否有调用方依赖新组件。',
    riskAnalysis: '重点看端口冲突、数据目录、配置目录、healthcheck。',
    bugAnalysis: '疑似 bug：目录挂错、端口冲突、容器健康检查缺失。',
    verificationCommands: 'docker compose config\ndocker compose ps\ncurl healthcheck'
  },
  NGINX_CONFIG: {
    componentKey: 'nginx',
    title: 'Nginx 配置上线',
    payloadPath: '/data/osh/config/nginx/conf.d/osh.conf',
    changeContent: '说明 server/upstream/location diff、蓝绿切流点和回切方式。',
    executionContent: '# 粘贴 nginx 配置 diff\n',
    rollbackContent: '# 粘贴上一版 nginx 配置或回切 upstream\n',
    codeChangeSummary: '非代码上线项；说明是否影响主站、治理台或静态资源。',
    riskAnalysis: '重点看 nginx -t、reload、蓝绿 upstream、证书和路径转发。',
    bugAnalysis: '疑似 bug：location 顺序错误、proxy header 缺失、缓存路径错、切流目标错。',
    verificationCommands: 'nginx -t\ncurl -I https://osh.lol/\ncurl -I https://osh.lol/release-console/'
  },
  FILEBEAT_CONFIG: componentConfigTemplate(
    'filebeat',
    'Filebeat 配置上线',
    '/data/osh/config/filebeat/filebeat.yml',
    '说明日志路径、index、pipeline 和采集范围。',
    '重点看采集路径、重复采集、ES index 和磁盘压力。',
    '疑似 bug：路径写错、multiline 规则错、索引名错。',
    'filebeat test config\n检查 ES 新日志索引\n检查容器日志'
  ),
  OTEL_CONFIG: componentConfigTemplate(
    'otel-collector',
    'OTel 配置上线',
    '/data/osh/config/otel-collector/config.yaml',
    '说明 receiver/processor/exporter diff、采样率和目标地址。',
    '重点看 exporter 地址、采样率、队列和丢数据。',
    '疑似 bug：配置语法错、exporter 连不上、采样率过高。',
    'otelcol --config config.yaml validate\n检查 collector 日志\n检查 tracing/metrics 是否入库'
  ),
  SECRET_CONFIG: componentConfigTemplate(
    'secret-manager',
    '密钥服务配置上线',
    '/data/osh/config/secret-manager/application.yml',
    '只记录配置 key 和版本，不保存密钥明文。',
    '重点看密钥明文泄露、版本兼容、调用方刷新和回滚。',
    '疑似 bug：key 名写错、权限不足、调用方缓存旧密钥。',
    '只读检查 key 是否存在\n调用方健康检查\n确认日志无密钥明文'
  ),
  QDRANT_COLLECTION: {
    componentKey: 'qdrant',
    title: 'Qdrant Collection 上线',
    payloadPath: 'qdrant/collection-name',
    changeContent: '说明 collection、向量维度、索引参数、alias 和回滚 collection。',
    executionContent: 'PUT /collections/new_collection\n{\n  "vectors": {"size": 1024, "distance": "Cosine"}\n}',
    rollbackContent: '切回旧 collection alias；确认后删除新 collection。',
    codeChangeSummary: '说明检索代码是否依赖新 collection 或向量维度。',
    riskAnalysis: '重点看向量维度、索引参数、alias 回切和查询性能。',
    bugAnalysis: '疑似 bug：维度不一致、payload schema 漏字段、alias 指错。',
    verificationCommands: 'GET /collections\n查询测试向量\n确认 alias 指向'
  },
  QDRANT_CONFIG: componentConfigTemplate(
    'qdrant',
    'Qdrant 配置上线',
    '/data/osh/config/qdrant/config.yaml',
    '说明 Qdrant 配置 diff、存储目录和端口影响。',
    '重点看数据目录、端口、内存和 collection 可用性。',
    '疑似 bug：目录挂错、端口冲突、collection 加载失败。',
    'GET /healthz\nGET /collections'
  ),
  MONGODB_SCRIPT: {
    componentKey: 'mongodb',
    title: 'MongoDB 脚本上线',
    payloadPath: 'mongodb/release/20260708/change.js',
    changeContent: '说明 MongoDB collection、索引、影响文档数和回滚方式。',
    executionContent: '// 粘贴 MongoDB 增量脚本\n',
    rollbackContent: '// 粘贴 MongoDB 回滚脚本\n',
    codeChangeSummary: '说明代码是否读取新 collection、字段或索引。',
    riskAnalysis: '重点看 collection、索引构建、影响文档数和幂等。',
    bugAnalysis: '疑似 bug：脚本重复执行、索引名冲突、字段类型不一致。',
    verificationCommands: 'db.collection.countDocuments()\ndb.collection.getIndexes()'
  }
}

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
  const allowed = [
    'mysql',
    'redis',
    'nacos',
    'zookeeper',
    'kafka',
    'elasticsearch',
    'kibana',
    'hbase',
    'xxl-job',
    'flink',
    'java-backend',
    'vue-frontend',
    'filebeat',
    'otel-collector',
    'secret-manager',
    'nginx',
    'docker-compose',
    'qdrant',
    'mongodb'
  ]
  const componentKeys = components.value.filter((item) => allowed.includes(item.componentKey)).map((item) => item.componentKey)
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
  page.value = 'changes'
  focusItemEditor()
}

async function openActionFromComponent(itemType, component) {
  if (!selectedChange.value && changes.value.length > 0) {
    await loadChange(changes.value[0].id)
  }
  startNewItem(itemType)
  if (component) {
    itemForm.value = {
      ...itemForm.value,
      componentKey: component.componentKey,
      title: `${component.componentName} ${itemTypeName(itemType)}上线`,
      payloadPath: itemType === 'CONFIG' || String(itemType).endsWith('_CONFIG')
        ? component.configDir
        : itemType === 'COMPOSE_CHANGE'
          ? component.deployPath
          : itemForm.value.payloadPath
    }
  }
}

function closeItemEditor() {
  creatingItem.value = false
  editingItem.value = null
}

function focusItemEditor() {
  nextTick(() => {
    document.querySelector('.item-editor')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
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
        evidence: `${item.title || item.componentName} 已在测试环境验证功能和回滚口径`
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
        evidence: `${item.title || item.componentName} 已在绿环境复核`
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
  if (!item || !item.id) {
    error.value = '找不到上线项'
    return
  }
  creatingItem.value = false
  editingItem.value = item
  itemForm.value = { ...item }
  notice.value = `正在编辑：${item.title || item.componentName}`
  focusItemEditor()
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
  const normalized = itemType || 'MYSQL_SQL'
  const template = typeTemplates[normalized] || genericTemplate(normalized)
  return {
    itemType: normalized,
    componentKey: template.componentKey,
    title: template.title,
    ownerUsername: 'ops',
    ownerDisplayName: '运维同学',
    payloadPath: template.payloadPath,
    changeContent: template.changeContent,
    executionContent: template.executionContent,
    incrementalPlan: '先上绿环境，先 dry-run，再执行；每一步都要留下结果。',
    rollbackContent: template.rollbackContent,
    rollbackPlan: '按节点逆序回滚；先恢复配置/数据/代码，再验证课程和用户模块没有异常。',
    codeChangeSummary: template.codeChangeSummary,
    riskAnalysis: template.riskAnalysis,
    bugAnalysis: template.bugAnalysis,
    verificationCommands: template.verificationCommands,
    testPlan: '两位评审分别在测试环境和绿环境验证功能、数据影响和回滚口径。',
    dataProbePlan: '采集上线前后数量摘要；课程模块和用户模块 added/removed/changed 必须为 0。'
  }
}

function componentConfigTemplate(componentKey, title, payloadPath, changeContent, riskAnalysis, bugAnalysis, verificationCommands) {
  return {
    componentKey,
    title,
    payloadPath,
    changeContent,
    executionContent: '# 粘贴配置 diff\n',
    rollbackContent: '# 粘贴上一版配置 diff\n',
    codeChangeSummary: '非代码上线项；说明调用方、刷新方式和兼容性。',
    riskAnalysis,
    bugAnalysis,
    verificationCommands
  }
}

function genericTemplate(itemType) {
  const label = itemTypeName(itemType)
  return {
    componentKey: itemType === 'CODE' ? 'java-backend' : 'nacos',
    title: `${label}上线项`,
    payloadPath: itemType === 'CODE' ? 'release/20260708' : '/data/osh/config',
    changeContent: '写清楚这次要上线什么、影响哪些模块、为什么要上。',
    executionContent: itemType === 'SQL' ? '-- 粘贴要上线的 SQL\n' : itemType === 'CONFIG' ? '# 粘贴配置 diff\n' : '填写组件增量执行内容。',
    rollbackContent: itemType === 'SQL' ? '-- 粘贴 SQL 回滚语句\n' : itemType === 'CONFIG' ? '# 粘贴回滚配置 diff\n' : '填写组件回滚内容。',
    codeChangeSummary: itemType === 'CODE' ? '写代码改动大纲：模块、接口、配置、数据库兼容性、前后端联动点。' : '非代码上线项；如脚本或配置影响代码路径，也要写清楚。',
    riskAnalysis: '写风险分析：是否影响课程/用户模块、是否有数据迁移、是否可灰度、是否可快速回滚。',
    bugAnalysis: '写疑似 bug 分析：空值、并发、索引、缓存、消息重复、配置拼写、前后端字段不一致。',
    verificationCommands: 'dry-run：\n健康检查：\n回滚验证：'
  }
}

function itemTypeName(itemType) {
  return itemTypeOptions.find((item) => item.value === itemType)?.label || itemType || '组件'
}

function actionTypes(component) {
  return String(component?.actionTypes || 'CONFIG,COMPONENT')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 5)
}

function observedStatusText(status) {
  const mapping = {
    FOUND_TEST_AND_PROD: '测试/生产已发现',
    SUPPORTED_NOT_FOUND: '支持，实例待确认',
    TEST_ONLY_FOUND: '测试服发现',
    BEHIND_NGINX: '网关托管',
    UNKNOWN: '待探测'
  }
  return mapping[status] || status || '待探测'
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
    },
    executionLabel(item) {
      if ((item.itemType || '').includes('CONFIG')) {
        return '配置 diff/片段'
      }
      if ((item.itemType || '').includes('TOPIC')) {
        return 'Topic 创建/配置内容'
      }
      if ((item.itemType || '').includes('INDEX')) {
        return '索引 mapping/settings/alias'
      }
      if ((item.itemType || '').includes('DDL')) {
        return 'DDL 执行内容'
      }
      if (item.itemType === 'CODE') {
        return '代码上线内容'
      }
      return '执行内容'
    }
  },
  render() {
    const item = this.item || {}
    return h('div', { class: 'payload-fields' }, [
      h('label', [
        h('span', this.executionLabel(item)),
        h('textarea', { rows: 6, value: item.executionContent || '', onInput: (event) => this.update('executionContent', event.target.value) })
      ]),
      h('label', [
        h('span', '回滚内容'),
        h('textarea', { rows: 5, value: item.rollbackContent || '', onInput: (event) => this.update('rollbackContent', event.target.value) })
      ]),
      h('label', [
        h('span', '代码/变更大纲'),
        h('textarea', { rows: 4, value: item.codeChangeSummary || '', onInput: (event) => this.update('codeChangeSummary', event.target.value) })
      ]),
      h('label', [
        h('span', '风险分析'),
        h('textarea', { rows: 4, value: item.riskAnalysis || '', onInput: (event) => this.update('riskAnalysis', event.target.value) })
      ]),
      h('label', [
        h('span', '疑似 bug 分析'),
        h('textarea', { rows: 4, value: item.bugAnalysis || '', onInput: (event) => this.update('bugAnalysis', event.target.value) })
      ]),
      h('label', [
        h('span', '验证命令'),
        h('textarea', { rows: 4, value: item.verificationCommands || '', onInput: (event) => this.update('verificationCommands', event.target.value) })
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
