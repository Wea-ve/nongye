<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">用户管理</div>
      <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>新增账号</el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="role" @change="load">
          <el-radio-button value="">全部（{{ d.allCount }}）</el-radio-button>
          <el-radio-button value="farmer">农户（{{ d.farmerCount }}）</el-radio-button>
          <el-radio-button value="dealer">经销商（{{ d.dealerCount }}）</el-radio-button>
        </el-radio-group>
        <el-input v-model="keyword" placeholder="搜索账号 / 姓名 / 手机号" clearable style="width: 240px"
                  @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">查询</el-button>
      </div>

      <el-table :data="d.list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'farmer' ? 'success' : 'warning'" size="small">
              {{ row.role === 'farmer' ? '农户' : '经销商' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">
              {{ row.status === 0 ? '禁用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addVisible" title="新增账号（仅农户/经销商）" width="460px">
      <el-form label-width="80px">
        <el-form-item label="角色" required>
          <el-radio-group v-model="addForm.role">
            <el-radio value="farmer">🧑‍🌾 农户</el-radio>
            <el-radio value="dealer">🚚 经销商</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号" required><el-input v-model="addForm.username" placeholder="至少 3 位" /></el-form-item>
        <el-form-item label="初始密码" required><el-input v-model="addForm.password" placeholder="至少 6 位" show-password /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="addForm.realName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="addForm.phone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="addForm.address" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { fmtTime } from '../../utils'
import type { AdminUsersData } from '../../types'

const d = ref<AdminUsersData>({ list: [], allCount: 0, farmerCount: 0, dealerCount: 0 })
const keyword = ref<string>('')
const role = ref<string>('')
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const addVisible = ref<boolean>(false)
const addForm = reactive({ role: 'farmer', username: '', password: '', realName: '', phone: '', address: '' })

async function load(): Promise<void> {
  loading.value = true
  try {
    d.value = await http.get<AdminUsersData>('/api/admin/users', {
      params: { keyword: keyword.value || undefined, role: role.value || undefined }
    })
  } finally { loading.value = false }
}

const openAdd = (): void => {
  Object.assign(addForm, { role: 'farmer', username: '', password: '', realName: '', phone: '', address: '' })
  addVisible.value = true
}

async function save(): Promise<void> {
  saving.value = true
  try {
    await http.post('/api/admin/users', addForm)
    ElMessage.success('账号创建成功')
    addVisible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(load)
</script>
