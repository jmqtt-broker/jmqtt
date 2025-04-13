<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true">
         <el-form-item label="BrokerId" prop="brokerId">
            <el-input
               v-model="queryParams.brokerId"
               placeholder="请选择Broker"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="客户端ID" prop="clientId">
            <el-input
               v-model="queryParams.clientId"
               placeholder="请输入客户端ID"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="客户端ID" prop="clientId">
            <el-input
                    v-model="queryParams.version"
                    placeholder="请输入版本"
                    clearable
                    style="width: 200px"
                    @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>
      <el-table
         v-loading="loading"
         :data="onlineList"
         style="width: 100%;"
      >
         <el-table-column label="序号" width="50" type="index" align="center">
            <template #default="scope">
               <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
            </template>
         </el-table-column>
         <el-table-column label="BrokerID" align="center" prop="brokerId" :show-overflow-tooltip="true" />
         <el-table-column label="客户端ID" align="center" prop="clientId" :show-overflow-tooltip="true" />
         <el-table-column label="上线时间" align="center" prop="onlineTime" :show-overflow-tooltip="true" />
         <el-table-column label="离线时间" align="center" prop="offlineTime" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center" prop="state" :show-overflow-tooltip="true" />
         <el-table-column label="版本" align="center" prop="version" :show-overflow-tooltip="true" />
         <el-table-column label="属性" align="center" prop="property" :show-overflow-tooltip="true" />
         <el-table-column label="登录时间" align="center" prop="loginTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.loginTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Delete" @click="handleForceLogout(scope.row)" v-hasPermi="['monitor:online:forceLogout']">强退</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />
   </div>
</template>

<script setup name="Online">
import { forceLogout, list as initData } from "@/api/monitor/online";

const { proxy } = getCurrentInstance();

const onlineList = ref([]);
const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const queryParams = ref({
   brokerId: undefined,
   clientId: undefined,
   state: undefined,
   version: undefined
});

function getList() {
  loading.value = true;
  initData(queryParams.value).then(response => {
    onlineList.value = response.data.list;
    total.value = response.data.totalCount;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  pageNum.value = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 强退按钮操作 */
function handleForceLogout(row) {
    proxy.$modal.confirm('是否确认强退名称为"' + row.userName + '"的用户?').then(function () {
  return forceLogout(row.tokenId);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
