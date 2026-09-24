# web/ —— 文档站（Spring Boot + Java）

一个独立的 Spring Boot 应用，用来跑 MineGenshin 的文档站。**与 Mod 本体完全隔离**：独立的 Gradle 构建、独立的依赖、独立的进程；根目录的 `./gradlew build` 与 Mod 的 jar 都不会与它产生关系。

## 为什么是 Spring Boot

- **Markdown 不再需要预生成**：文档在请求时由 Java 渲染（commonmark + GFM 表格扩展），改完源文件刷新即可，没有 Node 构建步骤。
- 文档只有一份源：`site.docs-root` 指向仓库根，直接读 `CHARACTER_SYSTEM.md`、`docs/*.md` 等，不存在副本漂移。
- 后续要加搜索、版本切换、鉴权或 API，都是普通的 Java 后端工作。

## 运行

```bash
cd web
./gradlew bootRun                 # 开发运行，默认 http://localhost:8081
./gradlew bootJar                 # 打包 → build/libs/minegenshin-web.jar
java -jar build/libs/minegenshin-web.jar
```

在仓库根目录外运行时，用参数或环境变量指定文档源目录：

```bash
java -jar minegenshin-web.jar --site.docs-root=E:/MCMOD/MineGenshin-26.2
SITE_DOCS_ROOT=/path/to/repo java -jar minegenshin-web.jar
```

端口用 `--server.port=8081` 覆盖；配置项都在 `src/main/resources/application.yml`。

## 路由

| 路径 | 内容 |
|---|---|
| `/` | 首页（卡片导航、协议说明） |
| `/doc/{slug}` | 由 Markdown 实时渲染的文档页，slug 见 `DocCatalog` |
| `/entity-development.html` | 手写的实体开发文档（`src/main/resources/static/`） |
| `/assets/docs.css`、`/assets/docs.js` | 站点样式与导航脚本 |

侧边栏、页内目录、过滤框与滚动高亮由 `docs.js` 提供：跨页菜单写在 `DOCS_PAGES`，页内目录直接读页面的 `h1`/`h2`/`h3`（新增章节不用改导航）。标题锚点 id 由 `MarkdownRenderer` 按 GitHub 规则注入，保证文档内部的 `#锚点` 链接可用。

## 加一篇文档

1. 把 Markdown 放进仓库（推荐 `docs/`）。
2. 在 `DocCatalog.DOCS` 里加一行：`new Doc("slug", "标题", "相对仓库根的路径.md")`。

菜单会自动出现（`docs.js` 的 `DOCS_PAGES` 里补一个同名条目即可）。

## 依赖隔离说明

| 关注点 | 结论 |
|---|---|
| Mod 编译/打包 | 不受影响：`web/` 是独立构建，根项目不 include 它 |
| 依赖来源 | 只有 web 侧用 Maven Central（Spring Boot、commonmark） |
| 版本工具链 | web 用 Java 21 + Gradle 8.14；Mod 用 Java 25 toolchain + Gradle 9.2.1 |
| 运行时 | 文档站是独立进程，Mod 不需要它也能跑 |

## 预览

浏览器打开 <http://localhost:8081/>。若要临时快速看静态内容，也可以直接访问 `src/main/resources/static/` 下的页面，但 `/doc/*` 路由需要应用在运行。

## 在 IntelliJ IDEA 里运行

仓库已提供两个共享运行配置（`.run/` 目录，IDEA 打开仓库根目录即可看到）：

| 配置 | 说明 |
|---|---|
| `MineGenshin Web (8081)` | Application 型：直接跑 `SiteApplication`，工作目录 `web/`，并传 `-Dsite.docs-root=$PROJECT_DIR$`。需要先在 IDEA 里把 `web` 链接为 Gradle 项目（Gradle 面板 → Link Gradle Project → 选 `web/settings.gradle`），否则模块 `minegenshin-web.main` 不存在 |
| `MineGenshin Web (gradle bootRun)` | Gradle 型：对 `web` 执行 `bootRun`，不依赖模块导入，只要 IDEA 认识这个 Gradle 构建即可 |

两种方式都默认 8081 端口；若 IDEA 把仓库根识别成项目、而 `web` 是独立构建，优先用 Gradle 型配置。
