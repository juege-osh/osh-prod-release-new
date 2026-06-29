# Spec: release-governance

## Requirement 1

系统必须保存至少两个环境：测试环境和生产环境。

### Scenario

- 进入总览页时，系统展示测试环境和生产环境的当前状态。

## Requirement 2

系统必须支持组件目录扩展。

### Scenario

- 用户新增 MongoDB 组件后，组件列表和变更单节点都能看到。

## Requirement 3

系统必须支持变更单按节点拆分上线顺序。

### Scenario

- 用户创建包含 MySQL、Redis、Kafka 的变更单后，系统自动给出节点顺序和回滚顺序。

## Requirement 4

系统必须支持双人评审。

### Scenario

- 变更单至少有两个评审人。
- 如果开发人兼评审人 A，系统要求先记录 A 向 B 的演示确认。

## Requirement 5

系统必须支持常规上线和紧急上线。

### Scenario

- 常规上线需要组内两人审核后再等觉哥确认。
- 紧急上线需要组内两人审核后，再加觉哥确认。

## Requirement 6

系统必须支持蓝绿切换和回滚。

### Scenario

- 系统先对绿环境做变更和测试。
- 测试通过后再切流。
- 切流后如果发现问题，可以立即回切到蓝环境。

## Requirement 7

系统必须输出功能测试、数据对比和 AI 汇总报告。

### Scenario

- 用户执行一次测试后，报告要显示新增、减少、变化的数据摘要。

