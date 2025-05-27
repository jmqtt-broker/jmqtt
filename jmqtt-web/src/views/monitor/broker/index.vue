<template>
   <div class="app-container">
      <el-card class="box-card" v-for="item in brokers" :key="item.brokerId">
         <div slot="header" class="clearfix">
            <span>{{item.brokerId}}</span>
            <el-button style="float: right; padding: 3px 0" type="text">操作</el-button>
         </div>
         <div class="text item">IP: {{item.ip}}</div>
         <div class="text item">状态: {{item.status}}</div>
         <div class="text item">TcpPort: {{item.tcpPort}}</div>
         <div class="text item">SslTcpPort: {{item.tcpPortSsl}}</div>
         <div class="text item">WsPort: {{item.wsPort}}</div>
         <div class="text item">SslWsPort: {{item.wsPortSsl}}</div>
         <div class="text item">上线时间: {{parseTime(item.onlineAt)}}</div>
         <div class="text item">离线时间: {{parseTime(item.offlineAt)}}</div>
      </el-card>
   </div>
</template>

<script setup name="Job">
import { page, brokerInfo } from "@/api/monitor/broker";
const router = useRouter();
const { proxy } = getCurrentInstance();

const brokers = ref([]);

/** 查询定时任务列表 */
function getList() {
   page({page:1, pageSize: 1000}).then(res => {
      brokers.value = res.data.list;
  });
}

getList();
</script>
