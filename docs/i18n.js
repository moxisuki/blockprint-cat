/* ================================================================
   BlockPrint Cat — i18n dictionary + language switcher
   ================================================================ */
var I18N = {
  zh: {
    'hero.sub':         'Android 端 Minecraft 蓝图管理与 3D 预览工具',
    'hero.sub_en':      'Minecraft blueprint manager & 3D preview for Android',
    'badge.oss':        '开源 / Open Source',
    'btn.download':     '下载',
    'btn.download_long':'下载最新版本',
    'btn.source_long':  '查看源码',
    'features.title':   '核心功能',
    'features.desc':    '一站式管理你的 Minecraft 蓝图——从本地文件到 3D 预览，从 PC 同步到社区探索。',
    'feature1.title':   '本地蓝图管理',
    'feature1.desc':    '导入 .litematic / .schematic / .nbt / .json 文件，在手机上浏览、搜索、转换、重命名。',
    'feature2.title':   'PC 端桥接同步',
    'feature2.desc':    '局域网实时连接 blockprint-link 模组，支持扫码配对与断点续传，双向同步蓝图。',
    'feature3.title':   '实时 3D 预览',
    'feature3.desc':    '基于 Filament 的实时光线追踪渲染，支持镜头切换、光照预设、原版与模组方块。',
    'feature.num1':     '01',
    'feature.num2':     '02',
    'feature.num3':     '03',
    'feature.tag1':     '本地',
    'feature.tag2':     '同步',
    'feature.tag3':     '预览',
    'screenshots.title':'应用截图',
    'screenshots.desc':'直观感知 BlockPrint Cat 在你手机上的样子。',
    'label.home':       '首页',
    'label.preview':    '3D 预览',
    'label.details':    '蓝图详情',
    'workflow.title':   '使用流程',
    'workflow.desc':    '四步串联你的建造工作流，从导入到分享一气呵成。',
    'step1.title':      '导入蓝图',
    'step1.desc':       '通过 SAF 文件选择器导入 .litematic / .schematic 等格式的蓝图文件。',
    'step2.title':      '3D 预览',
    'step2.desc':       '加载原版或模组资源包，实时渲染方块模型，分层查看与镜头控制。',
    'step3.title':      'PC 同步',
    'step3.desc':       '连接 PC 端 blockprint-link 模组，局域网传输蓝图，双向同步建筑方案。',
    'step4.title':      '社区探索',
    'step4.desc':       '浏览 MCS / CMS 社区作品，按标签与热度搜索，一键下载心仪蓝图。',
    'tech.title':       '技术栈 & 配套项目',
    'tech.desc':        '基于最新的 Android 技术栈构建，与两个核心配套项目协同工作。',
    'tech.kotlin':      '100% 与 Java 互操作，协程 + Flow 原生支持。',
    'tech.compose':     '声明式 UI · 状态驱动 · 实时预览。',
    'tech.hilt':        '基于 Dagger 的官方 DI，编译时安全。',
    'tech.room':        'SQLite 抽象层 · 类型安全 · 协程支持。',
    'tech.sceneview':   'Filament 引擎 · 实时 3D · 模组方块支持。',
    'tech.okhttp':      'HTTP/2 · WebSocket · 拦截器链。',
    'tech.companion':   '配套开源项目',
    'get.title':        '立即开始',
    'get.desc':         '前往 GitHub Releases 下载最新 APK，或克隆源码自行构建。',
    'get.tagline':      '准备好让 Minecraft 蓝图在手机上活起来了吗？',
    'footer.legal_zh':  '仅供学习交流',
    'footer.legal_en':  'For learning and communication only'
  },
  en: {
    'hero.sub':         'Android Minecraft Blueprint Manager & 3D Preview',
    'hero.sub_en':      'Minecraft blueprint manager & 3D preview for Android',
    'badge.oss':        'Open Source',
    'btn.download':     'Download',
    'btn.download_long':'Download Latest',
    'btn.source_long':  'Source Code',
    'features.title':   'Core Features',
    'features.desc':    'Manage Minecraft blueprints end-to-end—from local files to 3D preview, PC sync to community.',
    'feature1.title':   'Local Blueprint Management',
    'feature1.desc':    'Import .litematic, .schematic, .nbt, .json files. Browse, search, convert, rename on your phone.',
    'feature2.title':   'PC Bridge Sync',
    'feature2.desc':    'Connect to the blockprint-link mod over LAN. QR pairing, resumable transfers, two-way sync.',
    'feature3.title':   'Real-time 3D Preview',
    'feature3.desc':    'Real-time Filament-based rendering. Camera modes, lighting presets, vanilla & mod blocks.',
    'feature.num1':     '01',
    'feature.num2':     '02',
    'feature.num3':     '03',
    'feature.tag1':     'FILES',
    'feature.tag2':     'SYNC',
    'feature.tag3':     'PREVIEW',
    'screenshots.title':'Screenshots',
    'screenshots.desc':'See what BlockPrint Cat looks like on your phone.',
    'label.home':       'Home',
    'label.preview':    '3D Preview',
    'label.details':    'Details',
    'workflow.title':   'Workflow',
    'workflow.desc':    'Four steps from import to share.',
    'step1.title':      'Import Blueprints',
    'step1.desc':       'Pick files via SAF. Supports .litematic, .schematic, and more.',
    'step2.title':      '3D Preview',
    'step2.desc':       'Load vanilla or mod assets. Real-time block rendering with layer views and camera control.',
    'step3.title':      'PC Sync',
    'step3.desc':       'Connect to the blockprint-link mod on PC. Transfer blueprints over LAN, sync both ways.',
    'step4.title':      'Community Explore',
    'step4.desc':       'Browse MCS / CMS community. Filter by tag and popularity. One-click download.',
    'tech.title':       'Tech Stack & Companion Projects',
    'tech.desc':        'Built on the latest Android toolchain, working alongside two core companion projects.',
    'tech.kotlin':      '100% Java interop, native coroutines + Flow.',
    'tech.compose':     'Declarative UI · State-driven · Live preview.',
    'tech.hilt':        'Official Dagger-based DI, compile-time safe.',
    'tech.room':        'SQLite abstraction · Type-safe · Coroutine support.',
    'tech.sceneview':   'Filament engine · Real-time 3D · Mod block support.',
    'tech.okhttp':      'HTTP/2 · WebSocket · Interceptors.',
    'tech.companion':   'Companion Projects',
    'get.title':        'Get Started',
    'get.desc':         'Download the latest APK from GitHub Releases, or clone the source to build it yourself.',
    'get.tagline':      'Ready to bring Minecraft blueprints to life on your phone?',
    'footer.legal_zh':  'For learning and communication only',
    'footer.legal_en':  'For learning and communication only'
  }
};

