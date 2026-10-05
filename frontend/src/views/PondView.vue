<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({ pondId: { type: Number, default: null } })
const emit = defineEmits(['changed'])

const ponds = ref([])
const waters = ref([])
const harvests = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({ pondNo: '', area: 10, depth: 1.8, fishType: '草鱼', status: '在养' })

const fishTypes = ['草鱼', '鲢鱼', '鲫鱼', '南美白虾', '河蟹']
const statuses = ['在养', '清塘', '休整', '废弃']
const statusType = { '在养': 'success', '清塘': 'warning', '休整': 'info', '废弃': 'danger' }

async function load() {
  const [p, w, h] = await Promise.all([api.listPonds(), api.listWaters(), api.listHarvests()])
  ponds.value = p
  waters.value = w
  harvests.value = h
}

/** 某塘口最近一次水质检测 */
function latestOf(pondId) {
  const rows = waters.value.filter(w => w.pondId === pondId)
  if (!rows.length) return null
  return rows.slice().sort((a, b) => (a.checkDate < b.checkDate ? 1 : -1))[0]
}

/** 某塘口的捕捞批次流水（按日期倒序） */
function batchesOf(pondId) {
  return harvests.value
    .filter(h => h.pondId === pondId)
    .slice()
    .sort((a, b) => (a.harvestDate < b.harvestDate ? 1 : -1))
}

/** 水质三环：水温 / pH / 溶氧，越界标红 */
function ringsOf(pondId) {
  const w = latestOf(pondId)
  const temp = w && w.temperature !== null && w.temperature !== undefined ? Number(w.temperature) : null
  const ph = w && w.ph !== null && w.ph !== undefined ? Number(w.ph) : null
  const oxy = w && w.oxygen !== null && w.oxygen !== undefined ? Number(w.oxygen) : null
  const ratio = (v, max) => Math.max(0, Math.min(1, v / max))
  return [
    {
      key: 'temp', label: '水温',
      text: temp === null ? '未检' : temp.toFixed(1) + '℃',
      ratio: temp === null ? 0 : ratio(temp, 40),
      color: temp !== null && (temp < 10 || temp > 35) ? '#E53935' : 'var(--primary)'
    },
    {
      key: 'ph', label: 'pH',
      text: ph === null ? '未检' : ph.toFixed(2),
      ratio: ph === null ? 0 : ratio(ph, 14),
      color: ph !== null && (ph < 6.5 || ph > 9.0) ? '#E53935' : 'var(--primary)'
    },
    {
      key: 'oxy', label: '溶氧',
      text: oxy === null ? '未检' : oxy.toFixed(1),
      ratio: oxy === null ? 0 : ratio(oxy, 12),
      color: oxy === null ? 'var(--primary)' : (oxy < 3.0 ? '#E53935' : (oxy < 4.5 ? '#F9A825' : 'var(--primary)'))
    }
  ]
}

function statusColor(status) {
  if (status === '超标') return 'danger'
  if (status === '预警') return 'warning'
  return 'success'
}

const nextStatuses = {
  '在养': ['清塘', '休整', '废弃'],
  '清塘': ['休整', '在养', '废弃'],
  '休整': ['在养', '清塘', '废弃'],
  '废弃': []
}

