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
const form = ref({ pondId: null, fryType: '草鱼', qty: 1000, spec: 8, releaseDate: '', supplier: '', survivalRate: 90 })

const fishTypes = ['草鱼', '鲢鱼', '鲫鱼', '南美白虾', '河蟹']

async function load() {
  const [p, s] = await Promise.all([api.listPonds(), api.listStockings(props.pondId)])
  ponds.value = p
  list.value = s
}

const activePonds = computed(() => ponds.value.filter(p => p.status === '在养'))
const pondNo = (id) => {
  const p = ponds.value.find(x => x.id === id)
  return p ? p.pondNo : id
}

function openCreate() {
  editing.value = null
  form.value = {
    pondId: props.pondId || (activePonds.value[0] ? activePonds.value[0].id : null),
    fryType: '草鱼', qty: 1000, spec: 8,
    releaseDate: new Date().toISOString().slice(0, 10),
    supplier: '', survivalRate: 90
  }
  dialog.value = true
}
function openEdit(r) {
  editing.value = r.id
  form.value = {
    pondId: r.pondId, fryType: r.fryType, qty: r.qty, spec: r.spec,
    releaseDate: r.releaseDate, supplier: r.supplier, survivalRate: r.survivalRate
  }
  dialog.value = true
}
async function save() {
  const payload = {
    ...form.value,
    qty: Number(form.value.qty),
    spec: Number(form.value.spec),
    survivalRate: Number(form.value.survivalRate)
  }
  try {
    if (editing.value) {
      await api.updateStocking(editing.value, payload)
      ElMessage.success('已更新投放记录')
    } else {
      await api.createStocking(payload)
      ElMessage.success('已投放，塘口存栏已累加')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

onMounted(load)
watch(() => props.pondId, load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>鱼苗投放</h2>
      <el-button type="primary" @click="openCreate">登记投放</el-button>
    </div>

    <el-table :data="list" border size="small" empty-text="暂无投放记录">
      <el-table-column prop="pondId" label="塘口" width="90">
        <template #default="{ row }">{{ pondNo(row.pondId) }}</template>
      </el-table-column>
      <el-table-column prop="fryType" label="鱼苗品种" width="110" />
      <el-table-column prop="qty" label="投放尾数" width="100" />
      <el-table-column prop="spec" label="规格(尾/斤)" width="110" />
      <el-table-column prop="releaseDate" label="投放日期" width="120" />
      <el-table-column prop="supplier" label="供应商" min-width="160" />
      <el-table-column prop="survivalRate" label="成活率(%)" width="110">
        <template #default="{ row }">
          <span :class="{ warn: Number(row.survivalRate) < 0 || Number(row.survivalRate) > 100 }">
            {{ row.survivalRate }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑投放记录' : '登记鱼苗投放'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="投放塘口">
          <el-select v-model="form.pondId" placeholder="只能选择「在养」塘口">
            <el-option v-for="p in activePonds" :key="p.id" :label="p.pondNo + '（' + p.fishType + '）'" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="鱼苗品种">
          <el-select v-model="form.fryType">
            <el-option v-for="t in fishTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="投放尾数"><el-input-number v-model="form.qty" :min="1" :step="100" /></el-form-item>
        <el-form-item label="规格(尾/斤)"><el-input-number v-model="form.spec" :min="0.1" :step="1" /></el-form-item>
        <el-form-item label="投放日期">
          <el-input v-model="form.releaseDate" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="供应商"><el-input v-model="form.supplier" placeholder="如 江城鱼苗场" /></el-form-item>
        <el-form-item label="成活率(%)">
          <el-input-number v-model="form.survivalRate" :min="0" :max="100" :step="0.5" />
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
.warn { color: #E53935; font-weight: 600; }
</style>