var LANG_KEY = 'bpc-lang';
var LANG_SUPPORTED = ['zh', 'en'];

function detectLang() {
  try {
    var saved = localStorage.getItem(LANG_KEY);
    if (saved && LANG_SUPPORTED.indexOf(saved) !== -1) return saved;
  } catch (e) {}
  if (typeof navigator !== 'undefined') {
    var langs = (navigator.languages || [navigator.language] || []).join(',').toLowerCase();
    if (/[,\s]en-?/.test(',' + langs)) return 'en';
  }
  return 'zh';
}

function applyLangImmediate(lang) {
  if (LANG_SUPPORTED.indexOf(lang) === -1) lang = 'zh';
  var dict = I18N[lang] || {};
  document.documentElement.lang = (lang === 'zh') ? 'zh-CN' : 'en';
  document.documentElement.setAttribute('data-lang', lang);
  document.title = (lang === 'zh')
    ? 'BlockPrint Cat — Android Minecraft 蓝图管理 & 3D 预览工具'
    : 'BlockPrint Cat — Android Minecraft Blueprint Manager & 3D Preview';
  var meta = document.querySelector('meta[name="description"]');
  if (meta) {
    meta.content = (lang === 'zh')
      ? 'Android 端 Minecraft 蓝图管理与 3D 预览工具。导入 .litematic / .schematic 文件，局域网同步 PC 端，实时 3D 渲染预览。'
      : 'Android Minecraft blueprint manager & 3D preview. Import .litematic / .schematic files, sync with PC over LAN, real-time 3D rendering.';
  }
  document.querySelectorAll('[data-i18n]').forEach(function(el) {
    var k = el.getAttribute('data-i18n');
    if (dict[k] !== undefined) el.textContent = dict[k];
  });
  document.querySelectorAll('.lang-btn').forEach(function(btn) {
    var active = btn.getAttribute('data-lang') === lang;
    btn.classList.toggle('active', active);
    btn.setAttribute('aria-pressed', active ? 'true' : 'false');
  });
  try { localStorage.setItem(LANG_KEY, lang); } catch (e) {}
}

function applyLang(lang) {
  if (LANG_SUPPORTED.indexOf(lang) === -1) lang = 'zh';
  if (document.documentElement.getAttribute('data-lang') === lang) return;

  var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (reduced || !document.body.classList.contains('ready')) {
    applyLangImmediate(lang);
    return;
  }

  document.body.classList.add('lang-switching');
  document.body.classList.remove('lang-entered');

  setTimeout(function() {
    applyLangImmediate(lang);
    document.body.classList.remove('lang-switching');
    document.body.classList.add('lang-entered');
    setTimeout(function() {
      document.body.classList.remove('lang-entered');
    }, 380);
  }, 180);
}

document.addEventListener('DOMContentLoaded', function() {
  document.body.classList.add('ready');
  var initial = detectLang();
  applyLang(initial);
  document.querySelectorAll('.lang-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
      applyLang(btn.getAttribute('data-lang'));
    });
  });
});
