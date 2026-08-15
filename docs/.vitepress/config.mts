import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    lang: 'zh-CN',
    title: 'Lava',
    description: '一套一致, 安全, 开箱即用的 Java 基础设施工具库',
    themeConfig: {
        // https://vitepress.dev/reference/default-theme-config
        nav: [
            {text: '首页', link: '/'},
            {text: '快速开始', link: '/quick-start'},
            {text: 'GitHub', link: 'https://github.com/zhengshuyun-com/lava'}
        ],

        sidebar: [
            {
                text: '开始使用',
                collapsed: false,
                items: [
                    {text: '项目介绍', link: '/introduction'},
                    {text: '快速开始', link: '/quick-start'},
                    {text: '模块概览', link: '/modules'}
                ]
            },
            {
                text: 'lava-core',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-core/README'},
                    {text: 'RetryUtil 重试执行', link: '/lava-core/retry-util'},
                    {text: 'TimeUtil 时间解析', link: '/lava-core/time-util'},
                    {text: 'DurationFormatter 时长格式化', link: '/lava-core/duration-formatter'},
                    {text: 'IdUtil ID 生成', link: '/lava-core/id-util'},
                    {text: 'IoUtil IO 与数据传输', link: '/lava-core/io-util'},
                    {text: 'Validate 参数校验', link: '/lava-core/validate'},
                    {text: 'EmailValidator 邮箱校验', link: '/lava-core/email-validator'}
                ]
            },
            {
                text: 'lava-json',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-json/README'},
                    {text: 'JsonUtil 编解码', link: '/lava-json/json-util'},
                    {text: 'Tree 模型', link: '/lava-json/tree-model'},
                    {text: 'JsonBuilder 自定义配置', link: '/lava-json/json-builder'},
                    {text: '时间格式规范', link: '/lava-json/time-format'},
                    {text: 'SafeLong 长整型安全序列化', link: '/lava-json/safe-long'}
                ]
            },
            {
                text: 'lava-http',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-http/README'},
                    {text: 'HttpRequest 请求构建', link: '/lava-http/http-request'},
                    {text: 'HttpResponse 响应读取', link: '/lava-http/http-response'},
                    {text: 'HttpClient 客户端配置', link: '/lava-http/http-client-config'},
                    {text: 'Multipart 上传', link: '/lava-http/multipart-upload'},
                    {text: 'SSE 事件流', link: '/lava-http/sse'}
                ]
            },
            {
                text: 'lava-crypto',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-crypto/README'},
                    {text: 'PasswordHasher 密码哈希', link: '/lava-crypto/password-hasher'},
                    {text: 'EC 密钥生成与读取', link: '/lava-crypto/ec-keys'},
                    {text: 'PEM/DER/JKS/PKCS12 速查', link: '/lava-crypto/key-format-cheatsheet'}
                ]
            },
            {
                text: 'lava-jwt',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-jwt/README'},
                    {text: 'JwtUtil 签发与验签', link: '/lava-jwt/jwt-util'},
                    {text: 'JWT 算法选择', link: '/lava-jwt/jwt-algorithms'},
                    {text: 'JWT ES256 签发与验证', link: '/lava-jwt/jwt-es256'}
                ]
            },
            {
                text: 'lava-schedule',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-schedule/README'},
                    {text: 'TaskScheduler 任务提交', link: '/lava-schedule/task-scheduler'},
                    {text: 'Trigger 触发器', link: '/lava-schedule/trigger'},
                    {text: 'ScheduledTask 生命周期', link: '/lava-schedule/scheduled-task'},
                    {text: 'Cron 表达式', link: '/lava-schedule/cron-expression'},
                    {text: '任务执行器配置', link: '/lava-schedule/task-executor'}
                ]
            },
            {
                text: 'lava-mail',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-mail/README'},
                    {text: 'MailSender 发送邮件', link: '/lava-mail/mail-sender'},
                    {text: 'MailReader 读取邮件', link: '/lava-mail/mail-reader'},
                    {text: 'MailCredential 凭证', link: '/lava-mail/mail-credential'},
                    {text: 'MailProviders 厂商预置', link: '/lava-mail/mail-providers'},
                    {text: 'QQ 邮箱接入', link: '/lava-mail/qq-mail'},
                    {text: 'Outlook OAuth2 接入', link: '/lava-mail/outlook-oauth2'},
                    {text: '邮件附件', link: '/lava-mail/mail-attachment'}
                ]
            },
            {
                text: 'lava-bom',
                collapsed: false,
                items: [
                    {text: '概览', link: '/lava-bom/README'}
                ]
            }
        ],

        socialLinks: [
            {icon: 'github', link: 'https://github.com/zhengshuyun-com/lava'}
        ],

        docFooter: {
            prev: '上一页',
            next: '下一页'
        },

        outline: {
            label: '本页目录'
        },

        darkModeSwitchLabel: '主题',
        lightModeSwitchTitle: '切换到浅色模式',
        darkModeSwitchTitle: '切换到深色模式',
        sidebarMenuLabel: '菜单',
        returnToTopLabel: '返回顶部',
        langMenuLabel: '切换语言',
        skipToContentLabel: '跳转到内容',

        search: {
            provider: 'local',
            options: {
                translations: {
                    button: {
                        buttonText: '搜索',
                        buttonAriaLabel: '搜索'
                    },
                    modal: {
                        displayDetails: '显示详细列表',
                        resetButtonTitle: '重置搜索',
                        backButtonTitle: '关闭搜索',
                        noResultsText: '没有找到结果',
                        footer: {
                            selectText: '选择',
                            selectKeyAriaLabel: '回车',
                            navigateText: '切换',
                            navigateUpKeyAriaLabel: '上箭头',
                            navigateDownKeyAriaLabel: '下箭头',
                            closeText: '关闭',
                            closeKeyAriaLabel: 'Esc'
                        }
                    }
                }
            }
        },

        notFound: {
            title: '页面不存在',
            quote: '你访问的页面可能已经移动或删除.',
            linkLabel: '返回首页',
            linkText: '返回首页'
        },

        footer: {
            message: '一套一致, 安全, 开箱即用的 Java 基础设施工具库.'
        }
    }
})
