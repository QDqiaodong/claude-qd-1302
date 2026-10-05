<script setup>
import { computed, onMounted, ref } from 'vue'
import router from './router'
import api from './api'

import PondView from './views/PondView.vue'
import StockingView from './views/StockingView.vue'
import WaterView from './views/WaterView.vue'
import HarvestView from './views/HarvestView.vue'

// ------------------------------------------------------------------
// 导航形态：左侧塘口卡列表（点击卡片选中该塘口，右侧展示该塘口详情）。
// 操作范式：塘口卡 + 水质三环 + 捕捞批次流水。
// 右侧顶部四个模块页签驱动路由切换，选中塘口以 prop 下发给各视图。
// ------------------------------------------------------------------

const modules = [
  { path: '/pond', label: '塘口总览', comp: PondView },
  { path: '/stocking', label: '鱼苗投放', comp: StockingView },
  { path: '/water', label: '水质检测', comp: WaterView },
  { path: '/harvest', label: '捕捞出塘', comp: HarvestView }
]

const ponds = ref([])
const selectedPondId = ref(null)
const currentPath = ref(router.currentRoute.value.path)

const currentView = computed(() => {
  const m = modules.find(i => i.path === currentPath.value)
  return m ? m.comp : PondView
})

const currentPond = computed(() => ponds.value.find(p => p.id === selectedPondId.value) || null)

const statusType = {
  '在养': 'success',
  '清塘': 'warning',
  '休整': 'info',
  '废弃': 'danger'
}

async function loadPonds() {
  try {
    ponds.value = await api.listPonds()
    const still = ponds.value.some(p => p.id === selectedPondId.value)
    if (!still) {
      selectedPondId.value = ponds.value.length ? ponds.value[0].id : null
    }
  } catch (e) { /* 列表失败不影响主流程 */ }
}

function selectPond(p) {
  selectedPondId.value = p.id
  if (currentPath.value !== '/pond') {
    router.push('/pond')
  }
}

function go(path) {
  if (path !== currentPath.value) {
    router.push(path)
  }
}

router.afterEach((to) => { currentPath.value = to.path })

onMounted(async () => {
  currentPath.value = router.currentRoute.value.path
  await loadPonds()
})
</script>

<template>
  <div class="app-shell">
    <aside class="pond-col">
      <div class="brand">
        <span class="brand-mark">渔</span>
        <div class="brand-text">
          <div class="brand-title">水产养殖场</div>
          <div class="brand-sub">湖蓝主题 · 左侧塘口卡</div>
        </div>
      </div>
      <div class="col-title">塘口列表<span class="col-count">{{ ponds.length }}</span></div>
      <div class="pond-cards">
        <div
          v-for="p in ponds"
          :key="p.id"
          class="pond-card"
          :class="{ active: p.id === selectedPondId, dead: p.status === '废弃' }"
          @click="selectPond(p)"
        >
          <div class="pc-head">
            <span class="pc-no">{{ p.pondNo }}</span>
            <el-tag size="small" effect="plain">{{ p.fishType }}</el-tag>
          </div>
          <div class="pc-meta">{{ p.area }} 亩 · 水深 {{ p.depth }} 米</div>
          <div class="pc-foot">
            <el-tag size="small" :type="statusType[p.status]">{{ p.status }}</el-tag>
            <span class="pc-stock">存栏 {{ p.stockQty ?? 0 }} 尾</span>
          </div>
        </div>
        <div v-if="!ponds.length" class="empty">暂无塘口</div>
      </div>
      <div class="hint">点击塘口卡查看该塘详情</div>
    </aside>

    <main class="content">
      <div class="mod-tabs">
        <button
          v-for="m in modules"
          :key="m.path"
          class="mod-tab"
          :class="{ active: m.path === currentPath }"
          @click="go(m.path)"
        >{{ m.label }}</button>
        <span class="cur-pond">当前塘口：{{ currentPond ? currentPond.pondNo + '（' + currentPond.fishType + '）' : '未选择' }}</span>
      </div>
      <component :is="currentView" :pond-id="selectedPondId" @changed="loadPonds" />
    </main>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; display: flex; }

.pond-col {
  width: 240px; flex-shrink: 0;
  background: #fff; border-right: 1px solid var(--line);
  display: flex; flex-direction: column;
}
.brand {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 18px; border-bottom: 1px solid var(--line);
}
.brand-mark {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: var(--primary); color: #fff; font-weight: 700; font-size: 18px;
}
.brand-title { font-size: 15px; font-weight: 600; }
.brand-sub { font-size: 12px; color: var(--muted); }

.col-title {
  padding: 12px 18px 8px; font-size: 13px; font-weight: 600; color: var(--muted);
  display: flex; justify-content: space-between; align-items: center;
}
.col-count {
  background: var(--primary-wash); color: var(--primary-deep);
  border-radius: 10px; padding: 1px 8px; font-size: 12px;
}

.pond-cards { flex: 1; overflow: auto; padding: 0 12px 12px; }
.pond-card {
  border: 1px solid var(--line); border-radius: 10px;
  padding: 10px 12px; margin-bottom: 8px; cursor: pointer;
  background: #fff; transition: all .18s;
}
.pond-card:hover { border-color: var(--primary-soft); }
.pond-card.active {
  border-color: var(--primary); background: var(--primary-wash);
  box-shadow: 0 2px 8px rgba(3, 155, 229, .18);
}
.pond-card.dead { opacity: .62; }
.pc-head { display: flex; justify-content: space-between; align-items: center; }
.pc-no { font-weight: 700; font-size: 15px; color: var(--primary-deep); }
.pc-meta { font-size: 12px; color: var(--muted); padding: 4px 0; }
.pc-foot { display: flex; justify-content: space-between; align-items: center; }
.pc-stock { font-size: 12px; color: var(--ink); }
.empty { padding: 12px; font-size: 12px; color: var(--muted); }

.hint {
  padding: 12px 18px; font-size: 12px; color: var(--muted);
  border-top: 1px solid var(--line);
}

.content { flex: 1; padding: 16px 24px 28px; overflow: auto; min-width: 0; }
.mod-tabs {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 14px; flex-wrap: wrap;
}
.mod-tab {
  border: 1px solid var(--line); background: #fff; color: var(--ink);
  border-radius: 16px; padding: 5px 14px; font-size: 13px; cursor: pointer;
}
.mod-tab:hover { border-color: var(--primary-soft); color: var(--primary-deep); }
.mod-tab.active {
  background: var(--primary); border-color: var(--primary); color: #fff;
}
.cur-pond { margin-left: auto; font-size: 12px; color: var(--muted); }
</style>
