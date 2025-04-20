<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true">
         <el-form-item label="BrokerId" prop="brokerId" style="width: 200px">
            <el-select v-model="queryParams.brokerId" placeholder="请选择" @change="handleQuery" clearable>
               <el-option
                       v-for="item in brokers"
                       :key="item.object1"
                       :label="item.object1"
                       :value="item.object2">
               </el-option>
            </el-select>
         </el-form-item>
         <el-form-item label="客户端ID" prop="clientId" clearable>
            <el-input
               v-model="queryParams.clientId"
               placeholder="请输入客户端ID"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="版本" prop="version" style="width: 130px">
            <el-select v-model="queryParams.version" placeholder="版本" @change="handleQuery" clearable>
               <el-option key="0" label="全部" value=""></el-option>
               <el-option key="3" label="3.0.0" :value="3"></el-option>
               <el-option key="4" label="3.1.1（4）" :value="4"></el-option>
               <el-option key="5" label="5.0.0" :value="5"></el-option>
            </el-select>
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
         <el-table-column label="上线时间" align="center" prop="onlineTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.onlineTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="离线时间" align="center" prop="offlineTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.offlineTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="状态" align="center" prop="state" :show-overflow-tooltip="true" width="80">
            <template #default="scope">
            <span>
               <el-tag v-if="scope.row.state === 'ONLINE'" type="success">在线</el-tag>
               <el-tag v-if="scope.row.state === 'OFFLINE'" type="danger">离线</el-tag>
               <el-tag v-if="scope.row.state === 'NULL'" type="info">过期</el-tag>
            </span>
            </template>
         </el-table-column>
         <el-table-column label="版本" align="center" prop="version" :show-overflow-tooltip="true" width="80"/>
         <el-table-column label="清除会话" align="center" prop="cleanStart" :show-overflow-tooltip="true" width="80"/>
         <el-table-column label="心跳周期" align="center" prop="keepalive" :show-overflow-tooltip="true" width="80"/>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="">详情</el-button>
               <el-button v-if="scope.row.state === 'ONLINE'" link type="danger" icon="Delete" @click="handleForceLogout(scope.row)">强踢</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />
   </div>
</template>

<script setup name="Online">
import { brokerList, forceLogout, list as initData } from "@/api/monitor/online";

const { proxy } = getCurrentInstance();

const brokers = ref([{object1: '全部', object2: ''}]);
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

function getBrokers() {
   brokerList().then(response => {
      brokers.value = brokers.value.concat(response.data)
   });
}

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
    proxy.$modal.confirm('是否确认强退clientId为"' + row.clientId + '"的客户端?').then(function () {
  return forceLogout(row.clientId);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
getBrokers();

</script>
