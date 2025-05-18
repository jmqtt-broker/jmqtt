<template>
    <div class="app-container">
        <el-tabs v-model="tabSelect" :tab-position="'left'" style="height: 100%;" @tab-click="tabChange">
            <el-tab-pane label="基本信息" name="1">
                <el-descriptions :title="clientInfo.clientId" style="padding-left: 70px">
                    <el-descriptions-item label="BrokerID：">{{clientInfo.brokerId}}</el-descriptions-item>
                    <el-descriptions-item label="状态：">
                        <el-tag v-if="clientInfo.state === 'ONLINE'" type="success">在线</el-tag>
                        <el-tag v-if="clientInfo.state === 'OFFLINE'" type="danger">离线</el-tag>
                        <el-tag v-if="clientInfo.state === 'NULL'" type="info">过期</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="CleanStart：">
                        <el-tag v-if="clientInfo.cleanStart">true</el-tag>
                        <el-tag v-if="!clientInfo.cleanStart" type="danger">false</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="心跳周期：">{{clientInfo.keepalive}} 秒</el-descriptions-item>
                    <el-descriptions-item label="上一次上线：">{{parseTime(clientInfo.onlineTime)}}</el-descriptions-item>
                    <el-descriptions-item label="上一次离线：">{{parseTime(clientInfo.offlineTime)}}</el-descriptions-item>
                    <el-descriptions-item label="客户端版本：">{{clientInfo.version}}</el-descriptions-item>
                </el-descriptions>
            </el-tab-pane>
            <el-tab-pane v-if="isMqtt5" label="连接属性" name="3">
                <el-descriptions :title="'基本属性'" style="padding-left: 70px">
                    <el-descriptions-item label="Session有效期：">{{(property[17] + ' 秒') || ''}}</el-descriptions-item>
                    <el-descriptions-item label="最大接收值：">{{property[33] || ''}}</el-descriptions-item>
                    <el-descriptions-item label="最大数据包大小：">{{property[39] || ''}}</el-descriptions-item>
                    <el-descriptions-item label="别名最大值：">{{property[34] || ''}}</el-descriptions-item>
                    <el-descriptions-item label="请求响应信息：">{{property[25] || ''}}</el-descriptions-item>
                    <el-descriptions-item label="请求错误信息：">{{property[23] || ''}}</el-descriptions-item>
                </el-descriptions>
                <!--<el-descriptions :title="'用户属性'" style="padding-left: 70px">
                    <el-descriptions-item v-for="item in userProperty" :label="item.key + '：'">{{item.value}}</el-descriptions-item>
                </el-descriptions>-->
            </el-tab-pane>
            <el-tab-pane label="遗嘱消息" name="5">
                <el-empty v-if="Object.keys(will).length === 0" description="暂无消息"></el-empty>
                <el-descriptions v-if="Object.keys(will).length > 0" :title="'用户属性'" style="padding-left: 70px">
                    <el-descriptions-item v-for="item in userProperty" :label="item.key + '：'">{{item.value}}</el-descriptions-item>
                </el-descriptions>
                <el-divider/>
                <el-form ref="willForm" v-model="will" :rules="rules"
                         label-width="100px" disabled
                         v-if="Object.keys(will).length > 0">
                    <el-form-item label-width="130px" label="消息主题" prop="topic">
                        <el-input v-model="will.headers.topic" placeholder="请输入消息主题" :maxlength="100" show-word-limit
                                  clearable :style="{width: '100%'}"></el-input>
                    </el-form-item>
                    <el-form-item label-width="130px" label="QoS" prop="qos">
                        <el-radio-group v-model="will.headers.qos">
                            <el-radio v-for="(item, index) in qosOptions" :key="index" :value="item.value"
                                      :disabled="item.disabled">{{item.label}}</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label-width="130px" label="保留标志" prop="retain">
                        <el-switch v-model="will.headers.retain"></el-switch>
                    </el-form-item>
                    <el-form-item label-width="130px" label="消息内容" prop="payloadStr">
                        <el-input v-model="will.payloadStr" type="textarea" placeholder="请输入消息内容"
                                  :autosize="{minRows: 4, maxRows: 4}" :style="{width: '100%'}"></el-input>
                    </el-form-item>
                    <el-form-item label-width="130px" label="有效载荷指示器" prop="payloadFormatIndicator">
                        <el-switch v-model="will.properties[1]"></el-switch>
                    </el-form-item>
                    <el-form-item label-width="130px" label="发送延迟时间" prop="willDelayInterval">
                        <el-input v-model="will.properties[24]" placeholder="请输入发送延迟时间" clearable
                                  :style="{width: '100%'}"><template slot="append">秒</template></el-input>
                    </el-form-item>
                    <el-form-item label-width="130px" label="消息过期时间" prop="publicationExpiryInterval">
                        <el-input v-model="will.properties[2]" placeholder="请输入消息过期时间" clearable
                                  :style="{width: '100%'}"><template slot="append">秒</template></el-input>
                    </el-form-item>
                    <el-form-item label-width="130px" label="内容类型" prop="contentType">
                        <el-input v-model="will.properties[3]" placeholder="请输入内容类型" clearable :style="{width: '100%'}">
                        </el-input>
                    </el-form-item>
                    <el-form-item label-width="130px" label="响应主题" prop="responseTopic">
                        <el-input v-model="will.properties[8]" placeholder="请输入响应主题" clearable
                                  :style="{width: '100%'}"></el-input>
                    </el-form-item>
                    <el-form-item label-width="130px" label="对比数据" prop="correlationData">
                        <el-input v-model="will.properties[9]" placeholder="请输入对比数据" clearable
                                  :style="{width: '100%'}"></el-input>
                    </el-form-item>
                    <!--<el-form-item size="large">
                            <el-button type="primary" @click="submitForm">提交</el-button>
                            <el-button @click="resetForm">重置</el-button>
                        </el-form-item>-->
                </el-form>
            </el-tab-pane>
            <el-tab-pane label="订阅管理" name="0">
                <el-collapse accordion>
                    <el-empty v-if="subscriptions.length === 0" description="暂无订阅"></el-empty>
                    <el-table v-if="subscriptions.length > 0" :data="subscriptions" ref="subTable"
                              style="width: 100%" @row-click="rowClick">
                        <el-table-column type="expand">
                            <template #default="props">
                                <el-descriptions style="padding-left: 70px">
                                    <el-descriptions-item>{{ JSON.parse(props.row.opt).subscriptionIdentifier || '' }}
                                        <template #label>订阅标识符&nbsp;
                                            <el-tooltip class="item" effect="dark" placement="right-end">
                                                <template #content>
                                                    当服务端向该订阅转发消息时，它会在消息中附上对应的标识符。客户端可以使用消息中的订阅标识符，<br>
                                                    决定触发哪一个回调，进行后续操作。
                                                </template>
                                                <svg-icon icon-class="question" />
                                            </el-tooltip>
                                            ：
                                        </template>
                                    </el-descriptions-item>
                                    <el-descriptions-item>{{ JSON.parse(props.row.opt).noLocal || false }}
                                        <template #label>禁止本地转发&nbsp;
                                            <el-tooltip class="item" effect="dark" placement="right-end">
                                                <template #content>
                                                    0 和 1 两个取值，为 1 表示服务端不能将消息转发给发布这个消息的客户端。即如果客户端发布了一个topic，<br>
                                                    并且同时订阅了这个topic，那么该订阅不会收到自己发布的消息。
                                                </template>
                                                <svg-icon icon-class="question" />
                                            </el-tooltip>
                                            ：
                                        </template>
                                    </el-descriptions-item>
                                    <el-descriptions-item>{{ JSON.parse(props.row.opt).retainAsPublished || false }}
                                        <template #label>发布状态保留&nbsp;
                                            <el-tooltip class="item" effect="dark" placement="right-end">
                                                <template #content>
                                                    0 和 1 两个取值，为 1 表示服务端在向此订阅转发应用消息时需要保持消息中的 Retain 标识不变，为 0 则表示必须清除。
                                                </template>
                                                <svg-icon icon-class="question" />
                                            </el-tooltip>
                                            ：
                                        </template>
                                    </el-descriptions-item>
                                    <el-descriptions-item>{{ JSON.parse(props.row.opt).retainHandling || '' }}
                                        <template #label>保留消息处理&nbsp;
                                            <el-tooltip class="item" effect="dark" placement="right-end">
                                                <template #content>
                                                    0 表示只要订阅建立，就发送保留消息；<br>
                                                    1 表示只有建立全新的订阅而不是重复订阅时，才发送保留消息；<br>
                                                    2 表示订阅建立时不要发送保留消息。
                                                </template>
                                                <svg-icon icon-class="question" />
                                            </el-tooltip>
                                            ：
                                        </template>
                                    </el-descriptions-item>
                                </el-descriptions>
                            </template>
                        </el-table-column>
                        <el-table-column
                                label="订阅主题"
                                prop="topic">
                        </el-table-column>
                        <el-table-column
                                label="QoS"
                                prop="qos">
                        </el-table-column>
                    </el-table>
                </el-collapse>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script setup name="Online-Detail">
    import {sessionInfo, getSubscriptions, getWill} from "@/api/monitor/session";

    const {proxy} = getCurrentInstance();
    const route = useRoute();
    let tabSelect = ref('1')
    let clientId = ref('');
    let clientInfo = ref({});
    let property = ref({});
    let userProperty = ref([]);
    let subscriptions = ref([]);
    let will = ref({});
    let isMqtt5 = ref(false)
    let rules = {
        topic: [],
            qos: [],
            payloadStr: [],
            willDelayInterval: [],
            publicationExpiryInterval: [],
            contentType: [],
            responseTopic: [],
            correlationData: [],
    }
    let qosOptions = [{
        "label": "0",
        "value": 0
    }, {
        "label": "1",
        "value": 1
    }, {
        "label": "2",
        "value": 2
    }]

    const sideTheme = computed(() => settingsStore.sideTheme);
    watch(clientInfo , proxy => isMqtt5.value = proxy.version === 5)

    function tabChange(proxy) {
        if (proxy.props.name === '0' && clientId) {
            getSubscriptions(clientId.value).then(res => {
                subscriptions.value = res.data
            })
        } else if (proxy.props.name === '5' && clientId) {
            getWill(clientId.value).then(res => {
                if (res.data) {
                    will.value = res.data
                } else {
                    will.value = {}
                }
            })
        }
    }

    function rowClick(row, column, event) {
        proxy.$refs.subTable.toggleRowExpansion(row);
    }

    (() => {
        clientId.value = route.params && route.params.clientId;
        if (clientId.value) {
            sessionInfo(clientId.value).then(res => {
                clientInfo.value = res.data
                console.log(res.data)
                // 不能使用JSON.parse()，因为key中有int类型，JSON要求所有key都是字符串
                property.value = eval("(" + res.data.property + ")")
                userProperty.value = property.value[38]
            })
        }
    })()

</script>
