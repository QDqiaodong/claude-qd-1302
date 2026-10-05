import { createRouter, createWebHashHistory } from 'vue-router'

// 注意：main.js 里没有 app.use(router)，路由实例由 App.vue 直接驱动，
// 所以这里全部用静态 import —— <component :is> 拿到的是组件对象本身。
import PondView from '../views/PondView.vue'
import StockingView from '../views/StockingView.vue'
import WaterView from '../views/WaterView.vue'
import HarvestView from '../views/HarvestView.vue'

const routes = [
  { path: '/', redirect: '/pond' },
  { path: '/pond', name: 'pond', component: PondView },
  { path: '/stocking', name: 'stocking', component: StockingView },
  { path: '/water', name: 'water', component: WaterView },
  { path: '/harvest', name: 'harvest', component: HarvestView },
  { path: '/:pathMatch(.*)*', redirect: '/pond' }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
