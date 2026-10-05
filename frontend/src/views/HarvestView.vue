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
const form = ref({ pondId: null, harvestDate: '', qty: 500, avgSpec: 2, buyer: '', pricePerJin: 6 })

async function load() {
  const [p, h] = await Promise.all([api.listPonds(), api.listHarvests(props.pondId)])
  ponds.value = p
  list.value = h
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
    harvestDate: new Date().toISOString().slice(0, 10),
    qty: 500, avgSpec: 2, buyer: '', pricePerJin: 6
  }
  dialog.value = true
}
function openEdit(r) {
  editing.value = r.id
  form.value = {
    pondId: r.pondId, harvestDate: r.harvestDate, qty: r.qty,
    avgSpec: r.avgSpec, buyer: r.buyer, pricePerJin: r.pricePerJin
  }
  dialog.value = true
}
async function save() {
  const payload = {
    ...form.value,
    qty: Number(form.value.qty),
    avgSpec: Number(form.value.avgSpec),
    pricePerJin: Number(form.value.pricePerJin)
  }
  try {
    if (editing.value) {
      await api.updateHarvest(editing.value, payload)
      ElMessage.success('已更新，金额由服务端重算')
    } else {
      await api.createHarvest(payload)
      ElMessage.success('已出塘，金额与存栏已由服务端处理')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const total = computed(() => list.value.reduce((s, r) => s + Number(r.amount || 0), 0).toFixed(2))

onMounted(load)
watch(() => props.pondId, load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>捕捞出塘</h2>
      <div class="bar-right">
        <span class="tip">金额 = 斤数 × 单价（服务端计算）· 合计 {{ total }} 元</span>
        <el-button type="primary" @click="openCreate">登记出塘</el-button>
      </div>
    </div>

    <el-table :data="list" border size="small" empty-text="暂无出塘记录">
      <el-table-column prop="pondId" label="塘口" width="90">
        <template #default="{ row }">{{ pondNo(row.pondId) }}</template>
      </el-table-column>
      <el-table-column prop="harvestDate" label="出塘日期" width="120" />
      <el-table-column prop="qty" label="斤数" width="100" />
      <el-table-column prop="avgSpec" label="均重(斤/尾)" width="120" />
      <el-table-column prop="buyer" label="收购商" min-width="160" />
      <el-table-column prop="pricePerJin" label="单价(元/斤)" width="120" />
      <el-table-column prop="amount" label="金额(元)" width="120">
        <template #default="{ row }"><b>{{ row.amount }}</b></template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑出塘记录' : '登记捕捞出塘'">
      <el-form :model="form" label-width="110px">
        <el-form-item label="出塘塘口">
          <el-select v-model="form.pondId" placeholder="只能捕捞「在养」塘口">
            <el-option
              v-for="p in activePonds"
              :key="p.id"
              :label="p.pondNo + '（存栏 ' + (p.stockQty ?? 0) + ' 尾）'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出塘日期"><el-input v-model="form.harvestDate" placeholder="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="出塘斤数"><el-input-number v-model="form.qty" :min="1" :step="50" /></el-form-item>
        <el-form-item label="均重(斤/尾)"><el-input-number v-model="form.avgSpec" :min="0.01" :step="0.1" /></el-form-item>
        <el-form-item label="收购商"><el-input v-model="form.buyer" placeholder="如 江城水产批发市场" /></el-form-item>
        <el-form-item label="单价(元/斤)"><el-input-number v-model="form.pricePerJin" :min="0" :step="0.5" /></el-form-item>
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
</style>
