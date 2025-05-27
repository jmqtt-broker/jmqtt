<template>
   <div class="app-container">
      BrokerDetail
   </div>
</template>

<script setup name="JobLog">

const { proxy } = getCurrentInstance();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  }
});

(() => {

})();
</script>
