<template>
  <div class="login-bg">
    <div class="login-wrap">
      <div class="brand-head">
        <img class="brand-logo" src="../assets/logo-icon.svg" alt="安溯·知源 VeriTrace" />
        <div class="brand-title">安溯·知源</div>
        <div class="brand-sub">VeriTrace · AI 可信溯源决策平台</div>
      </div>

      <div class="login-card">
        <div class="role-tabs">
          <div v-for="(r, key) in roleMap" :key="key" class="role-tab"
               :class="{ active: form.role === key }" @click="switchRole(key)">
            <span class="tab-icon">{{ r.icon }}</span>{{ r.name }}
          </div>
        </div>

        <div class="form-area">
          <div class="role-tip">
            当前登录：<strong>{{ roleMap[form.role].name }}</strong> 通道 · 仅本角色账号可登录
          </div>
          <el-form @submit.prevent>
            <el-form-item>
              <el-input v-model="form.username" size="large" placeholder="请输入账号" autocomplete="username">
                <template #prefix>👤</template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.password" size="large" type="password" show-password
                        placeholder="请输入密码" autocomplete="current-password" @keyup.enter="onLogin">
                <template #prefix>🔒</template>
              </el-input>
            </el-form-item>
            <el-button class="btn-submit" size="large" :loading="loading" @click="onLogin">登 录</el-button>
          </el-form>
        </div>
      </div>

      <div class="card-foot">
        <div class="slogan">从田间到餐桌 · 每一步都可信</div>
        <div class="test-tip">测试账号：farmer01 / dealer01 / admin　密码均为 123456</div>
        <div class="consumer-entry">
          <router-link to="/assistant" class="entry-link">🛡️ 消费助手</router-link>
          <span class="mx">·</span>
          <router-link to="/complaint/new" class="entry-link">📢 我要投诉</router-link>
          <span class="mx">·</span>
          <router-link to="/complaint/query" class="entry-link">🔍 投诉查询</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, roleMap, type Role } from '../store/auth'

const route = useRoute()
const router = useRouter()
const form = reactive<{ username: string; password: string; role: Role }>({ username: '', password: '', role: 'farmer' })
const loading = ref<boolean>(false)

const switchRole = (key: string): void => {
  // 仅切换角色通道，不清空已输入的账号密码（避免用户输完再点角色时被清空）
  form.role = key as Role
}

async function onLogin(): Promise<void> {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const user = await login(form.username.trim(), form.password, form.role)
    ElMessage.success(`欢迎回来，${user.realName || user.username}`)
    const redirect = route.query.redirect
    if (redirect && typeof redirect === 'string' && redirect.startsWith('/')) {
      router.push(redirect)
    } else {
      router.push(roleMap[user.role].home)
    }
  } catch { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: linear-gradient(rgba(15, 45, 20, .35), rgba(15, 45, 20, .35)),
              url('../assets/login-bg.jpg') center / cover no-repeat,
              linear-gradient(135deg, #1b5e20 0%, #2e7d32 45%, #43a047 100%);
  background-size: cover;
  background-position: center;
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.login-wrap { width: 100%; max-width: 440px; }
.brand-head { text-align: center; margin-bottom: 22px; }
.brand-logo { width: 64px; height: 64px; filter: drop-shadow(0 4px 10px rgba(0, 0, 0, .25)); }
.brand-title {
  color: #fff; font-size: 30px; font-weight: 700; letter-spacing: 6px;
  margin: 10px 0 4px; text-shadow: 0 2px 8px rgba(0, 0, 0, .3);
}
.brand-sub { color: rgba(255, 255, 255, .82); font-size: 13px; letter-spacing: 3px; }
.login-card {
  border-radius: 20px; overflow: hidden;
  background: rgba(255, 255, 255, .78);
  backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, .6);
  box-shadow: 0 16px 50px rgba(27, 60, 30, .35);
}
.role-tabs { display: flex; background: rgba(255, 255, 255, .35); }
.role-tab {
  flex: 1; padding: 14px 0; text-align: center; cursor: pointer;
  color: #4a5d4e; font-size: 14.5px; font-weight: 600; letter-spacing: 1px;
  transition: all .25s ease; user-select: none; position: relative;
}
.role-tab .tab-icon { font-size: 17px; margin-right: 4px; }
.role-tab:hover { background: rgba(255, 255, 255, .3); color: #2e7d32; }
.role-tab.active { color: #fff; background: linear-gradient(135deg, #43a047, #2e7d32); }
.role-tab.active::after {
  content: ''; position: absolute; bottom: 0; left: 0; right: 0;
  height: 3px; background: #ffd54f; border-radius: 3px 3px 0 0;
}
.form-area { padding: 28px 32px 26px; }
.role-tip {
  text-align: center; font-size: 12.5px; color: #78909c; margin-bottom: 18px;
  padding: 6px 10px; background: rgba(255, 255, 255, .5); border-radius: 8px;
}
.role-tip strong { color: #2e7d32; }
.btn-submit {
  width: 100%; margin-top: 6px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #43a047, #1b5e20); color: #fff;
  font-size: 16px; letter-spacing: 8px; text-indent: 8px; font-weight: 600;
  box-shadow: 0 6px 16px rgba(46, 125, 50, .35);
}
.card-foot { text-align: center; color: rgba(255, 255, 255, .78); font-size: 12px; margin-top: 18px; line-height: 1.9; }
.slogan { font-size: 13px; letter-spacing: 2px; color: #fff; font-weight: 500; }
.test-tip {
  display: inline-block; background: rgba(255, 255, 255, .18);
  border: 1px solid rgba(255, 255, 255, .3); border-radius: 8px;
  padding: 6px 12px; font-size: 11.5px; margin-top: 4px;
}
.consumer-entry { margin-top: 8px; }
.entry-link { color: #ffe082; text-decoration: none; font-size: 13px; font-weight: 500; }
.entry-link:hover { color: #fff; text-decoration: underline; }
.mx { margin: 0 8px; color: rgba(255, 255, 255, .5); }
</style>
