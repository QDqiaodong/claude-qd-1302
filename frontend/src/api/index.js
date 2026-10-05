import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error?.response?.data?.message || error?.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

async function call(promise) {
  const response = await promise
  const body = response.data
  if (!body || body.ok === false) {
    throw new Error((body && body.message) || '请求失败')
  }
  return body.data
}

const params = (o) => ({ params: o })

export default {
  // ---------------- 塘口 pond ----------------
  listPonds: (status) => call(http.get('/ponds', status ? params({ status }) : {})),
  getPond: (id) => call(http.get(`/ponds/${id}`)),
  createPond: (d) => call(http.post('/ponds', d)),
  updatePond: (id, d) => call(http.put(`/ponds/${id}`, d)),
  transferPond: (id, status) => call(http.post(`/ponds/${id}/status`, null, params({ status }))),

  // ---------------- 鱼苗投放 stocking ----------------
  listStockings: (pondId) => call(http.get('/stockings', pondId ? params({ pondId }) : {})),
  createStocking: (d) => call(http.post('/stockings', d)),
  updateStocking: (id, d) => call(http.put(`/stockings/${id}`, d)),

  // ---------------- 水质检测 water ----------------
  listWaters: (pondId) => call(http.get('/waters', pondId ? params({ pondId }) : {})),
  latestWater: (pondId) => call(http.get('/waters/latest', params({ pondId }))),
  createWater: (d) => call(http.post('/waters', d)),
  updateWater: (id, d) => call(http.put(`/waters/${id}`, d)),
  recalcWater: (id) => call(http.post(`/waters/${id}/recalc`)),

  // ---------------- 捕捞出塘 harvest ----------------
  listHarvests: (pondId) => call(http.get('/harvests', pondId ? params({ pondId }) : {})),
  createHarvest: (d) => call(http.post('/harvests', d)),
  updateHarvest: (id, d) => call(http.put(`/harvests/${id}`, d))
}
