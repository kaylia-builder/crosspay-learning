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

// Route guard: check auth & role using to.matched for robust parent-child meta resolution
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  // Collect required auth & role from ALL matched route records
  const requiresAuth = to.matched.some(r => r.meta.requiresAuth)
  const requiredRole = to.matched.find(r => r.meta.role)?.meta?.role as string | undefined

  if (requiresAuth) {
    if (!token) {
      // Save intended path so we can redirect back after login
      next(role === 'ADMIN' || role === 'OPERATOR' ? '/admin/login' : '/login')
      return
    }
    if (requiredRole) {
      // Admin routes allow both ADMIN and OPERATOR
      const allowedRoles = requiredRole === 'ADMIN' ? ['ADMIN', 'OPERATOR'] : [requiredRole]
      if (!allowedRoles.includes(role || '')) {
        // Role mismatch: redirect to the appropriate home for the current role
        if (role === 'MERCHANT') next('/merchant/dashboard')
        else if (role === 'ADMIN' || role === 'OPERATOR') next('/admin/merchants')
        else next('/login')
        return
      }
    }
  }

  // Already logged in → redirect away from login pages to appropriate home
  if ((to.path === '/login' || to.path === '/admin/login') && token) {
    if (role === 'ADMIN' || role === 'OPERATOR') next('/admin/merchants')
    else next('/merchant/dashboard')
    return
  }

  next()
})

export default router