function openCreate() {
  editing.value = null
  form.value = { pondNo: '', area: 10, depth: 1.8, fishType: '草鱼', status: '在养' }
  dialog.value = true
}
function openEdit(r) {
  editing.value = r.id
  form.value = { pondNo: r.pondNo, area: r.area, depth: r.depth, fishType: r.fishType, status: r.status }
  dialog.value = true
}
async function save() {
  const payload = { ...form.value, area: Number(form.value.area), depth: Number(form.value.depth) }
  try {
    if (editing.value) {
      await api.updatePond(editing.value, payload)
      ElMessage.success('已更新塘口')
    } else {
      await api.createPond(payload)
      ElMessage.success('已新增塘口')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}
async function transfer(r, to) {
  try {
    await api.transferPond(r.id, to)
    ElMessage.success(`${r.pondNo} 已流转至「${to}」`)
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const orderedPonds = computed(() => {
  if (!props.pondId) return ponds.value
  return ponds.value.slice().sort((a, b) => (a.id === props.pondId ? -1 : (b.id === props.pondId ? 1 : 0)))
})

onMounted(load)
watch(() => props.pondId, () => { /* 选中塘口变化时数据已是全量，无需重载 */ })
</script>

<template>
  <div>
    <div class="bar">
      <h2>塘口总览</h2>
      <el-button type="primary" @click="openCreate">新增塘口</el-button>
    </div>

    <div class="grid">
      <el-card
        v-for="p in orderedPonds"
        :key="p.id"
        class="pond-card"
        :class="{ focus: p.id === pondId, dead: p.status === '废弃' }"
        shadow="hover"
      >
        <template #header>
          <span class="pond-no">{{ p.pondNo }}</span>
          <el-tag size="small" effect="plain">{{ p.fishType }}</el-tag>
          <el-tag size="small" :type="statusType[p.status]">{{ p.status }}</el-tag>
        </template>

        <div class="meta">{{ p.area }} 亩 · 水深 {{ p.depth }} 米 · 存栏 {{ p.stockQty ?? 0 }} 尾</div>

        <!-- 水质三环 -->
        <div class="rings">
          <div v-for="r in ringsOf(p.id)" :key="r.key" class="ring-box">
            <svg class="ring" viewBox="0 0 72 72">
              <circle class="ring-bg" cx="36" cy="36" r="28" />
              <circle
                class="ring-fg" cx="36" cy="36" r="28"
                :stroke="r.color"
                :stroke-dasharray="`${(r.ratio * 175.93).toFixed(2)} 175.93`"
              />
              <text class="ring-val" x="36" y="35">{{ r.text }}</text>
              <text class="ring-label" x="36" y="47">{{ r.label }}</text>
            </svg>
          </div>
          <div v-if="!latestOf(p.id)" class="no-water">暂无水质检测</div>
        </div>
        <div class="water-status" v-if="latestOf(p.id)">
          <span>检测 {{ latestOf(p.id).checkDate }}</span>
          <el-tag size="small" :type="statusColor(latestOf(p.id).status)">{{ latestOf(p.id).status }}</el-tag>
        </div>

        <!-- 捕捞批次流水 -->
        <div class="tl">
          <div class="tl-head">捕捞批次流水</div>
          <div v-for="h in batchesOf(p.id).slice(0, 4)" :key="h.id" class="tl-item">
            <span class="tl-dot" />
            <div class="tl-body">
              <div class="tl-title">{{ h.harvestDate }} · {{ h.qty }} 斤</div>
              <div class="tl-sub">{{ h.buyer }} · {{ h.pricePerJin }} 元/斤 · 金额 {{ h.amount }} 元</div>
            </div>
          </div>
          <div v-if="!batchesOf(p.id).length" class="tl-empty">暂无出塘记录</div>
        </div>

        <div class="actions">
          <el-button link type="primary" @click="openEdit(p)">编辑</el-button>
          <el-button
            v-for="to in nextStatuses[p.status]"
            :key="to"
            link
            :type="to === '废弃' ? 'danger' : 'success'"
            @click="transfer(p, to)"
          >{{ to }}</el-button>
          <span v-if="!nextStatuses[p.status].length" class="final">已判废·终态</span>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑塘口' : '新增塘口'">
      <el-form :model="form" label-width="90px">
        <el-form-item label="塘号"><el-input v-model="form.pondNo" placeholder="如 P07" /></el-form-item>
        <el-form-item label="面积（亩）"><el-input-number v-model="form.area" :min="0.1" :step="0.5" /></el-form-item>
        <el-form-item label="水深（米）"><el-input-number v-model="form.depth" :min="0.1" :step="0.1" /></el-form-item>
        <el-form-item label="养殖品种">
          <el-select v-model="form.fishType">
            <el-option v-for="t in fishTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.pond-card { position: relative; }
.pond-card.focus { outline: 2px solid var(--primary); }
.pond-card.dead { opacity: .78; }
.pond-no { font-weight: 700; margin-right: 8px; color: var(--primary-deep); }
.meta { font-size: 12px; color: var(--muted); padding-bottom: 8px; }

.rings { display: flex; gap: 6px; justify-content: space-around; }
.ring-box { text-align: center; }
.ring { width: 74px; height: 74px; }
.ring-bg { fill: none; stroke: var(--line); stroke-width: 7; }
.ring-fg {
  fill: none; stroke-width: 7; stroke-linecap: round;
  transform: rotate(-90deg); transform-origin: 36px 36px;
  transition: stroke-dasharray .4s;
}
.ring-val { font-size: 13px; font-weight: 700; text-anchor: middle; fill: var(--ink); }
.ring-label { font-size: 10px; text-anchor: middle; fill: var(--muted); }
.no-water { font-size: 12px; color: var(--muted); align-self: center; }

.water-status {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--muted); padding: 6px 0;
}

.tl { border-top: 1px dashed var(--line); margin-top: 8px; padding-top: 8px; }
.tl-head { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.tl-item { display: flex; gap: 8px; padding: 3px 0; }
.tl-dot {
  width: 8px; height: 8px; border-radius: 50%; background: var(--primary);
  margin-top: 5px; flex-shrink: 0;
}
.tl-title { font-size: 12px; font-weight: 600; }
.tl-sub { font-size: 11px; color: var(--muted); }
.tl-empty { font-size: 12px; color: var(--muted); }

.actions {
  display: flex; gap: 4px; flex-wrap: wrap;
  border-top: 1px solid var(--line); margin-top: 8px; padding-top: 6px;
}
.final { font-size: 12px; color: var(--muted); align-self: center; }
</style>
