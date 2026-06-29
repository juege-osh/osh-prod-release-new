package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.model.AppUser;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.model.ReleaseNode;
import com.juege.oshrelease.model.SourceProject;
import com.juege.oshrelease.model.TestReport;
import com.juege.oshrelease.repo.AppUserRepository;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.ReleaseNodeRepository;
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
    private final TestReportRepository testReportRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapServiceImpl(AppUserRepository appUserRepository,
                                EnvironmentRepository environmentRepository,
                                ComponentRepository componentRepository,
                                SourceProjectRepository sourceProjectRepository,
                                ReleaseChangeRepository releaseChangeRepository,
                                ReleaseNodeRepository releaseNodeRepository,
                                TestReportRepository testReportRepository,
                                PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.environmentRepository = environmentRepository;
        this.componentRepository = componentRepository;
        this.sourceProjectRepository = sourceProjectRepository;
        this.releaseChangeRepository = releaseChangeRepository;
        this.releaseNodeRepository = releaseNodeRepository;
        this.testReportRepository = testReportRepository;
        this.passwordEncoder = passwordEncoder;
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
        user("juege", "Juege@2026", "觉哥", "OWNER");
        user("reviewer_a", "Review@2026", "评审 A", "REVIEWER");
        user("reviewer_b", "Review@2026", "评审 B", "REVIEWER");
        user("ops", "Ops@2026", "运维同学", "OPS");
    }

    private void user(String username, String password, String displayName, String role) {
        Optional<AppUser> existing = appUserRepository.findByUsername(username);
        AppUser user = existing.orElseGet(AppUser::new);
        user.setUsername(username);
        if (!existing.isPresent()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(true);
        appUserRepository.save(user);
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
        component("kafka", "Kafka", "MESSAGE", "/data/osh/config/kafka", "/data/osh/data/kafka", "/data/osh/compose/kafka", true, true, true, false, true, 40, 60, "记录 topic、consumer group 和 lag 摘要。");
        component("elasticsearch", "Elasticsearch", "SEARCH", "/data/osh/config/es", "/data/osh/data/es", "/data/osh/compose/es", true, true, true, false, true, 50, 50, "索引变更必须带别名切换和回滚说明。");
        component("hbase", "HBase", "STORAGE", "/data/osh/config/hbase", "/data/osh/data/hbase", "/data/osh/compose/hbase", true, true, true, false, true, 60, 40, "采集 namespace/table 摘要，不导出敏感业务明细。");
        component("java-backend", "Java 后端服务", "APP", "/data/osh/config/backend", "/data/osh/apps/backend", "/data/osh/compose/backend", true, true, true, false, true, 70, 30, "后端只从 release 分支构建，先发布绿环境。");
        component("vue-frontend", "Vue 前端", "WEB", "/data/osh/config/frontend", "/data/osh/apps/frontend", "/data/osh/compose/frontend", true, true, true, false, true, 80, 20, "前端静态资源带版本号，支持快速回滚。");
        component("nginx", "Nginx 网关", "GATEWAY", "/data/osh/config/nginx", "/data/osh/data/nginx", "/data/osh/compose/nginx", true, true, true, false, true, 90, 10, "蓝绿切流只通过网关配置切换，必须保留上一版。");
        component("docker-compose", "Docker Compose 编排", "ORCHESTRATION", "/data/osh/config/compose", "/data/osh/data/compose", "/data/osh/compose", true, true, true, false, true, 100, 5, "新增组件必须符合配置目录、数据目录、compose 文件规范。");
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
        component.setNotes(notes);
        componentRepository.save(component);
    }

    private void seedDemoChange() {
        if (releaseChangeRepository.findByChangeCode("CHG-20260708-0001").isPresent()) {
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
        change.setSummary("覆盖 MySQL、Redis、Kafka、Nacos、后端和前端，先在绿环境完成自动化测试。");
        change.setContentJson("{\"scope\":\"release/20260708\",\"prodWritePolicy\":\"manual-confirm-required\",\"courseAndUserModules\":\"protected\"}");
        change.setCurrentStep("自动化测试中");
        change.setFinalMessage("演示数据，只写治理库，不执行生产命令。");
        releaseChangeRepository.save(change);

        List<ComponentDefinition> components = componentRepository.findAll();
        components.sort(Comparator.comparingInt(ComponentDefinition::getInstallOrder));
        int index = 1;
        for (ComponentDefinition component : components) {
            if (!Arrays.asList("mysql", "redis", "nacos", "kafka", "java-backend", "vue-frontend", "nginx").contains(component.getComponentKey())) {
                continue;
            }
            ReleaseNode node = new ReleaseNode();
            node.setChangeId(change.getId());
            node.setNodeKey("node-" + component.getComponentKey());
            node.setComponentKey(component.getComponentKey());
            node.setComponentName(component.getComponentName());
            node.setNodeType(component.getComponentType());
            node.setNodeOrder(index++);
            node.setRollbackOrder(component.getRollbackOrder());
            node.setStatus("PASSED");
            node.setActionType("GREEN_FIRST");
            node.setHostName("prod-green");
            node.setCommandHint("dry-run only; execute requires juege approval");
            node.setConfigDir(component.getConfigDir());
            node.setDataDir(component.getDataDir());
            node.setDetailJson("{\"incremental\":true,\"rollback\":true,\"prodWrite\":\"blocked-by-default\"}");
            releaseNodeRepository.save(node);
        }

        TestReport report = new TestReport();
        report.setChangeId(change.getId());
        report.setReportType("FUNCTION");
        report.setTitle("绿环境功能测试样例");
        report.setSummary("接口、缓存、消息、配置中心检查均为模拟通过，正式上线前需重新执行。");
        report.setDetailJson("{\"mysql\":\"ok\",\"redis\":\"ok\",\"kafka\":\"ok\",\"nacos\":\"ok\",\"api\":\"ok\"}");
        report.setAiVerdict("与上线范围一致，未发现课程和用户模块数据写入。");
        report.setPassed(true);
        testReportRepository.save(report);
    }
}
