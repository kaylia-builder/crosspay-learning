import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
    },
    {
      path: '/admin/login',
      name: 'AdminLogin',
      component: () => import('@/views/admin/AdminLogin.vue'),
    },
    // Merchant routes
    {
      path: '/merchant',
      component: () => import('@/components/MerchantLayout.vue'),
      meta: { requiresAuth: true, role: 'MERCHANT' },
      children: [
        { path: '', redirect: '/merchant/dashboard' },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/merchant/Dashboard.vue') },
        { path: 'transactions', name: 'Transactions', component: () => import('@/views/merchant/Transactions.vue') },
        { path: 'create-payment', name: 'CreatePayment', component: () => import('@/views/payment/CreatePayment.vue') },
        { path: 'settlements', name: 'Settlements', component: () => import('@/views/merchant/Settlements.vue') },
      ],
    },
    // Admin routes
    {
      path: '/admin',
      component: () => import('@/components/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        { path: '', redirect: '/admin/merchants' },
        { path: 'merchants', name: 'AdminMerchants', component: () => import('@/views/admin/Merchants.vue') },
        { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/Orders.vue') },
        { path: 'settlements', name: 'AdminSettlements', component: () => import('@/views/admin/Settlements.vue') },
        { path: 'ai-assistant', name: 'AiAssistant', component: () => import('@/views/admin/AiAssistant.vue') },
      ],
    },
    { path: '/', redirect: '/login' },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

// Route guard: check auth & role
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login')
      return
    }
    if (to.meta.role) {
      // Admin routes allow both ADMIN and OPERATOR
      const allowedRoles = to.meta.role === 'ADMIN' ? ['ADMIN', 'OPERATOR'] : [to.meta.role as string]
      if (!allowedRoles.includes(role || '')) {
        if (role === 'MERCHANT') next('/merchant/dashboard')
        else if (role === 'ADMIN' || role === 'OPERATOR') next('/admin/merchants')
        else next('/login')
        return
      }
    }
  }

  // Already logged in → don't go to login
  if (to.path === '/login' && token) {
    if (role === 'ADMIN' || role === 'OPERATOR') next('/admin/merchants')
    else next('/merchant/dashboard')
    return
  }

  next()
})

export default router
