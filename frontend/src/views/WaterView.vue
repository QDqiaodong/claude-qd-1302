<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({ pondId: { type: Number, default: null } })
const emit = defineEmits(['changed'])

const ponds = ref([])
const list = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({ pondId: null, checkDate: '', temperature: 25, ph: 7.5, oxygen: 6 })

async function load() {
  const [p, w] = await Promise.all([api.listPonds(), api.listWaters(props.pondId)])
  ponds.value = p
  list.value = w
}

const usablePonds = computed(() => ponds.value.filter(p => p.status !== '废弃'))
const pondNo = (id) => {
  const p = ponds.value.find(x => x.id === id)
  return p ? p.pondNo : id
}
const statusType = { '正常': 'success', '预警': 'warning', '超标': 'danger' }

function openCreate() {
  editing.value = null
  form.value = {
    pondId: props.pondId || (usablePonds.value[0] ? usablePonds.value[0].id : null),
    checkDate: new Date().toISOString().slice(0, 10),
    temperature: 25, ph: 7.5, oxygen: 6
  }
  dialog.value = true
}
function openEdit(r) {
  editing.value = r.id
  form.value = {
    pondId: r.pondId, checkDate: r.checkDate,
    temperature: r.temperature, ph: r.ph, oxygen: r.oxygen
  }
  dialog.value = true
}
async function save() {
  const payload = {
    ...form.value,
    temperature: Number(form.value.temperature),
    ph: Number(form.value.ph),
    oxygen: Number(form.value.oxygen)
  }
  try {
    if (editing.value) {
      await api.updateWater(editing.value, payload)
      ElMessage.success('已更新，状态按服务端规则重算')
    } else {
      await api.createWater(payload)
      ElMessage.success('已登记检测，状态由服务端判定')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}
async function recalc(row) {
  try {
    await api.recalcWater(row.id)
    ElMessage.success('已按服务端规则重算状态')
    await load()
  } catch (e) { ElMessage.error(e.message) }
}

onMounted(load)
watch(() => props.pondId, load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>水质检测</h2>
      <div class="bar-right">
        <span class="tip">状态由服务端判定：溶氧 &lt; 3.0 或 pH &lt; 6.5 / &gt; 9.0 判超标，溶氧 &lt; 4.5 判预警</span>
        <el-button type="primary" @click="openCreate">登记检测</el-button>
      </div>
    </div>

    <el-table :data="list" border size="small" empty-text="暂无检测记录">
      <el-table-column prop="pondId" label="塘口" width="90">
        <template #default="{ row }">{{ pondNo(row.pondId) }}</template>
      </el-table-column>
      <el-table-column prop="checkDate" label="检测日期" width="120" />
      <el-table-column prop="temperature" label="水温(℃)" width="100" />
      <el-table-column prop="ph" label="pH" width="90">
        <template #default="{ row }">
          <span :class="{ bad: Number(row.ph) < 6.5 || Number(row.ph) > 9.0 }">{{ row.ph }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="oxygen" label="溶氧(mg/L)" width="120">
        <template #default="{ row }">
          <span :class="{ bad: Number(row.oxygen) < 3.0, warn: Number(row.oxygen) >= 3.0 && Number(row.oxygen) < 4.5 }">
            {{ row.oxygen }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType[row.status]">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="recalc(row)">重算状态</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑检测记录' : '登记水质检测'">
      <el-form :model="form" label-width="110px">
        <el-form-item label="检测塘口">
          <el-select v-model="form.pondId" placeholder="废弃塘口不可检测">
            <el-option v-for="p in usablePonds" :key="p.id" :label="p.pondNo + '（' + p.fishType + '）'" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检测日期"><el-input v-model="form.checkDate" placeholder="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="水温(℃)"><el-input-number v-model="form.temperature" :step="0.5" /></el-form-item>
        <el-form-item label="pH"><el-input-number v-model="form.ph" :min="0" :max="14" :step="0.1" /></el-form-item>
        <el-form-item label="溶氧(mg/L)"><el-input-number v-model="form.oxygen" :min="0" :max="20" :step="0.1" /></el-form-item>
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
.bar-right { display: flex; align-items: center; gap: 12px; }
.tip { font-size: 12px; color: var(--muted); }
.bad { color: #E53935; font-weight: 600; }
.warn { color: #F9A825; font-weight: 600; }
</style>
