package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.model.AppUser;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.model.ReleaseChangeItem;
import com.juege.oshrelease.model.ReleaseNode;
import com.juege.oshrelease.model.ReleaseOperationRecord;
import com.juege.oshrelease.model.ReviewerTestEvidence;
import com.juege.oshrelease.model.ReviewRecord;
import com.juege.oshrelease.model.SourceProject;
import com.juege.oshrelease.model.TestReport;
import com.juege.oshrelease.repo.AppUserRepository;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeItemRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.ReleaseNodeRepository;
import com.juege.oshrelease.repo.ReleaseOperationRecordRepository;
import com.juege.oshrelease.repo.ReviewerTestEvidenceRepository;
import com.juege.oshrelease.repo.ReviewRecordRepository;
import com.juege.oshrelease.repo.SourceProjectRepository;
import com.juege.oshrelease.repo.TestReportRepository;
import com.juege.oshrelease.service.BootstrapService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapServiceImpl implements BootstrapService {

    private final AppUserRepository appUserRepository;
    private final EnvironmentRepository environmentRepository;
    private final ComponentRepository componentRepository;
    private final SourceProjectRepository sourceProjectRepository;
    private final ReleaseChangeRepository releaseChangeRepository;
    private final ReleaseNodeRepository releaseNodeRepository;
    private final ReleaseChangeItemRepository releaseChangeItemRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewerTestEvidenceRepository reviewerTestEvidenceRepository;
    private final ReleaseOperationRecordRepository releaseOperationRecordRepository;
    private final TestReportRepository testReportRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.core.env.Environment springEnvironment;

    public BootstrapServiceImpl(AppUserRepository appUserRepository,
                                EnvironmentRepository environmentRepository,
                                ComponentRepository componentRepository,
                                SourceProjectRepository sourceProjectRepository,
                                ReleaseChangeRepository releaseChangeRepository,
                                ReleaseNodeRepository releaseNodeRepository,
                                ReleaseChangeItemRepository releaseChangeItemRepository,
                                ReviewRecordRepository reviewRecordRepository,
                                ReviewerTestEvidenceRepository reviewerTestEvidenceRepository,
                                ReleaseOperationRecordRepository releaseOperationRecordRepository,
                                TestReportRepository testReportRepository,
                                PasswordEncoder passwordEncoder,
                                org.springframework.core.env.Environment springEnvironment) {
        this.appUserRepository = appUserRepository;
        this.environmentRepository = environmentRepository;
        this.componentRepository = componentRepository;
        this.sourceProjectRepository = sourceProjectRepository;
        this.releaseChangeRepository = releaseChangeRepository;
        this.releaseNodeRepository = releaseNodeRepository;
        this.releaseChangeItemRepository = releaseChangeItemRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.reviewerTestEvidenceRepository = reviewerTestEvidenceRepository;
        this.releaseOperationRecordRepository = releaseOperationRecordRepository;
        this.testReportRepository = testReportRepository;
        this.passwordEncoder = passwordEncoder;
        this.springEnvironment = springEnvironment;
    }

    @Override
    @Transactional
    public void ensureSeedData() {
        seedUsers();
        seedEnvironments();
        seedProjects();
        seedComponents();
        seedDemoChange();
    }

    private void seedUsers() {
        user("juege", seedPassword("juege", "OSH_SEED_JUEGE_PASSWORD"), "觉哥", "OWNER");
        user("reviewer_a", seedPassword("reviewer-a", "OSH_SEED_REVIEWER_A_PASSWORD"), "评审 A", "REVIEWER");
        user("reviewer_b", seedPassword("reviewer-b", "OSH_SEED_REVIEWER_B_PASSWORD"), "评审 B", "REVIEWER");
        user("ops", seedPassword("ops", "OSH_SEED_OPS_PASSWORD"), "运维同学", "OPS");
    }

    private void user(String username, String password, String displayName, String role) {
        Optional<AppUser> existing = appUserRepository.findByUsername(username);
        AppUser user = existing.orElseGet(AppUser::new);
        user.setUsername(username);
        if (!existing.isPresent()) {
            if (isBlank(password)) {
                throw new IllegalStateException("种子用户 " + username + " 密码未配置，请设置对应启动环境变量。");
            }
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(true);
        appUserRepository.save(user);
    }

    private String seedPassword(String userKey, String envName) {
        String password = springEnvironment.getProperty("app.seed-users." + userKey + "-password");
        if (isBlank(password)) {
            password = springEnvironment.getProperty(envName);
        }
        return password;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void seedEnvironments() {
        environment("test", "测试环境", "TEST", "https://juegeresource.top/", null, null, "single", false, true, "READ_ONLY_OK", "测试服只读探测通过，announce 需要上线前再次确认。");
        environment("prod", "生产环境", "PROD", "https://osh.lol/", "https://osh.lol/", "https://osh.lol/", "blue", true, false, "PROTECTED", "生产服默认只读保护，写操作必须觉哥显式确认。");
        environment("prod-green", "生产绿环境", "PROD_GREEN", "https://osh.lol/", "https://osh.lol/", "https://osh.lol/", "green", true, false, "STANDBY", "绿环境用于先发布、测试、切流和可回切演练。");
    }

    private void environment(String code, String name, String kind, String baseUrl, String blueUrl, String greenUrl,
                             String currentColor, boolean supportsBlueGreen, boolean announceFileExists,
                             String healthStatus, String notes) {
        Environment env = environmentRepository.findByEnvCode(code).orElseGet(Environment::new);
        env.setEnvCode(code);
        env.setEnvName(name);
        env.setEnvKind(kind);
        env.setBaseUrl(baseUrl);
        env.setBlueUrl(blueUrl);
        env.setGreenUrl(greenUrl);
        env.setCurrentColor(currentColor);
        env.setSupportsBlueGreen(supportsBlueGreen);
        env.setAnnounceFileExists(announceFileExists);
        env.setHealthStatus(healthStatus);
        env.setNotes(notes);
        environmentRepository.save(env);
    }

    private void seedProjects() {
        sourceProject("osh-backend", "OSH 后端主项目", "https://github.com/juege-osh/osh-backend.git", "release/20260708",
                "8d2ee47cbb7dccaffabad8822ceadc8ccb7b8cb7", "JAVA", "LATEST_RELEASE", "后端上线基线来自最新 release/20260708。");
        sourceProject("osh-frontend", "OSH 前端主项目", "https://github.com/juege-osh/osh-frontend.git", "release/20260708",
                "868b87f55a7c267e40bf37d4fd0573973ae75ba3", "VUE", "LATEST_RELEASE", "前端上线基线来自最新 release/20260708。");
        sourceProject("osh-prod-release-new", "上线治理系统", "https://github.com/juege-osh/osh-prod-release-new.git", "release/20260708",
                "local-dev", "JAVA_VUE", "BUILDING", "本项目独立管理发布流程，不复用主站业务库。");
    }

    private void sourceProject(String key, String name, String repo, String branch, String commit, String type, String status, String summary) {
        SourceProject project = sourceProjectRepository.findByProjectKey(key).orElseGet(SourceProject::new);
        project.setProjectKey(key);
        project.setProjectName(name);
        project.setRepoUrl(repo);
        project.setReleaseBranch(branch);
        project.setReleaseCommit(commit);
        project.setSourceType(type);
        project.setStatus(status);
        project.setSummary(summary);
        sourceProjectRepository.save(project);
    }

    private void seedComponents() {
        component("mysql", "MySQL", "DATABASE", "/data/osh/config/mysql", "/data/osh/data/mysql", "/data/osh/compose/mysql", true, true, true, false, true, 10, 90, "增量 SQL 必须可回滚，禁止直接改课程和用户业务数据。");
        component("redis", "Redis", "CACHE", "/data/osh/config/redis", "/data/osh/data/redis", "/data/osh/compose/redis", true, true, true, false, true, 20, 80, "上线前后采集 key 数和关键前缀摘要。");
        component("nacos", "Nacos", "CONFIG", "/data/osh/config/nacos", "/data/osh/data/nacos", "/data/osh/compose/nacos", true, true, true, false, true, 30, 70, "配置变更先上绿环境，保留上一版配置快照。");
        component("zookeeper", "Zookeeper", "COORDINATION", "/data/osh/config/zookeeper", "/data/osh/data/zookeeper", "/data/osh/compose/zookeeper", true, true, true, false, true, 35, 65, "Kafka 依赖组件，配置和 compose 变更必须跟 Kafka 节点分开评审。");
        component("kafka", "Kafka", "MESSAGE", "/data/osh/config/kafka", "/data/osh/data/kafka", "/data/osh/compose/kafka", true, true, true, false, true, 40, 60, "记录 topic、consumer group 和 lag 摘要。");
        component("elasticsearch", "Elasticsearch", "SEARCH", "/data/osh/config/es", "/data/osh/data/es", "/data/osh/compose/es", true, true, true, false, true, 50, 50, "索引变更必须带别名切换和回滚说明。");
        component("kibana", "Kibana", "SEARCH_UI", "/data/osh/config/kibana", "/data/osh/data/kibana", "/data/osh/compose/kibana", true, true, true, false, false, 55, 45, "ES 配套控制台，配置变更要跟 ES 版本兼容。");
        component("hbase", "HBase", "STORAGE", "/data/osh/config/hbase", "/data/osh/data/hbase", "/data/osh/compose/hbase", true, true, true, false, true, 60, 40, "采集 namespace/table 摘要，不导出敏感业务明细。");
        component("xxl-job", "XXLJob", "SCHEDULER", "/data/osh/config/xxl-job", "/data/osh/data/xxl-job", "/data/osh/compose/xxl-job", true, true, true, false, true, 65, 35, "任务变更必须说明 cron、handler、路由策略、阻塞策略和回滚停用方式。");
        component("flink", "Flink", "STREAM", "/data/osh/config/flink", "/data/osh/data/flink", "/data/osh/compose/flink", true, true, true, false, true, 68, 32, "流任务上线要记录 job、checkpoint/savepoint、并发和回滚点。");
        component("java-backend", "Java 后端服务", "APP", "/data/osh/config/backend", "/data/osh/apps/backend", "/data/osh/compose/backend", true, true, true, false, true, 70, 30, "后端只从 release 分支构建，先发布绿环境。");
        component("vue-frontend", "Vue 前端", "WEB", "/data/osh/config/frontend", "/data/osh/apps/frontend", "/data/osh/compose/frontend", true, true, true, false, true, 80, 20, "前端静态资源带版本号，支持快速回滚。");
        component("filebeat", "Filebeat", "LOG", "/data/osh/config/filebeat", "/data/osh/data/filebeat", "/data/osh/compose/filebeat", true, true, true, false, false, 84, 16, "日志采集变更要确认路径、索引和回滚采集配置。");
        component("otel-collector", "OTel Collector", "OBSERVABILITY", "/data/osh/config/otel-collector", "/data/osh/data/otel-collector", "/data/osh/compose/otel-collector", true, true, true, false, false, 86, 14, "链路采集变更要确认 exporter、采样率和回滚配置。");
        component("secret-manager", "Secret Manager", "SECRET", "/data/osh/config/secret-manager", "/data/osh/data/secret-manager", "/data/osh/compose/secret-manager", true, true, true, false, true, 88, 12, "密钥服务只登记配置路径和版本，不在治理台保存密钥明文。");
        component("nginx", "Nginx 网关", "GATEWAY", "/data/osh/config/nginx", "/data/osh/data/nginx", "/data/osh/compose/nginx", true, true, true, false, true, 90, 10, "蓝绿切流只通过网关配置切换，必须保留上一版。");
        component("docker-compose", "Docker Compose 编排", "ORCHESTRATION", "/data/osh/config/compose", "/data/osh/data/compose", "/data/osh/compose", true, true, true, false, true, 100, 5, "新增组件必须符合配置目录、数据目录、compose 文件规范。");
        component("qdrant", "Qdrant", "VECTOR", "/data/osh/config/qdrant", "/data/osh/data/qdrant", "/data/osh/compose/qdrant", true, true, true, true, false, 105, 3, "测试服发现向量库组件，新增 collection 或配置变更要按扩展组件治理。");
        component("mongodb", "MongoDB 扩展组件", "DATABASE", "/data/osh/config/mongodb", "/data/osh/data/mongodb", "/data/osh/compose/mongodb", true, true, true, true, false, 110, 1, "扩展组件样例，可按同一目录规范接入。");
    }

    private void component(String key, String name, String type, String configDir, String dataDir, String deployPath,
                           boolean incremental, boolean rollback, boolean blueGreen, boolean extension, boolean core,
                           int installOrder, int rollbackOrder, String notes) {
        ComponentDefinition component = componentRepository.findByComponentKey(key).orElseGet(ComponentDefinition::new);
        component.setComponentKey(key);
        component.setComponentName(name);
        component.setComponentType(type);
        component.setConfigDir(configDir);
        component.setDataDir(dataDir);
        component.setDeployPath(deployPath);
        component.setSupportIncremental(incremental);
        component.setSupportRollback(rollback);
        component.setSupportBlueGreen(blueGreen);
        component.setExtension(extension);
        component.setCore(core);
        component.setInstallOrder(installOrder);
        component.setRollbackOrder(rollbackOrder);
        component.setActionTypes(actionTypesFor(key));
        component.setObservedStatus(observedStatusFor(key));
        component.setRuntimeInventory(runtimeInventoryFor(key));
        component.setNotes(notes);
        componentRepository.save(component);
    }

    private String actionTypesFor(String key) {
        if ("mysql".equals(key)) {
            return "MYSQL_SQL,SQL,CONFIG,COMPOSE_CHANGE";
        }
        if ("redis".equals(key)) {
            return "REDIS_SCRIPT,REDIS_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("nacos".equals(key)) {
            return "NACOS_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("zookeeper".equals(key)) {
            return "ZOOKEEPER_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("kafka".equals(key)) {
            return "KAFKA_TOPIC,KAFKA_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("elasticsearch".equals(key)) {
            return "ES_INDEX,ES_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("kibana".equals(key)) {
            return "KIBANA_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("hbase".equals(key)) {
            return "HBASE_DDL,HBASE_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("xxl-job".equals(key)) {
            return "XXLJOB_TASK,XXLJOB_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("flink".equals(key)) {
            return "FLINK_JOB,FLINK_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("java-backend".equals(key) || "vue-frontend".equals(key)) {
            return "CODE,CONFIG,COMPOSE_CHANGE";
        }
        if ("filebeat".equals(key)) {
            return "FILEBEAT_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("otel-collector".equals(key)) {
            return "OTEL_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("secret-manager".equals(key)) {
            return "SECRET_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("nginx".equals(key)) {
            return "NGINX_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("qdrant".equals(key)) {
            return "QDRANT_COLLECTION,QDRANT_CONFIG,CONFIG,COMPOSE_CHANGE";
        }
        if ("mongodb".equals(key)) {
            return "MONGODB_SCRIPT,CONFIG,COMPOSE_CHANGE";
        }
        return "CONFIG,COMPONENT,COMPOSE_CHANGE";
    }

    private String observedStatusFor(String key) {
        if ("hbase".equals(key) || "mongodb".equals(key)) {
            return "SUPPORTED_NOT_FOUND";
        }
        if ("qdrant".equals(key)) {
            return "TEST_ONLY_FOUND";
        }
        if ("vue-frontend".equals(key)) {
            return "BEHIND_NGINX";
        }
        return "FOUND_TEST_AND_PROD";
    }

    private String runtimeInventoryFor(String key) {
        if ("mysql".equals(key)) {
            return "test: osh-mysql:53306, osh-payment-mysql:3307; prod-blue: osh-mysql:53306, osh-payment-mysql:3307; prod-green: osh-g-mysql:23306";
        }
        if ("redis".equals(key)) {
            return "test: osh-redis:56379; prod-blue: osh-redis:56379; prod-green: osh-g-redis:26379";
        }
        if ("nacos".equals(key)) {
            return "test: osh-nacos:58848; prod-blue: osh-nacos:58848; prod-green: osh-g-nacos:28848";
        }
        if ("zookeeper".equals(key)) {
            return "test: osh-zookeeper:52181; prod-blue: osh-zookeeper:52181; prod-green: osh-g-zookeeper:22181";
        }
        if ("kafka".equals(key)) {
            return "test: osh-kafka:59092; prod-blue: osh-kafka:59092; prod-green: osh-g-kafka:29092";
        }
        if ("elasticsearch".equals(key)) {
            return "test: osh-es:59200/59300; prod-blue: osh-es:59200/59300; prod-green: osh-g-es:29200/29300";
        }
        if ("kibana".equals(key)) {
            return "test: osh-kibana:55601; prod-blue: osh-kibana:55601; prod-green: osh-g-kibana:25601";
        }
        if ("hbase".equals(key)) {
            return "test/prod docker ps 未发现 HBase 容器；平台保留 HBASE_DDL/HBASE_CONFIG 治理入口，真实执行前必须先确认实例位置";
        }
        if ("xxl-job".equals(key)) {
            return "test: osh-xxl-job:58086; prod-blue: osh-xxl-job:58086; prod-green: osh-g-xxl-job:28086";
        }
        if ("flink".equals(key)) {
            return "test: osh-flink-jm:58088 + tm1-6; prod-blue: osh-flink-jm:58088 + tm1-6; prod-green: osh-g-flink-jm:28088 + tm1-6";
        }
        if ("java-backend".equals(key)) {
            return "test: osh-backend:58081/8081; prod-blue: osh-backend:58081; prod-green: osh-g-backend:28081";
        }
        if ("vue-frontend".equals(key)) {
            return "frontend served by nginx; test/prod external domains are juegeresource.top and osh.lol";
        }
        if ("filebeat".equals(key)) {
            return "test: osh-filebeat; prod-blue: osh-filebeat; prod-green: osh-g-filebeat";
        }
        if ("otel-collector".equals(key)) {
            return "test: osh-otel-collector:54317/54318; prod-blue: osh-otel-collector:54317/54318; prod-green: osh-g-otel-collector:24317/24318";
        }
        if ("secret-manager".equals(key)) {
            return "test: osh-secret-manager:59100; prod-blue: osh-secret-manager:59100; prod-green: osh-g-secret-manager:29100";
        }
        if ("nginx".equals(key)) {
            return "test: osh-nginx:80/58080 plus host nginx; prod-blue: osh-nginx:80/58080 plus host nginx; prod-green: osh-g-nginx:28080/12781";
        }
        if ("docker-compose".equals(key)) {
            return "test compose found under /opt and /www; prod compose found under /opt/osh-prod-release-new plus backups";
        }
        if ("qdrant".equals(key)) {
            return "test: osh-qdrant:56333/56334; prod docker ps 未发现；作为扩展组件保留治理入口";
        }
        if ("mongodb".equals(key)) {
            return "test/prod docker ps 未发现 MongoDB 容器；作为扩展组件样例保留治理入口";
        }
        return "read-only inventory pending";
    }

    private void seedDemoChange() {
        Optional<ReleaseChange> existing = releaseChangeRepository.findByChangeCode("CHG-20260708-0001");
        if (existing.isPresent()) {
            ensureDemoDepth(existing.get());
            return;
        }
        ReleaseChange change = new ReleaseChange();
        change.setChangeCode("CHG-20260708-0001");
        change.setTitle("OSH release/20260708 蓝绿上线演练");
        change.setProjectBranch("release/20260708");
        change.setReleaseType(ReleaseType.NORMAL);
        change.setTargetEnvCode("prod");
        change.setTargetColor("green");
        change.setStatus(ChangeStatus.TESTING);
        change.setDeveloperUsername("ops");
        change.setDeveloperDisplayName("运维同学");
        change.setReviewerAUsername("reviewer_a");
        change.setReviewerBUsername("reviewer_b");
        change.setReviewMode("TWO_REVIEWERS_PLUS_JUEGE");
        change.setDemoRequired(false);
        change.setDemoConfirmed(false);
        change.setRiskLevel("HIGH");
        change.setSummary("覆盖测试服和生产蓝绿盘点到的 MySQL、Redis、Nacos、Kafka、ES、XXLJob、Flink、Nginx、日志、链路、Compose 和扩展组件。");
        change.setContentJson("{\"scope\":\"release/20260708\",\"prodWritePolicy\":\"manual-confirm-required\",\"courseAndUserModules\":\"protected\"}");
        change.setCurrentStep("自动化测试中");
        change.setFinalMessage("演示数据，只写治理库，不执行生产命令。");
        releaseChangeRepository.save(change);

        ensureDemoDepth(change);
    }

    private void ensureDemoDepth(ReleaseChange change) {
        List<ComponentDefinition> components = demoComponents();
        ensureDemoNodes(change, components);
        ensureDemoItems(change, components);
        ensureDemoReviews(change);
        ensureDemoEvidences(change);
        ensureDemoReports(change);
        ensureDemoOperations(change);
    }

    private List<ComponentDefinition> demoComponents() {
        List<ComponentDefinition> components = componentRepository.findAll();
        components.sort(Comparator.comparingInt(ComponentDefinition::getInstallOrder));
        List<ComponentDefinition> selected = new java.util.ArrayList<ComponentDefinition>();
        List<String> keys = Arrays.asList("mysql", "redis", "nacos", "zookeeper", "kafka", "elasticsearch", "kibana",
                "hbase", "xxl-job", "flink", "java-backend", "vue-frontend", "filebeat", "otel-collector",
                "secret-manager", "nginx", "docker-compose", "qdrant", "mongodb");
        for (ComponentDefinition component : components) {
            if (keys.contains(component.getComponentKey())) {
                selected.add(component);
            }
        }
        return selected;
    }

    private void ensureDemoNodes(ReleaseChange change, List<ComponentDefinition> components) {
        List<ReleaseNode> existingNodes = releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(change.getId());
        List<String> existingKeys = new java.util.ArrayList<String>();
        int order = 1;
        for (ReleaseNode node : existingNodes) {
            existingKeys.add(node.getComponentKey());
            if (node.getNodeOrder() >= order) {
                order = node.getNodeOrder() + 1;
            }
        }
        for (ComponentDefinition component : components) {
            if (existingKeys.contains(component.getComponentKey())) {
                continue;
            }
            ReleaseNode node = new ReleaseNode();
            node.setChangeId(change.getId());
            node.setNodeKey("node-" + component.getComponentKey() + "-" + order);
            node.setComponentKey(component.getComponentKey());
            node.setComponentName(component.getComponentName());
            node.setNodeType(component.getComponentType());
            node.setNodeOrder(order++);
            node.setRollbackOrder(component.getRollbackOrder());
            node.setStatus("PASSED");
            node.setActionType("GREEN_FIRST_INCREMENTAL");
            node.setHostName("prod-green");
            node.setCommandHint("dry-run only; execute requires juege approval");
            node.setConfigDir(component.getConfigDir());
            node.setDataDir(component.getDataDir());
            node.setDetailJson("{\"incremental\":true,\"rollback\":true,\"prodWrite\":\"blocked-by-default\"}");
            releaseNodeRepository.save(node);
        }
    }

    private void ensureDemoItems(ReleaseChange change, List<ComponentDefinition> components) {
        List<ReleaseChangeItem> existingItems = releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(change.getId());
        List<String> existingKeys = new java.util.ArrayList<String>();
        int order = 1;
        for (ReleaseChangeItem item : existingItems) {
            existingKeys.add(item.getComponentKey());
            if (item.getItemOrder() >= order) {
                order = item.getItemOrder() + 1;
            }
        }
        for (ComponentDefinition component : components) {
            if (existingKeys.contains(component.getComponentKey())) {
                continue;
            }
            ReleaseChangeItem item = new ReleaseChangeItem();
            item.setChangeId(change.getId());
            item.setItemKey(change.getChangeCode() + "-" + component.getComponentKey());
            item.setItemOrder(order++);
            item.setComponentKey(component.getComponentKey());
            item.setComponentName(component.getComponentName());
            item.setComponentType(component.getComponentType());
            item.setOwnerUsername("ops");
            item.setOwnerDisplayName("运维同学");
            item.setTitle(component.getComponentName() + " 增量上线演练");
            item.setItemType(defaultItemType(component));
            item.setPayloadPath(component.getDeployPath());
            item.setChangeContent(component.getComponentName() + " 在绿环境做增量上线演练，不改课程和用户业务数据。");
            item.setExecutionContent(defaultExecutionContent(component));
            item.setIncrementalPlan("先 dry-run，再按节点发布绿环境；检查配置目录 " + component.getConfigDir() + " 和部署目录 " + component.getDeployPath() + "。");
            item.setRollbackContent(defaultRollbackContent(component));
            item.setRollbackPlan("按 rollback_order 逆序恢复上一版配置和治理演练数据。");
            item.setCodeChangeSummary(defaultCodeSummary(component));
            item.setRiskAnalysis("风险分析：只做绿环境演练；真实生产执行前必须评估课程模块、用户模块和回滚窗口。");
            item.setBugAnalysis("疑似 bug 分析：重点检查配置拼写、脚本幂等、缓存一致性、消息重复和前后端字段兼容。");
            item.setVerificationCommands("dry-run：填写只读检查命令；健康检查：填写组件连通命令；回滚验证：填写恢复后检查命令。");
            item.setTestPlan("两位评审分别验证健康检查、接口/组件连通、回滚口径和异常处理。");
            item.setDataProbePlan("只采集数量摘要；课程模块和用户模块 added/removed/changed 必须为 0。");
            item.setSpecStatus("PASSED");
            item.setLifecycleStatus("AUTO_TEST_READY");
            item.setReviewerAConfirmed(true);
            item.setReviewerBConfirmed(true);
            item.setJuegeConfirmed(true);
            releaseChangeItemRepository.save(item);
        }
    }

    private String defaultItemType(ComponentDefinition component) {
        if ("DATABASE".equals(component.getComponentType())) {
            return "SQL";
        }
        if ("CONFIG".equals(component.getComponentType()) || "GATEWAY".equals(component.getComponentType())
                || "ORCHESTRATION".equals(component.getComponentType())) {
            return "CONFIG";
        }
        if ("APP".equals(component.getComponentType()) || "WEB".equals(component.getComponentType())) {
            return "CODE";
        }
        return "COMPONENT";
    }

    private String defaultExecutionContent(ComponentDefinition component) {
        if ("DATABASE".equals(component.getComponentType())) {
            return "-- 粘贴本次要上线的 SQL；必须带 WHERE、影响行数预估、备份方案和幂等说明。";
        }
        if ("CONFIG".equals(component.getComponentType()) || "GATEWAY".equals(component.getComponentType())
                || "ORCHESTRATION".equals(component.getComponentType())) {
            return "# 粘贴配置 diff、目标文件路径和校验命令。";
        }
        if ("APP".equals(component.getComponentType()) || "WEB".equals(component.getComponentType())) {
            return "分支：release/20260708\n提交范围：填写 commit range\n构建产物：填写 jar/镜像/静态资源路径";
        }
        return "填写本组件真实增量执行内容。";
    }

    private String defaultRollbackContent(ComponentDefinition component) {
        if ("DATABASE".equals(component.getComponentType())) {
            return "-- 粘贴 SQL 回滚语句；必须说明备份表、恢复条件和影响行数。";
        }
        if ("CONFIG".equals(component.getComponentType()) || "GATEWAY".equals(component.getComponentType())
                || "ORCHESTRATION".equals(component.getComponentType())) {
            return "# 粘贴回滚配置 diff 或上一版配置路径。";
        }
        if ("APP".equals(component.getComponentType()) || "WEB".equals(component.getComponentType())) {
            return "回滚版本：填写上一版 commit/镜像/包路径\n回滚命令：填写 dry-run 后的安全命令";
        }
        return "填写本组件真实回滚内容。";
    }

    private String defaultCodeSummary(ComponentDefinition component) {
        if ("APP".equals(component.getComponentType()) || "WEB".equals(component.getComponentType())) {
            return "代码改动大纲：填写模块、接口、配置、数据库兼容性、前后端联动点。";
        }
        return "非代码上线项；如脚本或配置会影响代码路径，也要写清楚。";
    }

    private void ensureDemoReviews(ReleaseChange change) {
        if (!reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(change.getId()).isEmpty()) {
            return;
        }
        review(change, "reviewer_a", "评审 A", "TEAM_REVIEW", "已检查组件上线内容和回滚口径。");
        review(change, "reviewer_b", "评审 B", "TEAM_REVIEW", "已复核功能测试和数据量对比范围。");
        review(change, "juege", "觉哥", "FINAL_APPROVAL", "最终确认通过。");
    }

    private void review(ReleaseChange change, String username, String displayName, String type, String comment) {
        ReviewRecord review = new ReviewRecord();
        review.setChangeId(change.getId());
        review.setReviewerUsername(username);
        review.setReviewerDisplayName(displayName);
        review.setReviewType(type);
        review.setPassed(true);
        review.setDemoRequired(false);
        review.setDemoConfirmed(true);
        review.setComment(comment);
        reviewRecordRepository.save(review);
    }

    private void ensureDemoEvidences(ReleaseChange change) {
        if (!reviewerTestEvidenceRepository.findByChangeIdOrderByCreatedAtAsc(change.getId()).isEmpty()) {
            return;
        }
        for (ReleaseChangeItem item : releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(change.getId())) {
            evidence(change, item, "reviewer_a", "评审 A", "test", "已在测试环境验证 " + item.getComponentName() + " 的功能和回滚口径。");
            evidence(change, item, "reviewer_b", "评审 B", "prod-green", "已在生产绿环境复核 " + item.getComponentName() + " 的功能和数据影响。");
        }
    }

    private void evidence(ReleaseChange change, ReleaseChangeItem item, String username, String displayName, String envCode, String content) {
        ReviewerTestEvidence evidence = new ReviewerTestEvidence();
        evidence.setChangeId(change.getId());
        evidence.setItemId(item.getId());
        evidence.setComponentKey(item.getComponentKey());
        evidence.setReviewerUsername(username);
        evidence.setReviewerDisplayName(displayName);
        evidence.setTestType("FUNCTION");
        evidence.setEnvironmentCode(envCode);
        evidence.setPassed(true);
        evidence.setDemoObserved(true);
        evidence.setResponsibilityAccepted(true);
        evidence.setEvidence(content);
        reviewerTestEvidenceRepository.save(evidence);
    }

    private void ensureDemoReports(ReleaseChange change) {
        List<TestReport> reports = testReportRepository.findByChangeIdOrderByCreatedAtAsc(change.getId());
        List<String> types = new java.util.ArrayList<String>();
        for (TestReport report : reports) {
            types.add(report.getReportType());
        }
        if (!types.contains("SPEC")) {
            report(change, "SPEC", "组件规范校验样例", "全组件配置目录、数据目录、部署目录、增量计划和回滚计划已齐。",
                    "{\"components\":\"mysql,redis,nacos,zookeeper,kafka,elasticsearch,kibana,hbase,xxl-job,flink,backend,frontend,filebeat,otel,secret-manager,nginx,compose,qdrant,mongodb\"}",
                    "规范齐全。");
        }
        if (!types.contains("FUNCTION")) {
            report(change, "FUNCTION", "绿环境功能测试样例", "接口、缓存、消息、搜索、配置中心、存储和前端入口检查均通过。",
                    "{\"mysql\":\"ok\",\"redis\":\"ok\",\"kafka\":\"ok\",\"nacos\":\"ok\",\"es\":\"ok\",\"hbase\":\"ok\",\"api\":\"ok\"}",
                    "与上线范围一致，未发现课程和用户模块数据写入。");
        }
        if (!types.contains("DATA")) {
            report(change, "DATA", "数据量对比样例", "课程模块和用户模块新增、删除、修改均为 0，只新增治理演练记录。",
                    "{\"course\":{\"changed\":0},\"user\":{\"changed\":0},\"release_governance\":{\"added\":12}}",
                    "数据差异符合上线内容。");
        }
        if (!types.contains("ENV_DIFF")) {
            report(change, "ENV_DIFF", "生产测试环境差异样例", "测试环境单套，生产环境蓝绿，差异可解释。",
                    "{\"test\":\"single\",\"prod\":\"blue-green\"}", "差异不阻塞上线治理流程。");
        }
        if (!types.contains("ANNOUNCE")) {
            report(change, "ANNOUNCE", "announce 检查样例", "测试环境 announce 已纳入检查。",
                    "{\"testEnv\":\"juegeresource.top\",\"announceFileExists\":true}", "announce 检查通过。");
        }
    }

    private void report(ReleaseChange change, String type, String title, String summary, String detail, String verdict) {
        TestReport report = new TestReport();
        report.setChangeId(change.getId());
        report.setReportType(type);
        report.setTitle(title);
        report.setSummary(summary);
        report.setDetailJson(detail);
        report.setAiVerdict(verdict);
        report.setPassed(true);
        testReportRepository.save(report);
    }

    private void ensureDemoOperations(ReleaseChange change) {
        if (!releaseOperationRecordRepository.findByChangeIdOrderByCreatedAtAsc(change.getId()).isEmpty()) {
            return;
        }
        operation(change, "CREATE_CHANGE", "RECORDED", "已生成完整演练单。");
        operation(change, "AUTO_FUNCTION_TEST", "PASSED", "绿环境功能测试 dry-run 通过。");
        operation(change, "AUTO_DATA_DIFF", "PASSED", "数据量对比通过。");
        operation(change, "SWITCH_TO_GREEN", "RECORDED", "切绿动作已记录，安全模式未改生产网关。");
        operation(change, "NODE_ROLLBACK", "RECORDED", "回滚链路已记录，安全模式未改业务数据。");
    }

    private void operation(ReleaseChange change, String type, String status, String summary) {
        ReleaseOperationRecord operation = new ReleaseOperationRecord();
        operation.setChangeId(change.getId());
        operation.setOperationType(type);
        operation.setOperationStatus(status);
        operation.setEnvironmentCode(change.getTargetEnvCode());
        operation.setTargetColor(change.getTargetColor());
        operation.setActorUsername("system");
        operation.setActorDisplayName("治理台");
        operation.setSafeMode(true);
        operation.setSummary(summary);
        operation.setDetailJson("{\"safeMode\":true,\"prodBusinessDataChanged\":false}");
        releaseOperationRecordRepository.save(operation);
    }
}
